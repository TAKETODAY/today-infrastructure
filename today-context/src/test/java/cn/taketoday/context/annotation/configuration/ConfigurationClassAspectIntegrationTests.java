/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.support.StandardApplicationContext;

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
public class ConfigurationClassAspectIntegrationTests {

  private StandardBeanFactory beanFactory;

  private BootstrapContext loadingContext;

  @BeforeEach
  void setup() {
    StandardApplicationContext context = new StandardApplicationContext();
    beanFactory = context.getBeanFactory();
    loadingContext = new BootstrapContext(beanFactory, context);
  }

//  @Test
//  public void aspectAnnotatedConfiguration() {
//    assertAdviceWasApplied(AspectConfig.class);
//  }
//
//  @Test
//  public void configurationIncludesAspect() {
//    assertAdviceWasApplied(ConfigurationWithAspect.class);
//  }
//
//  private void assertAdviceWasApplied(Class<?> configClass) {
//    StandardBeanFactory factory = new StandardBeanFactory();
//    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
//            new ClassPathResource("aspectj-autoproxy-config.xml", ConfigurationClassAspectIntegrationTests.class));
//    DefaultApplicationContext ctx = new DefaultApplicationContext(factory);
//    ctx.addBeanFactoryPostProcessor(new ConfigurationClassPostProcessor(loadingContext));
//    ctx.registerBeanDefinition("config", new BeanDefinition(configClass));
//    ctx.refresh();
//
//    TestBean testBean = ctx.getBean("testBean", TestBean.class);
//    assertThat(testBean.getName()).isEqualTo("name");
//    testBean.absquatulate();
//    assertThat(testBean.getName()).isEqualTo("advisedName");
//  }

  @Test
  public void withInnerClassAndLambdaExpression() {
    ApplicationContext ctx = new StandardApplicationContext(Application.class, CountingAspect.class);
    ctx.getBeansOfType(Runnable.class).forEach((k, v) -> v.run());

    // TODO: returns just 1 as of AspectJ 1.9 beta 3, not detecting the applicable lambda expression anymore
    // assertEquals(2, ctx.getBean(CountingAspect.class).count);
  }

  @Aspect
  @Configuration
  static class AspectConfig {

    @Bean
    public TestBean testBean() {
      return new TestBean("name");
    }

    @Before("execution(* cn.taketoday.beans.testfixture.beans.TestBean.absquatulate(..)) && target(testBean)")
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

  @Aspect
  static class NameChangingAspect {

    @Before("execution(* cn.taketoday.beans.testfixture.beans.TestBean.absquatulate(..)) && target(testBean)")
    public void touchBean(TestBean testBean) {
      testBean.setName("advisedName");
    }
  }

  @Configuration
//  @EnableAspectJAutoProxy
  public static class Application {

    @Bean
    Runnable fromInnerClass() {
      return new Runnable() {
        @Override
        public void run() {
        }
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
