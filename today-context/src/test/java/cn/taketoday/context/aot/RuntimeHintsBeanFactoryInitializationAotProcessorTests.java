/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.context.aot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.ResourceBundleHint;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.factory.aot.AotProcessingException;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ImportRuntimeHints;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link RuntimeHintsBeanFactoryInitializationAotProcessor}.
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 */
class RuntimeHintsBeanFactoryInitializationAotProcessorTests {

  private GenerationContext generationContext;

  private ApplicationContextAotGenerator generator;

  @BeforeEach
  void setup() {
    this.generationContext = new TestGenerationContext();
    this.generator = new ApplicationContextAotGenerator();
  }

  @Test
  void shouldProcessRegistrarOnConfiguration() {
    GenericApplicationContext applicationContext = createApplicationContext(
            ConfigurationWithHints.class);
    this.generator.processAheadOfTime(applicationContext,
            this.generationContext);
    assertThatSampleRegistrarContributed();
  }

  @Test
  void shouldProcessRegistrarsOnInheritedConfiguration() {
    GenericApplicationContext applicationContext = createApplicationContext(
            ExtendedConfigurationWithHints.class);
    this.generator.processAheadOfTime(applicationContext,
            this.generationContext);
    assertThatInheritedSampleRegistrarContributed();
  }

  @Test
  void shouldProcessRegistrarOnBeanMethod() {
    GenericApplicationContext applicationContext = createApplicationContext(
            ConfigurationWithBeanDeclaringHints.class);
    this.generator.processAheadOfTime(applicationContext,
            this.generationContext);
    assertThatSampleRegistrarContributed();
  }

  @Test
  void shouldProcessRegistrarInInfraFactory() {
    GenericApplicationContext applicationContext = createApplicationContext();
    applicationContext.setClassLoader(
            new TestFactoriesClassLoader("test-runtime-hints-aot.factories"));
    this.generator.processAheadOfTime(applicationContext,
            this.generationContext);
    assertThatSampleRegistrarContributed();
  }

  @Test
  void shouldProcessDuplicatedRegistrarsOnlyOnce() {
    GenericApplicationContext applicationContext = createApplicationContext();
    applicationContext.registerBeanDefinition("incremental1",
            new RootBeanDefinition(ConfigurationWithIncrementalHints.class));
    applicationContext.registerBeanDefinition("incremental2",
            new RootBeanDefinition(ConfigurationWithIncrementalHints.class));
    applicationContext.setClassLoader(
            new TestFactoriesClassLoader("test-duplicated-runtime-hints-aot.factories"));
    IncrementalRuntimeHintsRegistrar.counter.set(0);
    this.generator.processAheadOfTime(applicationContext,
            this.generationContext);
    RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();
    assertThat(runtimeHints.resources().resourceBundleHints().map(ResourceBundleHint::getBaseName))
            .containsOnly("com.example.example0", "sample");
    assertThat(IncrementalRuntimeHintsRegistrar.counter.get()).isEqualTo(1);
  }

  @Test
  void shouldRejectRuntimeHintsRegistrarWithoutDefaultConstructor() {
    GenericApplicationContext applicationContext = createApplicationContext(
            ConfigurationWithIllegalRegistrar.class);
    assertThatExceptionOfType(AotProcessingException.class)
            .isThrownBy(() -> this.generator.processAheadOfTime(applicationContext, this.generationContext))
            .havingCause().isInstanceOf(BeanInstantiationException.class);
  }

  private void assertThatSampleRegistrarContributed() {
    Stream<ResourceBundleHint> bundleHints = this.generationContext.getRuntimeHints()
            .resources().resourceBundleHints();
    assertThat(bundleHints)
            .anyMatch(bundleHint -> "sample".equals(bundleHint.getBaseName()));
  }

  private void assertThatInheritedSampleRegistrarContributed() {
    assertThatSampleRegistrarContributed();
    Stream<ResourceBundleHint> bundleHints = this.generationContext.getRuntimeHints()
            .resources().resourceBundleHints();
    assertThat(bundleHints)
            .anyMatch(bundleHint -> "extendedSample".equals(bundleHint.getBaseName()));
  }

  private GenericApplicationContext createApplicationContext(
          Class<?>... configClasses) {
    GenericApplicationContext applicationContext = new GenericApplicationContext();
    AnnotationConfigUtils.registerAnnotationConfigProcessors(applicationContext);
    for (Class<?> configClass : configClasses) {
      applicationContext.registerBeanDefinition(configClass.getSimpleName(),
              new RootBeanDefinition(configClass));
    }
    return applicationContext;
  }

  @Configuration(proxyBeanMethods = false)
  @ImportRuntimeHints(SampleRuntimeHintsRegistrar.class)
  static class ConfigurationWithHints {
  }

  @Configuration(proxyBeanMethods = false)
  @ImportRuntimeHints(ExtendedSampleRuntimeHintsRegistrar.class)
  static class ExtendedConfigurationWithHints extends ConfigurationWithHints {
  }

  @Configuration(proxyBeanMethods = false)
  static class ConfigurationWithBeanDeclaringHints {

    @Bean
    @ImportRuntimeHints(SampleRuntimeHintsRegistrar.class)
    SampleBean sampleBean() {
      return new SampleBean();
    }

  }

  public static class SampleRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      hints.resources().registerResourceBundle("sample");
    }

  }

  public static class ExtendedSampleRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      hints.resources().registerResourceBundle("extendedSample");
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ImportRuntimeHints(IncrementalRuntimeHintsRegistrar.class)
  static class ConfigurationWithIncrementalHints {
  }

  static class IncrementalRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    static final AtomicInteger counter = new AtomicInteger();

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      hints.resources().registerResourceBundle("com.example.example" + counter.getAndIncrement());
    }
  }

  static class SampleBean {

  }

  @Configuration(proxyBeanMethods = false)
  @ImportRuntimeHints(IllegalRuntimeHintsRegistrar.class)
  static class ConfigurationWithIllegalRegistrar {

  }

  public static class IllegalRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    public IllegalRuntimeHintsRegistrar(String arg) {

    }

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      hints.resources().registerResourceBundle("sample");
    }

  }

  static class TestFactoriesClassLoader extends ClassLoader {

    private final String factoriesName;

    TestFactoriesClassLoader(String factoriesName) {
      super(Thread.currentThread().getContextClassLoader());
      this.factoriesName = factoriesName;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
      if ("META-INF/config/aot.factories".equals(name)) {
        return super.getResources(
                "cn/taketoday/context/aot/" + this.factoriesName);
      }
      return super.getResources(name);
    }

  }

}
