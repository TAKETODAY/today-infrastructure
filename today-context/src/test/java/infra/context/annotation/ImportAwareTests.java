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

package infra.context.annotation;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.support.GenericBeanDefinition;
import infra.context.BootstrapContext;
import infra.core.annotation.AnnotationAttributes;
import infra.core.type.AnnotatedTypeMetadata;
import infra.core.type.AnnotationMetadata;
import infra.core.type.StandardAnnotationMetadata;
import infra.lang.Assert;
import infra.scheduling.annotation.AsyncAnnotationBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that an ImportAware @Configuration classes gets injected with the
 * annotation metadata of the @Configuration class that imported it.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 4.0
 */
@Execution(ExecutionMode.SAME_THREAD)
public class ImportAwareTests {

  @Test
  public void directlyAnnotatedWithImport() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ImportingConfig.class);
    ctx.refresh();
    assertThat(ctx.getBean("importedConfigBean")).isNotNull();

    ImportedConfig importAwareConfig = ctx.getBean(ImportedConfig.class);
    AnnotationMetadata importMetadata = importAwareConfig.importMetadata;
    assertThat(importMetadata).isNotNull();
    assertThat(importMetadata.getClassName()).isEqualTo(ImportingConfig.class.getName());
    AnnotationAttributes importAttribs = AnnotationAttributes.fromMetadata(importMetadata, Import.class);
    Class<?>[] importedClasses = importAttribs.getClassArray("value");
    assertThat(importedClasses[0].getName()).isEqualTo(ImportedConfig.class.getName());
  }

  @Test
  public void indirectlyAnnotatedWithImport() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(IndirectlyImportingConfig.class);
    ctx.refresh();
    assertThat(ctx.getBean("importedConfigBean")).isNotNull();

    ImportedConfig importAwareConfig = ctx.getBean(ImportedConfig.class);
    AnnotationMetadata importMetadata = importAwareConfig.importMetadata;
    assertThat(importMetadata).isNotNull();
    assertThat(importMetadata.getClassName()).isEqualTo(IndirectlyImportingConfig.class.getName());
    AnnotationAttributes enableAttribs = AnnotationAttributes.fromMetadata(importMetadata, EnableImportedConfig.class);
    String foo = enableAttribs.getString("foo");
    assertThat(foo).isEqualTo("xyz");
  }

  @Test
  public void directlyAnnotatedWithImportLite() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ImportingConfigLite.class);
    ctx.refresh();
    assertThat(ctx.getBean("importedConfigBean")).isNotNull();

    ImportedConfigLite importAwareConfig = ctx.getBean(ImportedConfigLite.class);
    AnnotationMetadata importMetadata = importAwareConfig.importMetadata;
    assertThat(importMetadata).isNotNull();
    assertThat(importMetadata.getClassName()).isEqualTo(ImportingConfigLite.class.getName());
    AnnotationAttributes importAttribs = AnnotationAttributes.fromMetadata(importMetadata, Import.class);
    Class<?>[] importedClasses = importAttribs.getClassArray("value");
    assertThat(importedClasses[0].getName()).isEqualTo(ImportedConfigLite.class.getName());
  }

  @Test
  public void importRegistrar() {
    ImportedRegistrar.called = false;
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ImportingRegistrarConfig.class);
    ctx.refresh();
    assertThat(ctx.getBean("registrarImportedBean")).isNotNull();
    assertThat(ctx.getBean("otherImportedConfigBean")).isNotNull();
  }

  @Test
  public void importRegistrarWithImport() {
    ImportedRegistrar.called = false;
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ImportingRegistrarConfigWithImport.class);
    ctx.refresh();
    assertThat(ctx.getBean("registrarImportedBean")).isNotNull();
    assertThat(ctx.getBean("otherImportedConfigBean")).isNotNull();
    assertThat(ctx.getBean("importedConfigBean")).isNotNull();
    assertThat(ctx.getBean(ImportedConfig.class)).isNotNull();
  }

  @Test
  public void metadataFromImportsOneThenTwo() {
    AnnotationMetadata importMetadata = new AnnotationConfigApplicationContext(
            ConfigurationOne.class, ConfigurationTwo.class)
            .getBean(MetadataHolder.class).importMetadata;
    assertThat(((StandardAnnotationMetadata) importMetadata).getIntrospectedClass()).isEqualTo(ConfigurationOne.class);
  }

  @Test
  public void metadataFromImportsTwoThenOne() {
    AnnotationMetadata importMetadata = new AnnotationConfigApplicationContext(
            ConfigurationTwo.class, ConfigurationOne.class)
            .getBean(MetadataHolder.class).importMetadata;
    assertThat(((StandardAnnotationMetadata) importMetadata).getIntrospectedClass()).isEqualTo(ConfigurationOne.class);
  }

  @Test
  public void metadataFromImportsOneThenThree() {
    AnnotationMetadata importMetadata = new AnnotationConfigApplicationContext(
            ConfigurationOne.class, ConfigurationThree.class)
            .getBean(MetadataHolder.class).importMetadata;
    assertThat(((StandardAnnotationMetadata) importMetadata).getIntrospectedClass()).isEqualTo(ConfigurationOne.class);
  }

  @Test
  public void importAwareWithAnnotationAttributes() {
    new AnnotationConfigApplicationContext(ApplicationConfiguration.class);
  }

  @Configuration
  @Import(ImportedConfig.class)
  static class ImportingConfig {
  }

  @Configuration
  @EnableImportedConfig(foo = "xyz")
  static class IndirectlyImportingConfig {
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Import(ImportedConfig.class)
  public @interface EnableImportedConfig {
    String foo() default "";
  }

  @Configuration
  static class ImportedConfig implements ImportAware {

    AnnotationMetadata importMetadata;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
      this.importMetadata = importMetadata;
    }

    @Bean
    public BPP importedConfigBean() {
      return new BPP();
    }

    @Bean
    public AsyncAnnotationBeanPostProcessor asyncBPP() {
      return new AsyncAnnotationBeanPostProcessor();
    }
  }

  @Configuration
  static class OtherImportedConfig {

    @Bean
    public String otherImportedConfigBean() {
      return "";
    }
  }

  @Configuration
  @Import(ImportedConfigLite.class)
  static class ImportingConfigLite {
  }

  @Configuration(proxyBeanMethods = false)
  static class ImportedConfigLite implements ImportAware {

    AnnotationMetadata importMetadata;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
      this.importMetadata = importMetadata;
    }

    @Bean
    public BPP importedConfigBean() {
      return new BPP();
    }

    @Bean
    public AsyncAnnotationBeanPostProcessor asyncBPP() {
      return new AsyncAnnotationBeanPostProcessor();
    }
  }

  static class BPP implements InitializationBeanPostProcessor, BeanFactoryAware {

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
      return bean;
    }
  }

  @Configuration
  @EnableImportRegistrar
  static class ImportingRegistrarConfig {
  }

  @Configuration
  @EnableImportRegistrar
  @Import(ImportedConfig.class)
  static class ImportingRegistrarConfigWithImport {
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Import(ImportedRegistrar.class)
  public @interface EnableImportRegistrar {
  }

  static class ImportedRegistrar implements ImportBeanDefinitionRegistrar {

    static boolean called;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {

      GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
      beanDefinition.setBeanClassName(String.class.getName());
      context.registerBeanDefinition("registrarImportedBean", beanDefinition);
      GenericBeanDefinition beanDefinition2 = new GenericBeanDefinition();
      beanDefinition2.setBeanClass(OtherImportedConfig.class);
      context.registerBeanDefinition("registrarImportedConfig", beanDefinition2);
      Assert.state(!called, "ImportedRegistrar called twice");
      called = true;
    }

  }

  @EnableSomeConfiguration("bar")
  @Configuration
  public static class ConfigurationOne {
  }

  @Conditional(OnMissingBeanCondition.class)
  @EnableSomeConfiguration("foo")
  @Configuration
  public static class ConfigurationTwo {
  }

  @Conditional(OnMissingBeanCondition.class)
  @EnableLiteConfiguration("foo")
  @Configuration
  public static class ConfigurationThree {
  }

  @Import(SomeConfiguration.class)
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface EnableSomeConfiguration {

    String value() default "";
  }

  @Configuration
  public static class SomeConfiguration implements ImportAware {

    private AnnotationMetadata importMetadata;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
      this.importMetadata = importMetadata;
    }

    @Bean
    public MetadataHolder holder() {
      return new MetadataHolder(this.importMetadata);
    }
  }

  @Import(LiteConfiguration.class)
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface EnableLiteConfiguration {

    String value() default "";
  }

  @Configuration(proxyBeanMethods = false)
  public static class LiteConfiguration implements ImportAware {

    private AnnotationMetadata importMetadata;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
      this.importMetadata = importMetadata;
    }

    @Bean
    public MetadataHolder holder() {
      return new MetadataHolder(this.importMetadata);
    }
  }

  public static class MetadataHolder {

    private final AnnotationMetadata importMetadata;

    public MetadataHolder(AnnotationMetadata importMetadata) {
      this.importMetadata = importMetadata;
    }
  }

  private static final class OnMissingBeanCondition implements ConfigurationCondition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return (context.getBeanFactory().getBeanNamesForType(MetadataHolder.class, true, false).length == 0);
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.REGISTER_BEAN;
    }
  }

  @Configuration
  @EnableFeature(policies = {
          @EnableFeature.FeaturePolicy(name = "one"),
          @EnableFeature.FeaturePolicy(name = "two")
  })
  public static class ApplicationConfiguration {
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Import(FeatureConfiguration.class)
  public @interface EnableFeature {

    FeaturePolicy[] policies() default {};

    @interface FeaturePolicy {

      String name();
    }
  }

  @Configuration
  public static class FeatureConfiguration implements ImportAware {

    @Override
    public void setImportMetadata(AnnotationMetadata annotationMetadata) {
      AnnotationAttributes enableFeatureAttributes =
              AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(EnableFeature.class.getName()));
      assertThat(enableFeatureAttributes.annotationType()).isEqualTo(EnableFeature.class);
      Arrays.stream(enableFeatureAttributes.getAnnotationArray("policies"))
              .forEach(featurePolicyAttributes -> Assertions.assertThat(featurePolicyAttributes.annotationType()).isEqualTo(EnableFeature.FeaturePolicy.class));
    }
  }

}
