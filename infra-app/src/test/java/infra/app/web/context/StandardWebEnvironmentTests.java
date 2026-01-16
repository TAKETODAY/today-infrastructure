package infra.app.web.context;

import org.junit.jupiter.api.Test;

import infra.core.env.PropertySources;
import infra.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/13 15:50
 */
class StandardWebEnvironmentTests {

  @Test
  void shouldCreateStandardWebEnvironment() {
    // when
    StandardWebEnvironment environment = new StandardWebEnvironment();

    // then
    assertThat(environment).isNotNull();
    assertThat(environment).isInstanceOf(ConfigurableWebEnvironment.class);
    assertThat(environment).isInstanceOf(StandardEnvironment.class);
  }

  @Test
  void shouldCreateStandardWebEnvironmentWithPropertySources() {
    // given
    PropertySources propertySources = mock(PropertySources.class);

    // when
    StandardWebEnvironment environment = new StandardWebEnvironment(propertySources);

    // then
    assertThat(environment).isNotNull();
    assertThat(environment).isInstanceOf(ConfigurableWebEnvironment.class);
    assertThat(environment).isInstanceOf(StandardEnvironment.class);
  }

}