package infra.transaction.config;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/29 17:03
 */
class TransactionManagerCustomizationAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(TransactionManagerCustomizationAutoConfiguration.class));

  @Test
  void autoConfiguresTransactionManagerCustomizers() {
    this.contextRunner.withPropertyValues("transaction.default-timeout=30s").run((context) -> {
      TransactionManagerCustomizers customizers = context.getBean(TransactionManagerCustomizers.class);
      assertThat(customizers).extracting("customizers")
              .asInstanceOf(InstanceOfAssertFactories.LIST)
              .hasSize(2)
              .hasAtLeastOneElementOfType(TransactionProperties.class)
              .hasAtLeastOneElementOfType(ExecutionListenersTransactionManagerCustomizer.class);
    });
  }

  @Test
  void autoConfiguredTransactionManagerCustomizersBacksOff() {
    this.contextRunner.withUserConfiguration(CustomTransactionManagerCustomizersConfiguration.class)
            .run((context) -> {
              TransactionManagerCustomizers customizers = context.getBean(TransactionManagerCustomizers.class);
              assertThat(customizers).extracting("customizers")
                      .asInstanceOf(InstanceOfAssertFactories.LIST)
                      .isEmpty();
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomTransactionManagerCustomizersConfiguration {

    @Bean
    TransactionManagerCustomizers customTransactionManagerCustomizers() {
      return new TransactionManagerCustomizers(Collections.<TransactionManagerCustomizer<?>>emptyList());
    }

  }

}