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

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.BeanFactoryAnnotationUtils;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.testfixture.beans.NestedTestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.lang.Component;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
    StandardApplicationContext ctx =
            new StandardApplicationContext(StandardConfig.class, StandardPojo.class);
    assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    StandardPojo pojo = ctx.getBean(StandardPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    assertThat(pojo.testBean2.getName()).isEqualTo("boring");
  }

  @Test
  public void testScoped() {
    StandardApplicationContext ctx =
            new StandardApplicationContext(ScopedConfig.class, StandardPojo.class);
    assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    StandardPojo pojo = ctx.getBean(StandardPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    assertThat(pojo.testBean2.getName()).isEqualTo("boring");
  }

/*  @Test
  public void testScopedProxy() {
    StandardApplicationContext ctx =
            new StandardApplicationContext(ScopedProxyConfig.class, StandardPojo.class);
    assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isTrue();  // a shared scoped proxy
    StandardPojo pojo = ctx.getBean(StandardPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    assertThat(pojo.testBean2.getName()).isEqualTo("boring");
  }*/

  @Test
  public void testCustomWithLazyResolution() {
    StandardApplicationContext ctx =
            new StandardApplicationContext(CustomConfig.class, CustomPojo.class);
    assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
    assertThat(BeanFactoryAnnotationUtils.isQualifierMatch(value -> value.equals("boring"),
            "testBean2", ctx.getBeanFactory())).isTrue();
    CustomPojo pojo = ctx.getBean(CustomPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    TestBean testBean2 = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
            ctx.getBeanFactory(), TestBean.class, "boring");
    assertThat(testBean2.getName()).isEqualTo("boring");
  }

  @Test
  public void testCustomWithEarlyResolution() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(CustomConfig.class, CustomPojo.class);
    ctx.refresh();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
    ctx.getBean("testBean2");
    assertThat(BeanFactoryAnnotationUtils.isQualifierMatch(value -> value.equals("boring"),
            "testBean2", ctx.getBeanFactory())).isTrue();
    CustomPojo pojo = ctx.getBean(CustomPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");
  }

  @Test
  public void testCustomWithAsm() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.registerBeanDefinition("customConfig", new BeanDefinition(CustomConfig.class.getName()));
    BeanDefinition customPojo = new BeanDefinition(CustomPojo.class.getName());
    customPojo.setLazyInit(true);
    ctx.registerBeanDefinition("customPojo", customPojo);
    ctx.refresh();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
    CustomPojo pojo = ctx.getBean(CustomPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");
  }

  @Test
  public void testCustomWithAttributeOverride() {
    StandardApplicationContext ctx =
            new StandardApplicationContext(CustomConfigWithAttributeOverride.class, CustomPojo.class);
    assertThat(ctx.getBeanFactory().containsSingleton("testBeanX")).isFalse();
    CustomPojo pojo = ctx.getBean(CustomPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");
  }

  @Test
  public void testBeanNamesForAnnotation() {
    StandardApplicationContext ctx = new StandardApplicationContext(StandardConfig.class);

    Set<String> beanNamesForAnnotation = ctx.getBeanNamesForAnnotation(Configuration.class);
    assertThat(beanNamesForAnnotation).isEqualTo(Set.of("standardConfig"));
    assertThat(ctx.getBeanNamesForAnnotation(Lazy.class)).isEqualTo(Set.of("testBean1"));
    assertThat(ctx.getBeanNamesForAnnotation(Boring.class)).isEqualTo(Set.of("testBean2"));
    Set<String> beanNamesForAnnotation1 = ctx.getBeanNamesForAnnotation(Scope.class);
    assertThat(beanNamesForAnnotation1.isEmpty()).isTrue();
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
    @Scope(value = "prototype"/*, proxyMode = ScopedProxyMode.TARGET_CLASS*/)
    public TestBean testBean1() {
      return new TestBean("interesting");
    }

    @Bean
    @Boring
    @Scope(value = "prototype"/*, proxyMode = ScopedProxyMode.TARGET_CLASS*/)
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
