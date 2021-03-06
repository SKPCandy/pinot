package com.linkedin.pinot.controller.api.restlet.resources;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.pinot.common.config.AbstractTableConfig;
import com.linkedin.pinot.common.utils.CommonConstants.Helix.TableType;
import com.linkedin.pinot.controller.ControllerConf;
import com.linkedin.pinot.controller.helix.core.PinotHelixResourceManager;


public class PinotTableSegmentConfigs extends ServerResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(PinotTableSegmentConfigs.class);
  private final ControllerConf conf;
  private final PinotHelixResourceManager manager;
  private final File baseDataDir;
  private final File tempDir;

  public PinotTableSegmentConfigs() throws IOException {
    conf = (ControllerConf) getApplication().getContext().getAttributes().get(ControllerConf.class.toString());
    manager =
        (PinotHelixResourceManager) getApplication().getContext().getAttributes()
            .get(PinotHelixResourceManager.class.toString());
    baseDataDir = new File(conf.getDataDir());
    if (!baseDataDir.exists()) {
      FileUtils.forceMkdir(baseDataDir);
    }
    tempDir = new File(baseDataDir, "schemasTemp");
    if (!tempDir.exists()) {
      FileUtils.forceMkdir(tempDir);
    }
  }

  @Override
  @Put("json")
  public Representation put(Representation entity) {
    final String tableName = (String) getRequest().getAttributes().get("tableName");
    if (tableName == null) {
      return new StringRepresentation("tableName is not present");
    }

    AbstractTableConfig config = null;

    try {
      config = AbstractTableConfig.init(entity.getText());
      manager.updateSegmentsValidationAndRetentionConfigFor(config.getTableName(),
          TableType.valueOf(config.getTableType()), config.getValidationConfig());
      return new StringRepresentation("done");
    } catch (final Exception e) {
      LOGGER.error("errpr updating segments configs for table {}", config.getTableName(), e);
      return PinotSegmentUploadRestletResource.exceptionToStringRepresentation(e);
    }

  }
}
