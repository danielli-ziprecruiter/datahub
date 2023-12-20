package com.linkedin.datahub.graphql.types.metric;

import com.linkedin.common.urn.MetricUrn;
import java.net.URISyntaxException;

public class MetricUtils {

  private MetricUtils() {}

  static MetricUrn getMetricUrn(String urnStr) {
    try {
      return MetricUrn.createFromString(urnStr);
    } catch (URISyntaxException e) {
      throw new RuntimeException(
          String.format("Failed to retrieve metric with urn %s, invalid urn", urnStr));
    }
  }
}
