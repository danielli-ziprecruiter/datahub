package com.linkedin.datahub.graphql.types.metric.mappers;

import static com.linkedin.metadata.Constants.*;

import com.linkedin.common.GlobalTags;
import com.linkedin.common.TagAssociationArray;
import com.linkedin.common.urn.Urn;
import com.linkedin.datahub.graphql.generated.MetricUpdateInput;
import com.linkedin.datahub.graphql.types.common.mappers.InstitutionalMemoryUpdateMapper;
import com.linkedin.datahub.graphql.types.common.mappers.OwnershipUpdateMapper;
import com.linkedin.datahub.graphql.types.common.mappers.util.UpdateMappingHelper;
import com.linkedin.datahub.graphql.types.mappers.InputModelMapper;
import com.linkedin.datahub.graphql.types.tag.mappers.TagAssociationUpdateMapper;
import com.linkedin.metric.EditableMetricProperties;
import com.linkedin.mxe.MetadataChangeProposal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class MetricUpdateInputMapper
    implements InputModelMapper<MetricUpdateInput, Collection<MetadataChangeProposal>, Urn> {

  public static final MetricUpdateInputMapper INSTANCE = new MetricUpdateInputMapper();

  public static Collection<MetadataChangeProposal> map(
      @Nonnull final MetricUpdateInput metricUpdateInput, @Nonnull final Urn actor) {
    return INSTANCE.apply(metricUpdateInput, actor);
  }

  @Override
  public Collection<MetadataChangeProposal> apply(
      @Nonnull final MetricUpdateInput metricUpdateInput, @Nonnull final Urn actor) {
    final Collection<MetadataChangeProposal> proposals = new ArrayList<>(6);
    final UpdateMappingHelper updateMappingHelper = new UpdateMappingHelper(METRIC_ENTITY_NAME);

    if (metricUpdateInput.getOwnership() != null) {
      proposals.add(
          updateMappingHelper.aspectToProposal(
              OwnershipUpdateMapper.map(metricUpdateInput.getOwnership(), actor),
              OWNERSHIP_ASPECT_NAME));
    }

    if (metricUpdateInput.getInstitutionalMemory() != null) {
      proposals.add(
          updateMappingHelper.aspectToProposal(
              InstitutionalMemoryUpdateMapper.map(metricUpdateInput.getInstitutionalMemory()),
              INSTITUTIONAL_MEMORY_ASPECT_NAME));
    }

    if (metricUpdateInput.getTags() != null) {
      final GlobalTags globalTags = new GlobalTags();
      // Tags field overrides deprecated globalTags field
      globalTags.setTags(
          new TagAssociationArray(
              metricUpdateInput.getTags().getTags().stream()
                  .map(element -> TagAssociationUpdateMapper.map(element))
                  .collect(Collectors.toList())));
      proposals.add(updateMappingHelper.aspectToProposal(globalTags, GLOBAL_TAGS_ASPECT_NAME));
    }

    if (metricUpdateInput.getEditableProperties() != null) {
      final EditableMetricProperties editableMetricProperties = new EditableMetricProperties();
      editableMetricProperties.setDescription(
          metricUpdateInput.getEditableProperties().getDescription());
      proposals.add(
          updateMappingHelper.aspectToProposal(
              editableMetricProperties, EDITABLE_METRIC_PROPERTIES_ASPECT_NAME));
    }

    return proposals;
  }
}
