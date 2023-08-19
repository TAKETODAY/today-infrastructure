/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.io.EncodedResource;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.core.io.PropertySourceFactory;
import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the processing of @PropertySource annotations on @Configuration classes.
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 */
class PropertySourceAnnotationTests {

  @Test
  void withExplicitName() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithExplicitName.class);
    assertThat(ctx.getEnvironment().getPropertySources().contains("p1")).as("property source p1 was not added").isTrue();
    assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("p1TestBean");

    // assert that the property source was added last to the set of sources
    var sources = ctx.getEnvironment().getPropertySources();
    String name = sources.stream().toList().get(sources.size() - 1).getName();
    assertThat(name).isEqualTo("p1");
    ctx.close();
  }

  @Test
  void withImplicitName() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithImplicitName.class);
    String name = "class path resource [cn/taketoday/context/annotation/p1.properties]";
    assertThat(ctx.getEnvironment().getPropertySources().contains(name))
            .as("property source p1 was not added").isTrue();
    assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("p1TestBean");
    ctx.close();
  }

  @Test
  void withTestProfileBeans() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithTestProfileBeans.class);
    assertThat(ctx.containsBean("testBean")).isTrue();
    assertThat(ctx.containsBean("testProfileBean")).isTrue();
    ctx.close();
  }

  /**
   * Tests the LIFO behavior of @PropertySource annotations.
   * <p>The last one registered should 'win'.
   */
  @Test
  void orderingIsLifo() {
    {
      AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
      ctx.register(ConfigWithImplicitName.class, P2Config.class);
      ctx.refresh();
      // p2 should 'win' as it was registered last
      assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("p2TestBean");
      ctx.close();
    }

    {
      AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
      ctx.register(P2Config.class, ConfigWithImplicitName.class);
      ctx.refresh();
      // p1 should 'win' as it was registered last
      assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("p1TestBean");
      ctx.close();
    }
  }

  @Test
  void withCustomFactory() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithImplicitName.class, WithCustomFactory.class);
    ctx.refresh();
    assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("P2TESTBEAN");
    ctx.close();
  }

  @Test
  void withCustomFactoryAsMeta() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithImplicitName.class, WithCustomFactoryAsMeta.class);
    ctx.refresh();
    assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("P2TESTBEAN");
    ctx.close();
  }

  @Test
  void withUnresolvablePlaceholder() {
    assertThatExceptionOfType(BeanDefinitionStoreException.class)
            .isThrownBy(() -> new AnnotationConfigApplicationContext(ConfigWithUnresolvablePlaceholder.class))
            .withCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void withUnresolvablePlaceholderAndDefault() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithUnresolvablePlaceholderAndDefault.class);
    assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("p1TestBean");
    ctx.close();
  }

  @Test
  void withResolvablePlaceholder() {
    System.setProperty("path.to.properties", "cn/taketoday/context/annotation");
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithResolvablePlaceholder.class);
    assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("p1TestBean");
    System.clearProperty("path.to.properties");
    ctx.close();
  }

  @Test
  void withResolvablePlaceholderAndFactoryBean() {
    System.setProperty("path.to.properties", "cn/taketoday/context/annotation");
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithResolvablePlaceholderAndFactoryBean.class);
    assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("p1TestBean");
    System.clearProperty("path.to.properties");
    ctx.close();
  }

  @Test
  void withEmptyResourceLocations() {
    assertThatExceptionOfType(BeanDefinitionStoreException.class)
            .isThrownBy(() -> new AnnotationConfigApplicationContext(ConfigWithEmptyResourceLocations.class))
            .withCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void withNameAndMultipleResourceLocations() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithNameAndMultipleResourceLocations.class);
    assertEnvironmentContainsProperties(ctx, "from.p1", "from.p2");
    // p2 should 'win' as it was registered last
    assertEnvironmentProperty(ctx, "testbean.name", "p2TestBean");
    ctx.close();
  }

  @Test
  void withMultipleResourceLocations() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithMultipleResourceLocations.class);
    assertEnvironmentContainsProperties(ctx, "from.p1", "from.p2");
    // p2 should 'win' as it was registered last
    assertEnvironmentProperty(ctx, "testbean.name", "p2TestBean");
    ctx.close();
  }

  @Test
  void withRepeatedPropertySourcesInContainerAnnotation() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithPropertySources.class);
    assertEnvironmentContainsProperties(ctx, "from.p1", "from.p2");
    // p2 should 'win' as it was registered last
    assertEnvironmentProperty(ctx, "testbean.name", "p2TestBean");
    ctx.close();
  }

  @Test
  void withRepeatedPropertySources() {
    try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithRepeatedPropertySourceAnnotations.class)) {
      assertEnvironmentContainsProperties(ctx, "from.p1", "from.p2");
      // p2 should 'win' as it was registered last
      assertEnvironmentProperty(ctx, "testbean.name", "p2TestBean");
    }
  }

  @Test
  void withRepeatedPropertySourcesOnComposedAnnotation() {
    Class<?> configClass = ConfigWithRepeatedPropertySourceAnnotationsOnComposedAnnotation.class;
    String key = "custom.config.package";

    System.clearProperty(key);
    try (ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(configClass)) {
      assertEnvironmentContainsProperties(ctx, "from.p1", "from.p2");
      // p2 should 'win' as it was registered last
      assertEnvironmentProperty(ctx, "testbean.name", "p2TestBean");
    }

    System.setProperty(key, "cn/taketoday/context/annotation");
    try (ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(configClass)) {
      assertEnvironmentContainsProperties(ctx, "from.p1", "from.p2", "from.p3");
      // p3 should 'win' as it was registered last
      assertEnvironmentProperty(ctx, "testbean.name", "p3TestBean");
    }
    finally {
      System.clearProperty(key);
    }
  }

  @Test
  void multipleComposedPropertySourceAnnotations() {  // gh-30941
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(MultipleComposedAnnotationsConfig.class);
    ctx.getBean(MultipleComposedAnnotationsConfig.class);
    assertEnvironmentContainsProperties(ctx, "from.p1", "from.p2", "from.p3", "from.p4", "from.p5");
    // p5 should 'win' as it is registered via the last "locally declared" direct annotation
    assertEnvironmentProperty(ctx, "testbean.name", "p5TestBean");
    ctx.close();
  }

  @Test
  void multipleResourcesFromPropertySourcePattern() {  // gh-21325
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ResourcePatternConfig.class);
    ctx.getBean(ResourcePatternConfig.class);
    assertEnvironmentContainsProperties(ctx, "from.p1", "from.p2", "from.p3", "from.p4", "from.p5");
    ctx.close();
  }

  @Test
  void withNamedPropertySources() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithNamedPropertySources.class);
    assertEnvironmentContainsProperties(ctx, "from.p1", "from.p2");
    // p2 should 'win' as it was registered last
    assertEnvironmentProperty(ctx, "testbean.name", "p2TestBean");
    ctx.close();
  }

  @Test
  void withMissingPropertySource() {
    assertThatExceptionOfType(BeanDefinitionStoreException.class)
            .isThrownBy(() -> new AnnotationConfigApplicationContext(ConfigWithMissingPropertySource.class))
            .withCauseInstanceOf(FileNotFoundException.class);
  }

  @Test
  void withIgnoredPropertySource() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithIgnoredPropertySource.class);
    assertEnvironmentContainsProperties(ctx, "from.p1", "from.p2");
    ctx.close();
  }

  @Test
  void withSameSourceImportedInDifferentOrder() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithSameSourceImportedInDifferentOrder.class);
    assertEnvironmentContainsProperties(ctx, "from.p1", "from.p2");
    assertEnvironmentProperty(ctx, "testbean.name", "p2TestBean");
    ctx.close();
  }

  @Test
  void orderingWithAndWithoutNameAndMultipleResourceLocations() {
    // SPR-10820: p2 should 'win' as it was registered last
    AnnotationConfigApplicationContext ctxWithName = new AnnotationConfigApplicationContext(ConfigWithNameAndMultipleResourceLocations.class);
    AnnotationConfigApplicationContext ctxWithoutName = new AnnotationConfigApplicationContext(ConfigWithMultipleResourceLocations.class);
    assertEnvironmentProperty(ctxWithName, "testbean.name", "p2TestBean");
    assertEnvironmentProperty(ctxWithoutName, "testbean.name", "p2TestBean");
    ctxWithName.close();
    ctxWithoutName.close();
  }

  @Test
  void orderingWithFourResourceLocations() {
    // SPR-12198: p4 should 'win' as it was registered last
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithFourResourceLocations.class);
    assertEnvironmentProperty(ctx, "testbean.name", "p4TestBean");
    ctx.close();
  }

  @Test
  void orderingDoesntReplaceExisting() {
    // SPR-12198: mySource should 'win' as it was registered manually
    AnnotationConfigApplicationContext ctxWithoutName = new AnnotationConfigApplicationContext();
    MapPropertySource mySource = new MapPropertySource("mine", Map.of("testbean.name", "myTestBean"));
    ctxWithoutName.getEnvironment().getPropertySources().addLast(mySource);
    ctxWithoutName.register(ConfigWithFourResourceLocations.class);
    ctxWithoutName.refresh();
    assertEnvironmentProperty(ctxWithoutName, "testbean.name", "myTestBean");
    ctxWithoutName.close();
  }

  private static void assertEnvironmentContainsProperties(ApplicationContext ctx, String... names) {
    for (String name : names) {
      assertThat(ctx.getEnvironment().containsProperty(name)).as("environment contains property '%s'", name).isTrue();
    }
  }

  private static void assertEnvironmentProperty(ApplicationContext ctx, String name, Object value) {
    assertThat(ctx.getEnvironment().getProperty(name)).as("environment property '%s'", name).isEqualTo(value);
  }

  @Configuration
  @PropertySource("classpath:${unresolvable}/p1.properties")
  static class ConfigWithUnresolvablePlaceholder {
  }

  @Configuration
  @PropertySource("classpath:${unresolvable:cn/taketoday/context/annotation}/p1.properties")
  static class ConfigWithUnresolvablePlaceholderAndDefault {

    @Inject
    Environment env;

    @Bean
    TestBean testBean() {
      return new TestBean(env.getProperty("testbean.name"));
    }
  }

  @Configuration
  @PropertySource("classpath:${path.to.properties}/p1.properties")
  static class ConfigWithResolvablePlaceholder {

    @Inject
    Environment env;

    @Bean
    TestBean testBean() {
      return new TestBean(env.getProperty("testbean.name"));
    }
  }

  @Configuration
  @PropertySource("classpath:${path.to.properties}/p1.properties")
  static class ConfigWithResolvablePlaceholderAndFactoryBean {

    @Inject
    Environment env;

    @Bean
    FactoryBean<TestBean> testBean() {
      final String name = env.getProperty("testbean.name");
      return new FactoryBean<>() {
        @Override
        public TestBean getObject() {
          return new TestBean(name);
        }

        @Override
        public Class<?> getObjectType() {
          return TestBean.class;
        }

        @Override
        public boolean isSingleton() {
          return false;
        }
      };
    }
  }

  @Configuration
  @PropertySource(name = "p1", value = "classpath:cn/taketoday/context/annotation/p1.properties")
  static class ConfigWithExplicitName {

    @Inject
    Environment env;

    @Bean
    TestBean testBean() {
      return new TestBean(env.getProperty("testbean.name"));
    }
  }

  @Configuration
  @PropertySource("classpath:cn/taketoday/context/annotation/p1.properties")
  static class ConfigWithImplicitName {

    @Inject
    Environment env;

    @Bean
    TestBean testBean() {
      return new TestBean(env.getProperty("testbean.name"));
    }
  }

  @Configuration
  @PropertySource(name = "p1", value = "classpath:cn/taketoday/context/annotation/p1.properties")
  @ComponentScan("cn.taketoday.context.annotation.spr12111")
  static class ConfigWithTestProfileBeans {

    @Inject
    Environment env;

    @Bean
    @Profile("test")
    TestBean testBean() {
      return new TestBean(env.getProperty("testbean.name"));
    }
  }

  @Configuration
  @PropertySource("classpath:cn/taketoday/context/annotation/p2.properties")
  static class P2Config {
  }

  @Configuration
  @PropertySource(value = "classpath:cn/taketoday/context/annotation/p2.properties", factory = MyCustomFactory.class)
  static class WithCustomFactory {
  }

  @Configuration
  @MyPropertySource("classpath:cn/taketoday/context/annotation/p2.properties")
  static class WithCustomFactoryAsMeta {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @PropertySource(value = {}, factory = MyCustomFactory.class)
  @interface MyPropertySource {

    @AliasFor(annotation = PropertySource.class)
    String value();
  }

  static class MyCustomFactory implements PropertySourceFactory {

    @Override
    public cn.taketoday.core.env.PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
      Properties props = PropertiesUtils.loadProperties(resource);
      return new cn.taketoday.core.env.PropertySource<>("my" + name, props) {
        @Override
        public Object getProperty(String name) {
          String value = props.getProperty(name);
          return (value != null ? value.toUpperCase() : null);
        }
      };
    }
  }

  @Configuration
  @PropertySource(
          name = "psName",
          value = {
                  "classpath:cn/taketoday/context/annotation/p1.properties",
                  "classpath:cn/taketoday/context/annotation/p2.properties"
          })
  static class ConfigWithNameAndMultipleResourceLocations {
  }

  @Configuration
  @PropertySource({
          "classpath:cn/taketoday/context/annotation/p1.properties",
          "classpath:cn/taketoday/context/annotation/p2.properties"
  })
  static class ConfigWithMultipleResourceLocations {
  }

  @Configuration
  @PropertySources({
          @PropertySource("classpath:cn/taketoday/context/annotation/p1.properties"),
          @PropertySource("classpath:${base.package}/p2.properties")
  })
  static class ConfigWithPropertySources {
  }

  @Configuration
  @PropertySource("classpath:cn/taketoday/context/annotation/p1.properties")
  @PropertySource(value = "classpath:${base.package}/p2.properties", ignoreResourceNotFound = true)
  static class ConfigWithRepeatedPropertySourceAnnotations {
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Configuration
  @PropertySource("classpath:cn/taketoday/context/annotation/p1.properties")
  @PropertySource(value = "classpath:${base.package}/p2.properties", ignoreResourceNotFound = true)
  @PropertySource(value = "classpath:${custom.config.package:bogus/config}/p3.properties", ignoreResourceNotFound = true)
  @interface ComposedConfiguration {
  }

  @ComposedConfiguration
  static class ConfigWithRepeatedPropertySourceAnnotationsOnComposedAnnotation {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @PropertySource("classpath:cn/taketoday/context/annotation/p1.properties")
  @interface PropertySource1 {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @PropertySource("classpath:cn/taketoday/context/annotation/p2.properties")
  @PropertySources({
          @PropertySource("classpath:cn/taketoday/context/annotation/p3.properties"),
  })
  @interface PropertySource23 {
  }

  @Configuration
  @PropertySource1
  @PropertySource23
  @PropertySources({
          @PropertySource("classpath:cn/taketoday/context/annotation/p4.properties")
  })
  @PropertySource("classpath:cn/taketoday/context/annotation/p5.properties")
  static class MultipleComposedAnnotationsConfig {
  }

  @PropertySource("classpath*:cn/taketoday/context/annotation/p?.properties")
  static class ResourcePatternConfig {
  }

  @Configuration
  @PropertySources({
          @PropertySource(name = "psName", value = "classpath:cn/taketoday/context/annotation/p1.properties"),
          @PropertySource(name = "psName", value = "classpath:cn/taketoday/context/annotation/p2.properties"),
  })
  static class ConfigWithNamedPropertySources {
  }

  @Configuration
  @PropertySources({
          @PropertySource(name = "psName", value = "classpath:cn/taketoday/context/annotation/p1.properties"),
          @PropertySource(name = "psName", value = "classpath:cn/taketoday/context/annotation/missing.properties"),
          @PropertySource(name = "psName", value = "classpath:cn/taketoday/context/annotation/p2.properties")
  })
  static class ConfigWithMissingPropertySource {
  }

  @Configuration
  @PropertySources({
          @PropertySource(name = "psName", value = "classpath:cn/taketoday/context/annotation/p1.properties"),
          @PropertySource(name = "psName", value = "classpath:cn/taketoday/context/annotation/missing.properties", ignoreResourceNotFound = true),
          @PropertySource(name = "psName", value = "classpath:${myPath}/missing.properties", ignoreResourceNotFound = true),
          @PropertySource(name = "psName", value = "classpath:cn/taketoday/context/annotation/p2.properties")
  })
  static class ConfigWithIgnoredPropertySource {
  }

  @Configuration
  @PropertySource({})
  static class ConfigWithEmptyResourceLocations {
  }

  @Import(ConfigImportedWithSameSourceImportedInDifferentOrder.class)
  @PropertySources({
          @PropertySource("classpath:cn/taketoday/context/annotation/p1.properties"),
          @PropertySource("classpath:cn/taketoday/context/annotation/p2.properties")
  })
  @Configuration
  static class ConfigWithSameSourceImportedInDifferentOrder {
  }

  @Configuration
  @PropertySources({
          @PropertySource("classpath:cn/taketoday/context/annotation/p2.properties"),
          @PropertySource("classpath:cn/taketoday/context/annotation/p1.properties")
  })
  static class ConfigImportedWithSameSourceImportedInDifferentOrder {
  }

  @Configuration
  @PropertySource({
          "classpath:cn/taketoday/context/annotation/p1.properties",
          "classpath:cn/taketoday/context/annotation/p2.properties",
          "classpath:cn/taketoday/context/annotation/p3.properties",
          "classpath:cn/taketoday/context/annotation/p4.properties"
  })
  static class ConfigWithFourResourceLocations {
  }

}
