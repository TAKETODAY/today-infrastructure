/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.annotation.Value;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.TestBean;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Scope;
import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;
import infra.core.conversion.support.DefaultConversionService;
import infra.core.env.AbstractPropertyResolver;
import infra.core.env.EnumerablePropertySource;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.core.env.StandardEnvironment;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.core.testfixture.env.MockPropertySource;
import infra.lang.TodayStrategies;
import infra.util.PlaceholderResolutionException;
import infra.util.ReflectionUtils;

import static infra.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static infra.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static infra.core.env.AbstractPropertyResolver.DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/12 15:37
 */
class PropertySourcesPlaceholderConfigurerTests {

  @Test
  void replacementFromEnvironmentProperties() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    MockEnvironment env = new MockEnvironment();
    env.setProperty("my.name", "myValue");

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setEnvironment(env);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("myValue");
    assertThat(ppc.getAppliedPropertySources()).isNotNull();
  }

  /**
   * Ensure that a {@link Converter} registered in the {@link ConversionService}
   * used by the {@code Environment} is applied during placeholder resolution
   * against a {@link PropertySource} registered in the {@code Environment}.
   */
  @Test
  void replacementFromEnvironmentPropertiesWithConversion() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    record Point(int x, int y) {
    }

    Converter<Point, String> pointToStringConverter =
            point -> "(%d,%d)".formatted(point.x, point.y);

    DefaultConversionService conversionService = new DefaultConversionService();
    conversionService.addConverter(Point.class, String.class, pointToStringConverter);

    MockEnvironment env = new MockEnvironment();
    env.setConversionService(conversionService);
    env.setProperty("my.name", new Point(4, 5));

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setEnvironment(env);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("(4,5)");
  }

  /**
   * Ensure that a {@link PropertySource} added to the {@code Environment} after context
   * refresh (i.e., after {@link PropertySourcesPlaceholderConfigurer#postProcessBeanFactory(ConfigurableBeanFactory)}
   * has been invoked) can still contribute properties in late-binding scenarios.
   */
  @Test
  void replacementFromEnvironmentPropertiesWithLateBinding() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    PropertySources propertySources = context.getEnvironment().getPropertySources();
    propertySources.addFirst(new MockPropertySource("early properties").withProperty("foo", "bar"));

    context.register(PropertySourcesPlaceholderConfigurer.class);
    context.register(PrototypeBean.class);
    context.refresh();

    // Verify that placeholder resolution works for early binding.
    PrototypeBean prototypeBean = context.getBean(PrototypeBean.class);
    assertThat(prototypeBean.getName()).isEqualTo("bar");
    assertThat(prototypeBean.isJedi()).isFalse();

    // Add new PropertySource after context refresh.
    propertySources.addFirst(new MockPropertySource("late properties").withProperty("jedi", "true"));

    // Verify that placeholder resolution works for late binding: isJedi() switches to true.
    prototypeBean = context.getBean(PrototypeBean.class);
    assertThat(prototypeBean.getName()).isEqualTo("bar");
    assertThat(prototypeBean.isJedi()).isTrue();

    // Add yet another PropertySource after context refresh.
    propertySources.addFirst(new MockPropertySource("even later properties").withProperty("foo", "enigma"));

    // Verify that placeholder resolution works for even later binding: getName() switches to enigma.
    prototypeBean = context.getBean(PrototypeBean.class);
    assertThat(prototypeBean.getName()).isEqualTo("enigma");
    assertThat(prototypeBean.isJedi()).isTrue();
  }

  @Test
  void localPropertiesViaResource() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    Resource resource = new ClassPathResource("PropertySourcesPlaceholderConfigurerTests.properties", getClass());
    ppc.setLocation(resource);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("foo");
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void localPropertiesOverride(boolean override) {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${foo}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();

    ppc.setLocalOverride(override);
    ppc.setProperties(new Properties() {{
      setProperty("foo", "local");
    }});
    ppc.setEnvironment(new MockEnvironment().withProperty("foo", "enclosing"));
    ppc.postProcessBeanFactory(bf);
    if (override) {
      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("local");
    }
    else {
      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("enclosing");
    }
  }

  @Test
  void explicitPropertySources() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySources propertySources = new PropertySources();
    propertySources.addLast(new MockPropertySource().withProperty("my.name", "foo"));

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setPropertySources(propertySources);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("foo");
    assertThat(propertySources).containsExactlyElementsOf(ppc.getAppliedPropertySources());
  }

  @Test
  void explicitPropertySourcesExcludesEnvironment() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySources propertySources = new PropertySources();
    propertySources.addLast(new MockPropertySource());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setPropertySources(propertySources);
    ppc.setEnvironment(new MockEnvironment().withProperty("my.name", "env"));
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${my.name}");
    assertThat(propertySources).containsExactlyElementsOf(ppc.getAppliedPropertySources());
  }

  @Test
  @SuppressWarnings("serial")
  public void explicitPropertySourcesExcludesLocalProperties() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySources propertySources = new PropertySources();
    propertySources.addLast(new MockPropertySource());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setPropertySources(propertySources);
    ppc.setProperties(new Properties() {{
      put("my.name", "local");
    }});
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${my.name}");
  }

  @Test
  void ignoreUnresolvablePlaceholders_falseIsDefault() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    //pc.setIgnoreUnresolvablePlaceholders(false); // the default
    assertThatExceptionOfType(BeanDefinitionStoreException.class)
            .isThrownBy(() -> ppc.postProcessBeanFactory(bf))
            .havingCause()
            .isExactlyInstanceOf(PlaceholderResolutionException.class)
            .withMessage("Could not resolve placeholder 'my.name' in value \"${my.name}\"");
  }

  @Test
  void ignoreUnresolvablePlaceholders_true() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${my.name}");
  }

  @Test
  // https://github.com/spring-projects/spring-framework/issues/27947
  public void ignoreUnresolvablePlaceholdersInAtValueAnnotation__falseIsDefault() {
    MockPropertySource mockPropertySource = new MockPropertySource("test");
    mockPropertySource.setProperty("my.key", "${enigma}");
    @SuppressWarnings("resource")
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getPropertySources().addLast(mockPropertySource);
    context.register(IgnoreUnresolvablePlaceholdersFalseConfig.class);

    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(context::refresh)
            .havingCause()
            .isExactlyInstanceOf(PlaceholderResolutionException.class)
            .withMessage("Could not resolve placeholder 'enigma' in value \"${enigma}\" <-- \"${my.key}\"");
  }

  @Test
  // https://github.com/spring-projects/spring-framework/issues/27947
  public void ignoreUnresolvablePlaceholdersInAtValueAnnotation_true() {
    MockPropertySource mockPropertySource = new MockPropertySource("test");
    mockPropertySource.setProperty("my.key", "${enigma}");
    @SuppressWarnings("resource")
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getPropertySources().addLast(mockPropertySource);
    context.register(IgnoreUnresolvablePlaceholdersTrueConfig.class);
    context.refresh();

    IgnoreUnresolvablePlaceholdersTrueConfig config = context.getBean(IgnoreUnresolvablePlaceholdersTrueConfig.class);
    assertThat(config.value).isEqualTo("${enigma}");
  }

  @Test
  @SuppressWarnings("serial")
  public void nestedUnresolvablePlaceholder() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setProperties(new Properties() {{
      put("my.name", "${bogus}");
    }});
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
            ppc.postProcessBeanFactory(bf));
  }

  @Test
  @SuppressWarnings("serial")
  public void ignoredNestedUnresolvablePlaceholder() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setProperties(new Properties() {{
      put("my.name", "${bogus}");
    }});
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${bogus}");
  }

  @Test
  void withNonEnumerablePropertySource() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${foo}")
                    .getBeanDefinition());

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();

    PropertySource<?> ps = new PropertySource<>("simplePropertySource", new Object()) {
      @Override
      public Object getProperty(String key) {
        return "bar";
      }
    };

    MockEnvironment env = new MockEnvironment();
    env.getPropertySources().addFirst(ps);
    ppc.setEnvironment(env);

    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("bar");
  }

  @Test
  void withEnumerableAndNonEnumerablePropertySourcesInTheEnvironmentAndLocalProperties() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${foo:bogus}")
                    .addPropertyValue("jedi", "${local:false}")
                    .getBeanDefinition());

    // 1) MockPropertySource is an EnumerablePropertySource.
    MockPropertySource mockPropertySource = new MockPropertySource("mockPropertySource")
            .withProperty("foo", "${bar}");

    // 2) PropertySource is not an EnumerablePropertySource.
    PropertySource<?> rawPropertySource = new PropertySource<>("rawPropertySource", new Object()) {
      @Override
      public Object getProperty(String key) {
        return ("bar".equals(key) ? "quux" : null);
      }
    };

    MockEnvironment env = new MockEnvironment();
    env.getPropertySources().addFirst(mockPropertySource);
    env.getPropertySources().addLast(rawPropertySource);

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setEnvironment(env);
    // 3) Local properties are stored in a PropertiesPropertySource which is an EnumerablePropertySource.
    ppc.setProperties(new Properties() {{
      setProperty("local", "true");
    }});
    ppc.postProcessBeanFactory(bf);

    // Verify all properties can be resolved via the Environment.
    assertThat(env.getProperty("foo")).isEqualTo("quux");
    assertThat(env.getProperty("bar")).isEqualTo("quux");

    // Verify that placeholder resolution works.
    TestBean testBean = bf.getBean(TestBean.class);
    assertThat(testBean.getName()).isEqualTo("quux");
    assertThat(testBean.isJedi()).isTrue();

    // Verify that the presence of a non-EnumerablePropertySource does not prevent
    // accessing EnumerablePropertySources via getAppliedPropertySources().
    List<String> propertyNames = new ArrayList<>();
    for (PropertySource<?> propertySource : ppc.getAppliedPropertySources()) {
      if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource) {
        Collections.addAll(propertyNames, enumerablePropertySource.getPropertyNames());
      }
    }
    // Should not contain "foo" or "bar" from the Environment.
    assertThat(propertyNames).containsOnly("local");
  }

  @Test
  void customPlaceholderPrefixAndSuffix() {
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setPlaceholderPrefix("@<");
    ppc.setPlaceholderSuffix(">");

    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            rootBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "@<key1>")
                    .addPropertyValue("sex", "${key2}")
                    .getBeanDefinition());

    System.setProperty("key1", "systemKey1Value");
    System.setProperty("key2", "systemKey2Value");
    ppc.setEnvironment(new StandardEnvironment());
    ppc.postProcessBeanFactory(bf);
    System.clearProperty("key1");
    System.clearProperty("key2");

    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("systemKey1Value");
    assertThat(bf.getBean(TestBean.class).getSex()).isEqualTo("${key2}");
  }

  @Test
  void nullValueIsPreserved() {
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setNullValue("customNull");
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
            .addPropertyValue("name", "${my.name}")
            .getBeanDefinition());
    ppc.setEnvironment(new MockEnvironment().withProperty("my.name", "customNull"));
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isNull();
  }

  @Test
  void trimValuesIsOffByDefault() {
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
            .addPropertyValue("name", "${my.name}")
            .getBeanDefinition());
    ppc.setEnvironment(new MockEnvironment().withProperty("my.name", " myValue  "));
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo(" myValue  ");
  }

  @Test
  void trimValuesIsApplied() {
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setTrimValues(true);
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
            .addPropertyValue("name", "${my.name}")
            .getBeanDefinition());
    ppc.setEnvironment(new MockEnvironment().withProperty("my.name", " myValue  "));
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("myValue");
  }

  @Test
  void getAppliedPropertySourcesTooEarly() {
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    assertThatIllegalStateException().isThrownBy(
            ppc::getAppliedPropertySources);
  }

  @Test
  void multipleLocationsWithDefaultResolvedValue() {
    // SPR-10619
    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ClassPathResource doesNotHave = new ClassPathResource("test.properties", getClass());
    ClassPathResource setToTrue = new ClassPathResource("placeholder.properties", getClass());
    ppc.setLocations(doesNotHave, setToTrue);
    ppc.setIgnoreResourceNotFound(true);
    ppc.setIgnoreUnresolvablePlaceholders(true);
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("jedi", "${jedi:false}")
                    .getBeanDefinition());
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).isJedi()).isTrue();
  }

  @Test
  void optionalPropertyWithValue() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setConversionService(new DefaultConversionService());
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(OptionalTestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    MockEnvironment env = new MockEnvironment();
    env.setProperty("my.name", "myValue");

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setEnvironment(env);
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(OptionalTestBean.class).getName()).contains("myValue");
  }

  @Test
  void optionalPropertyWithoutValue() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setConversionService(new DefaultConversionService());
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(OptionalTestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    MockEnvironment env = new MockEnvironment();
    env.setProperty("my.name", "");

    PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
    ppc.setEnvironment(env);
    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.setNullValue("");
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(OptionalTestBean.class).getName()).isNotPresent();
  }

  /**
   * Tests that use the escape character (or disable it) with nested placeholder
   * resolution.
   */
  @Nested
  class EscapedNestedPlaceholdersTests {

    @Test
    void singleEscapeWithDefaultEscapeCharacter() {
      MockEnvironment env = new MockEnvironment()
              .withProperty("user.home", "admin")
              .withProperty("my.property", "\\DOMAIN\\${user.home}");

      StandardBeanFactory bf = createBeanFactory();
      PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
      ppc.setEnvironment(env);
      ppc.postProcessBeanFactory(bf);

      // \DOMAIN\${user.home} resolves to \DOMAIN${user.home} instead of \DOMAIN\admin
      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("\\DOMAIN${user.home}");
    }

    @Test
    void singleEscapeWithCustomEscapeCharacter() {
      MockEnvironment env = new MockEnvironment()
              .withProperty("user.home", "admin\\~${nested}")
              .withProperty("my.property", "DOMAIN\\${user.home}\\~${enigma}");

      StandardBeanFactory bf = createBeanFactory();
      PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
      ppc.setEnvironment(env);
      // Set custom escape character.
      ppc.setEscapeCharacter('~');
      ppc.postProcessBeanFactory(bf);

      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("DOMAIN\\admin\\${nested}\\${enigma}");
    }

    @Test
    void singleEscapeWithEscapeCharacterDisabled() {
      MockEnvironment env = new MockEnvironment()
              .withProperty("user.home", "admin\\")
              .withProperty("my.property", "\\DOMAIN\\${user.home}");

      StandardBeanFactory bf = createBeanFactory();
      PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
      ppc.setEnvironment(env);
      // Disable escape character.
      ppc.setEscapeCharacter(null);
      ppc.postProcessBeanFactory(bf);

      // \DOMAIN\${user.home} resolves to \DOMAIN\admin
      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("\\DOMAIN\\admin\\");
    }

    @Test
    void tripleEscapeWithDefaultEscapeCharacter() {
      MockEnvironment env = new MockEnvironment()
              .withProperty("user.home", "admin\\\\\\")
              .withProperty("my.property", "DOMAIN\\\\\\${user.home}#${user.home}");

      StandardBeanFactory bf = createBeanFactory();
      PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
      ppc.setEnvironment(env);
      ppc.postProcessBeanFactory(bf);

      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("DOMAIN\\\\${user.home}#admin\\\\\\");
    }

    @Test
    void tripleEscapeWithCustomEscapeCharacter() {
      MockEnvironment env = new MockEnvironment()
              .withProperty("user.home", "admin\\~${enigma}")
              .withProperty("my.property", "DOMAIN~~~${user.home}#${user.home}");

      StandardBeanFactory bf = createBeanFactory();
      PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
      ppc.setEnvironment(env);
      // Set custom escape character.
      ppc.setEscapeCharacter('~');
      ppc.postProcessBeanFactory(bf);

      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("DOMAIN~~${user.home}#admin\\${enigma}");
    }

    @Test
    void singleEscapeWithDefaultEscapeCharacterAndIgnoreUnresolvablePlaceholders() {
      MockEnvironment env = new MockEnvironment()
              .withProperty("user.home", "${enigma}")
              .withProperty("my.property", "\\${DOMAIN}${user.home}");

      StandardBeanFactory bf = createBeanFactory();
      PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
      ppc.setEnvironment(env);
      ppc.setIgnoreUnresolvablePlaceholders(true);
      ppc.postProcessBeanFactory(bf);

      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${DOMAIN}${enigma}");
    }

    @Test
    void singleEscapeWithCustomEscapeCharacterAndIgnoreUnresolvablePlaceholders() {
      MockEnvironment env = new MockEnvironment()
              .withProperty("user.home", "${enigma}")
              .withProperty("my.property", "~${DOMAIN}\\${user.home}");

      StandardBeanFactory bf = createBeanFactory();
      PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
      ppc.setEnvironment(env);
      // Set custom escape character.
      ppc.setEscapeCharacter('~');
      ppc.setIgnoreUnresolvablePlaceholders(true);
      ppc.postProcessBeanFactory(bf);

      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${DOMAIN}\\${enigma}");
    }

    @Test
    void tripleEscapeWithDefaultEscapeCharacterAndIgnoreUnresolvablePlaceholders() {
      MockEnvironment env = new MockEnvironment()
              .withProperty("user.home", "${enigma}")
              .withProperty("my.property", "X:\\\\\\${DOMAIN}${user.home}");

      StandardBeanFactory bf = createBeanFactory();
      PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
      ppc.setEnvironment(env);
      ppc.setIgnoreUnresolvablePlaceholders(true);
      ppc.postProcessBeanFactory(bf);

      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("X:\\\\${DOMAIN}${enigma}");
    }

    private static StandardBeanFactory createBeanFactory() {
      BeanDefinition beanDefinition = genericBeanDefinition(TestBean.class)
              .addPropertyValue("name", "${my.property}")
              .getBeanDefinition();
      StandardBeanFactory bf = new StandardBeanFactory();
      bf.registerBeanDefinition("testBean", beanDefinition);
      return bf;
    }

  }

  /**
   * Tests that globally set the default escape character (or disable it) and
   * rely on nested placeholder resolution.
   */
  @Nested
  class GlobalDefaultEscapeCharacterTests {

    private static final Field defaultEscapeCharacterField =
            ReflectionUtils.findField(AbstractPropertyResolver.class, "defaultEscapeCharacter");

    static {
      ReflectionUtils.makeAccessible(defaultEscapeCharacterField);
    }

    @BeforeEach
    void resetStateBeforeEachTest() {
      resetState();
    }

    @AfterAll
    static void resetState() {
      ReflectionUtils.setField(defaultEscapeCharacterField, null, Character.MIN_VALUE);
      setInfraProperty(null);
    }

    @Test
    void defaultEscapeCharacterSetToXyz() {
      setInfraProperty("XYZ");

      assertThatIllegalArgumentException()
              .isThrownBy(PropertySourcesPlaceholderConfigurer::new)
              .withMessage("Value [XYZ] for property [%s] must be a single character or an empty string",
                      DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME);
    }

    @Test
    void defaultEscapeCharacterDisabled() {
      setInfraProperty("");

      MockEnvironment env = new MockEnvironment()
              .withProperty("user.home", "admin")
              .withProperty("my.property", "\\DOMAIN\\${user.home}");

      StandardBeanFactory bf = createBeanFactory();
      PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
      ppc.setEnvironment(env);
      ppc.postProcessBeanFactory(bf);

      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("\\DOMAIN\\admin");
    }

    @Test
    void defaultEscapeCharacterSetToBackslash() {
      setInfraProperty("\\");

      MockEnvironment env = new MockEnvironment()
              .withProperty("user.home", "admin")
              .withProperty("my.property", "\\DOMAIN\\${user.home}");

      StandardBeanFactory bf = createBeanFactory();
      PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
      ppc.setEnvironment(env);
      ppc.postProcessBeanFactory(bf);

      // \DOMAIN\${user.home} resolves to \DOMAIN${user.home} instead of \DOMAIN\admin
      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("\\DOMAIN${user.home}");
    }

    @Test
    void defaultEscapeCharacterSetToTilde() {
      setInfraProperty("~");

      MockEnvironment env = new MockEnvironment()
              .withProperty("user.home", "admin\\~${nested}")
              .withProperty("my.property", "DOMAIN\\${user.home}\\~${enigma}");

      StandardBeanFactory bf = createBeanFactory();
      PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
      ppc.setEnvironment(env);
      ppc.postProcessBeanFactory(bf);

      assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("DOMAIN\\admin\\${nested}\\${enigma}");
    }

    private static void setInfraProperty(String value) {
      TodayStrategies.setProperty(DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME, value);
    }

    private static StandardBeanFactory createBeanFactory() {
      BeanDefinition beanDefinition = genericBeanDefinition(TestBean.class)
              .addPropertyValue("name", "${my.property}")
              .getBeanDefinition();
      StandardBeanFactory bf = new StandardBeanFactory();
      bf.registerBeanDefinition("testBean", beanDefinition);
      return bf;
    }

  }

  private static class OptionalTestBean {

    private Optional<String> name;

    public Optional<String> getName() {
      return name;
    }

    @SuppressWarnings("unused")
    public void setName(Optional<String> name) {
      this.name = name;
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class IgnoreUnresolvablePlaceholdersFalseConfig {

    @Value("${my.key}")
    String value;

    @Bean
    static PropertySourcesPlaceholderConfigurer pspc() {
      return new PropertySourcesPlaceholderConfigurer();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class IgnoreUnresolvablePlaceholdersTrueConfig {

    @Value("${my.key}")
    String value;

    @Bean
    static PropertySourcesPlaceholderConfigurer pspc() {
      PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
      pspc.setIgnoreUnresolvablePlaceholders(true);
      return pspc;
    }
  }

  @Scope(BeanDefinition.SCOPE_PROTOTYPE)
  static class PrototypeBean {

    @Value("${foo:bogus}")
    private String name;

    @Value("${jedi:false}")
    private boolean jedi;

    public String getName() {
      return this.name;
    }

    public boolean isJedi() {
      return this.jedi;
    }
  }

}
