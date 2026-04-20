package infra.jdbc.config;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;
import java.util.UUID;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import infra.app.config.jmx.JmxAutoConfiguration;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.config.AutoConfigurations;
import infra.jdbc.datasource.LazyConnectionDataSourceProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/4/20 12:34
 */
class LazyConnectionDataSourceConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
          .withPropertyValues("datasource.url:jdbc:h2:mem:test-" + new Random().nextInt());

  @Test
  void autoConfigurationConfiguresLazyProxyWhenEnabled() {
    this.contextRunner
            .withPropertyValues("datasource.type=" + HikariDataSource.class.getName(),
                    "datasource.connection-fetch=lazy")
            .run((context) -> {
              assertThat(context).hasSingleBean(DataSource.class).hasSingleBean(LazyConnectionDataSourceProxy.class);
              DataSource dataSource = context.getBean(LazyConnectionDataSourceProxy.class);
              HikariDataSource actualDataSource = dataSource.unwrap(HikariDataSource.class);
              assertThat(actualDataSource.getJdbcUrl()).startsWith("jdbc:h2:mem:test-");
            });
  }

  @ParameterizedTest
  @ValueSource(strings = { "eager", "lazy" })
  void autoConfigurationExposeDataSourceMBeanWhenEnabled(String connectionFetchStrategy) {
    String uniqueDomain = UUID.randomUUID().toString();
    this.contextRunner.withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class))
            .withPropertyValues("infra.jmx.enabled=true", "infra.jmx.default-domain=" + uniqueDomain,
                    "datasource.type=" + HikariDataSource.class.getName(),
                    "datasource.connection-fetch=" + connectionFetchStrategy)
            .run((context) -> {
              MBeanServer mBeanServer = context.getBean(MBeanServer.class);
              ObjectName objectName = new ObjectName(
                      "%s:type=%s,name=dataSource".formatted(uniqueDomain, HikariDataSource.class.getSimpleName()));
              assertThat(mBeanServer.isRegistered(objectName)).isTrue();
            });
  }

  @Test
  void autoConfigurationBacksOffWhenPropertyIsNotSet() {
    this.contextRunner.withPropertyValues("datasource.type=" + HikariDataSource.class.getName())
            .run((context) -> assertThat(context).hasSingleBean(DataSource.class)
                    .hasSingleBean(HikariDataSource.class)
                    .doesNotHaveBean(LazyConnectionDataSourceProxy.class));
  }

  @Test
  void autoConfigurationDoesNotConfigureLazyProxyWhenEager() {
    this.contextRunner
            .withPropertyValues("datasource.type=" + HikariDataSource.class.getName(),
                    "datasource.connection-fetch=eager")
            .run((context) -> assertThat(context).hasSingleBean(DataSource.class)
                    .hasSingleBean(HikariDataSource.class)
                    .doesNotHaveBean(LazyConnectionDataSourceProxy.class));
  }

  @Test
  void autoConfigurationBacksOffWhenUserProvidesDataSource() {
    DataSource dataSource = mock(DataSource.class);
    this.contextRunner.withBean(DataSource.class, () -> dataSource)
            .withPropertyValues("datasource.connection-fetch=lazy")
            .run((context) -> {
              assertThat(context).hasSingleBean(DataSource.class)
                      .doesNotHaveBean(LazyConnectionDataSourceProxy.class);
              assertThat(context.getBean(DataSource.class)).isSameAs(dataSource);
            });
  }

}