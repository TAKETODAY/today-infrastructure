/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.loader.ImportSelector;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Conditional} beans.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
@SuppressWarnings("resource")
public class ConfigurationClassWithConditionTests {

  @Test
  public void conditionalOnMissingBeanMatch() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(BeanOneConfiguration.class, BeanTwoConfiguration.class);
    ctx.refresh();
    assertThat(ctx.containsBean("bean1")).isTrue();
    assertThat(ctx.containsBean("bean2")).isFalse();
    assertThat(ctx.containsBean("configurationClassWithConditionTests.BeanTwoConfiguration")).isFalse();
  }

  @Test
  public void conditionalOnMissingBeanNoMatch() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(BeanTwoConfiguration.class);
    ctx.refresh();
    assertThat(ctx.containsBean("bean1")).isFalse();
    assertThat(ctx.containsBean("bean2")).isTrue();
    assertThat(ctx.containsBean("configurationClassWithConditionTests.BeanTwoConfiguration")).isTrue();
  }

  @Test
  public void conditionalOnBeanMatch() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(BeanOneConfiguration.class, BeanThreeConfiguration.class);
    ctx.refresh();
    assertThat(ctx.containsBean("bean1")).isTrue();
    assertThat(ctx.containsBean("bean3")).isTrue();
  }

  @Test
  public void conditionalOnBeanNoMatch() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(BeanThreeConfiguration.class);
    ctx.refresh();
    assertThat(ctx.containsBean("bean1")).isFalse();
    assertThat(ctx.containsBean("bean3")).isFalse();
  }

  @Test
  public void metaConditional() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigurationWithMetaCondition.class);
    ctx.refresh();
    assertThat(ctx.containsBean("bean")).isTrue();
  }

  @Test
  public void metaConditionalWithAsm() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("config", new RootBeanDefinition(ConfigurationWithMetaCondition.class.getName()));
    ctx.refresh();
    assertThat(ctx.containsBean("bean")).isTrue();
  }

  @Test
  public void nonConfigurationClass() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(NonConfigurationClass.class);
    ctx.refresh();
    assertThat(ctx.containsBean("bean1")).isFalse();
  }

  @Test
  public void nonConfigurationClassWithAsm() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("config", new RootBeanDefinition(NonConfigurationClass.class.getName()));
    ctx.refresh();
    assertThat(ctx.containsBean("bean1")).isFalse();
  }

  @Test
  public void methodConditional() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConditionOnMethodConfiguration.class);
    ctx.refresh();
    assertThat(ctx.containsBean("bean1")).isFalse();
  }

  @Test
  public void methodConditionalWithAsm() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("config", new RootBeanDefinition(ConditionOnMethodConfiguration.class.getName()));
    ctx.refresh();
    assertThat(ctx.containsBean("bean1")).isFalse();
  }

  @Test
  public void importsNotCreated() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ImportsNotCreated.class);
    ctx.refresh();
  }

  @Test
  public void conditionOnOverriddenMethodHonored() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithBeanSkipped.class);
    assertThat(context.getBeansOfType(ExampleBean.class).size()).isEqualTo(0);
  }

  @Test
  public void noConditionOnOverriddenMethodHonored() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithBeanReactivated.class);
    Map<String, ExampleBean> beans = context.getBeansOfType(ExampleBean.class);
    assertThat(beans.size()).isEqualTo(1);
    assertThat(beans.keySet().iterator().next()).isEqualTo("baz");
  }

  @Test
  public void configWithAlternativeBeans() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithAlternativeBeans.class);
    Map<String, ExampleBean> beans = context.getBeansOfType(ExampleBean.class);
    assertThat(beans.size()).isEqualTo(1);
    assertThat(beans.keySet().iterator().next()).isEqualTo("baz");
  }

  @Configuration
  static class BeanOneConfiguration {

    @Bean
    public ExampleBean bean1() {
      return new ExampleBean();
    }
  }

  @Configuration
  @Conditional(NoBeanOneCondition.class)
  static class BeanTwoConfiguration {

    @Bean
    public ExampleBean bean2() {
      return new ExampleBean();
    }
  }

  @Configuration
  @Conditional(HasBeanOneCondition.class)
  static class BeanThreeConfiguration {

    @Bean
    public ExampleBean bean3() {
      return new ExampleBean();
    }
  }

  @Configuration
  @MetaConditional("test")
  static class ConfigurationWithMetaCondition {

    @Bean
    public ExampleBean bean() {
      return new ExampleBean();
    }
  }

  @Conditional(MetaConditionalFilter.class)
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface MetaConditional {

    String value();
  }

  @Conditional(NeverCondition.class)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  public @interface Never {
  }

  @Conditional(AlwaysCondition.class)
  @Never
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  public @interface MetaNever {
  }

  static class NoBeanOneCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return !context.getBeanFactory().containsBeanDefinition("bean1");
    }
  }

  static class HasBeanOneCondition implements ConfigurationCondition {

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.REGISTER_BEAN;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return context.getBeanFactory().containsBeanDefinition("bean1");
    }
  }

  static class MetaConditionalFilter implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(MetaConditional.class.getName()));
      assertThat(attributes.getString("value")).isEqualTo("test");
      return true;
    }
  }

  static class NeverCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }
  }

  static class AlwaysCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return true;
    }
  }

  @Component
  @MetaNever
  static class NonConfigurationClass {

    @Bean
    public ExampleBean bean1() {
      return new ExampleBean();
    }
  }

  @Configuration
  static class ConditionOnMethodConfiguration {

    @Bean
    @Never
    public ExampleBean bean1() {
      return new ExampleBean();
    }
  }

  @Configuration
  @Never
  @Import({ ConfigurationNotCreated.class, RegistrarNotCreated.class, ImportSelectorNotCreated.class })
  static class ImportsNotCreated {

    static {
      if (true) {
        throw new RuntimeException();
      }
    }
  }

  @Configuration
  static class ConfigurationNotCreated {

    static {
      if (true) {
        throw new RuntimeException();
      }
    }
  }

  static class RegistrarNotCreated implements ImportBeanDefinitionRegistrar {

    static {
      if (true) {
        throw new RuntimeException();
      }
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {

    }
  }

  static class ImportSelectorNotCreated implements ImportSelector {

    static {
      if (true) {
        throw new RuntimeException();
      }
    }

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      return new String[] {};
    }

  }

  static class ExampleBean {
  }

  @Configuration
  static class ConfigWithBeanActive {

    @Bean
    public ExampleBean baz() {
      return new ExampleBean();
    }
  }

  static class ConfigWithBeanSkipped extends ConfigWithBeanActive {

    @Override
    @Bean
    @Conditional(NeverCondition.class)
    public ExampleBean baz() {
      return new ExampleBean();
    }
  }

  static class ConfigWithBeanReactivated extends ConfigWithBeanSkipped {

    @Override
    @Bean
    public ExampleBean baz() {
      return new ExampleBean();
    }
  }

  @Configuration
  static class ConfigWithAlternativeBeans {

    @Bean(name = "baz")
    @Conditional(AlwaysCondition.class)
    public ExampleBean baz1() {
      return new ExampleBean();
    }

    @Bean(name = "baz")
    @Conditional(NeverCondition.class)
    public ExampleBean baz2() {
      return new ExampleBean();
    }
  }

}
