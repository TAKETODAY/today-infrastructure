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

package cn.taketoday.context.properties;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.properties.bind.BindMethod;
import cn.taketoday.context.properties.scan.combined.c.CombinedConfiguration;
import cn.taketoday.context.properties.scan.combined.d.OtherCombinedConfiguration;
import cn.taketoday.context.properties.scan.valid.ConfigurationPropertiesScanConfiguration;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.classreading.SimpleMetadataReaderFactory;
import cn.taketoday.mock.env.MockEnvironment;

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
            "bing-cn.taketoday.context.properties.scan.valid.ConfigurationPropertiesScanConfiguration$BingProperties");
    BeanDefinition fooDefinition = this.beanFactory.getBeanDefinition(
            "foo-cn.taketoday.context.properties.scan.valid.ConfigurationPropertiesScanConfiguration$FooProperties");
    BeanDefinition barDefinition = this.beanFactory.getBeanDefinition(
            "bar-cn.taketoday.context.properties.scan.valid.ConfigurationPropertiesScanConfiguration$BarProperties");
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
            "foo-cn.taketoday.context.properties.scan.valid.ConfigurationPropertiesScanConfiguration$FooProperties"))
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
            "foo-cn.taketoday.context.properties.scan.valid.ConfigurationPropertiesScanConfiguration$FooProperties"))
            .isFalse();
    BeanDefinition aDefinition = beanFactory.getBeanDefinition(
            "a-cn.taketoday.context.properties.scan.valid.a.AScanConfiguration$AProperties");
    BeanDefinition bFirstDefinition = beanFactory.getBeanDefinition(
            "b.first-cn.taketoday.context.properties.scan.valid.b.BScanConfiguration$BFirstProperties");
    BeanDefinition bSecondDefinition = beanFactory.getBeanDefinition(
            "b.second-cn.taketoday.context.properties.scan.valid.b.BScanConfiguration$BSecondProperties");
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
