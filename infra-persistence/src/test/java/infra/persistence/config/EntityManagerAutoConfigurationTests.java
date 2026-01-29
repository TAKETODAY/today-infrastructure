package infra.persistence.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.config.AutoConfigurations;
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
import infra.persistence.PropertyFilter;

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
      assertThat(context).hasSingleBean(TypeHandlerManager.class);
      assertThat(context).getBean(TypeHandlerManager.class).isSameAs(TypeHandlerManager.sharedInstance);
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
                    () -> manager -> manager.setAutoGenerateId(false))
            .run(context -> {
              assertThat(context).hasSingleBean(EntityManager.class);
              assertThat(context.getBean(EntityManager.class))
                      .extracting("autoGenerateId").isEqualTo(false);
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

}