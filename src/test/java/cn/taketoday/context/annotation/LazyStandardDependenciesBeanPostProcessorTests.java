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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 4.0
 */
public class LazyStandardDependenciesBeanPostProcessorTests {

  private void doTestLazyResourceInjection(Class<? extends TestBeanHolder> annotatedBeanClass) {
    StandardApplicationContext ac = new StandardApplicationContext();
    BeanDefinition abd = new RootBeanDefinition(annotatedBeanClass);
    abd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    ac.registerBeanDefinition("annotatedBean", abd);
    BeanDefinition tbd = new RootBeanDefinition(TestBean.class);
    tbd.setLazyInit(true);
    ac.registerBeanDefinition("testBean", tbd);
    ac.refresh();

    ConfigurableBeanFactory bf = ac.getBeanFactory();
    TestBeanHolder bean = ac.getBean("annotatedBean", TestBeanHolder.class);
    assertThat(bf.containsSingleton("testBean")).isFalse();
    assertThat(bean.getTestBean()).isNotNull();
    assertThat(bean.getTestBean().getName()).isNull();
    assertThat(bf.containsSingleton("testBean")).isTrue();
    TestBean tb = (TestBean) ac.getBean("testBean");
    tb.setName("tb");
    assertThat(bean.getTestBean().getName()).isSameAs("tb");

    assertThat(ObjectUtils.containsElement(bf.getDependenciesForBean("annotatedBean"), "testBean")).isTrue();
    assertThat(ObjectUtils.containsElement(bf.getDependentBeans("testBean"), "annotatedBean")).isTrue();
  }

  @Test
  public void testLazyResourceInjectionWithField() {
    doTestLazyResourceInjection(FieldResourceInjectionBean.class);

    StandardApplicationContext ac = new StandardApplicationContext();
    BeanDefinition abd = new RootBeanDefinition(FieldResourceInjectionBean.class);
    abd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    ac.registerBeanDefinition("annotatedBean", abd);
    BeanDefinition tbd = new RootBeanDefinition(TestBean.class);
    tbd.setLazyInit(true);
    ac.registerBeanDefinition("testBean", tbd);
    ac.refresh();

    FieldResourceInjectionBean bean = ac.getBean("annotatedBean", FieldResourceInjectionBean.class);
    assertThat(ac.getBeanFactory().containsSingleton("testBean")).isFalse();
    assertThat(bean.getTestBeans().isEmpty()).isFalse();
    assertThat(bean.getTestBeans().get(0).getName()).isNull();
    assertThat(ac.getBeanFactory().containsSingleton("testBean")).isTrue();
    TestBean tb = (TestBean) ac.getBean("testBean");
    tb.setName("tb");
    assertThat(bean.getTestBean().getName()).isSameAs("tb");
  }

  @Test
  public void testLazyResourceInjectionWithFieldAndCustomAnnotation() {
    doTestLazyResourceInjection(FieldResourceInjectionBeanWithCompositeAnnotation.class);
  }

  @Test
  public void testLazyResourceInjectionWithMethod() {
    doTestLazyResourceInjection(MethodResourceInjectionBean.class);
  }

  @Test
  public void testLazyResourceInjectionWithMethodLevelLazy() {
    doTestLazyResourceInjection(MethodResourceInjectionBeanWithMethodLevelLazy.class);
  }

  @Test
  public void testLazyResourceInjectionWithMethodAndCustomAnnotation() {
    doTestLazyResourceInjection(MethodResourceInjectionBeanWithCompositeAnnotation.class);
  }

  @Test
  public void testLazyResourceInjectionWithConstructor() {
    doTestLazyResourceInjection(ConstructorResourceInjectionBean.class);
  }

  @Test
  public void testLazyResourceInjectionWithConstructorLevelLazy() {
    doTestLazyResourceInjection(ConstructorResourceInjectionBeanWithConstructorLevelLazy.class);
  }

