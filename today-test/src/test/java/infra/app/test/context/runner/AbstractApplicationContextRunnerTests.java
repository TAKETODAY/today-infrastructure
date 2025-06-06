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

package infra.app.test.context.runner;

import com.google.gson.Gson;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.app.test.context.FilteredClassLoader;
import infra.app.test.context.assertj.ApplicationContextAssertProvider;
import infra.beans.factory.BeanCurrentlyInCreationException;
import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.support.BeanDefinitionOverrideException;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.annotation.Profile;
import infra.context.annotation.config.Configurations;
import infra.context.annotation.config.UserConfigurations;
import infra.context.properties.ConfigurationProperties;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.env.Environment;
import infra.core.type.AnnotatedTypeMetadata;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Abstract tests for {@link AbstractApplicationContextRunner} implementations.
 *
 * @param <T> The runner type
 * @param <C> the context type
 * @param <A> the assertable context type
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
abstract class AbstractApplicationContextRunnerTests<T extends AbstractApplicationContextRunner<T, C, A>, C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> {

  @Test
  void runWithInitializerShouldInitialize() {
    AtomicBoolean called = new AtomicBoolean();
    get().withInitializer((context) -> called.set(true)).run((context) -> {
    });
    assertThat(called).isTrue();
  }

  @Test
  void runWithSystemPropertiesShouldSetAndRemoveProperties() {
    String key = "test." + UUID.randomUUID();
    assertThat(System.getProperties()).doesNotContainKey(key);
    get().withSystemProperties(key + "=value")
            .run((context) -> assertThat(System.getProperties()).containsEntry(key, "value"));
    assertThat(System.getProperties()).doesNotContainKey(key);
  }

  @Test
  void runWithSystemPropertiesWhenContextFailsShouldRemoveProperties() {
    String key = "test." + UUID.randomUUID();
    assertThat(System.getProperties()).doesNotContainKey(key);
    get().withSystemProperties(key + "=value")
            .withUserConfiguration(FailingConfig.class)
            .run((context) -> assertThat(context).hasFailed());
    assertThat(System.getProperties()).doesNotContainKey(key);
  }

  @Test
  void runWithSystemPropertiesShouldRestoreOriginalProperties() {
    String key = "test." + UUID.randomUUID();
    System.setProperty(key, "value");
    try {
      assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
      get().withSystemProperties(key + "=newValue")
              .run((context) -> assertThat(System.getProperties()).containsEntry(key, "newValue"));
      assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
    }
    finally {
      System.clearProperty(key);
    }
  }

  @Test
  void runWithSystemPropertiesWhenValueIsNullShouldRemoveProperty() {
    String key = "test." + UUID.randomUUID();
    System.setProperty(key, "value");
    try {
      assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
      get().withSystemProperties(key + "=")
              .run((context) -> assertThat(System.getProperties()).doesNotContainKey(key));
      assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
    }
    finally {
      System.clearProperty(key);
    }
  }

  @Test
  void runWithMultiplePropertyValuesShouldAllAllValues() {
    get().withPropertyValues("test.foo=1").withPropertyValues("test.bar=2").run((context) -> {
      Environment environment = context.getEnvironment();
      assertThat(environment.getProperty("test.foo")).isEqualTo("1");
      assertThat(environment.getProperty("test.bar")).isEqualTo("2");
    });
  }

  @Test
  void runWithPropertyValuesWhenHasExistingShouldReplaceValue() {
    get().withPropertyValues("test.foo=1").withPropertyValues("test.foo=2").run((context) -> {
      Environment environment = context.getEnvironment();
      assertThat(environment.getProperty("test.foo")).isEqualTo("2");
    });
  }

  @Test
  void runWithConfigurationsShouldRegisterConfigurations() {
    get().withUserConfiguration(FooConfig.class).run((context) -> assertThat(context).hasBean("foo"));
  }

  @Test
  void runWithUserConfigurationsRegistersDefaultBeanName() {
    get().withUserConfiguration(FooConfig.class)
            .run((context) -> assertThat(context).hasBean("abstractApplicationContextRunnerTests.FooConfig"));
  }

