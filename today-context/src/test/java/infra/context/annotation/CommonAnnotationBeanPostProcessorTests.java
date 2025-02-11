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

import java.util.Properties;
import java.util.function.Supplier;

import infra.beans.BeansException;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.DestructionAwareBeanPostProcessor;
import infra.beans.factory.config.PropertyPlaceholderConfigurer;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.INestedTestBean;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.NestedTestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.support.GenericApplicationContext;
import infra.context.testfixture.jndi.ExpectedLookupTemplate;
import infra.core.testfixture.io.SerializationTestUtils;
import infra.jndi.support.SimpleJndiBeanFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 12:52
 */
class CommonAnnotationBeanPostProcessorTests {

  @Test
  void processInjection() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setResourceFactory(bf);
    bf.addBeanPostProcessor(bpp);
    
    ResourceInjectionBean bean = new ResourceInjectionBean();
    assertThat(bean.getTestBean()).isNull();
    assertThat(bean.getTestBean2()).isNull();

    TestBean tb = new TestBean();
    bf.registerSingleton("testBean", tb);
    bpp.processInjection(bean);

    assertThat(bean.getTestBean()).isSameAs(tb);
    assertThat(bean.getTestBean2()).isSameAs(tb);
  }

  @Test
  public void testPostConstructAndPreDestroy() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.addBeanPostProcessor(new CommonAnnotationBeanPostProcessor());
    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(AnnotatedInitDestroyBean.class));

    AnnotatedInitDestroyBean bean = (AnnotatedInitDestroyBean) bf.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    bf.destroySingletons();
    assertThat(bean.destroyCalled).isTrue();
  }

  @Test
  public void testPostConstructAndPreDestroyWithPostProcessor() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.addBeanPostProcessor(new InitDestroyBeanPostProcessor());
    bf.addBeanPostProcessor(new CommonAnnotationBeanPostProcessor());
    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(AnnotatedInitDestroyBean.class));

    AnnotatedInitDestroyBean bean = (AnnotatedInitDestroyBean) bf.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    bf.destroySingletons();
    assertThat(bean.destroyCalled).isTrue();
  }

  @Test
  public void testPostConstructAndPreDestroyWithApplicationContextAndPostProcessor() {
    GenericApplicationContext ctx = new GenericApplicationContext();
    ctx.registerBeanDefinition("bpp1", new RootBeanDefinition(InitDestroyBeanPostProcessor.class));
    ctx.registerBeanDefinition("bpp2", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
    ctx.registerBeanDefinition("annotatedBean", new RootBeanDefinition(AnnotatedInitDestroyBean.class));
    ctx.refresh();

    AnnotatedInitDestroyBean bean = (AnnotatedInitDestroyBean) ctx.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    ctx.close();
    assertThat(bean.destroyCalled).isTrue();
  }

  @Test
  public void testPostConstructAndPreDestroyWithLegacyAnnotations() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.addBeanPostProcessor(new CommonAnnotationBeanPostProcessor());
    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(LegacyAnnotatedInitDestroyBean.class));

    LegacyAnnotatedInitDestroyBean bean = (LegacyAnnotatedInitDestroyBean) bf.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    bf.destroySingletons();
    assertThat(bean.destroyCalled).isTrue();
  }

  @Test
  public void testPostConstructAndPreDestroyWithManualConfiguration() {
    StandardBeanFactory bf = new StandardBeanFactory();
    InitDestroyAnnotationBeanPostProcessor bpp = new InitDestroyAnnotationBeanPostProcessor();
    bpp.setInitAnnotationType(PostConstruct.class);
    bpp.setDestroyAnnotationType(PreDestroy.class);
    bf.addBeanPostProcessor(bpp);
    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(AnnotatedInitDestroyBean.class));

    AnnotatedInitDestroyBean bean = (AnnotatedInitDestroyBean) bf.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    bf.destroySingletons();
    assertThat(bean.destroyCalled).isTrue();
  }

  @Test
  public void testPostProcessorWithNullBean() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.addBeanPostProcessor(new CommonAnnotationBeanPostProcessor());
    RootBeanDefinition rbd = new RootBeanDefinition(NullFactory.class);
    rbd.setFactoryMethodName("create");
    bf.registerBeanDefinition("bean", rbd);

    assertThat(bf.getBean("bean")).isNull();
    bf.destroySingletons();
  }

  @Test
  public void testSerialization() throws Exception {
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    CommonAnnotationBeanPostProcessor bpp2 = SerializationTestUtils.serializeAndDeserialize(bpp);

    AnnotatedInitDestroyBean bean = new AnnotatedInitDestroyBean();
    bpp2.postProcessBeforeDestruction(bean, "annotatedBean");
    assertThat(bean.destroyCalled).isTrue();
  }

  @Test
  public void testSerializationWithManualConfiguration() throws Exception {
    InitDestroyAnnotationBeanPostProcessor bpp = new InitDestroyAnnotationBeanPostProcessor();
    bpp.setInitAnnotationType(PostConstruct.class);
    bpp.setDestroyAnnotationType(PreDestroy.class);
    InitDestroyAnnotationBeanPostProcessor bpp2 = SerializationTestUtils.serializeAndDeserialize(bpp);

    AnnotatedInitDestroyBean bean = new AnnotatedInitDestroyBean();
    bpp2.postProcessBeforeDestruction(bean, "annotatedBean");
    assertThat(bean.destroyCalled).isTrue();
  }

  @Test
  public void testResourceInjection() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setResourceFactory(bf);
    bf.addBeanPostProcessor(bpp);
    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ResourceInjectionBean.class));
    TestBean tb = new TestBean();
    bf.registerSingleton("testBean", tb);
    TestBean tb2 = new TestBean();
    bf.registerSingleton("testBean2", tb2);

    ResourceInjectionBean bean = (ResourceInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    assertThat(bean.init2Called).isTrue();
    assertThat(bean.init3Called).isTrue();
    assertThat(bean.getTestBean()).isSameAs(tb);
    assertThat(bean.getTestBean2()).isSameAs(tb2);
    bf.destroySingletons();
    assertThat(bean.destroyCalled).isTrue();
    assertThat(bean.destroy2Called).isTrue();
    assertThat(bean.destroy3Called).isTrue();
  }

  @Test
  public void testResourceInjectionWithPrototypes() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setResourceFactory(bf);
    bf.addBeanPostProcessor(bpp);
    RootBeanDefinition abd = new RootBeanDefinition(ResourceInjectionBean.class);
    abd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    bf.registerBeanDefinition("annotatedBean", abd);
    RootBeanDefinition tbd1 = new RootBeanDefinition(TestBean.class);
    tbd1.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    bf.registerBeanDefinition("testBean", tbd1);
    RootBeanDefinition tbd2 = new RootBeanDefinition(TestBean.class);
    tbd2.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    bf.registerBeanDefinition("testBean2", tbd2);

    ResourceInjectionBean bean = (ResourceInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    assertThat(bean.init2Called).isTrue();
    assertThat(bean.init3Called).isTrue();

    TestBean tb = bean.getTestBean();
    TestBean tb2 = bean.getTestBean2();
    assertThat(tb).isNotNull();
    assertThat(tb2).isNotNull();

    ResourceInjectionBean anotherBean = (ResourceInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean).isNotSameAs(anotherBean);
    assertThat(tb).isNotSameAs(anotherBean.getTestBean());
    assertThat(tb2).isNotSameAs(anotherBean.getTestBean2());

    bf.destroyBean("annotatedBean", bean);
    assertThat(bean.destroyCalled).isTrue();
    assertThat(bean.destroy2Called).isTrue();
    assertThat(bean.destroy3Called).isTrue();
  }

  @Test
  public void testResourceInjectionWithLegacyAnnotations() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setResourceFactory(bf);
    bf.addBeanPostProcessor(bpp);
    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(LegacyResourceInjectionBean.class));
    TestBean tb = new TestBean();
    bf.registerSingleton("testBean", tb);
    TestBean tb2 = new TestBean();
    bf.registerSingleton("testBean2", tb2);

    LegacyResourceInjectionBean bean = (LegacyResourceInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    assertThat(bean.init2Called).isTrue();
    assertThat(bean.init3Called).isTrue();
    assertThat(bean.getTestBean()).isSameAs(tb);
    assertThat(bean.getTestBean2()).isSameAs(tb2);
    bf.destroySingletons();
    assertThat(bean.destroyCalled).isTrue();
    assertThat(bean.destroy2Called).isTrue();
    assertThat(bean.destroy3Called).isTrue();
  }

  @Test
  public void testResourceInjectionWithResolvableDependencyType() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);
    RootBeanDefinition abd = new RootBeanDefinition(ExtendedResourceInjectionBean.class);
    abd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    bf.registerBeanDefinition("annotatedBean", abd);
    RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
    tbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    bf.registerBeanDefinition("testBean4", tbd);

    bf.registerResolvableDependency(BeanFactory.class, bf);
    bf.registerResolvableDependency(INestedTestBean.class, (Supplier<NestedTestBean>) NestedTestBean::new);

    PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
    Properties props = new Properties();
    props.setProperty("tb", "testBean4");
    ppc.setProperties(props);
    ppc.postProcessBeanFactory(bf);

    ExtendedResourceInjectionBean bean = (ExtendedResourceInjectionBean) bf.getBean("annotatedBean");
    INestedTestBean tb = bean.getTestBean6();
    assertThat(tb).isNotNull();

    ExtendedResourceInjectionBean anotherBean = (ExtendedResourceInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean).isNotSameAs(anotherBean);
    assertThat(tb).isNotSameAs(anotherBean.getTestBean6());

    String[] depBeans = bf.getDependenciesForBean("annotatedBean");
    assertThat(depBeans).hasSize(1);
    assertThat(depBeans[0]).isEqualTo("testBean4");
  }

  @Test
  public void testResourceInjectionWithDefaultMethod() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);
    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(DefaultMethodResourceInjectionBean.class));
    TestBean tb2 = new TestBean();
    bf.registerSingleton("testBean2", tb2);
    NestedTestBean tb7 = new NestedTestBean();
    bf.registerSingleton("testBean7", tb7);

    DefaultMethodResourceInjectionBean bean = (DefaultMethodResourceInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean.getTestBean2()).isSameAs(tb2);
    assertThat(bean.counter).isSameAs(2);

    bf.destroySingletons();
    assertThat(bean.counter).isSameAs(3);
  }

  @Test
  public void testResourceInjectionWithTwoProcessors() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setResourceFactory(bf);
    bf.addBeanPostProcessor(bpp);
    CommonAnnotationBeanPostProcessor bpp2 = new CommonAnnotationBeanPostProcessor();
    bpp2.setResourceFactory(bf);
    bf.addBeanPostProcessor(bpp2);
    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ResourceInjectionBean.class));
    TestBean tb = new TestBean();
    bf.registerSingleton("testBean", tb);
    TestBean tb2 = new TestBean();
    bf.registerSingleton("testBean2", tb2);

    ResourceInjectionBean bean = (ResourceInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    assertThat(bean.init2Called).isTrue();
    assertThat(bean.getTestBean()).isSameAs(tb);
    assertThat(bean.getTestBean2()).isSameAs(tb2);
    bf.destroySingletons();
    assertThat(bean.destroyCalled).isTrue();
    assertThat(bean.destroy2Called).isTrue();
  }

  @Test
  public void testResourceInjectionFromJndi() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    SimpleJndiBeanFactory resourceFactory = new SimpleJndiBeanFactory();
    ExpectedLookupTemplate jndiTemplate = new ExpectedLookupTemplate();
    TestBean tb = new TestBean();
    jndiTemplate.addObject("java:comp/env/testBean", tb);
    TestBean tb2 = new TestBean();
    jndiTemplate.addObject("java:comp/env/testBean2", tb2);
    resourceFactory.setJndiTemplate(jndiTemplate);
    bpp.setResourceFactory(resourceFactory);
    bf.addBeanPostProcessor(bpp);
    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ResourceInjectionBean.class));

    ResourceInjectionBean bean = (ResourceInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    assertThat(bean.init2Called).isTrue();
    assertThat(bean.getTestBean()).isSameAs(tb);
    assertThat(bean.getTestBean2()).isSameAs(tb2);
    bf.destroySingletons();
    assertThat(bean.destroyCalled).isTrue();
    assertThat(bean.destroy2Called).isTrue();
  }

  @Test
  public void testExtendedResourceInjection() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);
    bf.registerResolvableDependency(BeanFactory.class, bf);

    @SuppressWarnings("deprecation")
    PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
    Properties props = new Properties();
    props.setProperty("tb", "testBean3");
    ppc.setProperties(props);
    ppc.postProcessBeanFactory(bf);

    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ExtendedResourceInjectionBean.class));
    bf.registerBeanDefinition("annotatedBean2", new RootBeanDefinition(NamedResourceInjectionBean.class));
    bf.registerBeanDefinition("annotatedBean3", new RootBeanDefinition(ConvertedResourceInjectionBean.class));
    TestBean tb = new TestBean();
    bf.registerSingleton("testBean", tb);
    TestBean tb2 = new TestBean();
    bf.registerSingleton("testBean2", tb2);
    TestBean tb3 = new TestBean();
    bf.registerSingleton("testBean3", tb3);
    TestBean tb4 = new TestBean();
    bf.registerSingleton("testBean4", tb4);
    NestedTestBean tb6 = new NestedTestBean();
    bf.registerSingleton("value", "5");
    bf.registerSingleton("xy", tb6);
    bf.registerAlias("xy", "testBean9");

    ExtendedResourceInjectionBean bean = (ExtendedResourceInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    assertThat(bean.init2Called).isTrue();
    assertThat(bean.getTestBean()).isSameAs(tb);
    assertThat(bean.getTestBean2()).isSameAs(tb2);
    assertThat(bean.getTestBean3()).isSameAs(tb4);
    assertThat(bean.getTestBean4()).isSameAs(tb3);
    assertThat(bean.testBean5).isSameAs(tb6);
    assertThat(bean.testBean6).isSameAs(tb6);
    assertThat(bean.beanFactory).isSameAs(bf);

    NamedResourceInjectionBean bean2 = (NamedResourceInjectionBean) bf.getBean("annotatedBean2");
    assertThat(bean2.testBean).isSameAs(tb6);

    ConvertedResourceInjectionBean bean3 = (ConvertedResourceInjectionBean) bf.getBean("annotatedBean3");
    assertThat(bean3.value).isSameAs(5);

    bf.destroySingletons();
    assertThat(bean.destroyCalled).isTrue();
    assertThat(bean.destroy2Called).isTrue();
  }

  @Test
  public void testExtendedResourceInjectionWithOverriding() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);
    bf.registerResolvableDependency(BeanFactory.class, bf);

    @SuppressWarnings("deprecation")
    PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
    Properties props = new Properties();
    props.setProperty("tb", "testBean3");
    ppc.setProperties(props);
    ppc.postProcessBeanFactory(bf);

    RootBeanDefinition annotatedBd = new RootBeanDefinition(ExtendedResourceInjectionBean.class);
    TestBean tb5 = new TestBean();
    annotatedBd.getPropertyValues().add("testBean2", tb5);
    bf.registerBeanDefinition("annotatedBean", annotatedBd);
    bf.registerBeanDefinition("annotatedBean2", new RootBeanDefinition(NamedResourceInjectionBean.class));
    TestBean tb = new TestBean();
    bf.registerSingleton("testBean", tb);
    TestBean tb2 = new TestBean();
    bf.registerSingleton("testBean2", tb2);
    TestBean tb3 = new TestBean();
    bf.registerSingleton("testBean3", tb3);
    TestBean tb4 = new TestBean();
    bf.registerSingleton("testBean4", tb4);
    NestedTestBean tb6 = new NestedTestBean();
    bf.registerSingleton("xy", tb6);

    ExtendedResourceInjectionBean bean = (ExtendedResourceInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    assertThat(bean.init2Called).isTrue();
    assertThat(bean.getTestBean()).isSameAs(tb);
    assertThat(bean.getTestBean2()).isSameAs(tb5);
    assertThat(bean.getTestBean3()).isSameAs(tb4);
    assertThat(bean.getTestBean4()).isSameAs(tb3);
    assertThat(bean.testBean5).isSameAs(tb6);
    assertThat(bean.testBean6).isSameAs(tb6);
    assertThat(bean.beanFactory).isSameAs(bf);

    try {
      bf.getBean("annotatedBean2");
    }
    catch (BeanCreationException ex) {
      boolean condition = ex.getRootCause() instanceof NoSuchBeanDefinitionException;
      assertThat(condition).isTrue();
      NoSuchBeanDefinitionException innerEx = (NoSuchBeanDefinitionException) ex.getRootCause();
      assertThat(innerEx.getBeanName()).isEqualTo("testBean9");
    }

    bf.destroySingletons();
    assertThat(bean.destroyCalled).isTrue();
    assertThat(bean.destroy2Called).isTrue();
  }

  @Test
  public void testExtendedEjbInjection() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);
    bf.registerResolvableDependency(BeanFactory.class, bf);

    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ExtendedEjbInjectionBean.class));
    TestBean tb = new TestBean();
    bf.registerSingleton("testBean", tb);
    TestBean tb2 = new TestBean();
    bf.registerSingleton("testBean2", tb2);
    TestBean tb3 = new TestBean();
    bf.registerSingleton("testBean3", tb3);
    TestBean tb4 = new TestBean();
    bf.registerSingleton("testBean4", tb4);
    NestedTestBean tb6 = new NestedTestBean();
    bf.registerSingleton("xy", tb6);
    bf.registerAlias("xy", "testBean9");

    ExtendedEjbInjectionBean bean = (ExtendedEjbInjectionBean) bf.getBean("annotatedBean");
    assertThat(bean.initCalled).isTrue();
    assertThat(bean.init2Called).isTrue();
    assertThat(bean.getTestBean()).isSameAs(tb);
    assertThat(bean.getTestBean2()).isSameAs(tb2);
    assertThat(bean.getTestBean3()).isSameAs(tb4);
    assertThat(bean.getTestBean4()).isSameAs(tb3);
    assertThat(bean.testBean5).isSameAs(tb6);
    assertThat(bean.testBean6).isSameAs(tb6);
    assertThat(bean.beanFactory).isSameAs(bf);

    bf.destroySingletons();
    assertThat(bean.destroyCalled).isTrue();
    assertThat(bean.destroy2Called).isTrue();
  }

  @Test
  public void testLazyResolutionWithResourceField() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);

    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(LazyResourceFieldInjectionBean.class));
    bf.registerBeanDefinition("testBean", new RootBeanDefinition(TestBean.class));

    LazyResourceFieldInjectionBean bean = (LazyResourceFieldInjectionBean) bf.getBean("annotatedBean");
    assertThat(bf.containsSingleton("testBean")).isFalse();
    bean.testBean.setName("notLazyAnymore");
    assertThat(bf.containsSingleton("testBean")).isTrue();
    TestBean tb = (TestBean) bf.getBean("testBean");
    assertThat(tb.getName()).isEqualTo("notLazyAnymore");
  }

  @Test
  public void testLazyResolutionWithResourceMethod() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);

    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(LazyResourceMethodInjectionBean.class));
    bf.registerBeanDefinition("testBean", new RootBeanDefinition(TestBean.class));

    LazyResourceMethodInjectionBean bean = (LazyResourceMethodInjectionBean) bf.getBean("annotatedBean");
    assertThat(bf.containsSingleton("testBean")).isFalse();
    bean.testBean.setName("notLazyAnymore");
    assertThat(bf.containsSingleton("testBean")).isTrue();
    TestBean tb = (TestBean) bf.getBean("testBean");
    assertThat(tb.getName()).isEqualTo("notLazyAnymore");
  }

  @Test
  public void testLazyResolutionWithCglibProxy() {
    StandardBeanFactory bf = new StandardBeanFactory();
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);

    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(LazyResourceCglibInjectionBean.class));
    bf.registerBeanDefinition("testBean", new RootBeanDefinition(TestBean.class));

    LazyResourceCglibInjectionBean bean = (LazyResourceCglibInjectionBean) bf.getBean("annotatedBean");
    assertThat(bf.containsSingleton("testBean")).isFalse();
    bean.testBean.setName("notLazyAnymore");
    assertThat(bf.containsSingleton("testBean")).isTrue();
    TestBean tb = (TestBean) bf.getBean("testBean");
    assertThat(tb.getName()).isEqualTo("notLazyAnymore");
  }

  @Test
  public void testLazyResolutionWithFallbackTypeMatch() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
    CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);

    bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(LazyResourceCglibInjectionBean.class));
    bf.registerBeanDefinition("tb", new RootBeanDefinition(TestBean.class));

    LazyResourceCglibInjectionBean bean = (LazyResourceCglibInjectionBean) bf.getBean("annotatedBean");
    assertThat(bf.containsSingleton("tb")).isFalse();
    bean.testBean.setName("notLazyAnymore");
    assertThat(bf.containsSingleton("tb")).isTrue();
    TestBean tb = (TestBean) bf.getBean("tb");
    assertThat(tb.getName()).isEqualTo("notLazyAnymore");
  }

  public static class AnnotatedInitDestroyBean {

    public boolean initCalled = false;

    public boolean destroyCalled = false;

    @PostConstruct
    private void init() {
      if (this.initCalled) {
        throw new IllegalStateException("Already called");
      }
      this.initCalled = true;
    }

    @PreDestroy
    private void destroy() {
      if (this.destroyCalled) {
        throw new IllegalStateException("Already called");
      }
      this.destroyCalled = true;
    }
  }

  public static class LegacyAnnotatedInitDestroyBean {

    public boolean initCalled = false;

    public boolean destroyCalled = false;

    @javax.annotation.PostConstruct
    private void init() {
      if (this.initCalled) {
        throw new IllegalStateException("Already called");
      }
      this.initCalled = true;
    }

    @javax.annotation.PreDestroy
    private void destroy() {
      if (this.destroyCalled) {
        throw new IllegalStateException("Already called");
      }
      this.destroyCalled = true;
    }
  }

  public static class InitDestroyBeanPostProcessor
          implements DestructionAwareBeanPostProcessor, InitializationBeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      if (bean instanceof AnnotatedInitDestroyBean) {
        assertThat(((AnnotatedInitDestroyBean) bean).initCalled).isFalse();
      }
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      if (bean instanceof AnnotatedInitDestroyBean) {
        assertThat(((AnnotatedInitDestroyBean) bean).initCalled).isTrue();
      }
      return bean;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
      if (bean instanceof AnnotatedInitDestroyBean) {
        assertThat(((AnnotatedInitDestroyBean) bean).destroyCalled).isFalse();
      }
    }

    @Override
    public boolean requiresDestruction(Object bean) {
      return true;
    }
  }

  public static class ResourceInjectionBean extends AnnotatedInitDestroyBean {

    public boolean init2Called = false;

    public boolean init3Called = false;

    public boolean destroy2Called = false;

    public boolean destroy3Called = false;

    @Resource
    private TestBean testBean;

    private TestBean testBean2;

    @PostConstruct
    protected void init2() {
      if (this.testBean == null || this.testBean2 == null) {
        throw new IllegalStateException("Resources not injected");
      }
      if (!this.initCalled) {
        throw new IllegalStateException("Superclass init method not called yet");
      }
      if (this.init2Called) {
        throw new IllegalStateException("Already called");
      }
      this.init2Called = true;
    }

    @PostConstruct
    private void init() {
      if (this.init3Called) {
        throw new IllegalStateException("Already called");
      }
      this.init3Called = true;
    }

    @PreDestroy
    protected void destroy2() {
      if (this.destroyCalled) {
        throw new IllegalStateException("Superclass destroy called too soon");
      }
      if (this.destroy2Called) {
        throw new IllegalStateException("Already called");
      }
      this.destroy2Called = true;
    }

    @PreDestroy
    private void destroy() {
      if (this.destroyCalled) {
        throw new IllegalStateException("Superclass destroy called too soon");
      }
      if (this.destroy3Called) {
        throw new IllegalStateException("Already called");
      }
      this.destroy3Called = true;
    }

    @Resource
    public void setTestBean2(TestBean testBean2) {
      if (this.testBean2 != null) {
        throw new IllegalStateException("Already called");
      }
      this.testBean2 = testBean2;
    }

    public TestBean getTestBean() {
      return testBean;
    }

    public TestBean getTestBean2() {
      return testBean2;
    }
  }

  public static class LegacyResourceInjectionBean extends LegacyAnnotatedInitDestroyBean {

    public boolean init2Called = false;

    public boolean init3Called = false;

    public boolean destroy2Called = false;

    public boolean destroy3Called = false;

    @javax.annotation.Resource
    private TestBean testBean;

    private TestBean testBean2;

    @javax.annotation.PostConstruct
    protected void init2() {
      if (this.testBean == null || this.testBean2 == null) {
        throw new IllegalStateException("Resources not injected");
      }
      if (!this.initCalled) {
        throw new IllegalStateException("Superclass init method not called yet");
      }
      if (this.init2Called) {
        throw new IllegalStateException("Already called");
      }
      this.init2Called = true;
    }

    @javax.annotation.PostConstruct
    private void init() {
      if (this.init3Called) {
        throw new IllegalStateException("Already called");
      }
      this.init3Called = true;
    }

    @javax.annotation.PreDestroy
    protected void destroy2() {
      if (this.destroyCalled) {
        throw new IllegalStateException("Superclass destroy called too soon");
      }
      if (this.destroy2Called) {
        throw new IllegalStateException("Already called");
      }
      this.destroy2Called = true;
    }

    @javax.annotation.PreDestroy
    private void destroy() {
      if (this.destroyCalled) {
        throw new IllegalStateException("Superclass destroy called too soon");
      }
      if (this.destroy3Called) {
        throw new IllegalStateException("Already called");
      }
      this.destroy3Called = true;
    }

    @javax.annotation.Resource
    public void setTestBean2(TestBean testBean2) {
      if (this.testBean2 != null) {
        throw new IllegalStateException("Already called");
      }
      this.testBean2 = testBean2;
    }

    public TestBean getTestBean() {
      return testBean;
    }

    public TestBean getTestBean2() {
      return testBean2;
    }
  }

  static class NonPublicResourceInjectionBean<B> extends ResourceInjectionBean {

    @Resource(name = "testBean4", type = TestBean.class)
    protected ITestBean testBean3;

    private B testBean4;

    @Resource
    INestedTestBean testBean5;

    INestedTestBean testBean6;

    @Resource
    BeanFactory beanFactory;

    @Override
    @Resource
    public void setTestBean2(TestBean testBean2) {
      super.setTestBean2(testBean2);
    }

    @Resource(name = "${tb}", type = ITestBean.class)
    private void setTestBean4(B testBean4) {
      if (this.testBean4 != null) {
        throw new IllegalStateException("Already called");
      }
      this.testBean4 = testBean4;
    }

    @Resource
    public void setTestBean6(INestedTestBean testBean6) {
      if (this.testBean6 != null) {
        throw new IllegalStateException("Already called");
      }
      this.testBean6 = testBean6;
    }

    public ITestBean getTestBean3() {
      return testBean3;
    }

    public B getTestBean4() {
      return testBean4;
    }

    public INestedTestBean getTestBean5() {
      return testBean5;
    }

    public INestedTestBean getTestBean6() {
      return testBean6;
    }

    @Override
    @PostConstruct
    protected void init2() {
      if (this.testBean3 == null || this.testBean4 == null) {
        throw new IllegalStateException("Resources not injected");
      }
      super.init2();
    }

    @Override
    @PreDestroy
    protected void destroy2() {
      super.destroy2();
    }
  }

  public static class ExtendedResourceInjectionBean extends NonPublicResourceInjectionBean<ITestBean> {
  }

  public interface InterfaceWithDefaultMethod {

    @Resource
    void setTestBean2(TestBean testBean2);

    @Resource
    default void setTestBean7(INestedTestBean testBean7) {
      increaseCounter();
    }

    @PostConstruct
    default void initDefault() {
      increaseCounter();
    }

    @PreDestroy
    default void destroyDefault() {
      increaseCounter();
    }

    void increaseCounter();
  }

  public static class DefaultMethodResourceInjectionBean extends ResourceInjectionBean
          implements InterfaceWithDefaultMethod {

    public int counter = 0;

    @Override
    public void increaseCounter() {
      counter++;
    }
  }

  public static class ExtendedEjbInjectionBean extends ResourceInjectionBean {

    @EJB(name = "testBean4", beanInterface = TestBean.class)
    protected ITestBean testBean3;

    private ITestBean testBean4;

    @EJB
    private INestedTestBean testBean5;

    private INestedTestBean testBean6;

    @Resource
    private BeanFactory beanFactory;

    @Override
    @EJB
    public void setTestBean2(TestBean testBean2) {
      super.setTestBean2(testBean2);
    }

    @EJB(beanName = "testBean3", beanInterface = ITestBean.class)
    private void setTestBean4(ITestBean testBean4) {
      if (this.testBean4 != null) {
        throw new IllegalStateException("Already called");
      }
      this.testBean4 = testBean4;
    }

    @EJB
    public void setTestBean6(INestedTestBean testBean6) {
      if (this.testBean6 != null) {
        throw new IllegalStateException("Already called");
      }
      this.testBean6 = testBean6;
    }

    public ITestBean getTestBean3() {
      return testBean3;
    }

    public ITestBean getTestBean4() {
      return testBean4;
    }

    @Override
    @PostConstruct
    protected void init2() {
      if (this.testBean3 == null || this.testBean4 == null) {
        throw new IllegalStateException("Resources not injected");
      }
      super.init2();
    }

    @Override
    @PreDestroy
    protected void destroy2() {
      super.destroy2();
    }
  }

  private static class NamedResourceInjectionBean {

    @Resource(name = "testBean9")
    private INestedTestBean testBean;
  }

  private static class ConvertedResourceInjectionBean {

    @Resource(name = "value")
    private int value;
  }

  private static class LazyResourceFieldInjectionBean {

    @Resource
    @Lazy
    private ITestBean testBean;
  }

  private static class LazyResourceMethodInjectionBean {

    private ITestBean testBean;

    @Resource
    @Lazy
    public void setTestBean(ITestBean testBean) {
      this.testBean = testBean;
    }
  }

  private static class LazyResourceCglibInjectionBean {

    private TestBean testBean;

    @Resource
    @Lazy
    public void setTestBean(TestBean testBean) {
      this.testBean = testBean;
    }
  }

  @SuppressWarnings("unused")
  private static class NullFactory {

    public static Object create() {
      return null;
    }
  }

}
