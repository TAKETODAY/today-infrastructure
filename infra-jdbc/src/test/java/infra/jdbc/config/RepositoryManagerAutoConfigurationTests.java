package infra.jdbc.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.config.AutoConfigurations;
import infra.jdbc.RepositoryManager;
import infra.jdbc.type.MappedTypes;
import infra.jdbc.type.TypeHandler;
import infra.jdbc.type.TypeHandlerManager;
import infra.jdbc.type.UnknownTypeHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/28 20:28
 */
class RepositoryManagerAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withPropertyValues("datasource.generate-unique-name=true")
          .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
                  DataSourceTransactionManagerAutoConfiguration.class, RepositoryManagerAutoConfiguration.class));

  @Test
  void repositoryManagerWhenNoAvailableRepositoryManagerAutoConfigurationIsNotCreated() {
    new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(RepositoryManager.class));
  }

  @Test
  void repositoryManager() {
    this.contextRunner.run(context -> {
      assertThat(context).hasSingleBean(RepositoryManager.class);
      assertThat(context).hasSingleBean(TypeHandlerManager.class);
      assertThat(context).getBean(TypeHandlerManager.class).isSameAs(TypeHandlerManager.sharedInstance);
    });
  }

  @Test
  void repositoryManagerWithCustomRepositoryManagerIsNotCreated() {
    this.contextRunner.withBean("customRepositoryManager", RepositoryManager.class, () -> mock(RepositoryManager.class))
            .run(context -> {
              assertThat(context).hasSingleBean(RepositoryManager.class);
              assertThat(context).getBean(TypeHandlerManager.class).isSameAs(TypeHandlerManager.sharedInstance);
              assertThat(context.getBean(RepositoryManager.class)).isEqualTo(context.getBean("customRepositoryManager"));
            });
  }

  @Test
  void typeHandlers() {
    contextRunner.withBean("customTypeHandler", CustomTypeHandler.class, CustomTypeHandler::new)
            .run(context -> {
              assertThat(context).hasSingleBean(RepositoryManager.class);
              assertThat(context).getBean(RepositoryManager.class).extracting("typeHandlerManager")
                      .isEqualTo(TypeHandlerManager.sharedInstance);
              assertThat(context).getBean(TypeHandlerManager.class).isSameAs(TypeHandlerManager.sharedInstance);
              assertThat(context.getBean(TypeHandlerManager.class).getTypeHandler(MyProperty.class))
                      .isEqualTo(context.getBean("customTypeHandler"));
            });
  }

  @Test
  void customizers() {
    contextRunner.withBean("myTypeHandlerManagerCustomizer", MyTypeHandlerManagerCustomizer.class, MyTypeHandlerManagerCustomizer::new)
            .run(context -> {
              assertThat(context).hasSingleBean(RepositoryManager.class);
              assertThat(context.getBean(TypeHandlerManager.class).getTypeHandler(MyProperty.class))
                      .isInstanceOf(CustomTypeHandler.class);
            });
  }

  @Test
  void typeHandlerManagerBean() {
    contextRunner.withBean("typeHandlerManager", TypeHandlerManager.class, TypeHandlerManager::new)
            .withBean("customTypeHandler", CustomTypeHandler.class, CustomTypeHandler::new)
            .run(context -> {
              assertThat(context).hasSingleBean(RepositoryManager.class);
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