package infra.persistence.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.config.AutoConfigurations;
import infra.jdbc.RepositoryManager;
import infra.jdbc.config.DataSourceAutoConfiguration;
import infra.jdbc.config.DataSourceTransactionManagerAutoConfiguration;
import infra.jdbc.config.RepositoryManagerAutoConfiguration;
import infra.jdbc.config.TypeHandlerManagerCustomizer;
import infra.jdbc.type.MappedTypes;
import infra.jdbc.type.TypeHandler;
import infra.jdbc.type.TypeHandlerManager;
import infra.jdbc.type.UnknownTypeHandler;
import infra.persistence.EntityManager;
import infra.persistence.EntityMetadataFactory;
import infra.persistence.IdGenerator;
import infra.persistence.Pageable;
import infra.persistence.PropertyFilter;
import infra.persistence.VersionIncrementStrategy;
import infra.persistence.annotation.GeneratedId;
import infra.persistence.annotation.Table;
import infra.persistence.query.EntityQueryFactory;
import infra.persistence.query.PropertyConditionStrategy;
import infra.persistence.support.DefaultEntityManager;
import infra.persistence.support.DefaultVersionIncrementStrategy;
import infra.persistence.support.IdGeneratorResolver;
import infra.test.context.runner.ApplicationContextRunner;
import infra.test.util.ReflectionTestUtils;
import infra.util.function.SupplierUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/29 14:26
 */
class EntityManagerAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withPropertyValues("datasource.generate-unique-name=true")
          .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
                  DataSourceTransactionManagerAutoConfiguration.class,
                  RepositoryManagerAutoConfiguration.class,
                  EntityManagerAutoConfiguration.class));

  @Test
  void entityManagerWhenNoAvailableEntityManagerAutoConfigurationIsNotCreated() {
    ApplicationContextRunner.forDefault()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(EntityManager.class));
  }

  @Test
  void entityManager() {
    this.contextRunner.run(context -> {
      assertThat(context).hasSingleBean(EntityManager.class);
      assertThat(context).hasSingleBean(IdGeneratorResolver.class);
      assertThat(((DefaultEntityManager) context.getBean(EntityManager.class)).getIdGeneratorResolver())
              .isSameAs(context.getBean(IdGeneratorResolver.class));
      assertThat(context).hasSingleBean(TypeHandlerManager.class);
      assertThat(context).getBean(TypeHandlerManager.class).isSameAs(TypeHandlerManager.sharedInstance);
    });
  }

  @ParameterizedTest
  @ValueSource(ints = { 1, 20, 100 })
  void configuredPageSizeIsApplied(int pageSize) {
    contextRunner.withPropertyValues("persistence.page-size=" + pageSize).run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context.getBean(PersistenceProperties.class).pageSize).isEqualTo(pageSize);
      Pageable pageable = (Pageable) ReflectionTestUtils.getField(context.getBean(EntityManager.class), "defaultPageable");
      assertThat(pageable.pageNumber()).isEqualTo(1);
      assertThat(pageable.pageSize()).isEqualTo(pageSize);
    });
  }

  @Test
  void defaultPageSizeIsTen() {
    contextRunner.run(context -> {
      assertThat(context.getBean(PersistenceProperties.class).pageSize).isEqualTo(10);
      Pageable pageable = (Pageable) ReflectionTestUtils.getField(context.getBean(EntityManager.class), "defaultPageable");
      assertThat(pageable.pageSize()).isEqualTo(10);
    });
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, -1 })
  void invalidPageSizeFailsStartup(int pageSize) {
    contextRunner.withPropertyValues("persistence.page-size=" + pageSize).run(context -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class)
              .hasRootCauseMessage("persistence.page-size must be positive");
    });
  }

  @Test
  void customIdGeneratorResolverIsInjected() {
    IdGeneratorResolver resolver = new IdGeneratorResolver(new StandardBeanFactory());
    contextRunner.withBean("customResolver", IdGeneratorResolver.class, () -> resolver)
            .run(context -> {
              assertThat(context).hasSingleBean(IdGeneratorResolver.class);
              assertThat(((DefaultEntityManager) context.getBean(EntityManager.class)).getIdGeneratorResolver())
                      .isSameAs(resolver);
            });
  }

  @Test
  void namedIdGeneratorUsesApplicationContext() {
    contextRunner.withBean("customIdGenerator", IdGenerator.class, () -> (object, metadata, id) -> "context-id")
            .run(context -> {
              RepositoryManager repository = context.getBean(RepositoryManager.class);
              repository.createNamedQuery("create table context_entity (id varchar(64) primary key)").executeUpdate();
              ContextEntity entity = new ContextEntity();
              context.getBean(EntityManager.class).persist(entity);
              assertThat(entity.id).isEqualTo("context-id");
              assertThat(repository.createNamedQuery("select id from context_entity").fetchFirst(String.class))
                      .isEqualTo("context-id");
            });
  }

  @Test
  void entityManagerWithCustomEntityManagerIsNotCreated() {
    this.contextRunner.withBean("customEntityManager", EntityManager.class, () -> mock(EntityManager.class))
            .run(context -> {
              assertThat(context).hasSingleBean(EntityManager.class);
              assertThat(context).getBean(TypeHandlerManager.class).isSameAs(TypeHandlerManager.sharedInstance);
              assertThat(context.getBean(EntityManager.class)).isEqualTo(context.getBean("customEntityManager"));
            });
  }

  @Test
  void typeHandlers() {
    contextRunner.withBean("customTypeHandler", CustomTypeHandler.class, CustomTypeHandler::new)
            .run(context -> {
              assertThat(context).hasSingleBean(EntityManager.class);
              assertThat(context).getBean(TypeHandlerManager.class).isSameAs(TypeHandlerManager.sharedInstance);
              assertThat(context.getBean(TypeHandlerManager.class).getTypeHandler(MyProperty.class))
                      .isEqualTo(context.getBean("customTypeHandler"));
            });
  }

  @Test
  void customizers() {
    contextRunner.withBean("myTypeHandlerManagerCustomizer", MyTypeHandlerManagerCustomizer.class, MyTypeHandlerManagerCustomizer::new)
            .run(context -> {
              assertThat(context).hasSingleBean(EntityManager.class);
              assertThat(context.getBean(TypeHandlerManager.class).getTypeHandler(MyProperty.class))
                      .isInstanceOf(CustomTypeHandler.class);
            });
  }

  @Test
  void entityManagerCustomizers() {
    contextRunner.withBean("entityManagerCustomizer", EntityManagerCustomizer.class,
                    () -> manager -> manager.setMaxBatchRecords(25))
            .run(context -> {
              assertThat(context).hasSingleBean(EntityManager.class);
              assertThat(context.getBean(EntityManager.class))
                      .extracting("maxBatchRecords").isEqualTo(25);
            });
  }

  @Test
  void entityQueryFactory() {
    EntityQueryFactory queryFactory = mock(EntityQueryFactory.class);
    contextRunner.withBean("queryFactory", EntityQueryFactory.class, () -> queryFactory)
            .run(context -> {
              assertThat(context).hasSingleBean(EntityManager.class);
              DefaultEntityManager entityManager = (DefaultEntityManager) context.getBean(EntityManager.class);
              assertThat(entityManager.getEntityQueryFactories().getFactories()).startsWith(queryFactory);
            });
  }

  @Test
  void entityQueryFactoriesIsNotExposedAsEntityQueryFactory() {
    contextRunner.run(context ->
            assertThat(context.getBeanNamesForType(EntityQueryFactory.class)).doesNotContain("entityQueryFactories"));
  }

  @Test
  void propertyConditionStrategies() {
    PropertyConditionStrategy strategy = mock(PropertyConditionStrategy.class);
    contextRunner.withBean("propertyConditionStrategy", PropertyConditionStrategy.class, () -> strategy)
            .run(context -> {
              DefaultEntityManager entityManager = (DefaultEntityManager) context.getBean(EntityManager.class);
              assertThat(entityManager.getEntityQueryFactories().getStrategies()).contains(strategy);
            });
  }

  @Test
  void entityMetadataFactoryCustomizer() {
    PropertyFilter propertyFilter = PropertyFilter.acceptAny();
    contextRunner.withBean("entityMetadataFactoryCustomizer", EntityMetadataFactoryCustomizer.class,
                    () -> factory -> factory.setPropertyFilter(propertyFilter))
            .run(context -> {
              assertThat(context).hasSingleBean(EntityManager.class);
              assertThat(context).hasSingleBean(EntityMetadataFactory.class);
              assertThat(context.getBean(EntityMetadataFactory.class))
                      .extracting("propertyFilter").isEqualTo(propertyFilter);
            });
  }

  @Test
  void typeHandlerManagerBean() {
    contextRunner.withBean("typeHandlerManager", TypeHandlerManager.class, TypeHandlerManager::new)
            .withBean("customTypeHandler", CustomTypeHandler.class, CustomTypeHandler::new)
            .run(context -> {
              assertThat(context).hasSingleBean(EntityManager.class);
              assertThat(context).hasSingleBean(TypeHandlerManager.class);
              assertThat(context.getBean(TypeHandlerManager.class)).isNotEqualTo(TypeHandlerManager.sharedInstance);
              assertThat(context.getBean(TypeHandlerManager.class).getTypeHandler(MyProperty.class))
                      .isInstanceOf(UnknownTypeHandler.class).isSameAs(context.getBean(TypeHandlerManager.class).getUnknownTypeHandler());
            });
  }

  @Test
  void versionIncrementStrategy() {
    class VersionIncrementStrategy0 implements VersionIncrementStrategy {

      @Override
      public @Nullable Object nextVersion(Object currentVersion) {
        return null;
      }
    }

    VersionIncrementStrategy versionIncrementStrategy = new VersionIncrementStrategy0();
    VersionIncrementStrategy strategy = versionIncrementStrategy.and(new DefaultVersionIncrementStrategy());

    contextRunner.withBean("versionIncrementStrategy", VersionIncrementStrategy.class, SupplierUtils.always(strategy))
            .run(context -> {
              assertThat(context).hasSingleBean(EntityManager.class);
              EntityManager entityManager = context.getBean(EntityManager.class);
              assertThat(entityManager).extracting("versionIncrementStrategy")
                      .isEqualTo(strategy);
            });
  }

  static class MyTypeHandlerManagerCustomizer implements TypeHandlerManagerCustomizer {

    @Override
    public void customize(TypeHandlerManager manager) {
      manager.register(new CustomTypeHandler());
    }
  }

  @MappedTypes(MyProperty.class)
  static class CustomTypeHandler implements TypeHandler<Integer> {

    @Override
    public void setParameter(PreparedStatement ps, int parameterIndex, @Nullable Integer arg) throws SQLException {

    }

    @Override
    public @Nullable Integer getResult(ResultSet rs, int columnIndex) throws SQLException {
      return 0;
    }
  }

  static class MyProperty {

  }

  @Table("context_entity")
  static class ContextEntity {

    @GeneratedId(generatorName = "customIdGenerator")
    public String id;
  }

}
