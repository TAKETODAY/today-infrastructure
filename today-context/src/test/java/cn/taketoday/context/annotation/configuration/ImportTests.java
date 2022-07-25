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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ConfigurationClassPostProcessor;
import cn.taketoday.context.annotation.DependsOn;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * System tests for {@link Import} annotation support.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class ImportTests {

  private StandardBeanFactory beanFactory;

  private BootstrapContext loadingContext;

  @BeforeEach
  void setup() {
    GenericApplicationContext context = new GenericApplicationContext();
    beanFactory = context.getBeanFactory();
    loadingContext = new BootstrapContext(beanFactory, context);
  }

  private StandardBeanFactory processConfigurationClasses(Class<?>... classes) {
    GenericApplicationContext context = new GenericApplicationContext();
    context.refresh();
    StandardBeanFactory beanFactory = context.getBeanFactory();
    BootstrapContext loadingContext = BootstrapContext.from(beanFactory);
    for (Class<?> clazz : classes) {
      beanFactory.registerBeanDefinition(clazz.getSimpleName(), new RootBeanDefinition(clazz));
    }
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
    pp.setBootstrapContext(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    return beanFactory;
  }

  private void assertBeanDefinitionCount(int expectedCount, Class<?>... classes) {
    StandardBeanFactory beanFactory = processConfigurationClasses(classes);
    assertThat(beanFactory.getBeanDefinitionCount()).isEqualTo(expectedCount);
    beanFactory.preInstantiateSingletons();
    for (Class<?> clazz : classes) {
      beanFactory.getBean(clazz);
    }

  }

  @Test
  public void testProcessImportsWithAsm() {
    int configClasses = 2;
    int beansInClasses = 2;
    beanFactory.registerBeanDefinition("config", new RootBeanDefinition(ConfigurationWithImportAnnotation.class.getName()));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    assertThat(beanFactory.getBeanDefinitionCount()).isEqualTo(configClasses + beansInClasses);
  }

  @Test
  public void testProcessImportsWithDoubleImports() {
    int configClasses = 3;
    int beansInClasses = 3;
    assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class, OtherConfigurationWithImportAnnotation.class);
  }

  @Test
  public void testProcessImportsWithExplicitOverridingBefore() {
    int configClasses = 2;
    int beansInClasses = 2;
    assertBeanDefinitionCount((configClasses + beansInClasses), OtherConfiguration.class, ConfigurationWithImportAnnotation.class);
  }

  @Test
  public void testProcessImportsWithExplicitOverridingAfter() {
    int configClasses = 2;
    int beansInClasses = 2;
    assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class, OtherConfiguration.class);
  }

  @Configuration
  @Import(OtherConfiguration.class)
  static class ConfigurationWithImportAnnotation {
    @Bean
    public ITestBean one() {
      return new TestBean();
    }
  }

  @Configuration
  @Import(OtherConfiguration.class)
  static class OtherConfigurationWithImportAnnotation {
    @Bean
    public ITestBean two() {
      return new TestBean();
    }
  }

  @Configuration
  static class OtherConfiguration {
    @Bean
    public ITestBean three() {
      return new TestBean();
    }
  }

  // ------------------------------------------------------------------------

  @Test
  public void testImportAnnotationWithTwoLevelRecursion() {
    int configClasses = 2;
    int beansInClasses = 3;
    assertBeanDefinitionCount((configClasses + beansInClasses), AppConfig.class);
  }

  @Configuration
  @Import(DataSourceConfig.class)
  static class AppConfig {

    @Bean
    public ITestBean transferService() {
      return new TestBean(accountRepository());
    }

    @Bean
    public ITestBean accountRepository() {
      return new TestBean();
    }
  }

  @Configuration
  static class DataSourceConfig {
    @Bean
    public ITestBean dataSourceA() {
      return new TestBean();
    }
  }

  // ------------------------------------------------------------------------

  @Test
  public void testImportAnnotationWithThreeLevelRecursion() {
    int configClasses = 4;
    int beansInClasses = 5;
    assertBeanDefinitionCount(configClasses + beansInClasses, FirstLevel.class);
  }

  // ------------------------------------------------------------------------

  @Test
  public void testImportAnnotationWithMultipleArguments() {
    int configClasses = 3;
    int beansInClasses = 3;
    assertBeanDefinitionCount((configClasses + beansInClasses), WithMultipleArgumentsToImportAnnotation.class);
  }

  @Test
  public void testImportAnnotationWithMultipleArgumentsResultingInOverriddenBeanDefinition() {
    beanFactory.registerBeanDefinition("config", new RootBeanDefinition(
            WithMultipleArgumentsThatWillCauseDuplication.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    assertThat(beanFactory.getBeanDefinitionCount()).isEqualTo(4);
    assertThat(beanFactory.getBean("foo", ITestBean.class).getName()).isEqualTo("foo2");
  }

  @Configuration
  @Import({ Foo1.class, Foo2.class })
  static class WithMultipleArgumentsThatWillCauseDuplication {
  }

  @Configuration
  static class Foo1 {
    @Bean
    public ITestBean foo() {
      return new TestBean("foo1");
    }
  }

  @Configuration
  static class Foo2 {
    @Bean
    public ITestBean foo() {
      return new TestBean("foo2");
    }
  }

  // ------------------------------------------------------------------------

  @Test
  public void testImportAnnotationOnInnerClasses() {
    int configClasses = 2;
    int beansInClasses = 2;
    assertBeanDefinitionCount((configClasses + beansInClasses), OuterConfig.InnerConfig.class);
  }

  @Configuration
  static class OuterConfig {
    @Bean
    String whatev() {
      return "whatev";
    }

    @Configuration
    @Import(ExternalConfig.class)
    static class InnerConfig {
      @Bean
      public ITestBean innerBean() {
        return new TestBean();
      }
    }
  }

  @Configuration
  static class ExternalConfig {
    @Bean
    public ITestBean extBean() {
      return new TestBean();
    }
  }

  // ------------------------------------------------------------------------

  @Configuration
  @Import(SecondLevel.class)
  static class FirstLevel {
    @Bean
    public TestBean m() {
      return new TestBean();
    }
  }

  @Configuration
  @Import({ ThirdLevel.class, InitBean.class })
  static class SecondLevel {
    @Bean
    public TestBean n() {
      return new TestBean();
    }
  }

  @Configuration
  @DependsOn("cn.taketoday.context.annotation.configuration.ImportTests$InitBean")
  static class ThirdLevel {
    public ThirdLevel() {
      assertThat(InitBean.initialized).isTrue();
    }

    @Bean
    public ITestBean thirdLevelA() {
      return new TestBean();
    }

    @Bean
    public ITestBean thirdLevelB() {
      return new TestBean();
    }

    @Bean
    public ITestBean thirdLevelC() {
      return new TestBean();
    }
  }

  static class InitBean {
    public static boolean initialized = false;

    public InitBean() {
      initialized = true;
    }
  }

  @Configuration
  @Import({ LeftConfig.class, RightConfig.class })
  static class WithMultipleArgumentsToImportAnnotation {
    @Bean
    public TestBean m() {
      return new TestBean();
    }
  }

  @Configuration
  static class LeftConfig {
    @Bean
    public ITestBean left() {
      return new TestBean();
    }
  }

  @Configuration
  static class RightConfig {
    @Bean
    public ITestBean right() {
      return new TestBean();
    }
  }

  // ------------------------------------------------------------------------

  @Test
  public void testImportNonConfigurationAnnotationClass() {
    int configClasses = 2;
    int beansInClasses = 0;
    assertBeanDefinitionCount((configClasses + beansInClasses), ConfigAnnotated.class);
  }

  @Configuration
  @Import(NonConfigAnnotated.class)
  static class ConfigAnnotated { }

  static class NonConfigAnnotated { }

  // ------------------------------------------------------------------------

  /**
   * Test that values supplied to @Configuration(value="...") are propagated as the
   * bean name for the configuration class even in the case of inclusion via @Import
   * or in the case of automatic registration via nesting
   */
  @Test
  public void reproSpr9023() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(B.class);
    ctx.refresh();
    System.out.println(ctx.getBeanFactory());
    assertThat(ctx.getBeanNamesForType(B.class).toArray()[0]).isEqualTo("config-b");
    assertThat(ctx.getBeanNamesForType(A.class).toArray()[0]).isEqualTo("config-a");
  }

  @Configuration("config-a")
  static class A { }

  @Configuration("config-b")
  @Import(A.class)
  static class B { }

  @Test
  public void testProcessImports() {
    int configClasses = 2;
    int beansInClasses = 2;
    assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class);
  }

}
