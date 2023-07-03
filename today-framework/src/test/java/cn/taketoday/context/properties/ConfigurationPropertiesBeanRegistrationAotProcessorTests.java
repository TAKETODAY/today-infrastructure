/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.properties;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Consumer;

import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.aot.AotServices;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.aot.ApplicationContextAotGenerator;
import cn.taketoday.context.properties.scan.valid.b.BScanConfiguration;
import cn.taketoday.context.properties.scan.valid.b.BScanConfiguration.BFirstProperties;
import cn.taketoday.context.properties.scan.valid.b.BScanConfiguration.BSecondProperties;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.test.tools.CompileWithForkedClassLoader;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesBeanRegistrationAotProcessor}.
 *
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesBeanRegistrationAotProcessorTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  private final ConfigurationPropertiesBeanRegistrationAotProcessor processor = new ConfigurationPropertiesBeanRegistrationAotProcessor();

  @Test
  void configurationPropertiesBeanRegistrationAotProcessorIsRegistered() {
    assertThat(AotServices.factories().load(BeanRegistrationAotProcessor.class))
            .anyMatch(ConfigurationPropertiesBeanRegistrationAotProcessor.class::isInstance);
  }

  @Test
  void processAheadOfTimeWithNoConfigurationPropertiesBean() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
    this.beanFactory.registerBeanDefinition("test", beanDefinition);
    BeanRegistrationAotContribution contribution = this.processor
            .processAheadOfTime(RegisteredBean.of(this.beanFactory, "test"));
    assertThat(contribution).isNull();
  }

  @Test
  void processAheadOfTimeWithJavaBeanConfigurationPropertiesBean() {
    BeanRegistrationAotContribution contribution = process(JavaBeanSampleBean.class);
    assertThat(contribution).isNull();
  }

  @Test
  void processAheadOfTimeWithValueObjectConfigurationPropertiesBean() {
    BeanRegistrationAotContribution contribution = process(ValueObjectSampleBean.class);
    assertThat(contribution).isNotNull();
  }

  private BeanRegistrationAotContribution process(Class<?> type) {
    ConfigurationPropertiesBeanRegistrar beanRegistrar = new ConfigurationPropertiesBeanRegistrar(new BootstrapContext(beanFactory, null));
    beanRegistrar.register(type);
    RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory,
            this.beanFactory.getBeanDefinitionNames()[0]);
    return this.processor.processAheadOfTime(registeredBean);
  }

  @Test
  @CompileWithForkedClassLoader
  void aotContributedInitializerBindsValueObject() {
    compile(createContext(ValueObjectSampleBeanConfiguration.class), (freshContext) -> {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(freshContext, "test.name=Hello");
      freshContext.refresh();
      ValueObjectSampleBean bean = freshContext.getBean(ValueObjectSampleBean.class);
      assertThat(bean.name).isEqualTo("Hello");
    });
  }

  @Test
  @CompileWithForkedClassLoader
  void aotContributedInitializerBindsJavaBean() {
    compile(createContext(JavaBeanSampleBeanConfiguration.class), (freshContext) -> {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(freshContext, "test.name=Hello");
      freshContext.refresh();
      JavaBeanSampleBean bean = freshContext.getBean(JavaBeanSampleBean.class);
      assertThat(bean.getName()).isEqualTo("Hello");
    });
  }

  @Test
  @CompileWithForkedClassLoader
  void aotContributedInitializerBindsScannedValueObject() {
    compile(createContext(ScanTestConfiguration.class), (freshContext) -> {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(freshContext, "b.first.name=Hello");
      freshContext.refresh();
      BFirstProperties bean = freshContext.getBean(BFirstProperties.class);
      assertThat(bean.getName()).isEqualTo("Hello");
    });
  }

  @Test
  @CompileWithForkedClassLoader
  void aotContributedInitializerBindsScannedJavaBean() {
    compile(createContext(ScanTestConfiguration.class), (freshContext) -> {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(freshContext, "b.second.number=42");
      freshContext.refresh();
      BSecondProperties bean = freshContext.getBean(BSecondProperties.class);
      assertThat(bean.getNumber()).isEqualTo(42);
    });
  }

  private GenericApplicationContext createContext(Class<?>... types) {
    GenericApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(JavaBeanSampleBeanConfiguration.class);
    Arrays.stream(types).forEach((type) -> context.registerBean(type));
    return context;
  }

  private void compile(GenericApplicationContext context, Consumer<GenericApplicationContext> freshContext) {
    TestGenerationContext generationContext = new TestGenerationContext(TestTarget.class);
    ClassName className = new ApplicationContextAotGenerator().processAheadOfTime(context, generationContext);
    generationContext.writeGeneratedContent();
    TestCompiler.forSystem().with(generationContext).compile((compiled) -> {
      GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
      ApplicationContextInitializer initializer = compiled.getInstance(ApplicationContextInitializer.class, className.toString());
      initializer.initialize(freshApplicationContext);
      freshContext.accept(freshApplicationContext);
    });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(JavaBeanSampleBean.class)
  static class JavaBeanSampleBeanConfiguration {

  }

  @ConfigurationProperties("test")
  public static class JavaBeanSampleBean {

    private String name;

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(ValueObjectSampleBean.class)
  static class ValueObjectSampleBeanConfiguration {

  }

  @ConfigurationProperties("test")
  public static class ValueObjectSampleBean {

    @SuppressWarnings("unused")
    private final String name;

    ValueObjectSampleBean(String name) {
      this.name = name;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConfigurationPropertiesScan(basePackageClasses = BScanConfiguration.class)
  static class ScanTestConfiguration {

  }

  static class TestTarget {

  }

}
