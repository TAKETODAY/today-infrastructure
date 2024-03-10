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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ConfigurationClassPostProcessor;
import cn.taketoday.context.annotation.DependsOn;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.componentscan.ordered.SiblingImportingConfigA;
import cn.taketoday.context.annotation.componentscan.ordered.SiblingImportingConfigB;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * System tests for {@link Import} annotation support.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class ImportTests {

  private StandardBeanFactory processConfigurationClasses(Class<?>... classes) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.setAllowBeanDefinitionOverriding(false);
    for (Class<?> clazz : classes) {
      beanFactory.registerBeanDefinition(clazz.getSimpleName(), new RootBeanDefinition(clazz));
    }
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
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

  // ------------------------------------------------------------------------

  @Test
  void testProcessImportsWithAsm() {
    int configClasses = 2;
    int beansInClasses = 2;
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("config", new RootBeanDefinition(ConfigurationWithImportAnnotation.class.getName()));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
    pp.postProcessBeanFactory(beanFactory);
    assertThat(beanFactory.getBeanDefinitionCount()).isEqualTo(configClasses + beansInClasses);
  }

  @Test
  void testProcessImportsWithDoubleImports() {
    int configClasses = 3;
    int beansInClasses = 3;
    assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class, OtherConfigurationWithImportAnnotation.class);
  }

  @Test
  void testProcessImportsWithExplicitOverridingBefore() {
    int configClasses = 2;
    int beansInClasses = 2;
    assertBeanDefinitionCount((configClasses + beansInClasses), OtherConfiguration.class, ConfigurationWithImportAnnotation.class);
  }

  @Test
  void testProcessImportsWithExplicitOverridingAfter() {
    int configClasses = 2;
    int beansInClasses = 2;
    assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class, OtherConfiguration.class);
  }

  @Configuration
  @Import(OtherConfiguration.class)
  static class ConfigurationWithImportAnnotation {
    @Bean
    ITestBean one() {
      return new TestBean();
    }
  }

  @Configuration
  @Import(OtherConfiguration.class)
  static class OtherConfigurationWithImportAnnotation {
    @Bean
    ITestBean two() {
      return new TestBean();
    }
  }

  @Configuration
  static class OtherConfiguration {
    @Bean
    ITestBean three() {
      return new TestBean();
    }
  }

  // ------------------------------------------------------------------------

  @Test
  void testImportAnnotationWithTwoLevelRecursion() {
    int configClasses = 2;
    int beansInClasses = 3;
    assertBeanDefinitionCount((configClasses + beansInClasses), AppConfig.class);
  }

  @Configuration
  @Import(DataSourceConfig.class)
  static class AppConfig {

    @Bean
    ITestBean transferService() {
      return new TestBean(accountRepository());
    }

    @Bean
    ITestBean accountRepository() {
      return new TestBean();
    }
  }

  @Configuration
  static class DataSourceConfig {
    @Bean
    ITestBean dataSourceA() {
      return new TestBean();
    }
  }

  // ------------------------------------------------------------------------

  @Test
  void testImportAnnotationWithThreeLevelRecursion() {
    int configClasses = 4;
    int beansInClasses = 5;
    assertBeanDefinitionCount(configClasses + beansInClasses, FirstLevel.class);
  }

  @Test
  void testImportAnnotationWithThreeLevelRecursionAndDoubleImport() {
    int configClasses = 5;
    int beansInClasses = 5;
    assertBeanDefinitionCount(configClasses + beansInClasses, FirstLevel.class, FirstLevelPlus.class);
  }

  // ------------------------------------------------------------------------

  @Test
  void testImportAnnotationWithMultipleArguments() {
    int configClasses = 3;
    int beansInClasses = 3;
    assertBeanDefinitionCount((configClasses + beansInClasses), WithMultipleArgumentsToImportAnnotation.class);
  }

  @Test
  void testImportAnnotationWithMultipleArgumentsResultingInOverriddenBeanDefinition() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("config", new RootBeanDefinition(
            WithMultipleArgumentsThatWillCauseDuplication.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
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
    ITestBean foo() {
      return new TestBean("foo1");
    }
  }

  @Configuration
  static class Foo2 {
    @Bean
    ITestBean foo() {
      return new TestBean("foo2");
    }
  }

  // ------------------------------------------------------------------------

  @Test
  void testImportAnnotationOnInnerClasses() {
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
      ITestBean innerBean() {
        return new TestBean();
      }
    }
  }

  @Configuration
  static class ExternalConfig {
    @Bean
    ITestBean extBean() {
      return new TestBean();
    }
  }

  // ------------------------------------------------------------------------

  @Configuration
  @Import(SecondLevel.class)
  static class FirstLevel {
    @Bean
    TestBean m() {
      return new TestBean();
    }
  }

  @Configuration
  @Import(ThirdLevel.class)
  static class FirstLevelPlus {
  }

  @Configuration
  @Import({ ThirdLevel.class, InitBean.class })
  static class SecondLevel {
    @Bean
    TestBean n() {
      return new TestBean();
    }
  }

  @Configuration
  @DependsOn("cn.taketoday.context.annotation.configuration.ImportTests$InitBean")
  static class ThirdLevel {
    ThirdLevel() {
      assertThat(InitBean.initialized).isTrue();
    }

    @Bean
    ITestBean thirdLevelA() {
      return new TestBean();
    }

    @Bean
    ITestBean thirdLevelB() {
      return new TestBean();
    }

    @Bean
    ITestBean thirdLevelC() {
      return new TestBean();
    }
  }

  static class InitBean {
    public static boolean initialized = false;

    InitBean() {
      initialized = true;
    }
  }

  @Configuration
  @Import({ LeftConfig.class, RightConfig.class })
  static class WithMultipleArgumentsToImportAnnotation {
    @Bean
    TestBean m() {
      return new TestBean();
    }
  }

  @Configuration
  static class LeftConfig {
    @Bean
    ITestBean left() {
      return new TestBean();
    }
  }

  @Configuration
  static class RightConfig {
    @Bean
    ITestBean right() {
      return new TestBean();
    }
  }

  // ------------------------------------------------------------------------

  @Test
  void testImportNonConfigurationAnnotationClass() {
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
  void reproSpr9023() {
    try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
      ctx.register(B.class);
      ctx.refresh();
      System.out.println(ctx.getBeanFactory());
      assertThat(ctx.getBeanNamesForType(B.class).toArray()[0]).isEqualTo("config-b");
      assertThat(ctx.getBeanNamesForType(A.class).toArray()[0]).isEqualTo("config-a");
    }
  }

  @Configuration("config-a")
  static class A { }

  @Configuration("config-b")
  @Import(A.class)
  static class B { }

  // ------------------------------------------------------------------------

  @Test
  void testProcessImports() {
    int configClasses = 2;
    int beansInClasses = 2;
    assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class);
  }

  /**
   * An imported config must override a scanned one, thus bean definitions
   * from the imported class is overridden by its importer.
   */
  @Test
  void importedConfigOverridesScanned() {
    try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
      ctx.scan(SiblingImportingConfigA.class.getPackage().getName());
      ctx.refresh();

      assertThat(ctx.getBean("a-imports-b")).isEqualTo("valueFromA");
      assertThat(ctx.getBean("b-imports-a")).isEqualTo("valueFromBR");
      assertThat(ctx.getBeansOfType(SiblingImportingConfigA.class)).hasSize(1);
      assertThat(ctx.getBeansOfType(SiblingImportingConfigB.class)).hasSize(1);
    }
  }

}
