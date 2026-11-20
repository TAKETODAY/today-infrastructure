package infra.test.context;

import org.junit.jupiter.api.Test;

import infra.context.ApplicationContextInitializer;
import infra.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

  @Test
  void resourcesReflectProcessedLocationsAndClasses() {
    ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(getClass());
    assertThat(attributes.hasResources()).isFalse();
    attributes.setLocations("classpath:test.xml");
    assertThat(attributes.hasLocations()).isTrue();
    assertThat(attributes.hasClasses()).isFalse();
    assertThat(attributes.hasResources()).isTrue();
    attributes.setLocations();
    attributes.setClasses(String.class);
    assertThat(attributes.hasLocations()).isFalse();
    assertThat(attributes.hasClasses()).isTrue();
    assertThat(attributes.hasResources()).isTrue();
    attributes.setClasses();
    assertThat(attributes.hasResources()).isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  void equalityIncludesEveryConfigurationAttribute() {
    ContextConfigurationAttributes attributes = attributes(getClass(), new String[0], new Class<?>[0],
            true, new Class[0], true, null, ContextLoader.class);
    assertThat(attributes).isEqualTo(attributes).isNotEqualTo(null).isNotEqualTo("other");
    ContextConfigurationAttributes same = new ContextConfigurationAttributes(getClass());
    assertThat(attributes).isEqualTo(same);
    assertThat(attributes.hashCode()).isEqualTo(same.hashCode());
    assertThat(attributes).isNotEqualTo(new ContextConfigurationAttributes(String.class));
    assertThat(attributes).isNotEqualTo(attributes(getClass(), new String[0], new Class<?>[] { String.class },
            true, new Class[0], true, null, ContextLoader.class));
    assertThat(attributes).isNotEqualTo(attributes(getClass(), new String[] { "test.xml" }, new Class<?>[0],
            true, new Class[0], true, null, ContextLoader.class));
    assertThat(attributes).isNotEqualTo(attributes(getClass(), new String[0], new Class<?>[0],
            false, new Class[0], true, null, ContextLoader.class));
    assertThat(attributes).isNotEqualTo(attributes(getClass(), new String[0], new Class<?>[0],
            true, new Class[] { ApplicationContextInitializer.class }, true, null, ContextLoader.class));
    assertThat(attributes).isNotEqualTo(attributes(getClass(), new String[0], new Class<?>[0],
            true, new Class[0], false, null, ContextLoader.class));
    assertThat(attributes).isNotEqualTo(attributes(getClass(), new String[0], new Class<?>[0],
            true, new Class[0], true, "named", ContextLoader.class));
    assertThat(attributes).isNotEqualTo(attributes(getClass(), new String[0], new Class<?>[0],
            true, new Class[0], true, null, AnnotationConfigContextLoader.class));
    assertThat(attributes).isEqualTo(attributes(getClass(), new String[0], new Class<?>[0],
            true, new Class[0], true, "  ", ContextLoader.class));
    assertThat(attributes.toString()).contains("declaringClass", "inheritLocations", "initializers",
            "inheritInitializers", "contextLoaderClass");
  }

  @Test
  void rejectsMissingDeclaringClass() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ContextConfigurationAttributes(null))
            .withMessage("'declaringClass' is required");
  }

  private ContextConfigurationAttributes attributes(Class<?> declaringClass, String[] locations,
          Class<?>[] classes, boolean inheritLocations, Class<? extends ApplicationContextInitializer>[] initializers,
          boolean inheritInitializers, String name, Class<? extends ContextLoader> loader) {
    return new ContextConfigurationAttributes(declaringClass, locations, classes, inheritLocations,
            initializers, inheritInitializers, name, loader);
  }

}
