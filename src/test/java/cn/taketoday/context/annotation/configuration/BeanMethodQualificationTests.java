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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.BeanFactoryAnnotationUtils;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.testfixture.beans.NestedTestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.context.annotation.ScopedProxyMode;
import cn.taketoday.lang.Component;

/**
 * Tests proving that @Qualifier annotations work when used
 * with @Configuration classes on @Component methods.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class BeanMethodQualificationTests {

  @Test
  public void testStandard() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(StandardConfig.class, StandardPojo.class);
    Assertions.assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    StandardPojo pojo = ctx.getBean(StandardPojo.class);
    Assertions.assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    Assertions.assertThat(pojo.testBean2.getName()).isEqualTo("boring");
  }

  @Test
  public void testScoped() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(ScopedConfig.class, StandardPojo.class);
    Assertions.assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    StandardPojo pojo = ctx.getBean(StandardPojo.class);
    Assertions.assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    Assertions.assertThat(pojo.testBean2.getName()).isEqualTo("boring");
  }

  @Test
  public void testScopedProxy() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(ScopedProxyConfig.class, StandardPojo.class);
    Assertions.assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isTrue();  // a shared scoped proxy
    StandardPojo pojo = ctx.getBean(StandardPojo.class);
    Assertions.assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    Assertions.assertThat(pojo.testBean2.getName()).isEqualTo("boring");
  }

  @Test
  public void testCustomWithLazyResolution() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(CustomConfig.class, CustomPojo.class);
    Assertions.assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    Assertions.assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
    Assertions.assertThat(BeanFactoryAnnotationUtils.isQualifierMatch(value -> value.equals("boring"),
            "testBean2", ctx.getBeanFactory())).isTrue();
    CustomPojo pojo = ctx.getBean(CustomPojo.class);
    Assertions.assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    TestBean testBean2 = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
            ctx.getBeanFactory(), TestBean.class, "boring");
    Assertions.assertThat(testBean2.getName()).isEqualTo("boring");
  }

  @Test
  public void testCustomWithEarlyResolution() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(CustomConfig.class, CustomPojo.class);
    ctx.refresh();
    Assertions.assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    Assertions.assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
    ctx.getBean("testBean2");
    Assertions.assertThat(BeanFactoryAnnotationUtils.isQualifierMatch(value -> value.equals("boring"),
            "testBean2", ctx.getBeanFactory())).isTrue();
    CustomPojo pojo = ctx.getBean(CustomPojo.class);
    Assertions.assertThat(pojo.testBean.getName()).isEqualTo("interesting");
  }

  @Test
  public void testCustomWithAsm() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("customConfig", new RootBeanDefinition(CustomConfig.class.getName()));
    RootBeanDefinition customPojo = new RootBeanDefinition(CustomPojo.class.getName());
    customPojo.setLazyInit(true);
    ctx.registerBeanDefinition("customPojo", customPojo);
    ctx.refresh();
    Assertions.assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    Assertions.assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
    CustomPojo pojo = ctx.getBean(CustomPojo.class);
    Assertions.assertThat(pojo.testBean.getName()).isEqualTo("interesting");
  }

  @Test
  public void testCustomWithAttributeOverride() {
    AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(CustomConfigWithAttributeOverride.class, CustomPojo.class);
    Assertions.assertThat(ctx.getBeanFactory().containsSingleton("testBeanX")).isFalse();
    CustomPojo pojo = ctx.getBean(CustomPojo.class);
    Assertions.assertThat(pojo.testBean.getName()).isEqualTo("interesting");
  }

  @Test
  public void testBeanNamesForAnnotation() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(StandardConfig.class);
    Assertions.assertThat(ctx.getBeanNamesForAnnotation(Configuration.class)).isEqualTo(Set.of("beanMethodQualificationTests.StandardConfig"));
    Assertions.assertThat(ctx.getBeanNamesForAnnotation(Scope.class)).isEqualTo(Set.of());
    Assertions.assertThat(ctx.getBeanNamesForAnnotation(Lazy.class)).isEqualTo(Set.of("testBean1"));
    Assertions.assertThat(ctx.getBeanNamesForAnnotation(Boring.class)).isEqualTo(Set.of("testBean2"));
  }

  @Configuration
  static class StandardConfig {

    @Bean
    @Qualifier("interesting")
    @Lazy
    public TestBean testBean1() {
      return new TestBean("interesting");
    }

    @Bean
    @Boring
    public TestBean testBean2(@Lazy TestBean testBean1) {
      TestBean tb = new TestBean("boring");
      tb.setSpouse(testBean1);
      return tb;
    }
  }

  @Configuration
  static class ScopedConfig {

    @Bean
    @Qualifier("interesting")
    @Scope("prototype")
    public TestBean testBean1() {
      return new TestBean("interesting");
    }

    @Bean
    @Boring
    @Scope("prototype")
    public TestBean testBean2(TestBean testBean1) {
      TestBean tb = new TestBean("boring");
      tb.setSpouse(testBean1);
      return tb;
    }
  }

  @Configuration
  static class ScopedProxyConfig {

    @Bean
    @Qualifier("interesting")
    @Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public TestBean testBean1() {
      return new TestBean("interesting");
    }

    @Bean
    @Boring
    @Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public TestBean testBean2(TestBean testBean1) {
      TestBean tb = new TestBean("boring");
      tb.setSpouse(testBean1);
      return tb;
    }
  }

  @Component
  @Lazy
  static class StandardPojo {

    @Autowired
    @Qualifier("interesting")
    TestBean testBean;

    @Autowired
    @Boring
    TestBean testBean2;
  }

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Boring {
  }

  @Configuration
  static class CustomConfig {

    @InterestingBean
    public TestBean testBean1() {
      return new TestBean("interesting");
    }

    @Bean
    @Qualifier("boring")
    @Lazy
    public TestBean testBean2(@Lazy TestBean testBean1) {
      TestBean tb = new TestBean("boring");
      tb.setSpouse(testBean1);
      return tb;
    }
  }

  @Configuration
  static class CustomConfigWithAttributeOverride {

    @InterestingBeanWithName(name = "testBeanX")
    public TestBean testBean1() {
      return new TestBean("interesting");
    }

    @Bean
    @Qualifier("boring")
    public TestBean testBean2(@Lazy TestBean testBean1) {
      TestBean tb = new TestBean("boring");
      tb.setSpouse(testBean1);
      return tb;
    }
  }

  @InterestingPojo
  static class CustomPojo {

    @InterestingNeed
    TestBean testBean;

    @InterestingNeedWithRequiredOverride(required = false)
    NestedTestBean nestedTestBean;
  }

  @Bean
  @Lazy
  @Qualifier("interesting")
  @Retention(RetentionPolicy.RUNTIME)
  public @interface InterestingBean {
  }

  @Bean
  @Lazy
  @Qualifier("interesting")
  @Retention(RetentionPolicy.RUNTIME)
  public @interface InterestingBeanWithName {

    String name();
  }

  @Autowired
  @Qualifier("interesting")
  @Retention(RetentionPolicy.RUNTIME)
  public @interface InterestingNeed {
  }

  @Autowired
  @Qualifier("interesting")
  @Retention(RetentionPolicy.RUNTIME)
  public @interface InterestingNeedWithRequiredOverride {

    boolean required();
  }

  @Component
  @Lazy
  @Retention(RetentionPolicy.RUNTIME)
  public @interface InterestingPojo {
  }

}
