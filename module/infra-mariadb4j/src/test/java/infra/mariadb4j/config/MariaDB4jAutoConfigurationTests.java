package infra.mariadb4j.config;

import org.junit.jupiter.api.Test;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.config.AutoConfigurations;
import infra.mariadb4j.DB;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
    contextRunner.run(context -> assertFalse(context.getBeansOfType(DB.class).isEmpty()));
  }
}