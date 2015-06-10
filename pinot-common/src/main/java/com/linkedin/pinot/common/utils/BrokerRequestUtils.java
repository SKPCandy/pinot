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
package com.linkedin.pinot.common.utils;

import com.linkedin.pinot.common.utils.CommonConstants.Helix.TableType;
import com.linkedin.pinot.common.request.BrokerRequest;


@Deprecated
public class BrokerRequestUtils {

  @Deprecated
  public static String buildRealtimeResourceNameForResource(String hybridResource) {
    return hybridResource + CommonConstants.Broker.DataResource.REALTIME_RESOURCE_SUFFIX;
  }

  @Deprecated
  public static String buildOfflineResourceNameForResource(String hybridResource) {
    return hybridResource + CommonConstants.Broker.DataResource.OFFLINE_RESOURCE_SUFFIX;
  }

  @Deprecated
  public static TableType getResourceTypeFromResourceName(String resourceName) {
    if (resourceName.endsWith(CommonConstants.Broker.DataResource.REALTIME_RESOURCE_SUFFIX)) {
      return TableType.REALTIME;
    }
    return TableType.OFFLINE;
  }

  @Deprecated
  public static String getRealtimeResourceNameForResource(String resourceName) {
    if (resourceName.endsWith(CommonConstants.Broker.DataResource.REALTIME_RESOURCE_SUFFIX)) {
      return resourceName;
    } else {
      return BrokerRequestUtils.buildRealtimeResourceNameForResource(resourceName);
    }
  }

  @Deprecated
  public static String getOfflineResourceNameForResource(String resourceName) {
    if (resourceName.endsWith(CommonConstants.Broker.DataResource.OFFLINE_RESOURCE_SUFFIX)) {
      return resourceName;
    } else {
      return BrokerRequestUtils.buildOfflineResourceNameForResource(resourceName);
    }
  }

  @Deprecated
  public static String getHybridResourceName(String resourceName) {
    if (resourceName.endsWith(CommonConstants.Broker.DataResource.REALTIME_RESOURCE_SUFFIX)) {
      return resourceName.substring(0, resourceName.length()
          - CommonConstants.Broker.DataResource.REALTIME_RESOURCE_SUFFIX.length());
    }
    if (resourceName.endsWith(CommonConstants.Broker.DataResource.OFFLINE_RESOURCE_SUFFIX)) {
      return resourceName.substring(0, resourceName.length()
          - CommonConstants.Broker.DataResource.OFFLINE_RESOURCE_SUFFIX.length());
    }
    return resourceName;
  }

  public static boolean areEquivalent(BrokerRequest left, BrokerRequest right) {
    boolean basicFieldsAreEquivalent = EqualityUtils.isEqual(left.getQueryType(), right.getQueryType()) &&
            EqualityUtils.isEqual(left.getQuerySource(), right.getQuerySource()) &&
            EqualityUtils.isEqual(left.getTimeInterval(), right.getTimeInterval()) &&
            EqualityUtils.isEqual(left.getDuration(), right.getDuration()) &&
            EqualityUtils.isEqual(left.getAggregationsInfo(), right.getAggregationsInfo()) &&
              (
                  EqualityUtils.isEqual(left.getGroupBy(), right.getGroupBy()) ||
                      // Pinot quirk: group by clauses may not be in the same order
                      (EqualityUtils.isEqualIgnoringOrder(left.getGroupBy().getColumns(), right.getGroupBy().getColumns()) &&
                      EqualityUtils.isEqual(left.getGroupBy().getTopN(), right.getGroupBy().getTopN()))
              ) &&
            EqualityUtils.isEqual(left.getSelections(), right.getSelections()) &&
            EqualityUtils.isEqual(left.getBucketHashKey(), right.getBucketHashKey());

    // TODO Compare filter query map

    return basicFieldsAreEquivalent;
  }
}
