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
import infra.persistence.DefaultEntityManager;
import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.EntityManager;
import infra.persistence.EntityMetadataFactory;
import infra.persistence.VersionIncrementStrategy;
import infra.persistence.event.DefaultEntityEventRegistry;
import infra.persistence.event.EntityEventRegistry;
import infra.persistence.event.Listener;
import infra.persistence.platform.Platform;
import infra.persistence.query.EntityQueryFactories;
import infra.persistence.query.EntityQueryFactory;
import infra.persistence.query.PropertyConditionStrategy;
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
  @ConditionalOnMissingBean(EntityManager.class)
  public static EntityManager entityManager(RepositoryManager manager, @Nullable Platform platform,
          EntityMetadataFactory entityMetadataFactory, SqlStatementLogger sqlStatementLogger,
          PersistenceProperties properties, @Nullable VersionIncrementStrategy versionIncrementStrategy,
          EntityEventRegistry entityEventRegistry, EntityQueryFactories entityQueryFactories,
          ObjectProvider<EntityManagerCustomizer> customizers) {
    DefaultEntityManager entityManager = new DefaultEntityManager(manager, platform);

    entityManager.setStatementLogger(sqlStatementLogger);
    entityManager.setEntityMetadataFactory(entityMetadataFactory);
    entityManager.setMaxBatchRecords(properties.maxBatchRecords);
    entityManager.setAutoGenerateId(properties.autoGenerateId);
    entityManager.setEntityEventRegistry(entityEventRegistry);
    entityManager.setEntityQueryFactories(entityQueryFactories);

    if (versionIncrementStrategy != null) {
      entityManager.setVersionIncrementStrategy(versionIncrementStrategy);
    }

    for (EntityManagerCustomizer customizer : customizers) {
      customizer.customize(entityManager);
    }
    return entityManager;
  }

  @Component
  @ConditionalOnMissingBean(Platform.class)
  public static Platform platform(DataSource dataSource) {
    return Platform.forDataSource(dataSource);
  }

  @Component
  @ConditionalOnMissingBean(EntityQueryFactories.class)
  static EntityQueryFactories entityQueryFactories(EntityMetadataFactory entityMetadataFactory,
          List<EntityQueryFactory> entityQueryFactories, List<PropertyConditionStrategy> strategies) {
    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory, entityQueryFactories);
    for (PropertyConditionStrategy strategy : strategies) {
      factories.addStrategy(strategy);
    }
    return factories;
  }

  @Component
  @ConditionalOnMissingBean(EntityEventRegistry.class)
  static EntityEventRegistry entityEventRegistry(List<Listener> listeners) {
    DefaultEntityEventRegistry registry = new DefaultEntityEventRegistry();
    registry.setListeners(listeners);
    return registry;
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
