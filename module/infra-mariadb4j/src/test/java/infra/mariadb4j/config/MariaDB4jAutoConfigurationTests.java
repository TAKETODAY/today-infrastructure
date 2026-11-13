package infra.mariadb4j.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.mariadb4j.DBConfiguration;
import infra.mariadb4j.MariaDB;
import infra.mariadb4j.MariaDB4jLifecycle;
import infra.stereotype.Component;
import infra.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/22 09:24
 */
class MariaDB4jAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner =
          new ApplicationContextRunner()
                  .withConfiguration(AutoConfigurations.of(MariaDB4jAutoConfiguration.class));

  @Test
  public void shouldAutoConfigureEmbeddedMariaDB() {
    contextRunner
            .withPropertyValues("mariadb4j.socket=/tmp/mariadb.sock")
            .run(context -> {
              assertThat(context).hasSingleBean(MariaDB.class).hasSingleBean(MariaDB4jLifecycle.class);
              assertThat(context.getBean(MariaDB4jLifecycle.class).isRunning()).isTrue();
            });
  }

  @Test
  void disabledDoesNotConfigureDatabaseOrLifecycle() {
    try (var factory = mockStatic(MariaDB.class)) {
      contextRunner.withPropertyValues("mariadb4j.enabled=false").run(context -> {
        assertThat(context).doesNotHaveBean(MariaDB.class).doesNotHaveBean(MariaDB4jLifecycle.class);
      });
      factory.verifyNoInteractions();
    }
  }

  @Test
  void existingDatabasePreventsEmbeddedDatabaseCreation() throws Exception {
    MariaDB database = mock(MariaDB.class);
    try (var factory = mockStatic(MariaDB.class)) {
      contextRunner.withBean("customMariaDB", MariaDB.class, () -> database).run(context -> {
        assertThat(context).hasSingleBean(MariaDB.class).hasSingleBean(MariaDB4jLifecycle.class);
        assertThat(context.getBean(MariaDB.class)).isSameAs(database);
        context.getBean(MariaDB4jLifecycle.class).stop();
      });
      factory.verifyNoInteractions();
      verify(database).stop();
    }
  }

  @Test
  void mapsServerProperties() {
    MariaDB database = mock(MariaDB.class);
    List<DBConfiguration> configurations = new ArrayList<>();
    try (var factory = mockStatic(MariaDB.class)) {
      factory.when(() -> MariaDB.newEmbeddedDB(any(DBConfiguration.class))).thenAnswer(invocation -> {
        configurations.add(invocation.getArgument(0));
        return database;
      });
      contextRunner.withPropertyValues("mariadb4j.enabled=true", "mariadb4j.port=13306",
                      "mariadb4j.socket=/tmp/custom-mariadb.sock", "mariadb4j.default-charset=utf8mb4",
                      "mariadb4j.os-user=test-user", "mariadb4j.unpack=false")
              .run(context -> assertThat(context).hasSingleBean(MariaDB.class));

      assertThat(configurations).hasSize(1);
      DBConfiguration configuration = configurations.get(0);
      assertThat(configuration.getPort()).isEqualTo(13306);
      assertThat(configuration.getSocketFile().getPath()).isEqualTo("/tmp/custom-mariadb.sock");
      assertThat(configuration.getDefaultCharacterSet()).isEqualTo("utf8mb4");
      assertThat(configuration.getArgs()).contains("--user=test-user");
      assertThat(configuration.getBinariesClassPathLocation()).isNull();
    }
  }

  @Test
  void startsDatabaseBeforeCreatingDatabaseAndExecutingScript() throws Exception {
    MariaDB database = mock(MariaDB.class);
    try (var factory = mockStatic(MariaDB.class)) {
      factory.when(() -> MariaDB.newEmbeddedDB(any(DBConfiguration.class))).thenReturn(database);
      contextRunner.withPropertyValues("mariadb4j.create-database=application",
                      "mariadb4j.script-resource=classpath:init.sql")
              .run(context -> assertThat(context).hasSingleBean(MariaDB.class));

      var ordered = inOrder(database);
      ordered.verify(database).start();
      ordered.verify(database).createDB("application");
      ordered.verify(database).source("classpath:init.sql");
    }
  }

  @Test
  void blankDatabaseAndScriptAreIgnored() throws Exception {
    MariaDB database = mock(MariaDB.class);
    try (var factory = mockStatic(MariaDB.class)) {
      factory.when(() -> MariaDB.newEmbeddedDB(any(DBConfiguration.class))).thenReturn(database);
      contextRunner.withPropertyValues("mariadb4j.create-database= ", "mariadb4j.script-resource= ")
              .run(context -> assertThat(context).hasSingleBean(MariaDB.class));

      verify(database).start();
      verify(database, never()).createDB(anyString());
      verify(database, never()).source(anyString());
    }
  }

  @Test
  void initializesDatabaseBeforeUserDefinedDataSource() throws Exception {
    MariaDB database = mock(MariaDB.class);
    List<String> initialization = new ArrayList<>();
    willAnswer(invocation -> {
      initialization.add("mariadb");
      return null;
    }).given(database).start();
    try (var factory = mockStatic(MariaDB.class)) {
      factory.when(() -> MariaDB.newEmbeddedDB(any(DBConfiguration.class))).thenReturn(database);
      contextRunner.withBean("initialization", List.class, () -> initialization)
              .withUserConfiguration(DataSourceConfiguration.class)
              .run(context -> {
                assertThat(context).hasSingleBean(DataSource.class);
                assertThat(initialization).containsExactly("mariadb", "dataSource");
                assertThat(context.getBeanFactory().getBeanDefinition("dataSource").getDependsOn())
                        .contains("mariadb");
              });
    }
  }

  @Test
  void startupFailurePreventsDataSourceCreation() throws Exception {
    MariaDB database = mock(MariaDB.class);
    List<String> initialization = new ArrayList<>();
    willAnswer(invocation -> {
      throw new IllegalStateException("database startup failed");
    }).given(database).start();
    try (var factory = mockStatic(MariaDB.class)) {
      factory.when(() -> MariaDB.newEmbeddedDB(any(DBConfiguration.class))).thenReturn(database);
      contextRunner.withBean("initialization", List.class, () -> initialization)
              .withUserConfiguration(DataSourceConfiguration.class)
              .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasRootCauseMessage("database startup failed");
                assertThat(initialization).isEmpty();
              });
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class DataSourceConfiguration {

    @Component
    static DataSource dataSource(List<String> initialization) {
      initialization.add("dataSource");
      return mock(DataSource.class);
    }
  }
}
