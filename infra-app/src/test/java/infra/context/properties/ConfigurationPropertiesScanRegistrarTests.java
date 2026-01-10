/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.properties;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.BootstrapContext;
import infra.context.properties.bind.BindMethod;
import infra.context.properties.scan.combined.c.CombinedConfiguration;
import infra.context.properties.scan.combined.d.OtherCombinedConfiguration;
import infra.context.properties.scan.valid.ConfigurationPropertiesScanConfiguration;
import infra.core.type.AnnotationMetadata;
import infra.core.type.classreading.SimpleMetadataReaderFactory;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesScanRegistrar}.
 *
 * @author Madhura Bhave
 */
class ConfigurationPropertiesScanRegistrarTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  private final ConfigurationPropertiesScanRegistrar registrar = new ConfigurationPropertiesScanRegistrar(
          new MockEnvironment(), null);

  @Test
  void registerBeanDefinitionsShouldScanForConfigurationProperties() throws IOException {
    this.registrar.registerBeanDefinitions(getAnnotationMetadata(ConfigurationPropertiesScanConfiguration.class),
            new BootstrapContext(beanFactory, null));
    BeanDefinition bingDefinition = this.beanFactory.getBeanDefinition(
            "bing-infra.context.properties.scan.valid.ConfigurationPropertiesScanConfiguration$BingProperties");
    BeanDefinition fooDefinition = this.beanFactory.getBeanDefinition(
            "foo-infra.context.properties.scan.valid.ConfigurationPropertiesScanConfiguration$FooProperties");
    BeanDefinition barDefinition = this.beanFactory.getBeanDefinition(
            "bar-infra.context.properties.scan.valid.ConfigurationPropertiesScanConfiguration$BarProperties");
    assertThat(bingDefinition).satisfies(hasBindMethod(BindMethod.JAVA_BEAN));
    assertThat(fooDefinition).satisfies(hasBindMethod(BindMethod.JAVA_BEAN));
    assertThat(barDefinition).satisfies(hasBindMethod(BindMethod.VALUE_OBJECT));
  }

  @Test
  void scanWhenBeanDefinitionExistsShouldSkip() throws IOException {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.setAllowBeanDefinitionOverriding(false);
    this.registrar.registerBeanDefinitions(
            getAnnotationMetadata(ConfigurationPropertiesScanConfiguration.TestConfiguration.class), new BootstrapContext(beanFactory, null));
    assertThat(beanFactory.containsBeanDefinition(
            "foo-infra.context.properties.scan.valid.ConfigurationPropertiesScanConfiguration$FooProperties"))
            .isTrue();
    assertThat(beanFactory.getBeanDefinitionNames())
            .filteredOn((name) -> name.toLowerCase(Locale.ENGLISH).contains("fooproperties"))
            .hasSize(1);
  }

  @Test
  void scanWhenBasePackagesAndBasePackageClassesProvidedShouldUseThat() throws IOException {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.setAllowBeanDefinitionOverriding(false);
    this.registrar.registerBeanDefinitions(
            getAnnotationMetadata(ConfigurationPropertiesScanConfiguration.DifferentPackageConfiguration.class),
            new BootstrapContext(beanFactory, null));
    assertThat(beanFactory.containsBeanDefinition(
            "foo-infra.context.properties.scan.valid.ConfigurationPropertiesScanConfiguration$FooProperties"))
            .isFalse();
    BeanDefinition aDefinition = beanFactory.getBeanDefinition(
            "a-infra.context.properties.scan.valid.a.AScanConfiguration$AProperties");
    BeanDefinition bFirstDefinition = beanFactory.getBeanDefinition(
            "b.first-infra.context.properties.scan.valid.b.BScanConfiguration$BFirstProperties");
    BeanDefinition bSecondDefinition = beanFactory.getBeanDefinition(
            "b.second-infra.context.properties.scan.valid.b.BScanConfiguration$BSecondProperties");
    assertThat(aDefinition).satisfies(hasBindMethod(BindMethod.JAVA_BEAN));
    // Constructor injection
    assertThat(bFirstDefinition).satisfies(hasBindMethod(BindMethod.VALUE_OBJECT));
    // Post-processing injection
    assertThat(bSecondDefinition).satisfies(hasBindMethod(BindMethod.JAVA_BEAN));
  }

  @Test
  void scanWhenComponentAnnotationPresentShouldSkipType() throws IOException {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.setAllowBeanDefinitionOverriding(false);
    this.registrar.registerBeanDefinitions(getAnnotationMetadata(CombinedScanConfiguration.class),
            new BootstrapContext(beanFactory, null));
    assertThat(beanFactory.getBeanDefinitionCount()).isZero();
  }

  @Test
  void scanWhenOtherComponentAnnotationPresentShouldSkipType() throws IOException {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.setAllowBeanDefinitionOverriding(false);
    this.registrar.registerBeanDefinitions(getAnnotationMetadata(OtherCombinedScanConfiguration.class),
            new BootstrapContext(beanFactory, null));
    assertThat(beanFactory.getBeanDefinitionCount()).isZero();
  }

  private Consumer<BeanDefinition> hasBindMethod(BindMethod bindMethod) {
    return (definition) -> {
      assertThat(definition.hasAttribute(BindMethod.class.getName())).isTrue();
      assertThat(definition.getAttribute(BindMethod.class.getName())).isEqualTo(bindMethod);
    };
  }

  private AnnotationMetadata getAnnotationMetadata(Class<?> source) throws IOException {
    return new SimpleMetadataReaderFactory().getMetadataReader(source.getName()).getAnnotationMetadata();
  }

  @ConfigurationPropertiesScan(basePackageClasses = CombinedConfiguration.class)
  static class CombinedScanConfiguration {

  }

  @ConfigurationPropertiesScan(basePackageClasses = OtherCombinedConfiguration.class)
  static class OtherCombinedScanConfiguration {

  }

}
