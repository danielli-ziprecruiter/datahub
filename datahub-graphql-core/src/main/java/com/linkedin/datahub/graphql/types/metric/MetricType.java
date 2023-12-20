package com.linkedin.datahub.graphql.types.metric;

import static com.linkedin.datahub.graphql.Constants.*;
import static com.linkedin.metadata.Constants.*;

import com.datahub.authorization.ConjunctivePrivilegeGroup;
import com.datahub.authorization.DisjunctivePrivilegeGroup;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.linkedin.common.urn.CorpuserUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.data.template.StringArray;
import com.linkedin.datahub.graphql.QueryContext;
import com.linkedin.datahub.graphql.authorization.AuthorizationUtils;
import com.linkedin.datahub.graphql.exception.AuthorizationException;
import com.linkedin.datahub.graphql.generated.BrowsePath;
import com.linkedin.datahub.graphql.generated.BrowseResults;
import com.linkedin.datahub.graphql.generated.Entity;
import com.linkedin.datahub.graphql.generated.EntityType;
import com.linkedin.datahub.graphql.generated.FacetFilterInput;
import com.linkedin.datahub.graphql.generated.Metric;
import com.linkedin.datahub.graphql.generated.MetricUpdateInput;
import com.linkedin.datahub.graphql.resolvers.ResolverUtils;
import com.linkedin.datahub.graphql.types.BrowsableEntityType;
import com.linkedin.datahub.graphql.types.MutableType;
import com.linkedin.datahub.graphql.types.mappers.BrowsePathsMapper;
import com.linkedin.datahub.graphql.types.mappers.BrowseResultMapper;
import com.linkedin.datahub.graphql.types.metric.mappers.MetricMapper;
import com.linkedin.datahub.graphql.types.metric.mappers.MetricUpdateInputMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.authorization.PoliciesConfig;
import com.linkedin.metadata.browse.BrowseResult;
import com.linkedin.mxe.MetadataChangeProposal;
import com.linkedin.r2.RemoteInvocationException;
import graphql.execution.DataFetcherResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MetricType
    implements BrowsableEntityType<Metric, String>, MutableType<MetricUpdateInput, Metric> {

  private static final Set<String> ASPECTS_TO_RESOLVE =
      ImmutableSet.of(
          METRIC_KEY_ASPECT_NAME,
          METRIC_PROPERTIES_ASPECT_NAME,
          EDITABLE_METRIC_PROPERTIES_ASPECT_NAME,
          OWNERSHIP_ASPECT_NAME,
          INSTITUTIONAL_MEMORY_ASPECT_NAME,
          GLOBAL_TAGS_ASPECT_NAME,
          GLOSSARY_TERMS_ASPECT_NAME,
          BROWSE_PATHS_V2_ASPECT_NAME);

  private static final Set<String> FACET_FIELDS = ImmutableSet.of("tier");
  private static final String ENTITY_NAME = "metric";

  private final EntityClient _entityClient;

  public MetricType(final EntityClient entityClient) {
    _entityClient = entityClient;
  }

  @Override
  public Class<Metric> objectClass() {
    return Metric.class;
  }

  @Override
  public Class<MetricUpdateInput> inputClass() {
    return MetricUpdateInput.class;
  }

  @Override
  public EntityType type() {
    return EntityType.METRIC;
  }

  @Override
  public Function<Entity, String> getKeyProvider() {
    return Entity::getUrn;
  }

  @Override
  public BrowseResults browse(
      @Nonnull List<String> path,
      @Nullable List<FacetFilterInput> filters,
      int start,
      int count,
      @Nonnull final QueryContext context)
      throws Exception {
    final Map<String, String> facetFilters = ResolverUtils.buildFacetFilters(filters, FACET_FIELDS);
    final String pathStr =
        path.size() > 0 ? BROWSE_PATH_DELIMITER + String.join(BROWSE_PATH_DELIMITER, path) : "";
    final BrowseResult result =
        _entityClient.browse(
            "Metric", pathStr, facetFilters, start, count, context.getAuthentication());
    return BrowseResultMapper.map(result);
  }

  @Override
  public List<BrowsePath> browsePaths(@Nonnull String urn, @Nonnull final QueryContext context)
      throws Exception {
    final StringArray result =
        _entityClient.getBrowsePaths(MetricUtils.getMetricUrn(urn), context.getAuthentication());
    return BrowsePathsMapper.map(result);
  }

  public List<DataFetcherResult<Metric>> batchLoad(
      final List<String> urnStrs, @Nonnull final QueryContext context) throws Exception {
    final List<Urn> urns = urnStrs.stream().map(UrnUtils::getUrn).collect(Collectors.toList());
    try {
      final Map<Urn, EntityResponse> metricMap =
          _entityClient.batchGetV2(
              Constants.METRIC_ENTITY_NAME,
              new HashSet<>(urns),
              ASPECTS_TO_RESOLVE,
              context.getAuthentication());

      final List<EntityResponse> gmsResults = new ArrayList<>();
      for (Urn urn : urns) {
        gmsResults.add(metricMap.getOrDefault(urn, null));
      }
      return gmsResults.stream()
          .map(
              gmsMetric ->
                  gmsMetric == null
                      ? null
                      : DataFetcherResult.<Metric>newResult()
                          .data(MetricMapper.map(gmsMetric))
                          .build())
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("Failed to batch load Metrics", e);
    }
  }

  @Override
  public Metric update(
      @Nonnull String urn, @Nonnull MetricUpdateInput input, @Nonnull QueryContext context)
      throws Exception {
    if (isAuthorized(urn, input, context)) {
      final CorpuserUrn actor =
          CorpuserUrn.createFromString(context.getAuthentication().getActor().toUrnStr());
      final Collection<MetadataChangeProposal> proposals =
          MetricUpdateInputMapper.map(input, actor);
      proposals.forEach(proposal -> proposal.setEntityUrn(UrnUtils.getUrn(urn)));

      try {
        _entityClient.batchIngestProposals(proposals, context.getAuthentication(), false);
      } catch (RemoteInvocationException e) {
        throw new RuntimeException(String.format("Failed to write entity with urn %s", urn), e);
      }

      return load(urn, context).getData();
    }
    throw new AuthorizationException(
        "Unauthorized to perform this action. Please contact your DataHub administrator.");
  }

  private boolean isAuthorized(
      @Nonnull String urn, @Nonnull MetricUpdateInput update, @Nonnull QueryContext context) {
    // Decide whether the current principal should be allowed to update the Dataset.
    final DisjunctivePrivilegeGroup orPrivilegeGroups = getAuthorizedPrivileges(update);
    return AuthorizationUtils.isAuthorized(
        context.getAuthorizer(),
        context.getAuthentication().getActor().toUrnStr(),
        PoliciesConfig.METRIC_PRIVILEGES.getResourceType(),
        urn,
        orPrivilegeGroups);
  }

  private DisjunctivePrivilegeGroup getAuthorizedPrivileges(final MetricUpdateInput updateInput) {

    final ConjunctivePrivilegeGroup allPrivilegesGroup =
        new ConjunctivePrivilegeGroup(
            ImmutableList.of(PoliciesConfig.EDIT_ENTITY_PRIVILEGE.getType()));

    List<String> specificPrivileges = new ArrayList<>();
    if (updateInput.getOwnership() != null) {
      specificPrivileges.add(PoliciesConfig.EDIT_ENTITY_OWNERS_PRIVILEGE.getType());
    }
    if (updateInput.getEditableProperties() != null) {
      specificPrivileges.add(PoliciesConfig.EDIT_ENTITY_DOCS_PRIVILEGE.getType());
    }
    if (updateInput.getTags() != null) {
      specificPrivileges.add(PoliciesConfig.EDIT_ENTITY_TAGS_PRIVILEGE.getType());
    }
    final ConjunctivePrivilegeGroup specificPrivilegeGroup =
        new ConjunctivePrivilegeGroup(specificPrivileges);

    // If you either have all entity privileges, or have the specific privileges required, you are
    // authorized.
    return new DisjunctivePrivilegeGroup(
        ImmutableList.of(allPrivilegesGroup, specificPrivilegeGroup));
  }
}
