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

import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.MessageSource;
import cn.taketoday.context.annotation2.NamedStubDao2;
import cn.taketoday.context.loader.CandidateComponentsTestClassLoader;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.core.type.filter.AssignableTypeFilter;
import cn.taketoday.lang.Component;
import example.scannable.CustomComponent;
import example.scannable.FooService;
import example.scannable.FooServiceImpl;
import example.scannable.NamedStubDao;
import example.scannable.StubFooDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ClassPathBeanDefinitionScannerTests {

  private static final String BASE_PACKAGE = "example.scannable";

  @Test
  public void testSimpleScanWithDefaultFiltersAndPostProcessors() {
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
  public void testSimpleScanWithDefaultFiltersAndPrimaryLazyBean() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.scan(BASE_PACKAGE);
    scanner.scan("cn.taketoday.context.annotation5");
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
  public void testDoubleScan() {
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
  public void testWithIndex() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.setClassLoader(CandidateComponentsTestClassLoader.index(
            ClassPathScanningCandidateComponentProviderTests.class.getClassLoader(),
            new ClassPathResource("spring.components", FooServiceImpl.class)));

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
  public void testDoubleScanWithIndex() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.setClassLoader(CandidateComponentsTestClassLoader.index(
            ClassPathScanningCandidateComponentProviderTests.class.getClassLoader(),
            new ClassPathResource("spring.components", FooServiceImpl.class)));

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
  public void testSimpleScanWithDefaultFiltersAndNoPostProcessors() {
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
  public void testSimpleScanWithDefaultFiltersAndOverridingBean() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBeanDefinition("stubFooDao", new RootBeanDefinition(TestBean.class));
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    // should not fail!
    scanner.scan(BASE_PACKAGE);
  }

  @Test
  public void testSimpleScanWithDefaultFiltersAndDefaultBeanNameClash() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    scanner.scan("cn.taketoday.context.annotation3");
    assertThatIllegalStateException().isThrownBy(() ->
                    scanner.scan(BASE_PACKAGE))
            .withMessageContaining("stubFooDao")
            .withMessageContaining(StubFooDao.class.getName());
  }

  @Test
  public void testSimpleScanWithDefaultFiltersAndOverriddenEqualNamedBean() {
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
  public void testSimpleScanWithDefaultFiltersAndOverriddenCompatibleNamedBean() {
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
  public void testSimpleScanWithDefaultFiltersAndSameBeanTwice() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    // should not fail!
    scanner.scan(BASE_PACKAGE);
    scanner.scan(BASE_PACKAGE);
  }

  @Test
  public void testSimpleScanWithDefaultFiltersAndSpecifiedBeanNameClash() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(false);
    scanner.scan("cn.taketoday.context.annotation2");
    assertThatIllegalStateException().isThrownBy(() ->
                    scanner.scan(BASE_PACKAGE))
            .withMessageContaining("myNamedDao")
            .withMessageContaining(NamedStubDao.class.getName())
            .withMessageContaining(NamedStubDao2.class.getName());
  }

  @Test
  public void testCustomIncludeFilterWithoutDefaultsButIncludingPostProcessors() {
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
  public void testCustomIncludeFilterWithoutDefaultsAndNoPostProcessors() {
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
  public void testCustomIncludeFilterAndDefaults() {
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
  public void testCustomAnnotationExcludeFilterAndDefaults() {
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
  public void testCustomAssignableTypeExcludeFilterAndDefaults() {
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
  public void testCustomAssignableTypeExcludeFilterAndDefaultsWithoutPostProcessors() {
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
  public void testMultipleCustomExcludeFiltersAndDefaults() {
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
  public void testCustomBeanNameGenerator() {
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
  public void testMultipleBasePackagesWithDefaultsOnly() {
    GenericApplicationContext singlePackageContext = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner singlePackageScanner = new ClassPathBeanDefinitionScanner(singlePackageContext);
    GenericApplicationContext multiPackageContext = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner multiPackageScanner = new ClassPathBeanDefinitionScanner(multiPackageContext);
    int singlePackageBeanCount = singlePackageScanner.scan(BASE_PACKAGE);
    assertThat(singlePackageBeanCount).isGreaterThanOrEqualTo(12);
    multiPackageScanner.scan(BASE_PACKAGE, "cn.taketoday.dao.annotation");
    // assertTrue(multiPackageBeanCount > singlePackageBeanCount);
  }

  @Test
  public void testMultipleScanCalls() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    int initialBeanCount = context.getBeanDefinitionCount();
    int scannedBeanCount = scanner.scan(BASE_PACKAGE);
    assertThat(scannedBeanCount).isGreaterThanOrEqualTo(12);
    assertThat((context.getBeanDefinitionCount() - initialBeanCount)).isEqualTo(scannedBeanCount);
    int addedBeanCount = scanner.scan("cn.taketoday.aop.aspectj.annotation");
    assertThat(context.getBeanDefinitionCount()).isEqualTo((initialBeanCount + scannedBeanCount + addedBeanCount));
  }

  @Test
  public void testBeanAutowiredWithAnnotationConfigEnabled() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBeanDefinition("myBf", new RootBeanDefinition(StandardBeanFactory.class));
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setBeanNameGenerator(new TestBeanNameGenerator());
    int beanCount = scanner.scan(BASE_PACKAGE);
    assertThat(beanCount).isGreaterThanOrEqualTo(9);
    context.refresh();

    FooServiceImpl fooService = context.getBean("fooService", FooServiceImpl.class);
    StandardBeanFactory myBf = (StandardBeanFactory) context.getBean("myBf");
    MessageSource ms = (MessageSource) context.getBean("messageSource");
    assertThat(fooService.isInitCalled()).isTrue();
    assertThat(fooService.foo(113)).isEqualTo("bar");
    assertThat(fooService.lookupFoo(113)).isEqualTo("bar");
    assertThat(fooService.beanFactory).isSameAs(context.getBeanFactory());
    assertThat(fooService.BeanFactory.size()).isEqualTo(2);
    assertThat(fooService.BeanFactory.get(0)).isSameAs(context.getBeanFactory());
    assertThat(fooService.BeanFactory.get(1)).isSameAs(myBf);
    assertThat(fooService.resourceLoader).isSameAs(context);
    assertThat(fooService.resourcePatternResolver).isSameAs(context);
    assertThat(fooService.eventPublisher).isSameAs(context);
    assertThat(fooService.messageSource).isSameAs(ms);
    assertThat(fooService.context).isSameAs(context);
    assertThat(fooService.configurableContext.length).isEqualTo(1);
    assertThat(fooService.configurableContext[0]).isSameAs(context);
    assertThat(fooService.genericContext).isSameAs(context);
  }

  @Test
  public void testBeanNotAutowiredWithAnnotationConfigDisabled() {
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
  public void testAutowireCandidatePatternMatches() {
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
  public void testAutowireCandidatePatternDoesNotMatch() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
    scanner.setIncludeAnnotationConfig(true);
    scanner.setBeanNameGenerator(new TestBeanNameGenerator());
    scanner.setAutowireCandidatePatterns("*NoSuchDao");
    scanner.scan(BASE_PACKAGE);
    context.refresh();
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    context.getBean("fooService"))
            .satisfies(ex -> assertThat(ex.getMostSpecificCause()).isInstanceOf(NoSuchBeanDefinitionException.class));
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

}
