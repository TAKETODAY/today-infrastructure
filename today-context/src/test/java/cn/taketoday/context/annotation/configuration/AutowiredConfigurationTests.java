/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.beans.testfixture.beans.Colour;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.stereotype.Component;
import jakarta.inject.Provider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * System tests covering use of {@link Autowired} and {@link Value} within
 * {@link Configuration} classes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@Execution(ExecutionMode.SAME_THREAD)
public class AutowiredConfigurationTests {

  @Test
  public void testAutowiredConfigurationDependencies() {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
            AutowiredConfigurationTests.class.getSimpleName() + ".xml", AutowiredConfigurationTests.class);

    assertThat(context.getBean("colour", Colour.class)).isEqualTo(Colour.RED);
    assertThat(context.getBean("testBean", TestBean.class).getName()).isEqualTo(Colour.RED.toString());
  }

  @Test
  public void testAutowiredConfigurationMethodDependencies() {
    StandardApplicationContext context = new StandardApplicationContext(
            AutowiredMethodConfig.class, ColorConfig.class);

    assertThat(context.getBean(Colour.class)).isEqualTo(Colour.RED);
    assertThat(context.getBean(TestBean.class).getName()).isEqualTo("RED-RED");
  }

  @Test
  public void testAutowiredConfigurationMethodDependenciesWithOptionalAndAvailable() {
    StandardApplicationContext context = new StandardApplicationContext(
            OptionalAutowiredMethodConfig.class, ColorConfig.class);

    assertThat(context.getBean(Colour.class)).isEqualTo(Colour.RED);
    assertThat(context.getBean(TestBean.class).getName()).isEqualTo("RED-RED");
  }

  @Test
  public void testAutowiredConfigurationMethodDependenciesWithOptionalAndNotAvailable() {
    StandardApplicationContext context = new StandardApplicationContext(
            OptionalAutowiredMethodConfig.class);

    assertThat(context.getBeansOfType(Colour.class).isEmpty()).isTrue();
    assertThat(context.getBean(TestBean.class).getName()).isEqualTo("");
  }

  @Test
  public void testAutowiredSingleConstructorSupported() {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
            new ClassPathResource("annotation-config.xml", AutowiredConstructorConfig.class));
    GenericApplicationContext ctx = new GenericApplicationContext(factory);
    ctx.registerBeanDefinition("config1", new RootBeanDefinition(AutowiredConstructorConfig.class));
    ctx.registerBeanDefinition("config2", new RootBeanDefinition(ColorConfig.class));
    ctx.refresh();
    assertThat(ctx.getBean(Colour.class)).isSameAs(ctx.getBean(AutowiredConstructorConfig.class).colour);
  }

  @Test
  public void testObjectFactoryConstructorWithTypeVariable() {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
            new ClassPathResource("annotation-config.xml", ObjectFactoryConstructorConfig.class));
    GenericApplicationContext ctx = new GenericApplicationContext(factory);
    ctx.registerBeanDefinition("config1", new RootBeanDefinition(ObjectFactoryConstructorConfig.class));
    ctx.registerBeanDefinition("config2", new RootBeanDefinition(ColorConfig.class));
    ctx.refresh();
    assertThat(ctx.getBean(Colour.class)).isSameAs(ctx.getBean(ObjectFactoryConstructorConfig.class).colour);
  }

  @Test
  public void testAutowiredAnnotatedConstructorSupported() {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
            new ClassPathResource("annotation-config.xml", MultipleConstructorConfig.class));
    GenericApplicationContext ctx = new GenericApplicationContext(factory);
    ctx.registerBeanDefinition("config1", new RootBeanDefinition(MultipleConstructorConfig.class));
    ctx.registerBeanDefinition("config2", new RootBeanDefinition(ColorConfig.class));
    ctx.refresh();
    assertThat(ctx.getBean(Colour.class)).isSameAs(ctx.getBean(MultipleConstructorConfig.class).colour);
  }

  @Test
  public void testValueInjection() {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
            "ValueInjectionTests.xml", AutowiredConfigurationTests.class);
    doTestValueInjection(context);
  }

  @Test
  public void testValueInjectionWithMetaAnnotation() {
    StandardApplicationContext context =
            new StandardApplicationContext(ValueConfigWithMetaAnnotation.class);
    doTestValueInjection(context);
  }

  @Test
  public void testValueInjectionWithAliasedMetaAnnotation() {
    StandardApplicationContext context =
            new StandardApplicationContext(ValueConfigWithAliasedMetaAnnotation.class);
    doTestValueInjection(context);
  }

  @Test
  public void testValueInjectionWithProviderFields() {
    StandardApplicationContext context =
            new StandardApplicationContext(ValueConfigWithProviderFields.class);
    doTestValueInjection(context);
  }

  @Test
  public void testValueInjectionWithProviderConstructorArguments() {
    StandardApplicationContext context =
            new StandardApplicationContext(ValueConfigWithProviderConstructorArguments.class);
    doTestValueInjection(context);
  }

  @Test
  public void testValueInjectionWithProviderMethodArguments() {
    StandardApplicationContext context =
            new StandardApplicationContext(ValueConfigWithProviderMethodArguments.class);
    doTestValueInjection(context);
  }

  private void doTestValueInjection(BeanFactory context) {
    System.clearProperty("myProp");

    TestBean testBean = context.getBean("testBean", TestBean.class);
    assertThat((Object) testBean.getName()).isNull();

    testBean = context.getBean("testBean2", TestBean.class);
    assertThat((Object) testBean.getName()).isNull();

    System.setProperty("myProp", "foo");

    testBean = context.getBean("testBean", TestBean.class);
    assertThat(testBean.getName()).isEqualTo("foo");

    testBean = context.getBean("testBean2", TestBean.class);
    assertThat(testBean.getName()).isEqualTo("foo");

    System.clearProperty("myProp");

    testBean = context.getBean("testBean", TestBean.class);
    assertThat((Object) testBean.getName()).isNull();

    testBean = context.getBean("testBean2", TestBean.class);
    assertThat((Object) testBean.getName()).isNull();
  }

  @Test
  public void testCustomPropertiesWithClassPathContext() throws IOException {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
            "AutowiredConfigurationTests-custom.xml", AutowiredConfigurationTests.class);

    TestBean testBean = context.getBean("testBean", TestBean.class);
    assertThat(testBean.getName()).isEqualTo("localhost");
    assertThat(testBean.getAge()).isEqualTo(contentLength());
  }

  @Test
  public void testCustomPropertiesWithGenericContext() throws IOException {
    GenericApplicationContext context = new GenericApplicationContext();
    new XmlBeanDefinitionReader(context).loadBeanDefinitions(
            new ClassPathResource("AutowiredConfigurationTests-custom.xml", AutowiredConfigurationTests.class));
    context.refresh();

    TestBean testBean = context.getBean("testBean", TestBean.class);
    assertThat(testBean.getName()).isEqualTo("localhost");
    assertThat(testBean.getAge()).isEqualTo(contentLength());
  }

  @Test
  void testValueInjectionWithRecord() {
    System.setProperty("recordBeanName", "enigma");
    GenericApplicationContext context = new AnnotationConfigApplicationContext(RecordBean.class);
    try {
      assertThat(context.getBean(RecordBean.class).name()).isEqualTo("enigma");
    }
    finally {
      System.clearProperty("recordBeanName");
    }
  }

  private int contentLength() throws IOException {
    return (int) new ClassPathResource("do_not_delete_me.txt").contentLength();
  }

  @Configuration
  static class AutowiredConfig {

    @Autowired
    private Colour colour;

    @Bean
    public TestBean testBean() {
      return new TestBean(colour.toString());
    }
  }

  @Configuration
  static class AutowiredMethodConfig {

    @Bean
    public TestBean testBean(Colour colour, List<Colour> colours) {
      return new TestBean(colour.toString() + "-" + colours.get(0).toString());
    }
  }

  @Configuration
  static class OptionalAutowiredMethodConfig {

    @Bean
    public TestBean testBean(Optional<Colour> colour, Optional<List<Colour>> colours) {
      if (!colour.isPresent() && !colours.isPresent()) {
        return new TestBean("");
      }
      else {
        return new TestBean(colour.get().toString() + "-" + colours.get().get(0).toString());
      }
    }
  }

  @Configuration
  static class AutowiredConstructorConfig {

    Colour colour;

    // @Autowired
    AutowiredConstructorConfig(Colour colour) {
      this.colour = colour;
    }
  }

  @Configuration
  static class ObjectFactoryConstructorConfig {

    Colour colour;

    // @Autowired
    ObjectFactoryConstructorConfig(Supplier<Colour> colourFactory) {
      this.colour = colourFactory.get();
    }
  }

  @Configuration
  static class MultipleConstructorConfig {

    Colour colour;

    @Autowired
    MultipleConstructorConfig(Colour colour) {
      this.colour = colour;
    }

    MultipleConstructorConfig(String test) {
      this.colour = new Colour(test);
    }
  }

  @Configuration
  static class ColorConfig {

    @Bean
    public Colour colour() {
      return Colour.RED;
    }
  }

  @Configuration
  static class ValueConfig {

    @Value("#{systemProperties['myProp']}")
    private String name;

    private String name2;

    @Value("#{systemProperties['myProp']}")
    public void setName2(String name) {
      this.name2 = name;
    }

    @Bean
    @Scope("prototype")
    public TestBean testBean() {
      return new TestBean(name);
    }

    @Bean
    @Scope("prototype")
    public TestBean testBean2() {
      return new TestBean(name2);
    }
  }

  @Value("#{systemProperties['myProp']}")
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MyProp {
  }

  @Configuration
  @Scope("prototype")
  static class ValueConfigWithMetaAnnotation {

    @MyProp
    private String name;

    private String name2;

    @MyProp
    public void setName2(String name) {
      this.name2 = name;
    }

    @Bean
    @Scope("prototype")
    public TestBean testBean() {
      return new TestBean(name);
    }

    @Bean
    @Scope("prototype")
    public TestBean testBean2() {
      return new TestBean(name2);
    }
  }

  @Value("")
  @Retention(RetentionPolicy.RUNTIME)
  public @interface AliasedProp {

    @AliasFor(annotation = Value.class)
    String value();
  }

  @Configuration
  @Scope("prototype")
  static class ValueConfigWithAliasedMetaAnnotation {

    @AliasedProp("#{systemProperties['myProp']}")
    private String name;

    private String name2;

    @AliasedProp("#{systemProperties['myProp']}")
    public void setName2(String name) {
      this.name2 = name;
    }

    @Bean
    @Scope("prototype")
    public TestBean testBean() {
      return new TestBean(name);
    }

    @Bean
    @Scope("prototype")
    public TestBean testBean2() {
      return new TestBean(name2);
    }
  }

  @Configuration
  static class ValueConfigWithProviderFields {

    @Value("#{systemProperties['myProp']}")
    private Provider<String> name;

    private Provider<String> name2;

    @Value("#{systemProperties['myProp']}")
    public void setName2(Provider<String> name) {
      this.name2 = name;
    }

    @Bean
    @Scope("prototype")
    public TestBean testBean() {
      return new TestBean(name.get());
    }

    @Bean
    @Scope("prototype")
    public TestBean testBean2() {
      return new TestBean(name2.get());
    }
  }

  static class ValueConfigWithProviderConstructorArguments {

    private final Provider<String> name;

    private final Provider<String> name2;

    @Autowired
    public ValueConfigWithProviderConstructorArguments(
            @Value("#{systemProperties['myProp']}") Provider<String> name,
            @Value("#{systemProperties['myProp']}") Provider<String> name2) {
      this.name = name;
      this.name2 = name2;
    }

    @Bean
    @Scope("prototype")
    public TestBean testBean() {
      return new TestBean(name.get());
    }

    @Bean
    @Scope("prototype")
    public TestBean testBean2() {
      return new TestBean(name2.get());
    }
  }

  @Configuration
  static class ValueConfigWithProviderMethodArguments {

    @Bean
    @Scope("prototype")
    public TestBean testBean(@Value("#{systemProperties['myProp']}") Provider<String> name) {
      return new TestBean(name.get());
    }

    @Bean
    @Scope("prototype")
    public TestBean testBean2(@Value("#{systemProperties['myProp']}") Provider<String> name2) {
      return new TestBean(name2.get());
    }
  }

  @Configuration
  static class PropertiesConfig {

    private String hostname;

    private Resource resource;

    @Value("#{myProps.hostname}")
    public void setHostname(String hostname) {
      this.hostname = hostname;
    }

    @Value("do_not_delete_me.txt")
    public void setResource(Resource resource) {
      this.resource = resource;
    }

    @Bean
    public TestBean testBean() throws IOException {
      return new TestBean(hostname, (int) resource.contentLength());
    }
  }

  @Component
  record RecordBean(@Value("${recordBeanName}") String name) {

  }

}