  @Test
  public void testLazyResourceInjectionWithConstructorAndCustomAnnotation() {
    doTestLazyResourceInjection(ConstructorResourceInjectionBeanWithCompositeAnnotation.class);
  }

//  @Test
//  public void testLazyResourceInjectionWithNonExistingTarget() {
//    StandardBeanFactory bf = new StandardBeanFactory();
//    bf.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
//    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
//    bpp.setBeanFactory(bf);
//    bf.addBeanPostProcessor(bpp);
//    BeanDefinition bd = new BeanDefinition(FieldResourceInjectionBean.class);
//    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
//    bf.registerBeanDefinition("annotatedBean", bd);
//
//    FieldResourceInjectionBean bean = (FieldResourceInjectionBean) bf.getBean("annotatedBean");
//    assertThat(bean.getTestBean()).isNotNull();
//    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
//            bean.getTestBean().getName());
//  }

//  @Test
//  public void testLazyOptionalResourceInjectionWithNonExistingTarget() {
//    StandardBeanFactory bf = new StandardBeanFactory();
//    bf.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
//    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
//    bpp.setBeanFactory(bf);
//    bf.addBeanPostProcessor(bpp);
//    BeanDefinition bd = new BeanDefinition(OptionalFieldResourceInjectionBean.class);
//    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
//    bf.registerBeanDefinition("annotatedBean", bd);
//
//    OptionalFieldResourceInjectionBean bean = (OptionalFieldResourceInjectionBean) bf.getBean("annotatedBean");
//    assertThat(bean.getTestBean()).isNotNull();
//    assertThat(bean.getTestBeans()).isNotNull();
//    assertThat(bean.getTestBeans().isEmpty()).isTrue();
//    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
//            bean.getTestBean().getName());
//  }

  public interface TestBeanHolder {

    TestBean getTestBean();
  }

  public static class FieldResourceInjectionBean implements TestBeanHolder {

    @Autowired
    @Lazy
    private TestBean testBean;

    @Autowired
    @Lazy
    private List<TestBean> testBeans;

    @Override
    public TestBean getTestBean() {
      return this.testBean;
    }

    public List<TestBean> getTestBeans() {
      return testBeans;
    }
  }

  public static class OptionalFieldResourceInjectionBean implements TestBeanHolder {

    @Autowired(required = false)
    @Lazy
    private TestBean testBean;

    @Autowired(required = false)
    @Lazy
    private List<TestBean> testBeans;

    @Override
    public TestBean getTestBean() {
      return this.testBean;
    }

    public List<TestBean> getTestBeans() {
      return this.testBeans;
    }
  }

  public static class FieldResourceInjectionBeanWithCompositeAnnotation implements TestBeanHolder {

    @LazyInject
    private TestBean testBean;

    @Override
    public TestBean getTestBean() {
      return this.testBean;
    }
  }

  public static class MethodResourceInjectionBean implements TestBeanHolder {

    private TestBean testBean;

    @Autowired
    public void setTestBean(@Lazy TestBean testBean) {
      if (this.testBean != null) {
        throw new IllegalStateException("Already called");
      }
      this.testBean = testBean;
    }

    @Override
    public TestBean getTestBean() {
      return this.testBean;
    }
  }

  public static class MethodResourceInjectionBeanWithMethodLevelLazy implements TestBeanHolder {

    private TestBean testBean;

    @Autowired
    @Lazy
    public void setTestBean(TestBean testBean) {
      if (this.testBean != null) {
        throw new IllegalStateException("Already called");
      }
      this.testBean = testBean;
    }

    @Override
    public TestBean getTestBean() {
      return this.testBean;
    }
  }

  public static class MethodResourceInjectionBeanWithCompositeAnnotation implements TestBeanHolder {

    private TestBean testBean;

    @LazyInject
    public void setTestBean(TestBean testBean) {
      if (this.testBean != null) {
        throw new IllegalStateException("Already called");
      }
      this.testBean = testBean;
    }

    @Override
    public TestBean getTestBean() {
      return this.testBean;
    }
  }

  public static class ConstructorResourceInjectionBean implements TestBeanHolder {

    private final TestBean testBean;

    @Autowired
    public ConstructorResourceInjectionBean(@Lazy TestBean testBean) {
      this.testBean = testBean;
    }

    @Override
    public TestBean getTestBean() {
      return this.testBean;
    }
  }

  public static class ConstructorResourceInjectionBeanWithConstructorLevelLazy implements TestBeanHolder {

    private final TestBean testBean;

    @Autowired
    @Lazy
    public ConstructorResourceInjectionBeanWithConstructorLevelLazy(TestBean testBean) {
      this.testBean = testBean;
    }

    @Override
    public TestBean getTestBean() {
      return this.testBean;
    }
  }

  public static class ConstructorResourceInjectionBeanWithCompositeAnnotation implements TestBeanHolder {

    private final TestBean testBean;

    @LazyInject
    public ConstructorResourceInjectionBeanWithCompositeAnnotation(TestBean testBean) {
      this.testBean = testBean;
    }

    @Override
    public TestBean getTestBean() {
      return this.testBean;
    }
  }

  @Autowired
  @Lazy
  @Retention(RetentionPolicy.RUNTIME)
  public @interface LazyInject {
  }

}
