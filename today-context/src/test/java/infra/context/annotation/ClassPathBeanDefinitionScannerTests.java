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

package infra.context.annotation;

import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import example.scannable.CustomComponent;
import example.scannable.FooService;
import example.scannable.FooServiceImpl;
import example.scannable.NamedStubDao;
import example.scannable.ServiceInvocationCounter;
import example.scannable.StubFooDao;
import infra.beans.BeanInstantiationException;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StaticListableBeanFactory;
import infra.beans.testfixture.beans.TestBean;
import infra.context.MessageSource;
import infra.context.annotation2.NamedStubDao2;
import infra.context.index.CandidateComponentsIndex;
import infra.context.index.CandidateComponentsIndexLoader;
import infra.context.index.CandidateComponentsTestClassLoader;
import infra.context.support.GenericApplicationContext;
import infra.core.io.ClassPathResource;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.core.io.Resource;
import infra.core.type.filter.AnnotationTypeFilter;
import infra.core.type.filter.AssignableTypeFilter;
import infra.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class ClassPathBeanDefinitionScannerTests {

  private static final String BASE_PACKAGE = "example.scannable";

  @Test
  void testSimpleScanWithDefaultFiltersAndPostProcessors() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(12);
    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
    assertThat(context.containsBean("thoreau")).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();

    context.refresh();
    FooServiceImpl fooService = context.getBean("fooServiceImpl", FooServiceImpl.class);
    assertThat(context.getBeanFactory().containsSingleton("myNamedComponent")).isTrue();
    assertThat(fooService.foo(123)).isEqualTo("bar");
    assertThat(fooService.lookupFoo(123)).isEqualTo("bar");
    assertThat(context.isPrototype("thoreau")).isTrue();
  }

  @Test
  void testSimpleScanWithDefaultFiltersAndPrimaryLazyBean() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.scan(BASE_PACKAGE);
    scanner.scan("infra.context.annotation5");
    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
    assertThat(context.containsBean("otherFooDao")).isTrue();
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton("otherFooDao")).isFalse();
    assertThat(context.getBeanFactory().containsSingleton("fooServiceImpl")).isFalse();
    FooServiceImpl fooService = context.getBean("fooServiceImpl", FooServiceImpl.class);
    assertThat(context.getBeanFactory().containsSingleton("otherFooDao")).isTrue();
    assertThat(fooService.foo(123)).isEqualTo("other");
    assertThat(fooService.lookupFoo(123)).isEqualTo("other");
  }

  @Test
  void testSimpleScanWithIndex() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.setClassLoader(CandidateComponentsTestClassLoader.index(
            ClassPathScanningCandidateComponentProviderTests.class.getClassLoader(),
            new ClassPathResource("today.components", FooServiceImpl.class)));

    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(12);
    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
    assertThat(context.containsBean("thoreau")).isTrue();
  }

  @Test
  void testDoubleScan() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(12);

    ClassPathBeanDefinitionScanner scanner2 = new ClassPathBeanDefinitionScanner(context) {
      @Override
      protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
        super.postProcessBeanDefinition(beanDefinition, beanName);
        beanDefinition.setAttribute("someDifference", "someValue");
      }
    };
    scanner2.scan(BASE_PACKAGE);

    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
    assertThat(context.containsBean("thoreau")).isTrue();
  }

  @Test
  void testDoubleScanWithIndex() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.setClassLoader(CandidateComponentsTestClassLoader.index(
            ClassPathScanningCandidateComponentProviderTests.class.getClassLoader(),
            new ClassPathResource("today.components", FooServiceImpl.class)));

    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(12);

    ClassPathBeanDefinitionScanner scanner2 = new ClassPathBeanDefinitionScanner(context) {
      @Override
      protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
        super.postProcessBeanDefinition(beanDefinition, beanName);
        beanDefinition.setAttribute("someDifference", "someValue");
      }
    };
    scanner2.scan(BASE_PACKAGE);

    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
    assertThat(context.containsBean("thoreau")).isTrue();
  }

  @Test
  void testSimpleScanWithDefaultFiltersAndNoPostProcessors() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(7);
    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
  }

  @Test
  void testSimpleScanWithDefaultFiltersAndOverridingBean() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.setAllowBeanDefinitionOverriding(true);
    context.registerBeanDefinition("stubFooDao", new RootBeanDefinition(TestBean.class));
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);

    // should not fail!
    scanner.scan(BASE_PACKAGE);
  }

  @Test
  void testSimpleScanWithDefaultFiltersAndOverridingBeanNotAllowed() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.getBeanFactory().setAllowBeanDefinitionOverriding(false);
    context.registerBeanDefinition("stubFooDao", new RootBeanDefinition(TestBean.class));
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);

    assertThatIllegalStateException().isThrownBy(() -> scanner.scan(BASE_PACKAGE))
            .withMessageContaining("stubFooDao")
            .withMessageContaining(StubFooDao.class.getName());
  }

  @Test
  void testSimpleScanWithDefaultFiltersAndOverridingBeanAcceptedForSameBeanClass() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.getBeanFactory().setAllowBeanDefinitionOverriding(false);
    context.registerBeanDefinition("stubFooDao", new RootBeanDefinition(StubFooDao.class));
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);

    // should not fail!
    scanner.scan(BASE_PACKAGE);
  }

  @Test
  void testSimpleScanWithDefaultFiltersAndDefaultBeanNameClash() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    scanner.scan("infra.context.annotation3");

    assertThatIllegalStateException().isThrownBy(() -> scanner.scan(BASE_PACKAGE))
            .withMessageContaining("stubFooDao")
            .withMessageContaining(StubFooDao.class.getName());
  }

  @Test
  void testSimpleScanWithDefaultFiltersAndOverriddenEqualNamedBean() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBeanDefinition("myNamedDao", new RootBeanDefinition(NamedStubDao.class));
    int initialBeanCount = context.getBeanDefinitionCount();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    int scannedBeanCount = scanner.scan(BASE_PACKAGE);

    assertThat(scannedBeanCount).isGreaterThanOrEqualTo(6);
    assertThat(context.getBeanDefinitionCount()).isEqualTo((initialBeanCount + scannedBeanCount));
    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
  }

  @Test
  void testSimpleScanWithDefaultFiltersAndOverriddenCompatibleNamedBean() {
    GenericApplicationContext context = new GenericApplicationContext();
    RootBeanDefinition bd = new RootBeanDefinition(NamedStubDao.class);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    context.registerBeanDefinition("myNamedDao", bd);
    int initialBeanCount = context.getBeanDefinitionCount();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    int scannedBeanCount = scanner.scan(BASE_PACKAGE);

    assertThat(scannedBeanCount).isGreaterThanOrEqualTo(6);
    assertThat(context.getBeanDefinitionCount()).isEqualTo((initialBeanCount + scannedBeanCount));
    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
  }

  @Test
  void testSimpleScanWithDefaultFiltersAndSameBeanTwice() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    // should not fail!
    scanner.scan(BASE_PACKAGE);
    scanner.scan(BASE_PACKAGE);
  }

  @Test
  void testSimpleScanWithDefaultFiltersAndSpecifiedBeanNameClash() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    scanner.scan("infra.context.annotation2");

    assertThatIllegalStateException().isThrownBy(() -> scanner.scan(BASE_PACKAGE))
            .withMessageContaining("myNamedDao")
            .withMessageContaining(NamedStubDao.class.getName())
            .withMessageContaining(NamedStubDao2.class.getName());
  }

  @Test
  void testCustomIncludeFilterWithoutDefaultsButIncludingPostProcessors() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(CustomComponent.class));
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(6);
    assertThat(context.containsBean("messageBean")).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
  }

  @Test
  void testCustomIncludeFilterWithoutDefaultsAndNoPostProcessors() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(CustomComponent.class));
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(6);
    assertThat(context.containsBean("messageBean")).isTrue();
    assertThat(context.containsBean("serviceInvocationCounter")).isFalse();
    assertThat(context.containsBean("fooServiceImpl")).isFalse();
    assertThat(context.containsBean("stubFooDao")).isFalse();
    assertThat(context.containsBean("myNamedComponent")).isFalse();
    assertThat(context.containsBean("myNamedDao")).isFalse();
    assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
  }

  @Test
  void testCustomIncludeFilterAndDefaults() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
    scanner.addIncludeFilter(new AnnotationTypeFilter(CustomComponent.class));
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(13);
    assertThat(context.containsBean("messageBean")).isTrue();
    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
  }

  @Test
  void testCustomAnnotationExcludeFilterAndDefaults() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
    scanner.addExcludeFilter(new AnnotationTypeFilter(Aspect.class));
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(11);
    assertThat(context.containsBean("serviceInvocationCounter")).isFalse();
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
  }

  @Test
  void testCustomAssignableTypeExcludeFilterAndDefaults() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
    scanner.addExcludeFilter(new AssignableTypeFilter(FooService.class));
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(11);
    assertThat(context.containsBean("fooServiceImpl")).isFalse();
    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
  }

  @Test
  void testCustomAssignableTypeExcludeFilterAndDefaultsWithoutPostProcessors() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
    scanner.setIncludeAnnotationConfig(false);
    scanner.addExcludeFilter(new AssignableTypeFilter(FooService.class));
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(6);
    assertThat(context.containsBean("fooServiceImpl")).isFalse();
    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isFalse();
    assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isFalse();
  }

  @Test
  void testMultipleCustomExcludeFiltersAndDefaults() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
    scanner.addExcludeFilter(new AssignableTypeFilter(FooService.class));
    scanner.addExcludeFilter(new AnnotationTypeFilter(Aspect.class));
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(10);
    assertThat(context.containsBean("fooServiceImpl")).isFalse();
    assertThat(context.containsBean("serviceInvocationCounter")).isFalse();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
  }

  @Test
  void testCustomBeanNameGenerator() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setBeanNameGenerator(new TestBeanNameGenerator());
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(12);
    assertThat(context.containsBean("fooServiceImpl")).isFalse();
    assertThat(context.containsBean("fooService")).isTrue();
    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
    assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
  }

  @Test
  void testMultipleBasePackagesWithDefaultsOnly() {
    GenericApplicationContext singlePackageContext = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner singlePackageScanner = new ClassPathBeanDefinitionScanner(singlePackageContext);
    GenericApplicationContext multiPackageContext = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner multiPackageScanner = new ClassPathBeanDefinitionScanner(multiPackageContext);
    int singlePackageBeanCount = singlePackageScanner.scan(BASE_PACKAGE);
    assertThat(singlePackageBeanCount).isGreaterThanOrEqualTo(12);
    multiPackageScanner.scan(BASE_PACKAGE, "infra.dao.annotation");
    // assertTrue(multiPackageBeanCount > singlePackageBeanCount);
  }

  @Test
  void testMultipleScanCalls() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    int initialBeanCount = context.getBeanDefinitionCount();
    int scannedBeanCount = scanner.scan(BASE_PACKAGE);
    assertThat(scannedBeanCount).isGreaterThanOrEqualTo(12);
    assertThat((context.getBeanDefinitionCount() - initialBeanCount)).isEqualTo(scannedBeanCount);
    int addedBeanCount = scanner.scan("infra.aop.aspectj.annotation");
    assertThat(context.getBeanDefinitionCount()).isEqualTo((initialBeanCount + scannedBeanCount + addedBeanCount));
  }

  @Test
  void testBeanAutowiredWithAnnotationConfigEnabled() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBeanDefinition("myBf", new RootBeanDefinition(StaticListableBeanFactory.class));
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setBeanNameGenerator(new TestBeanNameGenerator());
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(12);

    context.refresh();
    FooServiceImpl fooService = context.getBean("fooService", FooServiceImpl.class);
    StaticListableBeanFactory myBf = (StaticListableBeanFactory) context.getBean("myBf");
    MessageSource ms = (MessageSource) context.getBean("messageSource");

    assertThat(fooService.isInitCalled()).isTrue();
    assertThat(fooService.foo(123)).isEqualTo("bar");
    assertThat(fooService.lookupFoo(123)).isEqualTo("bar");
    assertThat(fooService.beanFactory).isSameAs(context.getBeanFactory());
    assertThat(fooService.listableBeanFactory).containsExactly(context.getBeanFactory(), myBf);
    assertThat(fooService.resourceLoader).isSameAs(context);
    assertThat(fooService.resourcePatternResolver).isSameAs(context);
    assertThat(fooService.eventPublisher).isSameAs(context);
    assertThat(fooService.messageSource).isSameAs(ms);
    assertThat(fooService.context).isSameAs(context);
    assertThat(fooService.configurableContext).containsExactly(context);
    assertThat(fooService.genericContext).isSameAs(context);
  }

  @Test
  void testBeanNotAutowiredWithAnnotationConfigDisabled() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    scanner.setBeanNameGenerator(new TestBeanNameGenerator());
    int beanCount = scanner.scan(BASE_PACKAGE);

    assertThat(beanCount).isGreaterThanOrEqualTo(7);

    context.refresh();
    try {
      context.getBean("fooService");
    }
    catch (BeanCreationException expected) {
      assertThat(expected.contains(BeanInstantiationException.class)).isTrue();
      // @Lookup method not substituted
    }
  }

  @Test
  void testAutowireCandidatePatternMatches() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(true);
    scanner.setBeanNameGenerator(new TestBeanNameGenerator());
    scanner.setAutowireCandidatePatterns("*FooDao");
    scanner.scan(BASE_PACKAGE);
    context.refresh();

    FooServiceImpl fooService = (FooServiceImpl) context.getBean("fooService");
    assertThat(fooService.foo(123)).isEqualTo("bar");
    assertThat(fooService.lookupFoo(123)).isEqualTo("bar");
  }

  @Test
  void testAutowireCandidatePatternDoesNotMatch() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(true);
    scanner.setBeanNameGenerator(new TestBeanNameGenerator());
    scanner.setAutowireCandidatePatterns("*NoSuchDao");
    scanner.scan(BASE_PACKAGE);
    context.refresh();
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> context.getBean("fooService"))
            .satisfies(ex ->
                    assertThat(ex.getMostSpecificCause()).isInstanceOf(NoSuchBeanDefinitionException.class));
  }

  @Test
  void testWithManualProgrammaticIndex() {
    // Pre-populating an index in order to replace a runtime scan

    GenericApplicationContext context = new GenericApplicationContext();
    context.setResourceLoader(new RestrictedResourcePatternResolver());

    CandidateComponentsIndex index = new CandidateComponentsIndex();
    index.registerScan("example");
    index.registerCandidateType(ServiceInvocationCounter.class.getName(), Component.class.getName());
    index.registerCandidateType(FooServiceImpl.class.getName(), Component.class.getName());
    index.registerCandidateType(StubFooDao.class.getName(), Component.class.getName());
    CandidateComponentsIndexLoader.addIndex(context.getClassLoader(), index);

    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    int beanCount = scanner.scan(BASE_PACKAGE);  // from index
    CandidateComponentsIndexLoader.clearCache();

    assertThat(beanCount).isEqualTo(3);
    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
  }

  @Test
  void testWithDerivedProgrammaticIndex() {
    // Recording an index from a scan (e.g. during refreshForAotProcessing)

    GenericApplicationContext context = new GenericApplicationContext();

    CandidateComponentsIndex scannedIndex = new CandidateComponentsIndex();
    CandidateComponentsIndexLoader.addIndex(context.getClassLoader(), scannedIndex);

    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.scan(BASE_PACKAGE);  // actual scan, populating the index instance above
    CandidateComponentsIndexLoader.clearCache();

    // Rebuilding a pre-computed index from the scanned index (AOT style)
    // through String-based registerScan and registerCandidateType calls.

    context = new GenericApplicationContext();
    context.setResourceLoader(new RestrictedResourcePatternResolver());

    CandidateComponentsIndex derivedIndex = new CandidateComponentsIndex();
    for (String basePackage : scannedIndex.getRegisteredScans()) {
      derivedIndex.registerScan(basePackage);
    }
    for (String stereotype : scannedIndex.getRegisteredStereotypes()) {
      for (String type : scannedIndex.getCandidateTypes(BASE_PACKAGE, stereotype)) {
        derivedIndex.registerCandidateType(type, stereotype);
      }
    }
    CandidateComponentsIndexLoader.addIndex(context.getClassLoader(), derivedIndex);

    scanner = new ClassPathBeanDefinitionScanner(context);
    int beanCount = scanner.scan(BASE_PACKAGE);  // from index
    CandidateComponentsIndexLoader.clearCache();

    assertThat(beanCount).isGreaterThanOrEqualTo(12);
    assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("myNamedComponent")).isTrue();
    assertThat(context.containsBean("myNamedDao")).isTrue();
    assertThat(context.containsBean("thoreau")).isTrue();
  }

  private static class TestBeanNameGenerator extends AnnotationBeanNameGenerator {

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
      String beanName = super.generateBeanName(definition, registry);
      return beanName.replace("Impl", "");
    }
  }

  @Component("toBeIgnored")
  public class NonStaticInnerClass {
  }

  private static final class RestrictedResourcePatternResolver extends PathMatchingPatternResourceLoader {

    @Override
    public Resource[] getResourcesArray(String locationPattern) throws IOException {
      throw new UnsupportedOperationException(locationPattern);
    }
  }

}
