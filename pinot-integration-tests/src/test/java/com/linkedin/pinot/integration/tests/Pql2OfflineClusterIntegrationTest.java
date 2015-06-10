/**
 * Copyright (C) 2014-2015 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.pinot.integration.tests;

import com.linkedin.pinot.common.ZkTestUtils;
import com.linkedin.pinot.common.utils.FileUploadUtils;
import com.linkedin.pinot.common.utils.TarGzCompressionUtils;
import com.linkedin.pinot.util.TestUtils;
import java.io.File;
import java.io.FileInputStream;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.helix.ExternalViewChangeListener;
import org.apache.helix.HelixManager;
import org.apache.helix.HelixManagerFactory;
import org.apache.helix.InstanceType;
import org.apache.helix.NotificationContext;
import org.apache.helix.model.ExternalView;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Integration test that converts avro data for 12 segments and runs queries against it.
 *
 * @author jfim
 */
public class Pql2OfflineClusterIntegrationTest extends BaseClusterIntegrationTest {
  private final File _tmpDir = new File("/tmp/OfflineClusterIntegrationTest");
  private final File _segmentDir = new File("/tmp/OfflineClusterIntegrationTest/segmentDir");
  private final File _tarDir = new File("/tmp/OfflineClusterIntegrationTest/tarDir");
  private File schemaFile;

  private static final int SEGMENT_COUNT = 12;
  private static final int QUERY_COUNT = 1000;

  protected void startCluster() {
    startZk();
    startController();
    startBroker();
    startServer();
  }

  protected void createResource() throws Exception {
    File schemaFile =
        new File(Pql2OfflineClusterIntegrationTest.class.getClassLoader()
            .getResource("On_Time_On_Time_Performance_2014_100k_subset_nonulls.schema").getFile());

    // Create a table
    setUpTable(schemaFile, 1, 1);
  }

  protected void setUpTable(File schemaFile, int numBroker, int numOffline) throws Exception {
    addSchema(schemaFile, "schemaFile");
    addOfflineTable("myresource", "DaysSinceEpoch", "daysSinceEpoch", 3000, "DAYS", null, null);
  }

  @BeforeClass
  public void setUp() throws Exception {
    //Clean up
    FileUtils.deleteDirectory(_tmpDir);
    FileUtils.deleteDirectory(_segmentDir);
    FileUtils.deleteDirectory(_tarDir);
    _tmpDir.mkdirs();
    _segmentDir.mkdirs();
    _tarDir.mkdirs();

    // Start the cluster
    startCluster();

    // Unpack the Avro files
    TarGzCompressionUtils.unTar(
        new File(TestUtils.getFileFromResourceUrl(Pql2OfflineClusterIntegrationTest.class.getClassLoader().getResource(
            "On_Time_On_Time_Performance_2014_100k_subset_nonulls.tar.gz"))), _tmpDir);

    _tmpDir.mkdirs();

    final List<File> avroFiles = new ArrayList<File>(SEGMENT_COUNT);
    for (int segmentNumber = 1; segmentNumber <= SEGMENT_COUNT; ++segmentNumber) {
      avroFiles.add(new File(_tmpDir.getPath() + "/On_Time_On_Time_Performance_2014_" + segmentNumber + ".avro"));
    }

    createResource();

    // Create segments from Avro data
    ExecutorService executor = Executors.newCachedThreadPool();
    buildSegmentsFromAvro(avroFiles, executor, 0, _segmentDir, _tarDir, "myresource");

    // Initialize query generator
    executor.execute(new Runnable() {
      @Override
      public void run() {
        _queryGenerator = new QueryGenerator(avroFiles, "'myresource'", "mytable");
      }
    });

    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.MINUTES);

