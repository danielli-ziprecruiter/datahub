package com.linkedin.common.urn;

import com.linkedin.common.FabricType;
import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;

import java.net.URISyntaxException;


public final class MetricUrn extends Urn {

  public static final String ENTITY_TYPE = "metric";

  private final String _metricName;

  public MetricUrn(String name) {
    super(ENTITY_TYPE, TupleKey.create(name));
    this._metricName = name;
  }
  public String getMetricNameEntity() {
    return _metricName;
  }

  public static MetricUrn createFromString(String rawUrn) throws URISyntaxException {
    return createFromUrn(Urn.createFromString(rawUrn));
  }

  public static MetricUrn createFromUrn(Urn urn) throws URISyntaxException {
    if (!"li".equals(urn.getNamespace())) {
      throw new URISyntaxException(urn.toString(), "Urn namespace type should be 'li'.");
    } else if (!ENTITY_TYPE.equals(urn.getEntityType())) {
      throw new URISyntaxException(urn.toString(), "Urn entity type should be 'dataset'.");
    } else {
      TupleKey key = urn.getEntityKey();
      if (key.size() != 1) {
        throw new URISyntaxException(urn.toString(), "Invalid number of keys.");
      } else {
        try {
          return new MetricUrn((String) key.getAs(1, String.class));
        } catch (Exception var3) {
          throw new URISyntaxException(urn.toString(), "Invalid URN Parameter: '" + var3.getMessage());
        }
      }
    }
  }

  public static MetricUrn deserialize(String rawUrn) throws URISyntaxException {
    return createFromString(rawUrn);
  }

  static {
    Custom.initializeCustomClass(MetricUrn.class);
    Custom.registerCoercer(new DirectCoercer<MetricUrn>() {
      public Object coerceInput(MetricUrn object) throws ClassCastException {
        return object.toString();
      }

      public MetricUrn coerceOutput(Object object) throws TemplateOutputCastException {
        try {
          return MetricUrn.createFromString((String) object);
        } catch (URISyntaxException e) {
          throw new TemplateOutputCastException("Invalid URN syntax: " + e.getMessage(), e);
        }
      }
    }, MetricUrn.class);
  }
}
