package infra.persistence.config;

import org.jspecify.annotations.Nullable;

import java.util.List;

import javax.sql.DataSource;

import infra.beans.factory.ObjectProvider;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnSingleCandidate;
import infra.context.properties.EnableConfigurationProperties;
import infra.jdbc.RepositoryManager;
import infra.jdbc.config.RepositoryManagerAutoConfiguration;
import infra.jdbc.format.SqlStatementLogger;
import infra.jdbc.type.TypeHandlerManager;
import infra.persistence.BatchPersistListener;
import infra.persistence.ConditionPropertyExtractor;
import infra.persistence.DefaultEntityManager;
import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.EntityManager;
import infra.persistence.EntityMetadataFactory;
import infra.persistence.platform.Platform;
import infra.stereotype.Component;

/**
 * Auto-configuration class for setting up the {@link EntityManager}.
 * This configuration provides default beans for {@link EntityManager},
 * {@link SqlStatementLogger}, and {@link EntityMetadataFactory} when not already present.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/29 14:22
 */
@EnableConfigurationProperties(PersistenceProperties.class)
@DisableDIAutoConfiguration(after = RepositoryManagerAutoConfiguration.class)
@ConditionalOnSingleCandidate(DataSource.class)
public final class EntityManagerAutoConfiguration {

  @Component
  @SuppressWarnings("rawtypes")
  @ConditionalOnMissingBean(EntityManager.class)
  public static EntityManager entityManager(RepositoryManager manager, @Nullable Platform platform,
          EntityMetadataFactory entityMetadataFactory, SqlStatementLogger sqlStatementLogger,
          PersistenceProperties properties,
          List<BatchPersistListener> batchPersistListeners,
          List<ConditionPropertyExtractor> conditionPropertyExtractors,
          ObjectProvider<EntityManagerCustomizer> customizers) {
    DefaultEntityManager entityManager = new DefaultEntityManager(manager, platform);

    entityManager.setStatementLogger(sqlStatementLogger);
    entityManager.setEntityMetadataFactory(entityMetadataFactory);
    entityManager.setBatchPersistListeners(batchPersistListeners);
    entityManager.setMaxBatchRecords(properties.maxBatchRecords);
    entityManager.setAutoGenerateId(properties.autoGenerateId);
    entityManager.setConditionPropertyExtractors(conditionPropertyExtractors);

    for (EntityManagerCustomizer customizer : customizers) {
      customizer.customize(entityManager);
    }
    return entityManager;
  }

  @Component
  @ConditionalOnMissingBean(SqlStatementLogger.class)
  public static SqlStatementLogger sqlStatementLogger() {
    return SqlStatementLogger.sharedInstance;
  }

  @Component
  @ConditionalOnMissingBean(EntityMetadataFactory.class)
  public static EntityMetadataFactory entityMetadataFactory(TypeHandlerManager typeHandlerManager,
          ObjectProvider<EntityMetadataFactoryCustomizer> customizers) {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
    factory.setTypeHandlerManager(typeHandlerManager);

    for (EntityMetadataFactoryCustomizer customizer : customizers) {
      customizer.customize(factory);
    }
    return factory;
  }
}
