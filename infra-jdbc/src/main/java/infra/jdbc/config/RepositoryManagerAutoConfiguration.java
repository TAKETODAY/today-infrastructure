package infra.jdbc.config;

import org.jspecify.annotations.Nullable;

import javax.sql.DataSource;

import infra.beans.factory.ObjectProvider;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnSingleCandidate;
import infra.context.properties.EnableConfigurationProperties;
import infra.jdbc.PrimitiveTypeNullHandler;
import infra.jdbc.RepositoryManager;
import infra.jdbc.type.TypeHandler;
import infra.jdbc.type.TypeHandlerManager;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Component;
import infra.transaction.PlatformTransactionManager;

/**
 * Auto-configuration for Repository Manager and related components.
 * This configuration sets up the {@link RepositoryManager} and {@link TypeHandlerManager}
 * based on the available beans in the application context and configured properties.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/28 20:25
 */
@DisableDIAutoConfiguration(after = {
        DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class })
@ConditionalOnSingleCandidate(DataSource.class)
@EnableConfigurationProperties(RepositoryProperties.class)
public final class RepositoryManagerAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(RepositoryManagerAutoConfiguration.class);

  @Component
  @ConditionalOnMissingBean(RepositoryManager.class)
  public static RepositoryManager repositoryManager(DataSource dataSource,
          PlatformTransactionManager transactionManager, TypeHandlerManager typeHandlerManager,
          @Nullable PrimitiveTypeNullHandler primitiveTypeNullHandler, RepositoryProperties properties) {
    RepositoryManager manager = new RepositoryManager(dataSource, transactionManager);
    manager.setTypeHandlerManager(typeHandlerManager);
    manager.setDefaultCaseSensitive(properties.defaultCaseSensitive);
    manager.setDefaultColumnMappings(properties.defaultColumnMappings);
    manager.setCatchResourceCloseErrors(properties.catchResourceCloseErrors);
    manager.setGeneratedKeys(properties.generatedKeys);
    manager.setPrimitiveTypeNullHandler(primitiveTypeNullHandler);
    return manager;
  }

  @Component
  @ConditionalOnMissingBean(TypeHandlerManager.class)
  public static TypeHandlerManager typeHandlerManager(ObjectProvider<TypeHandler<?>> typeHandlers,
          ObjectProvider<TypeHandlerManagerCustomizer> customizers) {

    TypeHandlerManager manager = TypeHandlerManager.sharedInstance;
    for (TypeHandler<?> typeHandler : typeHandlers) {
      manager.register(typeHandler);
      log.debug("Registered type handler: '{}'", typeHandler);
    }

    for (TypeHandlerManagerCustomizer customizer : customizers) {
      customizer.customize(manager);
    }

    return manager;
  }

}
