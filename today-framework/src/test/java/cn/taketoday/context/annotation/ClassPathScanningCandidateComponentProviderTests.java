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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.regex.Pattern;

import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.annotation.gh24375.AnnotatedComponent;
import cn.taketoday.context.loader.CandidateComponentsTestClassLoader;
import cn.taketoday.context.loader.ClassPathScanningCandidateComponentProvider;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.core.type.filter.AssignableTypeFilter;
import cn.taketoday.core.type.filter.RegexPatternTypeFilter;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Repository;
import cn.taketoday.lang.Service;
import cn.taketoday.web.annotation.Controller;
import example.profilescan.DevComponent;
import example.profilescan.ProfileAnnotatedComponent;
import example.profilescan.ProfileMetaAnnotatedComponent;
import example.scannable.AutowiredQualifierFooService;
import example.scannable.CustomStereotype;
import example.scannable.DefaultNamedComponent;
import example.scannable.FooDao;
import example.scannable.FooService;
import example.scannable.FooServiceImpl;
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
  public void defaultsWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    testDefault(provider);
  }

  @Test
  public void defaultsWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    testDefault(provider);
  }

  private void testDefault(ClassPathScanningCandidateComponentProvider provider) {
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(containsBeanClass(candidates, DefaultNamedComponent.class)).isTrue();
    assertThat(containsBeanClass(candidates, NamedComponent.class)).isTrue();
    assertThat(containsBeanClass(candidates, FooServiceImpl.class)).isTrue();
    assertThat(containsBeanClass(candidates, StubFooDao.class)).isTrue();
    assertThat(containsBeanClass(candidates, NamedStubDao.class)).isTrue();
    assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
    assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
    assertThat(candidates.size()).isEqualTo(7);
    assertBeanDefinitionType(candidates);
  }

  @Test
  public void antStylePackageWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    testAntStyle(provider);
  }

  @Test
  public void antStylePackageWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    testAntStyle(provider);
  }

  private void testAntStyle(ClassPathScanningCandidateComponentProvider provider) {
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE + ".**.sub");
    assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
    assertThat(candidates.size()).isEqualTo(1);
    assertBeanDefinitionType(candidates);
  }

  @Test
  public void bogusPackageWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents("bogus");
    assertThat(candidates.size()).isEqualTo(0);
  }

  @Test
  public void bogusPackageWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents("bogus");
    assertThat(candidates.size()).isEqualTo(0);
  }

  @Test
  public void customFiltersFollowedByResetUseIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    provider.resetFilters(true);
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertBeanDefinitionType(candidates);
  }

  @Test
  public void customAnnotationTypeIncludeFilterWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    testCustomAnnotationTypeIncludeFilter(provider);
  }

  @Test
  public void customAnnotationTypeIncludeFilterWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    testCustomAnnotationTypeIncludeFilter(provider);
  }

  private void testCustomAnnotationTypeIncludeFilter(ClassPathScanningCandidateComponentProvider provider) {
    provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    testDefault(provider);
  }

  @Test
  public void customAssignableTypeIncludeFilterWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    testCustomAssignableTypeIncludeFilter(provider);
  }

  @Test
  public void customAssignableTypeIncludeFilterWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    testCustomAssignableTypeIncludeFilter(provider);
  }

  private void testCustomAssignableTypeIncludeFilter(ClassPathScanningCandidateComponentProvider provider) {
    provider.addIncludeFilter(new AssignableTypeFilter(FooService.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    // Interfaces/Abstract class are filtered out automatically.
    assertThat(containsBeanClass(candidates, AutowiredQualifierFooService.class)).isTrue();
    assertThat(containsBeanClass(candidates, FooServiceImpl.class)).isTrue();
    assertThat(containsBeanClass(candidates, ScopedProxyTestBean.class)).isTrue();
    assertThat(candidates.size()).isEqualTo(3);
    assertBeanDefinitionType(candidates);
  }

  @Test
  public void customSupportedIncludeAndExcludedFilterWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    testCustomSupportedIncludeAndExcludeFilter(provider);
  }

  @Test
  public void customSupportedIncludeAndExcludeFilterWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    testCustomSupportedIncludeAndExcludeFilter(provider);
  }

  private void testCustomSupportedIncludeAndExcludeFilter(ClassPathScanningCandidateComponentProvider provider) {
    provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    provider.addExcludeFilter(new AnnotationTypeFilter(Service.class));
    provider.addExcludeFilter(new AnnotationTypeFilter(Repository.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(containsBeanClass(candidates, NamedComponent.class)).isTrue();
    assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
    assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
    assertThat(candidates.size()).isEqualTo(3);
    assertBeanDefinitionType(candidates);
  }

  @Test
  public void customSupportIncludeFilterWithNonIndexedTypeUseScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    // This annotation type is not directly annotated with Indexed so we can use
    // the index to find candidates
    provider.addIncludeFilter(new AnnotationTypeFilter(CustomStereotype.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(containsBeanClass(candidates, DefaultNamedComponent.class)).isTrue();
    assertThat(candidates.size()).isEqualTo(1);
    assertBeanDefinitionType(candidates);
  }

  @Test
  public void customNotSupportedIncludeFilterUseScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    provider.addIncludeFilter(new AssignableTypeFilter(FooDao.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(containsBeanClass(candidates, StubFooDao.class)).isTrue();
    assertThat(candidates.size()).isEqualTo(1);
    assertBeanDefinitionType(candidates);
  }

  @Test
  public void excludeFilterWithScan() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
    provider.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(TEST_BASE_PACKAGE + ".*Named.*")));
    testExclude(provider);
  }

  @Test
  public void excludeFilterWithIndex() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
    provider.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(TEST_BASE_PACKAGE + ".*Named.*")));
    testExclude(provider);
  }

  private void testExclude(ClassPathScanningCandidateComponentProvider provider) {
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(containsBeanClass(candidates, FooServiceImpl.class)).isTrue();
    assertThat(containsBeanClass(candidates, StubFooDao.class)).isTrue();
    assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
    assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
    assertThat(candidates.size()).isEqualTo(4);
    assertBeanDefinitionType(candidates);
  }

  @Test
  public void testWithNoFilters() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(candidates.size()).isEqualTo(0);
  }

  @Test
  public void testWithComponentAnnotationOnly() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    provider.addExcludeFilter(new AnnotationTypeFilter(Repository.class));
    provider.addExcludeFilter(new AnnotationTypeFilter(Service.class));
    provider.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(candidates.size()).isEqualTo(3);
    assertThat(containsBeanClass(candidates, NamedComponent.class)).isTrue();
    assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
    assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
    assertThat(containsBeanClass(candidates, FooServiceImpl.class)).isFalse();
    assertThat(containsBeanClass(candidates, StubFooDao.class)).isFalse();
    assertThat(containsBeanClass(candidates, NamedStubDao.class)).isFalse();
  }

  @Test
  public void testWithAspectAnnotationOnly() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(Aspect.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(candidates.size()).isEqualTo(1);
    assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
  }

  @Test
  public void testWithInterfaceType() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AssignableTypeFilter(FooDao.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(candidates.size()).isEqualTo(1);
    assertThat(containsBeanClass(candidates, StubFooDao.class)).isTrue();
  }

  @Test
  public void testWithClassType() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AssignableTypeFilter(MessageBean.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(candidates.size()).isEqualTo(1);
    assertThat(containsBeanClass(candidates, MessageBean.class)).isTrue();
  }

  @Test
  public void testWithMultipleMatchingFilters() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    provider.addIncludeFilter(new AssignableTypeFilter(FooServiceImpl.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(candidates.size()).isEqualTo(7);
    assertThat(containsBeanClass(candidates, NamedComponent.class)).isTrue();
    assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
    assertThat(containsBeanClass(candidates, FooServiceImpl.class)).isTrue();
    assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
  }

  @Test
  public void testExcludeTakesPrecedence() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    provider.addIncludeFilter(new AssignableTypeFilter(FooServiceImpl.class));
    provider.addExcludeFilter(new AssignableTypeFilter(FooService.class));
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
    assertThat(candidates.size()).isEqualTo(6);
    assertThat(containsBeanClass(candidates, NamedComponent.class)).isTrue();
    assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
    assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
    assertThat(containsBeanClass(candidates, FooServiceImpl.class)).isFalse();
  }

  @Test
  public void testWithNullEnvironment() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_PROFILE_PACKAGE);
    assertThat(containsBeanClass(candidates, ProfileAnnotatedComponent.class)).isFalse();
  }

  @Test
  public void testWithInactiveProfile() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    ConfigurableEnvironment env = new StandardEnvironment();
    env.setActiveProfiles("other");
    provider.setEnvironment(env);
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_PROFILE_PACKAGE);
    assertThat(containsBeanClass(candidates, ProfileAnnotatedComponent.class)).isFalse();
  }

  @Test
  public void testWithActiveProfile() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    ConfigurableEnvironment env = new StandardEnvironment();
    env.setActiveProfiles(ProfileAnnotatedComponent.PROFILE_NAME);
    provider.setEnvironment(env);
    Set<AnnotatedBeanDefinition> candidates = provider.findCandidateComponents(TEST_PROFILE_PACKAGE);
    assertThat(containsBeanClass(candidates, ProfileAnnotatedComponent.class)).isTrue();
  }

  @Test
  public void testIntegrationWithStandardApplicationContext_noProfile() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(ProfileAnnotatedComponent.class);
    ctx.refresh();
    assertThat(ctx.containsBean(ProfileAnnotatedComponent.BEAN_NAME)).isFalse();
  }

  @Test
  public void testIntegrationWithStandardApplicationContext_validProfile() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.getEnvironment().setActiveProfiles(ProfileAnnotatedComponent.PROFILE_NAME);
    ctx.register(ProfileAnnotatedComponent.class);
    ctx.refresh();
    assertThat(ctx.containsBean(ProfileAnnotatedComponent.BEAN_NAME)).isTrue();
  }

  @Test
  public void testIntegrationWithStandardApplicationContext_validMetaAnnotatedProfile() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.getEnvironment().setActiveProfiles(DevComponent.PROFILE_NAME);
    ctx.register(ProfileMetaAnnotatedComponent.class);
    ctx.refresh();
    assertThat(ctx.containsBean(ProfileMetaAnnotatedComponent.BEAN_NAME)).isTrue();
  }

  @Test
  public void testIntegrationWithStandardApplicationContext_invalidProfile() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.getEnvironment().setActiveProfiles("other");
    ctx.register(ProfileAnnotatedComponent.class);
    ctx.refresh();
    assertThat(ctx.containsBean(ProfileAnnotatedComponent.BEAN_NAME)).isFalse();
  }

  @Test
  public void testIntegrationWithStandardApplicationContext_invalidMetaAnnotatedProfile() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.getEnvironment().setActiveProfiles("other");
    ctx.register(ProfileMetaAnnotatedComponent.class);
    ctx.refresh();
    assertThat(ctx.containsBean(ProfileMetaAnnotatedComponent.BEAN_NAME)).isFalse();
  }

  @Test
  public void testIntegrationWithStandardApplicationContext_defaultProfile() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
    // no active profiles are set
    ctx.register(DefaultProfileAnnotatedComponent.class);
    ctx.refresh();
    assertThat(ctx.containsBean(DefaultProfileAnnotatedComponent.BEAN_NAME)).isTrue();
  }

  @Test
  public void testIntegrationWithStandardApplicationContext_defaultAndDevProfile() {
    Class<?> beanClass = DefaultAndDevProfileAnnotatedComponent.class;
    String beanName = DefaultAndDevProfileAnnotatedComponent.BEAN_NAME;
    {
      StandardApplicationContext ctx = new StandardApplicationContext();
      ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
      // no active profiles are set
      ctx.register(beanClass);
      ctx.refresh();
      assertThat(ctx.containsBean(beanName)).isTrue();
    }
    {
      StandardApplicationContext ctx = new StandardApplicationContext();
      ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
      ctx.getEnvironment().setActiveProfiles("dev");
      ctx.register(beanClass);
      ctx.refresh();
      assertThat(ctx.containsBean(beanName)).isTrue();
    }
    {
      StandardApplicationContext ctx = new StandardApplicationContext();
      ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
      ctx.getEnvironment().setActiveProfiles("other");
      ctx.register(beanClass);
      ctx.refresh();
      assertThat(ctx.containsBean(beanName)).isFalse();
    }
  }

  @Test
  public void testIntegrationWithStandardApplicationContext_metaProfile() {
    Class<?> beanClass = MetaProfileAnnotatedComponent.class;
    String beanName = MetaProfileAnnotatedComponent.BEAN_NAME;
    {
      StandardApplicationContext ctx = new StandardApplicationContext();
      ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
      // no active profiles are set
      ctx.register(beanClass);
      ctx.refresh();
      assertThat(ctx.containsBean(beanName)).isTrue();
    }
    {
      StandardApplicationContext ctx = new StandardApplicationContext();
      ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
      ctx.getEnvironment().setActiveProfiles("dev");
      ctx.register(beanClass);
      ctx.refresh();
      assertThat(ctx.containsBean(beanName)).isTrue();
    }
    {
      StandardApplicationContext ctx = new StandardApplicationContext();
      ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
      ctx.getEnvironment().setActiveProfiles("other");
      ctx.register(beanClass);
      ctx.refresh();
      assertThat(ctx.containsBean(beanName)).isFalse();
    }
  }

  @Test
  public void componentScanningFindsComponentsAnnotatedWithAnnotationsContainingNestedAnnotations() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
    Set<AnnotatedBeanDefinition> components = provider.findCandidateComponents(AnnotatedComponent.class.getPackage().getName());
    assertThat(components).hasSize(1);
    assertThat(components.iterator().next().getBeanClassName()).isEqualTo(AnnotatedComponent.class.getName());
  }

  private boolean containsBeanClass(Set<AnnotatedBeanDefinition> candidates, Class<?> beanClass) {
    for (BeanDefinition candidate : candidates) {
      if (beanClass.getName().equals(candidate.getBeanClassName())) {
        return true;
      }
    }
    return false;
  }

  private void assertBeanDefinitionType(Set<AnnotatedBeanDefinition> candidates) {
    candidates.forEach(c ->
            assertThat(c).isInstanceOf(ScannedGenericBeanDefinition.class)
    );
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
