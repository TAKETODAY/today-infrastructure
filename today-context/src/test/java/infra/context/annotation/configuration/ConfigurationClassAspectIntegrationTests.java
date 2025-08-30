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

package infra.context.annotation.configuration;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import infra.aop.framework.autoproxy.AbstractAutoProxyCreator;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.IOther;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.BootstrapContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.ConfigurationClassPostProcessor;
import infra.context.annotation.EnableAspectJAutoProxy;
import infra.context.annotation.Proxyable;
import infra.context.support.GenericApplicationContext;
import infra.core.io.ClassPathResource;

import static infra.context.annotation.ProxyType.INTERFACES;
import static infra.context.annotation.ProxyType.TARGET_CLASS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * System tests covering use of AspectJ {@link Aspect}s in conjunction with {@link Configuration} classes.
 * {@link Bean} methods may return aspects, or Configuration classes may themselves be annotated with Aspect.
 * In the latter case, advice methods are declared inline within the Configuration class.  This makes for a
 * particularly convenient syntax requiring no extra artifact for the aspect.
 *
 * <p>Currently it is assumed that the user is bootstrapping Configuration class processing via XML (using
 * annotation-config or component-scan), and thus will also use {@code <aop:aspectj-autoproxy/>} to enable
 * processing of the Aspect annotation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
class ConfigurationClassAspectIntegrationTests {

  @Test
  void aspectAnnotatedConfiguration() {
    assertAdviceWasApplied(AspectConfig.class);
  }

  @Test
  void configurationIncludesAspect() {
    assertAdviceWasApplied(ConfigurationWithAspect.class);
  }

  @Test
  void configurationIncludesAspectAndProxyable() {
    assertAdviceWasApplied(ConfigurationWithAspectAndProxyable.class, TestBean.class);
  }

  @Test
  void configurationIncludesAspectAndProxyableInterfaces() {
    assertAdviceWasApplied(ConfigurationWithAspectAndProxyableInterfaces.class, TestBean.class, Comparable.class);
  }

  @Test
  void configurationIncludesAspectAndProxyableTargetClass() {
    assertAdviceWasApplied(ConfigurationWithAspectAndProxyableTargetClass.class);
  }

  private void assertAdviceWasApplied(Class<?> configClass, Class<?>... notImplemented) {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
            new ClassPathResource("aspectj-autoproxy-config.xml", ConfigurationClassAspectIntegrationTests.class));
    GenericApplicationContext ctx = new GenericApplicationContext(factory);

    BootstrapContext loadingContext = new BootstrapContext(factory, ctx);
    ctx.setBootstrapContext(loadingContext);

    ctx.addBeanFactoryPostProcessor(new ConfigurationClassPostProcessor(loadingContext));
    ctx.registerBeanDefinition("config",
            new RootBeanDefinition(configClass, AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR, false));
    ctx.refresh();

    ITestBean testBean = ctx.getBean("testBean", ITestBean.class);
    if (notImplemented.length > 0) {
      assertThat(testBean).isNotInstanceOfAny(notImplemented);
    }
    else {
      assertThat(testBean).isInstanceOf(TestBean.class);
    }
    assertThat(testBean.getName()).isEqualTo("name");
    ((IOther) testBean).absquatulate();
    assertThat(testBean.getName()).isEqualTo("advisedName");
    ctx.close();
  }

  @Test
  void withInnerClassAndLambdaExpression() {
    ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(Application.class, CountingAspect.class);
    ctx.getBeansOfType(Runnable.class).forEach((k, v) -> v.run());

    // TODO: returns just 1 as of AspectJ 1.9 beta 3, not detecting the applicable lambda expression anymore
    // assertEquals(2, ctx.getBean(CountingAspect.class).count);
    ctx.close();
  }

  @Aspect
  @Configuration
  static class AspectConfig {

    @Bean
    public TestBean testBean() {
      return new TestBean("name");
    }

    @Before("execution(* infra.beans.testfixture.beans.TestBean.absquatulate(..)) && target(testBean)")
    public void touchBean(TestBean testBean) {
      testBean.setName("advisedName");
    }
  }

  @Configuration
  static class ConfigurationWithAspect {

    @Bean
    public TestBean testBean() {
      return new TestBean("name");
    }

    @Bean
    public NameChangingAspect nameChangingAspect() {
      return new NameChangingAspect();
    }
  }

  @Configuration
  static class ConfigurationWithAspectAndProxyable {

    @Bean
    @Proxyable(INTERFACES)
    public TestBean testBean() {
      return new TestBean("name");
    }

    @Bean
    public NameChangingAspect nameChangingAspect() {
      return new NameChangingAspect();
    }
  }

  @Configuration()
  static class ConfigurationWithAspectAndProxyableInterfaces {

    @Bean
    @Proxyable(interfaces = { ITestBean.class, IOther.class })
    public TestBean testBean() {
      return new TestBean("name");
    }

    @Bean
    public NameChangingAspect nameChangingAspect() {
      return new NameChangingAspect();
    }
  }

  @Configuration
  static class ConfigurationWithAspectAndProxyableTargetClass {

    public ConfigurationWithAspectAndProxyableTargetClass(AbstractAutoProxyCreator autoProxyCreator) {
      autoProxyCreator.setProxyTargetClass(false);
    }

    @Bean
    @Proxyable(TARGET_CLASS)
    public TestBean testBean() {
      return new TestBean("name");
    }

    @Bean
    public NameChangingAspect nameChangingAspect() {
      return new NameChangingAspect();
    }
  }

  @Aspect
  static class NameChangingAspect {

    @Before("execution(* infra.beans.testfixture.beans.TestBean.absquatulate(..)) && target(testBean)")
    public void touchBean(TestBean testBean) {
      testBean.setName("advisedName");
    }
  }

  @Configuration
  @EnableAspectJAutoProxy
  public static class Application {

    @Bean
    Runnable fromInnerClass() {
      return () -> {
      };
    }

    @Bean
    Runnable fromLambdaExpression() {
      return () -> {
      };
    }
  }

  @Aspect
  public static class CountingAspect {

    public int count = 0;

    @After("execution(* java.lang.Runnable.*(..))")
    public void after(JoinPoint joinPoint) {
      count++;
    }
  }

}
