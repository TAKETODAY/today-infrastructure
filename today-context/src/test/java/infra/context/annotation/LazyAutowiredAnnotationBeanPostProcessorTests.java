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

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import infra.aop.TargetSource;
import infra.aop.framework.Advised;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.TestBean;
import infra.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/20 21:29
 */
class LazyAutowiredAnnotationBeanPostProcessorTests {

  private void doTestLazyResourceInjection(Class<? extends TestBeanHolder> annotatedBeanClass) {
    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
    RootBeanDefinition abd = new RootBeanDefinition(annotatedBeanClass);
    abd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    ac.registerBeanDefinition("annotatedBean", abd);
    RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
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
    ac.close();
  }

  @Test
  void lazyResourceInjectionWithField() throws Exception {
    doTestLazyResourceInjection(FieldResourceInjectionBean.class);

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
    RootBeanDefinition abd = new RootBeanDefinition(FieldResourceInjectionBean.class);
    abd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    ac.registerBeanDefinition("annotatedBean", abd);
    RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
    tbd.setLazyInit(true);
    ac.registerBeanDefinition("testBean", tbd);
    ac.refresh();

    FieldResourceInjectionBean bean = ac.getBean("annotatedBean", FieldResourceInjectionBean.class);
    assertThat(ac.getBeanFactory().containsSingleton("testBean")).isFalse();
    assertThat(bean.getTestBeans()).isNotEmpty();
    assertThat(bean.getTestBeans().get(0).getName()).isNull();
    assertThat(ac.getBeanFactory().containsSingleton("testBean")).isTrue();

    TestBean tb = (TestBean) ac.getBean("testBean");
    tb.setName("tb");
    assertThat(bean.getTestBean().getName()).isSameAs("tb");

    assertThat(bean.getTestBeans() instanceof Advised).isTrue();
    TargetSource targetSource = ((Advised) bean.getTestBeans()).getTargetSource();
    assertThat(targetSource.getTarget()).isSameAs(targetSource.getTarget());

    ac.close();
  }

  @Test
  void lazyResourceInjectionWithFieldForPrototype() {
    doTestLazyResourceInjection(FieldResourceInjectionBean.class);

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
    RootBeanDefinition abd = new RootBeanDefinition(FieldResourceInjectionBean.class);
    abd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    ac.registerBeanDefinition("annotatedBean", abd);
    RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
    tbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    tbd.setLazyInit(true);
    ac.registerBeanDefinition("testBean", tbd);
    ac.refresh();

    FieldResourceInjectionBean bean = ac.getBean("annotatedBean", FieldResourceInjectionBean.class);
    assertThat(bean.getTestBeans()).isNotEmpty();
    TestBean tb = bean.getTestBeans().get(0);
    assertThat(bean.getTestBeans().get(0)).isNotSameAs(tb);
    ac.close();
  }

  @Test
  void lazyResourceInjectionWithFieldAndCustomAnnotation() {
    doTestLazyResourceInjection(FieldResourceInjectionBeanWithCompositeAnnotation.class);
  }

  @Test
  void lazyResourceInjectionWithMethod() {
    doTestLazyResourceInjection(MethodResourceInjectionBean.class);
  }

  @Test
  void lazyResourceInjectionWithMethodLevelLazy() {
    doTestLazyResourceInjection(MethodResourceInjectionBeanWithMethodLevelLazy.class);
  }

  @Test
  void lazyResourceInjectionWithMethodAndCustomAnnotation() {
    doTestLazyResourceInjection(MethodResourceInjectionBeanWithCompositeAnnotation.class);
  }

  @Test
  void lazyResourceInjectionWithConstructor() {
    doTestLazyResourceInjection(ConstructorResourceInjectionBean.class);
  }

  @Test
  void lazyResourceInjectionWithConstructorLevelLazy() {
    doTestLazyResourceInjection(ConstructorResourceInjectionBeanWithConstructorLevelLazy.class);
  }

  @Test
  void lazyResourceInjectionWithConstructorAndCustomAnnotation() {
    doTestLazyResourceInjection(ConstructorResourceInjectionBeanWithCompositeAnnotation.class);
  }

  @Test
  void lazyResourceInjectionWithNonExistingTarget() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
    AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);
    RootBeanDefinition bd = new RootBeanDefinition(FieldResourceInjectionBean.class);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    bf.registerBeanDefinition("annotatedBean", bd);

    FieldResourceInjectionBean bean = (FieldResourceInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean.getTestBean()).isNotNull();
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
            bean.getTestBean().getName());
  }

  @Test
  void lazyOptionalResourceInjectionWithNonExistingTarget() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
    AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);
    RootBeanDefinition bd = new RootBeanDefinition(OptionalFieldResourceInjectionBean.class);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    bf.registerBeanDefinition("annotatedBean", bd);

    OptionalFieldResourceInjectionBean bean = (OptionalFieldResourceInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean.getTestBean()).isNotNull();
    assertThat(bean.getTestBeans()).isNotNull();
    assertThat(bean.getTestBeans()).isEmpty();
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
            bean.getTestBean().getName());
  }

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
