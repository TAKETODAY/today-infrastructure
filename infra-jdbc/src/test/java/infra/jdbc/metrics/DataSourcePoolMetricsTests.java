package infra.jdbc.metrics;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;

import javax.sql.DataSource;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.jdbc.config.DataSourceAutoConfiguration;
import infra.jdbc.metadata.DataSourcePoolMetadataProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/5/7 19:46
 */
class DataSourcePoolMetricsTests {

  @Test
  void dataSourceIsInstrumented() {
    new ApplicationContextRunner().withUserConfiguration(DataSourceConfig.class, MetricsApp.class)
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withPropertyValues("datasource.generate-unique-name=true", "metrics.use-global-registry=false")
            .run((context) -> {
              context.getBean(DataSource.class).getConnection().getMetaData();
              context.getBean(MeterRegistry.class).get("jdbc.connections.max").meter();
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class MetricsApp {

    @Bean
    MeterRegistry registry() {
      return new SimpleMeterRegistry();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class DataSourceConfig {

    DataSourceConfig(DataSource dataSource, Collection<DataSourcePoolMetadataProvider> metadataProviders,
            MeterRegistry registry) {
      new DataSourcePoolMetrics(dataSource, metadataProviders, "data.source", Collections.emptyList())
              .bindTo(registry);
    }

  }

}