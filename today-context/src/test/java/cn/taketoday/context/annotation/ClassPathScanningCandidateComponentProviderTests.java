/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.index.CandidateComponentsTestClassLoader;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.core.type.filter.AssignableTypeFilter;
import cn.taketoday.core.type.filter.RegexPatternTypeFilter;
import cn.taketoday.stereotype.Component;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.stereotype.Repository;
import cn.taketoday.stereotype.Service;
import example.gh24375.AnnotatedComponent;
import example.indexed.IndexedJakartaManagedBeanComponent;
import example.indexed.IndexedJakartaNamedComponent;
import example.profilescan.DevComponent;
import example.profilescan.ProfileAnnotatedComponent;
import example.profilescan.ProfileMetaAnnotatedComponent;
import example.scannable.AutowiredQualifierFooService;
import example.scannable.CustomStereotype;
import example.scannable.DefaultNamedComponent;
import example.scannable.FooDao;
import example.scannable.FooService;
import example.scannable.FooServiceImpl;
import example.scannable.JakartaManagedBeanComponent;
import example.scannable.JakartaNamedComponent;
import example.scannable.MessageBean;
import example.scannable.NamedComponent;
import example.scannable.NamedStubDao;
import example.scannable.ScopedProxyTestBean;
import example.scannable.ServiceInvocationCounter;
import example.scannable.StubFooDao;
import example.scannable.sub.BarComponent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 4.0 2021/12/9 21:53
 */
public class ClassPathScanningCandidateComponentProviderTests {

  private static final String TEST_BASE_PACKAGE = "example.scannable";
  private static final String TEST_PROFILE_PACKAGE = "example.profilescan";
  private static final String TEST_DEFAULT_PROFILE_NAME = "testDefault";

  private static final ClassLoader TEST_BASE_CLASSLOADER = CandidateComponentsTestClassLoader.index(
          ClassPathScanningCandidateComponentProviderTests.class.getClassLoader(),
          new ClassPathResource("today.components", NamedComponent.class));

