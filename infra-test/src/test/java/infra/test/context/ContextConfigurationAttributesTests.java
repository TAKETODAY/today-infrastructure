package infra.test.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/30 11:41
 */
class ContextConfigurationAttributesTests {

  @Test
  void defaultsConstructor() {
    var configAttributes = new ContextConfigurationAttributes(getClass());

    assertThat(configAttributes.getDeclaringClass()).isEqualTo(getClass());
    assertThat(configAttributes.getClasses()).isEmpty();
    assertThat(configAttributes.getLocations()).isEmpty();
    assertThat(configAttributes.getInitializers()).isEmpty();
    assertThat(configAttributes.getContextLoaderClass()).isEqualTo(ContextLoader.class);
    assertThat(configAttributes.isInheritInitializers()).isTrue();
    assertThat(configAttributes.isInheritLocations()).isTrue();
  }

}