  @Test
  void runWithUserConfigurationsWhenHasSameShortClassNamedRegistersWithoutBeanName() {
    get()
            .withUserConfiguration(infra.app.test.context.example.duplicate.first.EmptyConfig.class,
                    infra.app.test.context.example.duplicate.second.EmptyConfig.class)
            .run((context) -> assertThat(context.getStartupFailure())
                    .isInstanceOf(BeanDefinitionOverrideException.class));
  }

  @Test
  void runFullyQualifiedNameConfigurationsRegistersFullyQualifiedBeanName() {
    get().withConfiguration(FullyQualifiedNameConfigurations.of(FooConfig.class))
            .run((context) -> assertThat(context).hasBean(FooConfig.class.getName()));
  }

  @Test
  void runWithFullyQualifiedNameConfigurationsWhenHasSameShortClassNamedRegistersWithFullyQualifiedBeanName() {
    get().withConfiguration(FullyQualifiedNameConfigurations.of(
                    infra.app.test.context.example.duplicate.first.EmptyConfig.class,
                    infra.app.test.context.example.duplicate.second.EmptyConfig.class))
            .run((context) -> assertThat(context)
                    .hasSingleBean(infra.app.test.context.example.duplicate.first.EmptyConfig.class)
                    .hasSingleBean(infra.app.test.context.example.duplicate.second.EmptyConfig.class));
  }

  @Test
  void runWithUserNamedBeanShouldRegisterBean() {
    get().withBean("foo", String.class, () -> "foo").run((context) -> assertThat(context).hasBean("foo"));
  }

  @Test
  void runWithUserBeanShouldRegisterBeanWithDefaultName() {
    get().withBean(String.class, () -> "foo").run((context) -> assertThat(context).hasBean("string"));
  }

  @Test
  void runWithMultipleConfigurationsShouldRegisterAllConfigurations() {
    get().withUserConfiguration(FooConfig.class)
            .withConfiguration(UserConfigurations.of(BarConfig.class))
            .run((context) -> assertThat(context).hasBean("foo").hasBean("bar"));
  }

  @Test
  void runWithFailedContextShouldReturnFailedAssertableContext() {
    get().withUserConfiguration(FailingConfig.class).run((context) -> assertThat(context).hasFailed());
  }

  @Test
  void runWithClassLoaderShouldSetClassLoaderOnContext() {
    get().withClassLoader(new FilteredClassLoader(Gson.class.getPackage().getName()))
            .run((context) -> assertThatExceptionOfType(ClassNotFoundException.class)
                    .isThrownBy(() -> ClassUtils.forName(Gson.class.getName(), context.getClassLoader())));
  }

  @Test
  void runWithClassLoaderShouldSetClassLoaderOnConditionContext() {
    get().withClassLoader(new FilteredClassLoader(Gson.class.getPackage().getName()))
            .withUserConfiguration(ConditionalConfig.class)
            .run((context) -> assertThat(context).hasSingleBean(ConditionalConfig.class));
  }

  @Test
  void consecutiveRunWithFilteredClassLoaderShouldHaveBeanWithLazyProperties() {
    get().withClassLoader(new FilteredClassLoader(Gson.class))
            .withUserConfiguration(LazyConfig.class)
            .run((context) -> assertThat(context).hasSingleBean(ExampleBeanWithLazyProperties.class));

    get().withClassLoader(new FilteredClassLoader(Gson.class))
            .withUserConfiguration(LazyConfig.class)
            .run((context) -> assertThat(context).hasSingleBean(ExampleBeanWithLazyProperties.class));
  }

  @Test
  void thrownRuleWorksWithCheckedException() {
    get().run((context) -> assertThatIOException().isThrownBy(() -> throwCheckedException("Expected message"))
            .withMessageContaining("Expected message"));
  }