    // Set up a Helix spectator to count the number of segments that are uploaded and unlock the latch once 12 segments are online
    final CountDownLatch latch = new CountDownLatch(1);
    HelixManager manager =
        HelixManagerFactory.getZKHelixManager(getHelixClusterName(), "test_instance", InstanceType.SPECTATOR,
            ZkTestUtils.DEFAULT_ZK_STR);
    manager.connect();
    manager.addExternalViewChangeListener(new ExternalViewChangeListener() {
      @Override
      public void onExternalViewChange(List<ExternalView> externalViewList, NotificationContext changeContext) {
        for (ExternalView externalView : externalViewList) {
          if (externalView.getId().contains("myresource")) {

            Set<String> partitionSet = externalView.getPartitionSet();
            if (partitionSet.size() == SEGMENT_COUNT) {
              int onlinePartitionCount = 0;

              for (String partitionId : partitionSet) {
                Map<String, String> partitionStateMap = externalView.getStateMap(partitionId);
                if (partitionStateMap.containsValue("ONLINE")) {
                  onlinePartitionCount++;
                }
              }

              if (onlinePartitionCount == SEGMENT_COUNT) {
                System.out.println("Got " + SEGMENT_COUNT + " online resources, unlatching the main thread");
                latch.countDown();
              }
            }
          }
        }
      }
    });

    // Upload the segments
    int i = 0;
    for (String segmentName : _tarDir.list()) {
      System.out.println("Uploading segment " + (i++) + " : " + segmentName);
      File file = new File(_tarDir, segmentName);
      FileUploadUtils.sendSegmentFile("localhost", "8998", segmentName, new FileInputStream(file), file.length());
    }

    // Wait for all segments to be online
    latch.await();
    TOTAL_DOCS = 115545;
    long timeInTwoMinutes = System.currentTimeMillis() + 2 * 60 * 1000L;
    long numDocs;
    while ((numDocs = getCurrentServingNumDocs()) < TOTAL_DOCS) {
      System.out.println("Current number of documents: " + numDocs);
      if (System.currentTimeMillis() < timeInTwoMinutes) {
        Thread.sleep(1000);
      } else {
        Assert.fail("Segments were not completely loaded within two minutes");
      }
    }
  }

  @Override
  protected void runQuery(String pqlQuery, List<String> sqlQueries) throws Exception {
    JSONObject pql1Response = postQuery(pqlQuery, BROKER_BASE_API_URL, false);
    JSONObject pql2Response = postQuery(pqlQuery, BROKER_BASE_API_URL, true);
    Assert.assertEquals(pql2Response.getInt("numDocsScanned"), pql1Response.getInt("numDocsScanned"));
  }

  @Override
  @Test
  public void testMultipleQueries() throws Exception {
    super.testMultipleQueries();
  }

  @Override
  @Test
  public void testHardcodedQuerySet() throws Exception {
    super.testHardcodedQuerySet();
  }

  @Override
  @Test
  public void testGeneratedQueries() throws Exception {
    super.testGeneratedQueries();
  }

  @Test
  public void testSingleQuery() throws Exception {
    String query;
    query = "select count(*) from 'myresource' where DaysSinceEpoch >= 16312";
    runQuery(query, Collections.singletonList(query.replace("'myresource'", "mytable")));
    query = "select count(*) from 'myresource' where DaysSinceEpoch < 16312";
    runQuery(query, Collections.singletonList(query.replace("'myresource'", "mytable")));
    query = "select count(*) from 'myresource' where DaysSinceEpoch <= 16312";
    runQuery(query, Collections.singletonList(query.replace("'myresource'", "mytable")));
    query = "select count(*) from 'myresource' where DaysSinceEpoch > 16312";
    runQuery(query, Collections.singletonList(query.replace("'myresource'", "mytable")));

  }

  @Override
  protected String getHelixClusterName() {
    return "OfflineClusterIntegrationTest";
  }

  @AfterClass
  public void tearDown() throws Exception {
    stopBroker();
    stopController();
    stopServer();
    try {
      stopZk();
    } catch (Exception e) {
      // Swallow ZK Exceptions.
    }
    FileUtils.deleteDirectory(_tmpDir);
  }

  @Override
  protected int getGeneratedQueryCount() {
    return QUERY_COUNT;
  }
}
