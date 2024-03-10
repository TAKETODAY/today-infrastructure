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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.context.annotation.ScopedProxyMode;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests proving that @Qualifier annotations work when used
 * with @Configuration classes on @Component methods.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class BeanMethodQualificationTests {

  @Test
  void standard() {
    AnnotationConfigApplicationContext ctx = context(StandardConfig.class, StandardPojo.class);

    assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    StandardPojo pojo = ctx.getBean(StandardPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    assertThat(pojo.testBean2.getName()).isEqualTo("boring");

    ctx.close();
  }

  @Test
  void scoped() {
    AnnotationConfigApplicationContext ctx = context(ScopedConfig.class, StandardPojo.class);

    assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    StandardPojo pojo = ctx.getBean(StandardPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    assertThat(pojo.testBean2.getName()).isEqualTo("boring");

    ctx.close();
  }

  @Test
  void scopedProxy() {
    AnnotationConfigApplicationContext ctx = context(ScopedProxyConfig.class, StandardPojo.class);

    assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isTrue();  // a shared scoped proxy
    StandardPojo pojo = ctx.getBean(StandardPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    assertThat(pojo.testBean2.getName()).isEqualTo("boring");

    ctx.close();
  }

  @SuppressWarnings("unchecked")
  @ParameterizedTest
  @ValueSource(classes = { PrimaryConfig.class, FallbackConfig.class })
  void primaryVersusFallback(Class<?> configClass) {
    AnnotationConfigApplicationContext ctx = context(configClass, StandardPojo.class, ConstructorPojo.class);

    StandardPojo pojo = ctx.getBean(StandardPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    assertThat(pojo.testBean2.getName()).isEqualTo("boring");
    assertThat(pojo.testBean2.getSpouse().getName()).isEqualTo("interesting");
    assertThat((List<Object>) pojo.testBean2.getPets()).contains(
            ctx.getBean("testBean1x"), ctx.getBean("testBean2x"));  // array injection
    assertThat((List<Object>) pojo.testBean2.getSomeList()).contains(
            ctx.getBean("testBean1x"), ctx.getBean("testBean2x"));  // list injection
    assertThat((Map<String, TestBean>) pojo.testBean2.getSomeMap()).containsKeys(
            "testBean1x", "testBean2x");  // map injection

    ConstructorPojo pojo2 = ctx.getBean(ConstructorPojo.class);
    assertThat(pojo2.testBean).isSameAs(pojo.testBean);
    assertThat(pojo2.testBean2).isSameAs(pojo.testBean2);

    ctx.close();
  }

  /**
   * One regular bean along with fallback beans is considered effective primary
   */
  @Test
  void effectivePrimary() {
    AnnotationConfigApplicationContext ctx = context(EffectivePrimaryConfig.class);

    TestBean testBean = ctx.getBean(TestBean.class);
    assertThat(testBean.getName()).isEqualTo("effective-primary");

    ctx.close();
  }

  @Test
  void customWithLazyResolution() {
    AnnotationConfigApplicationContext ctx = context(CustomConfig.class, CustomPojo.class);

    assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
    assertThat(BeanFactoryAnnotationUtils.isQualifierMatch(value -> value.equals("boring"),
            "testBean2", ctx.getBeanFactory())).isTrue();

    CustomPojo pojo = ctx.getBean(CustomPojo.class);
    assertThat(pojo.plainBean).isNull();
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");

    TestBean testBean2 = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
            ctx.getBeanFactory(), TestBean.class, "boring");
    assertThat(testBean2.getName()).isEqualTo("boring");

    ctx.close();
  }

  @Test
  void customWithEarlyResolution() {
    AnnotationConfigApplicationContext ctx = context(CustomConfig.class, CustomPojo.class);

    assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
    ctx.getBean("testBean2");
    assertThat(BeanFactoryAnnotationUtils.isQualifierMatch(value -> value.equals("boring"),
            "testBean2", ctx.getBeanFactory())).isTrue();

    CustomPojo pojo = ctx.getBean(CustomPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");

    ctx.close();
  }

  @Test
  void customWithAsm() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("customConfig", new RootBeanDefinition(CustomConfig.class.getName()));
    RootBeanDefinition customPojo = new RootBeanDefinition(CustomPojo.class.getName());
    customPojo.setLazyInit(true);
    ctx.registerBeanDefinition("customPojo", customPojo);
    ctx.refresh();

    assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
    assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
    CustomPojo pojo = ctx.getBean(CustomPojo.class);
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");

    ctx.close();
  }

  @Test
  void customWithAttributeOverride() {
    AnnotationConfigApplicationContext ctx = context(CustomConfigWithAttributeOverride.class, CustomPojo.class);

    assertThat(ctx.getBeanFactory().containsSingleton("testBeanX")).isFalse();
    CustomPojo pojo = ctx.getBean(CustomPojo.class);
    assertThat(pojo.plainBean).isNull();
    assertThat(pojo.testBean.getName()).isEqualTo("interesting");
    assertThat(pojo.nestedTestBean).isNull();

    ctx.close();
  }

  @Test
  void beanNamesForAnnotation() {
    AnnotationConfigApplicationContext ctx = context(StandardConfig.class);

    assertThat(ctx.getBeanNamesForAnnotation(Configuration.class))
            .containsExactly("beanMethodQualificationTests.StandardConfig");
    assertThat(ctx.getBeanNamesForAnnotation(Scope.class)).isEmpty();
    assertThat(ctx.getBeanNamesForAnnotation(Lazy.class)).containsExactly("testBean1");
    assertThat(ctx.getBeanNamesForAnnotation(Boring.class))
            .containsExactly("beanMethodQualificationTests.StandardConfig", "testBean2");

    ctx.close();
  }

  @Configuration
  @Boring
  static class StandardConfig {

    @Bean
    @Qualifier("interesting")
    @Lazy
    public static TestBean testBean1() {
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
  @Boring
  static class ScopedConfig {

    @Bean
    @Qualifier("interesting")
    @Scope("prototype")
    public static TestBean testBean1() {
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
  @Boring
  static class ScopedProxyConfig {

    @Bean
    @Qualifier("interesting")
    @Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public static TestBean testBean1() {
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

  @Configuration
  static class PrimaryConfig {

    @Bean
    @Qualifier("interesting")
    @Primary
    public static TestBean testBean1() {
      return new TestBean("interesting");
    }

    @Bean
    @Qualifier("interesting")
    public static TestBean testBean1x() {
      return new TestBean("");
    }

    @Bean
    @Boring
    @Primary
    public TestBean testBean2(TestBean testBean1, TestBean[] testBeanArray,
            List<TestBean> testBeanList, Map<String, TestBean> testBeanMap) {

      TestBean tb = new TestBean("boring");
      tb.setSpouse(testBean1);
      tb.setPets(CollectionUtils.arrayToList(testBeanArray));
      tb.setSomeList(testBeanList);
      tb.setSomeMap(testBeanMap);
      return tb;
    }

    @Bean
    @Boring
    public TestBean testBean2x() {
      return new TestBean("");
    }
  }

  @Configuration
  static class FallbackConfig {

    @Bean
    @Qualifier("interesting")
    public static TestBean testBean1() {
      return new TestBean("interesting");
    }

    @Bean
    @Qualifier("interesting")
    @Fallback
    public static TestBean testBean1x() {
      return new TestBean("");
    }

    @Bean
    @Boring
    public TestBean testBean2(TestBean testBean1, TestBean[] testBeanArray,
            List<TestBean> testBeanList, Map<String, TestBean> testBeanMap) {

      TestBean tb = new TestBean("boring");
      tb.setSpouse(testBean1);
      tb.setPets(CollectionUtils.arrayToList(testBeanArray));
      tb.setSomeList(testBeanList);
      tb.setSomeMap(testBeanMap);
      return tb;
    }

    @Bean
    @Boring
    @Fallback
    public TestBean testBean2x() {
      return new TestBean("");
    }
  }

  @Configuration
  static class EffectivePrimaryConfig {

    @Bean
    public TestBean effectivePrimary() {
      return new TestBean("effective-primary");
    }

    @Bean
    @Fallback
    public TestBean fallback1() {
      return new TestBean("fallback1");
    }

    @Bean
    @Fallback
    public TestBean fallback2() {
      return new TestBean("fallback2");
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

  @Component
  @Lazy
  static class ConstructorPojo {

    TestBean testBean;

    TestBean testBean2;

    ConstructorPojo(@Qualifier("interesting") TestBean testBean, @Boring TestBean testBean2) {
      this.testBean = testBean;
      this.testBean2 = testBean2;
    }
  }

  @Configuration
  static class CustomConfig {

    @InterestingBean
    public TestBean testBean1() {
      return new TestBean("interesting");
    }

    @Bean(defaultCandidate = false)
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

    @Bean(defaultCandidate = false)
    @Qualifier("boring")
    public TestBean testBean2(@Lazy TestBean testBean1) {
      TestBean tb = new TestBean("boring");
      tb.setSpouse(testBean1);
      return tb;
    }
  }

  @InterestingPojo
  static class CustomPojo {

    TestBean plainBean;

    @InterestingNeed
    TestBean testBean;

    @InterestingNeedWithRequiredOverride(required = false)
    NestedTestBean nestedTestBean;

    public CustomPojo(Optional<TestBean> plainBean) {
      this.plainBean = plainBean.orElse(null);
    }
  }

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @interface Boring {
  }

  @Bean(defaultCandidate = false)
  @Lazy
  @Qualifier("interesting")
  @Retention(RetentionPolicy.RUNTIME)
  @interface InterestingBean {
  }

  @Bean(defaultCandidate = false)
  @Lazy
  @Qualifier("interesting")
  @Retention(RetentionPolicy.RUNTIME)
  @interface InterestingBeanWithName {

    @AliasFor(annotation = Bean.class)
    String name();
  }

  @Autowired
  @Qualifier("interesting")
  @Retention(RetentionPolicy.RUNTIME)
  @interface InterestingNeed {
  }

  @Autowired
  @Qualifier("interesting")
  @Retention(RetentionPolicy.RUNTIME)
  @interface InterestingNeedWithRequiredOverride {

    @AliasFor(annotation = Autowired.class)
    boolean required();
  }

  @Component
  @Lazy
  @Retention(RetentionPolicy.RUNTIME)
  @interface InterestingPojo {
  }

  private static AnnotationConfigApplicationContext context(Class<?>... componentClasses) {
    return new AnnotationConfigApplicationContext(componentClasses);
  }

}