  @Test
  void runDisablesBeanOverridingByDefault() {
    get().withUserConfiguration(FooConfig.class).withBean("foo", Integer.class, () -> 42).run((context) -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure()).isInstanceOf(BeanDefinitionStoreException.class)
              .hasMessageContaining("Invalid bean definition with name 'foo'")
              .hasMessageContaining("@Component definition illegally overridden by existing bean definition");
    });
  }

  @Test
  void runDisablesCircularReferencesByDefault() {
    get().withUserConfiguration(ExampleConsumerConfiguration.class, ExampleProducerConfiguration.class)
            .run((context) -> {
              assertThat(context).hasFailed();
              assertThat(context).getFailure().hasRootCauseInstanceOf(BeanCurrentlyInCreationException.class);
            });
  }

  @Test
  void circularReferencesCanBeAllowed() {
    get().withAllowCircularReferences(true)
            .withUserConfiguration(ExampleConsumerConfiguration.class, ExampleProducerConfiguration.class)
            .run((context) -> assertThat(context).hasNotFailed());
  }

  @Test
  void runWithUserBeanShouldBeRegisteredInOrder() {
    get().withAllowBeanDefinitionOverriding(true)
            .withBean(String.class, () -> "one")
            .withBean(String.class, () -> "two")
            .withBean(String.class, () -> "three")
            .run((context) -> {
              assertThat(context).hasBean("string");
              assertThat(context.getBean("string")).isEqualTo("three");
            });
  }

  @Test
  void runWithConfigurationsAndUserBeanShouldRegisterUserBeanLast() {
    get().withAllowBeanDefinitionOverriding(true)
            .withUserConfiguration(FooConfig.class)
            .withBean("foo", String.class, () -> "overridden")
            .run((context) -> {
              assertThat(context).hasBean("foo");
              assertThat(context.getBean("foo")).isEqualTo("overridden");
            });
  }

  @Test
  void changesMadeByInitializersShouldBeVisibleToRegisteredClasses() {
    get().withInitializer((context) -> context.getEnvironment().setActiveProfiles("test"))
            .withUserConfiguration(ProfileConfig.class)
            .run((context) -> assertThat(context).hasSingleBean(ProfileConfig.class));
  }

  @Test
  void prepareDoesNotRefreshContext() {
    get().withUserConfiguration(FooConfig.class).prepare((context) -> {
      assertThatIllegalStateException().isThrownBy(() -> context.getBean(String.class))
              .withMessageContaining("not been refreshed");
      context.getSourceApplicationContext().refresh();
      assertThat(context.getBean(String.class)).isEqualTo("foo");
    });
  }

  protected abstract T get();

  private static void throwCheckedException(String message) throws IOException {
    throw new IOException(message);
  }

  @Configuration(proxyBeanMethods = false)
  static class FailingConfig {

    @Bean
    String foo() {
      throw new IllegalStateException("Failed");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class FooConfig {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class BarConfig {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(FilteredClassLoaderCondition.class)
  static class ConditionalConfig {

  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(ExampleProperties.class)
  static class LazyConfig {

    @Bean
    ExampleBeanWithLazyProperties exampleBeanWithLazyProperties() {
      return new ExampleBeanWithLazyProperties();
    }

  }

  static class ExampleBeanWithLazyProperties {

    @Autowired
    @Lazy
    ExampleProperties exampleProperties;

  }

  @ConfigurationProperties
  public static class ExampleProperties {

  }

  static class FilteredClassLoaderCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return context.getClassLoader() instanceof FilteredClassLoader;
    }

  }

  static class Example {

  }

  @FunctionalInterface
  interface ExampleConfigurer {

    void configure(Example example);

  }

  @Configuration(proxyBeanMethods = false)
  static class ExampleProducerConfiguration {

    @Bean
    Example example(ObjectProvider<ExampleConfigurer> configurers) {
      Example example = new Example();
      configurers.orderedStream().forEach((configurer) -> configurer.configure(example));
      return example;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ExampleConsumerConfiguration {

    @Autowired
    Example example;

    @Bean
    ExampleConfigurer configurer() {
      return (example) -> {
      };
    }

  }

  @Profile("test")
  @Configuration(proxyBeanMethods = false)
  static class ProfileConfig {

  }

  static class FullyQualifiedNameConfigurations extends Configurations {

    protected FullyQualifiedNameConfigurations(Collection<Class<?>> classes) {
      super(null, classes, Class::getName);
    }

    @Override
    protected Configurations merge(Set<Class<?>> mergedClasses) {
      return new FullyQualifiedNameConfigurations(mergedClasses);
    }

    static FullyQualifiedNameConfigurations of(Class<?>... classes) {
      return new FullyQualifiedNameConfigurations(List.of(classes));
    }

  }
}