  @Test
  void defaultsWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    testDefault(provider, true, false);
  }

  @Test
  void defaultsWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    testDefault(provider, "example", true, true);
  }

  private static final Set<Class<?>> springComponents = Set.of(
          DefaultNamedComponent.class,
          NamedComponent.class,
          FooServiceImpl.class,
          StubFooDao.class,
          NamedStubDao.class,
          ServiceInvocationCounter.class,
          BarComponent.class
  );

  private static final Set<Class<?>> scannedJakartaComponents = Set.of(
          JakartaNamedComponent.class,
          JakartaManagedBeanComponent.class
  );

  private static final Set<Class<?>> indexedJakartaComponents = Set.of(
          IndexedJakartaNamedComponent.class,
          IndexedJakartaManagedBeanComponent.class
  );

  private void testDefault(ClassPathScanningCandidateComponentProvider provider, boolean includeScannedJakartaComponents, boolean includeIndexedJakartaComponents) {
    testDefault(provider, TEST_BASE_PACKAGE, includeScannedJakartaComponents, includeIndexedJakartaComponents);
  }

  private void testDefault(ClassPathScanningCandidateComponentProvider provider, String basePackage, boolean includeScannedJakartaComponents, boolean includeIndexedJakartaComponents) {
    Set<Class<?>> expectedTypes = new HashSet<>(springComponents);
    if (includeScannedJakartaComponents) {
      expectedTypes.addAll(scannedJakartaComponents);
    }
    if (includeIndexedJakartaComponents) {
      expectedTypes.addAll(indexedJakartaComponents);
    }

    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(basePackage);
    assertScannedBeanDefinitions(candidates);
    assertBeanTypes(candidates, expectedTypes);
  }

  @Test
  void antStylePackageWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    testAntStyle(provider);
  }

  @Test
  void antStylePackageWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    testAntStyle(provider);
  }

  private void testAntStyle(ClassPathScanningCandidateComponentProvider provider) {
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE + ".**.sub");
    assertScannedBeanDefinitions(candidates);
    assertBeanTypes(candidates, BarComponent.class);
  }

  @Test
  void bogusPackageWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents("bogus");
    assertThat(candidates).isEmpty();
  }

  @Test
  void bogusPackageWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents("bogus");
    assertThat(candidates).isEmpty();
  }

  @Test
  void customFiltersFollowedByResetUseIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    provider.resetFilters(true);
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertScannedBeanDefinitions(candidates);
  }

  @Test
  void customAnnotationTypeIncludeFilterWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    testCustomAnnotationTypeIncludeFilter(provider);
  }

  @Test
  void customAnnotationTypeIncludeFilterWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    testCustomAnnotationTypeIncludeFilter(provider);
  }

  private void testCustomAnnotationTypeIncludeFilter(ClassPathScanningCandidateComponentProvider provider) {
    provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    testDefault(provider, false, false);
  }

  @Test
  void customAssignableTypeIncludeFilterWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    testCustomAssignableTypeIncludeFilter(provider);
  }

  @Test
  void customAssignableTypeIncludeFilterWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    testCustomAssignableTypeIncludeFilter(provider);
  }

  private void testCustomAssignableTypeIncludeFilter(ClassPathScanningCandidateComponentProvider provider) {
    provider.addIncludeFilter(new AssignableTypeFilter(FooService.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertScannedBeanDefinitions(candidates);
    // Interfaces/Abstract class are filtered out automatically.
    assertBeanTypes(candidates, AutowiredQualifierFooService.class, FooServiceImpl.class, ScopedProxyTestBean.class);
  }

  @Test
  void customSupportedIncludeAndExcludedFilterWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    testCustomSupportedIncludeAndExcludeFilter(provider);
  }

  @Test
  void customSupportedIncludeAndExcludeFilterWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    testCustomSupportedIncludeAndExcludeFilter(provider);
  }

  private void testCustomSupportedIncludeAndExcludeFilter(ClassPathScanningCandidateComponentProvider provider) {
    provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    provider.addExcludeFilter(new AnnotationTypeFilter(Service.class));
    provider.addExcludeFilter(new AnnotationTypeFilter(Repository.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertScannedBeanDefinitions(candidates);
    assertBeanTypes(candidates, NamedComponent.class, ServiceInvocationCounter.class, BarComponent.class);
  }

  @Test
  void customSupportIncludeFilterWithNonIndexedTypeUseScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    // This annotation type is not directly annotated with @Indexed so we can use
    // the index to find candidates.
    provider.addIncludeFilter(new AnnotationTypeFilter(CustomStereotype.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertScannedBeanDefinitions(candidates);
    assertBeanTypes(candidates, DefaultNamedComponent.class);
  }

  @Test
  void customNotSupportedIncludeFilterUseScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    provider.addIncludeFilter(new AssignableTypeFilter(FooDao.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertScannedBeanDefinitions(candidates);
    assertBeanTypes(candidates, StubFooDao.class);
  }

  @Test
  void excludeFilterWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    provider.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(TEST_BASE_PACKAGE + ".*Named.*")));
    testExclude(provider);
  }

  @Test
  void excludeFilterWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    provider.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(TEST_BASE_PACKAGE + ".*Named.*")));
    testExclude(provider);
  }

  private void testExclude(ClassPathScanningCandidateComponentProvider provider) {
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertScannedBeanDefinitions(candidates);
    assertBeanTypes(candidates, FooServiceImpl.class, StubFooDao.class, ServiceInvocationCounter.class,
            BarComponent.class, JakartaManagedBeanComponent.class);
  }

  @Test
  void withNoFilters() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(candidates).isEmpty();
  }

  @Test
  void withComponentAnnotationOnly() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    provider.addExcludeFilter(new AnnotationTypeFilter(Repository.class));
    provider.addExcludeFilter(new AnnotationTypeFilter(Service.class));
    provider.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertBeanTypes(candidates, NamedComponent.class, ServiceInvocationCounter.class, BarComponent.class);
  }

  @Test
  void withAspectAnnotationOnly() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(Aspect.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertBeanTypes(candidates, ServiceInvocationCounter.class);
  }

  @Test
  void withInterfaceType() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AssignableTypeFilter(FooDao.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertBeanTypes(candidates, StubFooDao.class);
  }

  @Test
  void withClassType() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AssignableTypeFilter(MessageBean.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertBeanTypes(candidates, MessageBean.class);
  }

  @Test
  void withMultipleMatchingFilters() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    provider.addIncludeFilter(new AssignableTypeFilter(FooServiceImpl.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertBeanTypes(candidates, NamedComponent.class, ServiceInvocationCounter.class, FooServiceImpl.class,
            BarComponent.class, DefaultNamedComponent.class, NamedStubDao.class, StubFooDao.class);
  }

  @Test
  void excludeTakesPrecedence() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    provider.addIncludeFilter(new AssignableTypeFilter(FooServiceImpl.class));
    provider.addExcludeFilter(new AssignableTypeFilter(FooService.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertBeanTypes(candidates, NamedComponent.class, ServiceInvocationCounter.class, BarComponent.class,
            DefaultNamedComponent.class, NamedStubDao.class, StubFooDao.class);
  }

  @Test
  void withNullEnvironment() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_PROFILE_PACKAGE);
    assertThat(candidates).isEmpty();
  }

  @Test
  void withInactiveProfile() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    ConfigurableEnvironment env = new StandardEnvironment();
    env.setActiveProfiles("other");
    provider.setEnvironment(env);
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_PROFILE_PACKAGE);
    assertThat(candidates).isEmpty();
  }

  @Test
  void withActiveProfile() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    ConfigurableEnvironment env = new StandardEnvironment();
    env.setActiveProfiles(ProfileAnnotatedComponent.PROFILE_NAME);
    provider.setEnvironment(env);
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_PROFILE_PACKAGE);
    assertBeanTypes(candidates, ProfileAnnotatedComponent.class);
  }

  @Test
  void integrationWithAnnotationConfigApplicationContext_noProfile() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ProfileAnnotatedComponent.class);
    ctx.refresh();
    assertThat(ctx.containsBean(ProfileAnnotatedComponent.BEAN_NAME)).isFalse();
    ctx.close();
  }

  @Test
  void integrationWithAnnotationConfigApplicationContext_validProfile() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.getEnvironment().setActiveProfiles(ProfileAnnotatedComponent.PROFILE_NAME);
    ctx.register(ProfileAnnotatedComponent.class);
    ctx.refresh();
    assertThat(ctx.containsBean(ProfileAnnotatedComponent.BEAN_NAME)).isTrue();
    ctx.close();
  }

  @Test
  void integrationWithAnnotationConfigApplicationContext_validMetaAnnotatedProfile() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.getEnvironment().setActiveProfiles(DevComponent.PROFILE_NAME);
    ctx.register(ProfileMetaAnnotatedComponent.class);
    ctx.refresh();
    assertThat(ctx.containsBean(ProfileMetaAnnotatedComponent.BEAN_NAME)).isTrue();
    ctx.close();
  }

  @Test
  void integrationWithAnnotationConfigApplicationContext_invalidProfile() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.getEnvironment().setActiveProfiles("other");
    ctx.register(ProfileAnnotatedComponent.class);
    ctx.refresh();
    assertThat(ctx.containsBean(ProfileAnnotatedComponent.BEAN_NAME)).isFalse();
    ctx.close();
  }

  @Test
  void integrationWithAnnotationConfigApplicationContext_invalidMetaAnnotatedProfile() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.getEnvironment().setActiveProfiles("other");
    ctx.register(ProfileMetaAnnotatedComponent.class);
    ctx.refresh();
    assertThat(ctx.containsBean(ProfileMetaAnnotatedComponent.BEAN_NAME)).isFalse();
    ctx.close();
  }

  @Test
  void integrationWithAnnotationConfigApplicationContext_defaultProfile() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
    // no active profiles are set
    ctx.register(DefaultProfileAnnotatedComponent.class);
    ctx.refresh();
    assertThat(ctx.containsBean(DefaultProfileAnnotatedComponent.BEAN_NAME)).isTrue();
    ctx.close();
  }

  @Test
  void integrationWithAnnotationConfigApplicationContext_defaultAndDevProfile() {
    Class<?> beanClass = DefaultAndDevProfileAnnotatedComponent.class;
    String beanName = DefaultAndDevProfileAnnotatedComponent.BEAN_NAME;
    {
      AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
      ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
      // no active profiles are set
      ctx.register(beanClass);
      ctx.refresh();
      assertThat(ctx.containsBean(beanName)).isTrue();
      ctx.close();
    }
    {
      AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
      ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
      ctx.getEnvironment().setActiveProfiles("dev");
      ctx.register(beanClass);
      ctx.refresh();
      assertThat(ctx.containsBean(beanName)).isTrue();
      ctx.close();
    }
    {
      AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
      ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
      ctx.getEnvironment().setActiveProfiles("other");
      ctx.register(beanClass);
      ctx.refresh();
      assertThat(ctx.containsBean(beanName)).isFalse();
      ctx.close();
    }
  }

  @Test
  void integrationWithAnnotationConfigApplicationContext_metaProfile() {
    Class<?> beanClass = MetaProfileAnnotatedComponent.class;
    String beanName = MetaProfileAnnotatedComponent.BEAN_NAME;
    {
      AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
      ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
      // no active profiles are set
      ctx.register(beanClass);
      ctx.refresh();
      assertThat(ctx.containsBean(beanName)).isTrue();
      ctx.close();
    }
    {
      AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
      ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
      ctx.getEnvironment().setActiveProfiles("dev");
      ctx.register(beanClass);
      ctx.refresh();
      assertThat(ctx.containsBean(beanName)).isTrue();
      ctx.close();
    }
    {
      AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
      ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
      ctx.getEnvironment().setActiveProfiles("other");
      ctx.register(beanClass);
      ctx.refresh();
      assertThat(ctx.containsBean(beanName)).isFalse();
      ctx.close();
    }
  }

  @Test
  void componentScanningFindsComponentsAnnotatedWithAnnotationsContainingNestedAnnotations() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    Set<AnnotatedBeanDefinition> components = provider.findCandidateComponents(AnnotatedComponent.class.getPackage().getName());
    assertThat(components).hasSize(1);
    assertThat(components.iterator().next().getBeanClassName()).isEqualTo(AnnotatedComponent.class.getName());
  }

  private static void assertBeanTypes(Set<AnnotatedBeanDefinition> candidates, Class<?>... expectedTypes) {
    assertBeanTypes(candidates, Arrays.stream(expectedTypes));
  }

  private static void assertBeanTypes(Set<AnnotatedBeanDefinition> candidates, Collection<Class<?>> expectedTypes) {
    assertBeanTypes(candidates, expectedTypes.stream());
  }

  private static void assertBeanTypes(Set<AnnotatedBeanDefinition> candidates, Stream<Class<?>> expectedTypes) {
    List<String> actualTypeNames = candidates.stream().map(BeanDefinition::getBeanClassName).distinct().sorted().toList();
    List<String> expectedTypeNames = expectedTypes.map(Class::getName).distinct().sorted().toList();
    assertThat(actualTypeNames).containsExactlyElementsOf(expectedTypeNames);
  }

  private static void assertScannedBeanDefinitions(Set<AnnotatedBeanDefinition> candidates) {
    candidates.forEach(type -> assertThat(type).isInstanceOf(ScannedGenericBeanDefinition.class));
  }

  @Profile(TEST_DEFAULT_PROFILE_NAME)
  @Component(DefaultProfileAnnotatedComponent.BEAN_NAME)
  private static class DefaultProfileAnnotatedComponent {
    static final String BEAN_NAME = "defaultProfileAnnotatedComponent";
  }

  @Profile({ TEST_DEFAULT_PROFILE_NAME, "dev" })
  @Component(DefaultAndDevProfileAnnotatedComponent.BEAN_NAME)
  private static class DefaultAndDevProfileAnnotatedComponent {
    static final String BEAN_NAME = "defaultAndDevProfileAnnotatedComponent";
  }

  @DefaultProfile
  @DevProfile
  @Component(MetaProfileAnnotatedComponent.BEAN_NAME)
  private static class MetaProfileAnnotatedComponent {
    static final String BEAN_NAME = "metaProfileAnnotatedComponent";
  }

  @Profile(TEST_DEFAULT_PROFILE_NAME)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface DefaultProfile {
  }

  @Profile("dev")
  @Retention(RetentionPolicy.RUNTIME)
  public @interface DevProfile {
  }

}
