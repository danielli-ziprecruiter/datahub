package com.linkedin.datahub.graphql.types.metric.mappers;

import static com.linkedin.metadata.Constants.*;

import com.linkedin.common.BrowsePathsV2;
import com.linkedin.common.GlobalTags;
import com.linkedin.common.GlossaryTerms;
import com.linkedin.common.InstitutionalMemory;
import com.linkedin.common.Ownership;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.DataMap;
import com.linkedin.datahub.graphql.generated.EntityType;
import com.linkedin.datahub.graphql.generated.Metric;
import com.linkedin.datahub.graphql.generated.MetricEditableProperties;
import com.linkedin.datahub.graphql.types.common.mappers.BrowsePathsV2Mapper;
import com.linkedin.datahub.graphql.types.common.mappers.CustomPropertiesMapper;
import com.linkedin.datahub.graphql.types.common.mappers.InstitutionalMemoryMapper;
import com.linkedin.datahub.graphql.types.common.mappers.OwnershipMapper;
import com.linkedin.datahub.graphql.types.common.mappers.util.MappingHelper;
import com.linkedin.datahub.graphql.types.glossary.mappers.GlossaryTermsMapper;
import com.linkedin.datahub.graphql.types.mappers.ModelMapper;
import com.linkedin.datahub.graphql.types.tag.mappers.GlobalTagsMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.metadata.key.MetricKey;
import com.linkedin.metric.EditableMetricProperties;
import com.linkedin.metric.MetricProperties;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps GMS response objects to objects conforming to the GQL schema.
 *
 * <p>To be replaced by auto-generated mappers implementations
 */
@Slf4j
public class MetricMapper implements ModelMapper<EntityResponse, Metric> {

  public static final MetricMapper INSTANCE = new MetricMapper();

  public static Metric map(@Nonnull final EntityResponse metric) {
    return INSTANCE.apply(metric);
  }

  public Metric apply(@Nonnull final EntityResponse entityResponse) {
    Metric result = new Metric();
    Urn entityUrn = entityResponse.getUrn();
    result.setUrn(entityResponse.getUrn().toString());
    result.setType(EntityType.METRIC);

    EnvelopedAspectMap aspectMap = entityResponse.getAspects();

    MappingHelper<Metric> mappingHelper = new MappingHelper<>(aspectMap, result);
    mappingHelper.mapToResult(METRIC_KEY_ASPECT_NAME, this::mapMetricKey);
    mappingHelper.mapToResult(
        METRIC_PROPERTIES_ASPECT_NAME,
        (entity, dataMap) -> this.mapMetricProperties(entity, dataMap, entityUrn));
    mappingHelper.mapToResult(
        EDITABLE_METRIC_PROPERTIES_ASPECT_NAME, this::mapEditableMetricProperties);
    mappingHelper.mapToResult(
        INSTITUTIONAL_MEMORY_ASPECT_NAME,
        (metric, dataMap) ->
            metric.setInstitutionalMemory(
                InstitutionalMemoryMapper.map(new InstitutionalMemory(dataMap), entityUrn)));
    mappingHelper.mapToResult(
        OWNERSHIP_ASPECT_NAME,
        (metric, dataMap) ->
            metric.setOwnership(OwnershipMapper.map(new Ownership(dataMap), entityUrn)));
    mappingHelper.mapToResult(
        GLOBAL_TAGS_ASPECT_NAME,
        (metric, dataMap) -> this.mapGlobalTags(metric, dataMap, entityUrn));
    mappingHelper.mapToResult(
        GLOSSARY_TERMS_ASPECT_NAME,
        (metric, dataMap) ->
            metric.setGlossaryTerms(
                GlossaryTermsMapper.map(new GlossaryTerms(dataMap), entityUrn)));
    mappingHelper.mapToResult(
        BROWSE_PATHS_V2_ASPECT_NAME,
        (metric, dataMap) ->
            metric.setBrowsePathV2(BrowsePathsV2Mapper.map(new BrowsePathsV2(dataMap))));
    return mappingHelper.getResult();
  }

  private void mapMetricKey(@Nonnull Metric metric, @Nonnull DataMap dataMap) {
    final MetricKey gmsKey = new MetricKey(dataMap);
  }

  private void mapMetricProperties(
      @Nonnull Metric metric, @Nonnull DataMap dataMap, @Nonnull Urn entityUrn) {
    final MetricProperties gmsProperties = new MetricProperties(dataMap);
    final com.linkedin.datahub.graphql.generated.MetricProperties properties =
        new com.linkedin.datahub.graphql.generated.MetricProperties();
    properties.setDescription(gmsProperties.getDescription());
    if (gmsProperties.getExternalUrl() != null) {
      properties.setExternalUrl(gmsProperties.getExternalUrl().toString());
    }
    properties.setCustomProperties(
        CustomPropertiesMapper.map(gmsProperties.getCustomProperties(), entityUrn));
    if (gmsProperties.getName() != null) {
      properties.setName(gmsProperties.getName());
    }
    metric.setProperties(properties);
  }

  private void mapEditableMetricProperties(@Nonnull Metric metric, @Nonnull DataMap dataMap) {
    final EditableMetricProperties editableMetricProperties = new EditableMetricProperties(dataMap);
    final MetricEditableProperties editableProperties = new MetricEditableProperties();
    editableProperties.setDescription(editableMetricProperties.getDescription());
    metric.setEditableProperties(editableProperties);
  }

  private void mapGlobalTags(
      @Nonnull Metric metric, @Nonnull DataMap dataMap, @Nonnull final Urn entityUrn) {
    com.linkedin.datahub.graphql.generated.GlobalTags globalTags =
        GlobalTagsMapper.map(new GlobalTags(dataMap), entityUrn);
    metric.setTags(globalTags);
  }
}
