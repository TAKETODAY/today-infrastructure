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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.aop.proxy.DefaultAdvisorAutoProxyCreator;
import cn.taketoday.aop.scope.ScopedObject;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.interceptor.SimpleTraceInterceptor;
import cn.taketoday.beans.Lazy;
import cn.taketoday.beans.Primary;
import cn.taketoday.beans.dependency.StandardDependenciesBeanPostProcessor;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionRegistryPostProcessor;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.beans.factory.StandardBeanFactory;
import cn.taketoday.beans.factory.support.ITestBean;
import cn.taketoday.beans.factory.support.InitDestroyAnnotationBeanPostProcessor;
import cn.taketoday.beans.factory.support.TestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.componentscan.simple.SimpleComponent;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.core.Order;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.DescriptiveResource;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.SyncTaskExecutor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Qualifier;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class ConfigurationClassPostProcessorTests {

  private StandardBeanFactory beanFactory;

  private DefinitionLoadingContext loadingContext;

  @BeforeEach
  void setup() {
    StandardApplicationContext context = new StandardApplicationContext();
    loadingContext = new DefinitionLoadingContext(beanFactory, context);
    beanFactory = context.getBeanFactory();
  }

  /**
   * Enhanced {@link Configuration} classes are only necessary for respecting
   * certain bean semantics, like singleton-scoping, scoped proxies, etc.
   * <p>Technically, {@link ConfigurationClassPostProcessor} could fail to enhance the
   * registered Configuration classes and many use cases would still work.
   * Certain cases, however, like inter-bean singleton references would not.
   * We test for such a case below, and in doing so prove that enhancement is working.
   */
  @Test
  void enhancementIsPresentBecauseSingletonSemanticsAreRespected() {
    beanFactory.registerBeanDefinition("config", new BeanDefinition(SingletonBeanConfig.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    assertThat(beanFactory.getBeanDefinition("config").hasBeanClass()).isTrue();
    Foo foo = beanFactory.getBean("foo", Foo.class);
    Bar bar = beanFactory.getBean("bar", Bar.class);
    assertThat(bar.foo).isSameAs(foo);
    assertThat(beanFactory.getDependentBeans("foo")).contains("bar");
    assertThat(beanFactory.getDependentBeans("config")).contains("foo");
    assertThat(beanFactory.getDependentBeans("config")).contains("bar");
  }

  @Test
  void enhancementIsPresentBecauseSingletonSemanticsAreRespectedUsingAsm() {
    beanFactory.registerBeanDefinition("config", new BeanDefinition("config", SingletonBeanConfig.class.getName()));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    assertThat(beanFactory.getBeanDefinition("config").hasBeanClass()).isTrue();
    Foo foo = beanFactory.getBean("foo", Foo.class);
    Bar bar = beanFactory.getBean("bar", Bar.class);
    assertThat(bar.foo).isSameAs(foo);
    assertThat(beanFactory.getDependentBeans("foo")).contains("bar");
    assertThat(beanFactory.getDependentBeans("config")).contains("foo");
    assertThat(beanFactory.getDependentBeans("config")).contains("bar");
  }

  @Test
  void enhancementIsNotPresentForProxyBeanMethodsFlagSetToFalse() {
    beanFactory.registerBeanDefinition("config", new BeanDefinition(NonEnhancedSingletonBeanConfig.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    assertThat(((BeanDefinition) beanFactory.getBeanDefinition("config")).hasBeanClass()).isTrue();
    Foo foo = beanFactory.getBean("foo", Foo.class);
    Bar bar = beanFactory.getBean("bar", Bar.class);
    assertThat(bar.foo).isNotSameAs(foo);
  }

  @Test
  void enhancementIsNotPresentForProxyBeanMethodsFlagSetToFalseUsingAsm() {
    beanFactory.registerBeanDefinition("config", new BeanDefinition(NonEnhancedSingletonBeanConfig.class.getName()));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    assertThat(beanFactory.getBeanDefinition("config").hasBeanClass()).isTrue();
    Foo foo = beanFactory.getBean("foo", Foo.class);
    Bar bar = beanFactory.getBean("bar", Bar.class);
    assertThat(bar.foo).isNotSameAs(foo);
  }

  @Test
  void enhancementIsNotPresentForStaticMethods() {
    beanFactory.registerBeanDefinition("config", new BeanDefinition(StaticSingletonBeanConfig.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    assertThat(beanFactory.getBeanDefinition("config").hasBeanClass()).isTrue();
    assertThat(beanFactory.getBeanDefinition("foo").hasBeanClass()).isTrue();
    assertThat(beanFactory.getBeanDefinition("bar").hasBeanClass()).isTrue();
    Foo foo = beanFactory.getBean("foo", Foo.class);
    Bar bar = beanFactory.getBean("bar", Bar.class);
    assertThat(bar.foo).isNotSameAs(foo);
  }

  @Test
  void enhancementIsNotPresentForStaticMethodsUsingAsm() {
    beanFactory.registerBeanDefinition("config", new BeanDefinition(StaticSingletonBeanConfig.class.getName()));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    assertThat(beanFactory.getBeanDefinition("config").hasBeanClass()).isTrue();
    assertThat(beanFactory.getBeanDefinition("foo").hasBeanClass()).isTrue();
    assertThat(beanFactory.getBeanDefinition("bar").hasBeanClass()).isTrue();
    Foo foo = beanFactory.getBean("foo", Foo.class);
    Bar bar = beanFactory.getBean("bar", Bar.class);
    assertThat(bar.foo).isNotSameAs(foo);
  }

  @Test
  void configurationIntrospectionOfInnerClassesWorksWithDotNameSyntax() {
    beanFactory.registerBeanDefinition("config", new BeanDefinition(getClass().getName() + ".SingletonBeanConfig"));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    Foo foo = beanFactory.getBean("foo", Foo.class);
    Bar bar = beanFactory.getBean("bar", Bar.class);
    assertThat(bar.foo).isSameAs(foo);
  }

  /**
   * Tests the fix for SPR-5655, a special workaround that prefers reflection over ASM
   * if a bean class is already loaded.
   */
  @Test
  void alreadyLoadedConfigurationClasses() {
    beanFactory.registerBeanDefinition("unloadedConfig", new BeanDefinition(UnloadedConfig.class.getName()));
    beanFactory.registerBeanDefinition("loadedConfig", new BeanDefinition(LoadedConfig.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    beanFactory.getBean("foo");
    beanFactory.getBean("bar");
  }

  /**
   * Tests whether a bean definition without a specified bean class is handled correctly.
   */
  @Test
  void postProcessorIntrospectsInheritedDefinitionsCorrectly() {
    beanFactory.registerBeanDefinition("config", new BeanDefinition(SingletonBeanConfig.class));
    beanFactory.registerBeanDefinition("parent", new BeanDefinition(TestBean.class));
//    beanFactory.registerBeanDefinition("child", new ChildBeanDefinition("parent"));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    Foo foo = beanFactory.getBean("foo", Foo.class);
    Bar bar = beanFactory.getBean("bar", Bar.class);
    assertThat(bar.foo).isSameAs(foo);
  }

  @Test
  void postProcessorWorksWithComposedConfigurationUsingReflection() {
    BeanDefinition beanDefinition = new BeanDefinition(ComposedConfigurationClass.class);
    assertSupportForComposedAnnotation(beanDefinition);
  }

  @Test
  void postProcessorWorksWithComposedConfigurationUsingAsm() {
    BeanDefinition beanDefinition = new BeanDefinition(ComposedConfigurationClass.class.getName());
    assertSupportForComposedAnnotation(beanDefinition);
  }

  @Test
  void postProcessorWorksWithComposedConfigurationWithAttributeOverrideForBasePackageUsingReflection() {
    BeanDefinition beanDefinition = new BeanDefinition(
            ComposedConfigurationWithAttributeOverrideForBasePackage.class);
    assertSupportForComposedAnnotation(beanDefinition);
  }

  @Test
  void postProcessorWorksWithComposedConfigurationWithAttributeOverrideForBasePackageUsingAsm() {
    BeanDefinition beanDefinition = new BeanDefinition(
            ComposedConfigurationWithAttributeOverrideForBasePackage.class.getName());
    assertSupportForComposedAnnotation(beanDefinition);
  }

  @Test
  void postProcessorWorksWithComposedConfigurationWithAttributeOverrideForExcludeFilterUsingReflection() {
    BeanDefinition beanDefinition = new BeanDefinition(
            ComposedConfigurationWithAttributeOverrideForExcludeFilter.class);
    assertSupportForComposedAnnotationWithExclude(beanDefinition);
  }

  @Test
  void postProcessorWorksWithComposedConfigurationWithAttributeOverrideForExcludeFilterUsingAsm() {
    BeanDefinition beanDefinition = new BeanDefinition(
            ComposedConfigurationWithAttributeOverrideForExcludeFilter.class.getName());
    assertSupportForComposedAnnotationWithExclude(beanDefinition);
  }

  @Test
  void postProcessorWorksWithExtendedConfigurationWithAttributeOverrideForExcludesFilterUsingReflection() {
    BeanDefinition beanDefinition = new BeanDefinition(
            ExtendedConfigurationWithAttributeOverrideForExcludeFilter.class);
    assertSupportForComposedAnnotationWithExclude(beanDefinition);
  }

  @Test
  void postProcessorWorksWithExtendedConfigurationWithAttributeOverrideForExcludesFilterUsingAsm() {
    BeanDefinition beanDefinition = new BeanDefinition(
            ExtendedConfigurationWithAttributeOverrideForExcludeFilter.class.getName());
    assertSupportForComposedAnnotationWithExclude(beanDefinition);
  }

  @Test
  void postProcessorWorksWithComposedComposedConfigurationWithAttributeOverridesUsingReflection() {
    BeanDefinition beanDefinition = new BeanDefinition(
            ComposedComposedConfigurationWithAttributeOverridesClass.class);
    assertSupportForComposedAnnotation(beanDefinition);
  }

  @Test
  void postProcessorWorksWithComposedComposedConfigurationWithAttributeOverridesUsingAsm() {
    BeanDefinition beanDefinition = new BeanDefinition(
            ComposedComposedConfigurationWithAttributeOverridesClass.class.getName());
    assertSupportForComposedAnnotation(beanDefinition);
  }

  @Test
  void postProcessorWorksWithMetaComponentScanConfigurationWithAttributeOverridesUsingReflection() {
    BeanDefinition beanDefinition = new BeanDefinition(
            MetaComponentScanConfigurationWithAttributeOverridesClass.class);
    assertSupportForComposedAnnotation(beanDefinition);
  }

  @Test
  void postProcessorWorksWithMetaComponentScanConfigurationWithAttributeOverridesUsingAsm() {
    BeanDefinition beanDefinition = new BeanDefinition(
            MetaComponentScanConfigurationWithAttributeOverridesClass.class.getName());
    assertSupportForComposedAnnotation(beanDefinition);
  }

  @Test
  void postProcessorWorksWithMetaComponentScanConfigurationWithAttributeOverridesSubclassUsingReflection() {
    BeanDefinition beanDefinition = new BeanDefinition(
            SubMetaComponentScanConfigurationWithAttributeOverridesClass.class);
    assertSupportForComposedAnnotation(beanDefinition);
  }

  @Test
  void postProcessorWorksWithMetaComponentScanConfigurationWithAttributeOverridesSubclassUsingAsm() {
    BeanDefinition beanDefinition = new BeanDefinition("config",
            SubMetaComponentScanConfigurationWithAttributeOverridesClass.class.getName());
    assertSupportForComposedAnnotation(beanDefinition);
  }

  private void assertSupportForComposedAnnotation(BeanDefinition beanDefinition) {
    beanFactory.registerBeanDefinition("config", beanDefinition);
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    SimpleComponent simpleComponent = beanFactory.getBean(SimpleComponent.class);
    assertThat(simpleComponent).isNotNull();
  }

  private void assertSupportForComposedAnnotationWithExclude(BeanDefinition beanDefinition) {
    beanFactory.registerBeanDefinition("config", beanDefinition);
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
            beanFactory.getBean(SimpleComponent.class));
  }

  @Test
  void postProcessorOverridesNonApplicationBeanDefinitions() {
    BeanDefinition rbd = new BeanDefinition(TestBean.class);
    rbd.setRole(BeanDefinition.ROLE_SUPPORT);
    beanFactory.registerBeanDefinition("bar", rbd);
    beanFactory.registerBeanDefinition("config", new BeanDefinition(SingletonBeanConfig.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    Foo foo = beanFactory.getBean("foo", Foo.class);
    Bar bar = beanFactory.getBean("bar", Bar.class);
    assertThat(bar.foo).isSameAs(foo);
  }

  @Test
  void postProcessorDoesNotOverrideRegularBeanDefinitions() {
    BeanDefinition rbd = new BeanDefinition(TestBean.class);
    rbd.setSource(new DescriptiveResource("XML or something"));
    beanFactory.registerBeanDefinition("bar", rbd);
    beanFactory.registerBeanDefinition("config", new BeanDefinition(SingletonBeanConfig.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    beanFactory.getBean("foo", Foo.class);
    beanFactory.getBean("bar", TestBean.class);
  }

//  @Test
//  void postProcessorDoesNotOverrideRegularBeanDefinitionsEvenWithScopedProxy() {
//    BeanDefinition rbd = new BeanDefinition(TestBean.class);
//    rbd.setSource(new DescriptiveResource("XML or something"));
//    BeanDefinitionHolder proxied = ScopedProxyUtils.createScopedProxy(
//            new BeanDefinitionHolder(rbd, "bar"), beanFactory, true);
//    beanFactory.registerBeanDefinition("bar", proxied.getBeanDefinition());
//    beanFactory.registerBeanDefinition("config", new BeanDefinition(SingletonBeanConfig.class));
//    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
//    pp.postProcessBeanFactory(beanFactory);
//    beanFactory.getBean("foo", Foo.class);
//    beanFactory.getBean("bar", TestBean.class);
//  }

  @Test
  void postProcessorFailsOnImplicitOverrideIfOverridingIsNotAllowed() {
    BeanDefinition rbd = new BeanDefinition(TestBean.class);
    rbd.setSource(new DescriptiveResource("XML or something"));
    beanFactory.registerBeanDefinition("bar", rbd);
    beanFactory.registerBeanDefinition("config", new BeanDefinition(SingletonBeanConfig.class));
    beanFactory.setAllowBeanDefinitionOverriding(false);
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
                    pp.postProcessBeanFactory(beanFactory))
            .withMessageContaining("bar")
            .withMessageContaining("SingletonBeanConfig")
            .withMessageContaining(TestBean.class.getName());
  }

  @Test
    // gh-25430
  void detectAliasOverride() {
    StandardApplicationContext context = new StandardApplicationContext();
    StandardBeanFactory beanFactory = context.getBeanFactory();
    beanFactory.setAllowBeanDefinitionOverriding(false);
    context.register(FirstConfiguration.class, SecondConfiguration.class);
    assertThatIllegalStateException().isThrownBy(context::refresh)
            .withMessageContaining("alias 'taskExecutor'")
            .withMessageContaining("name 'applicationTaskExecutor'")
            .withMessageContaining("bean definition 'taskExecutor'");
  }

  @Test
  void configurationClassesProcessedInCorrectOrder() {
    beanFactory.registerBeanDefinition("config1", new BeanDefinition(OverridingSingletonBeanConfig.class));
    beanFactory.registerBeanDefinition("config2", new BeanDefinition(SingletonBeanConfig.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);

    Foo foo = beanFactory.getBean(Foo.class);
    boolean condition = foo instanceof ExtendedFoo;
    assertThat(condition).isTrue();
    Bar bar = beanFactory.getBean(Bar.class);
    assertThat(bar.foo).isSameAs(foo);
  }

  @Test
  void configurationClassesWithValidOverridingForProgrammaticCall() {
    beanFactory.registerBeanDefinition("config1", new BeanDefinition(OverridingAgainSingletonBeanConfig.class));
    beanFactory.registerBeanDefinition("config2", new BeanDefinition(OverridingSingletonBeanConfig.class));
    beanFactory.registerBeanDefinition("config3", new BeanDefinition(SingletonBeanConfig.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);

    Foo foo = beanFactory.getBean(Foo.class);
    boolean condition = foo instanceof ExtendedAgainFoo;
    assertThat(condition).isTrue();
    Bar bar = beanFactory.getBean(Bar.class);
    assertThat(bar.foo).isSameAs(foo);
  }

  @Test
  void configurationClassesWithInvalidOverridingForProgrammaticCall() {
    beanFactory.registerBeanDefinition("config1", new BeanDefinition(InvalidOverridingSingletonBeanConfig.class));
    beanFactory.registerBeanDefinition("config2", new BeanDefinition(OverridingSingletonBeanConfig.class));
    beanFactory.registerBeanDefinition("config3", new BeanDefinition(SingletonBeanConfig.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);

    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    beanFactory.getBean(Bar.class))
            .withMessageContaining("OverridingSingletonBeanConfig.foo")
            .withMessageContaining(ExtendedFoo.class.getName())
            .withMessageContaining(Foo.class.getName())
            .withMessageContaining("InvalidOverridingSingletonBeanConfig");
  }

  @Test
    // SPR-15384
  void nestedConfigurationClassesProcessedInCorrectOrder() {
    beanFactory.registerBeanDefinition("config", new BeanDefinition(ConfigWithOrderedNestedClasses.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);

    Foo foo = beanFactory.getBean(Foo.class);
    boolean condition = foo instanceof ExtendedFoo;
    assertThat(condition).isTrue();
    Bar bar = beanFactory.getBean(Bar.class);
    assertThat(bar.foo).isSameAs(foo);
  }

  @Test
    // SPR-16734
  void innerConfigurationClassesProcessedInCorrectOrder() {
    beanFactory.registerBeanDefinition("config", new BeanDefinition(ConfigWithOrderedInnerClasses.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(new StandardDependenciesBeanPostProcessor());

    Foo foo = beanFactory.getBean(Foo.class);
    boolean condition = foo instanceof ExtendedFoo;
    assertThat(condition).isTrue();
    Bar bar = beanFactory.getBean(Bar.class);
    assertThat(bar.foo).isSameAs(foo);
  }

  @Test
  void scopedProxyTargetMarkedAsNonAutowireCandidate() {
    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
    bpp.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(bpp);
    beanFactory.registerBeanDefinition("config", new BeanDefinition(ScopedProxyConfigurationClass.class));
    beanFactory.registerBeanDefinition("consumer", new BeanDefinition(ScopedProxyConsumer.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);

    ITestBean injected = beanFactory.getBean("consumer", ScopedProxyConsumer.class).testBean;
    boolean condition = injected instanceof ScopedObject;
    assertThat(condition).isTrue();
    assertThat(injected).isSameAs(beanFactory.getBean("scopedClass"));
    assertThat(injected).isSameAs(beanFactory.getBean(ITestBean.class));
  }

  @Test
  void processingAllowedOnlyOncePerProcessorRegistryPair() {
    StandardBeanFactory bf1 = new StandardBeanFactory();
    StandardBeanFactory bf2 = new StandardBeanFactory();
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(bf1); // first invocation -- should succeed
    assertThatIllegalStateException().isThrownBy(() ->
            pp.postProcessBeanFactory(bf1)); // second invocation for bf1 -- should throw
    pp.postProcessBeanFactory(bf2); // first invocation for bf2 -- should succeed
    assertThatIllegalStateException().isThrownBy(() ->
            pp.postProcessBeanFactory(bf2)); // second invocation for bf2 -- should throw
  }

  @Test
  void genericsBasedInjection() {
    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
    bpp.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(bpp);
    BeanDefinition bd = new BeanDefinition(RepositoryInjectionBean.class);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    beanFactory.registerBeanDefinition("annotatedBean", bd);
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RepositoryConfiguration.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);

    RepositoryInjectionBean bean = (RepositoryInjectionBean) beanFactory.getBean("annotatedBean");
    assertThat(bean.stringRepository.toString()).isEqualTo("Repository<String>");
    assertThat(bean.integerRepository.toString()).isEqualTo("Repository<Integer>");
  }

  @Test
  void genericsBasedInjectionWithScoped() {
    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
    bpp.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(bpp);
    BeanDefinition bd = new BeanDefinition(RepositoryInjectionBean.class);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    beanFactory.registerBeanDefinition("annotatedBean", bd);
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(ScopedRepositoryConfiguration.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);

    RepositoryInjectionBean bean = (RepositoryInjectionBean) beanFactory.getBean("annotatedBean");
    assertThat(bean.stringRepository.toString()).isEqualTo("Repository<String>");
    assertThat(bean.integerRepository.toString()).isEqualTo("Repository<Integer>");
  }

  @Test
  void genericsBasedInjectionWithScopedProxy() {
    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
    bpp.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(bpp);
    BeanDefinition bd = new BeanDefinition(RepositoryInjectionBean.class);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    beanFactory.registerBeanDefinition("annotatedBean", bd);
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(ScopedProxyRepositoryConfiguration.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
//    beanFactory.freezeConfiguration();

    RepositoryInjectionBean bean = (RepositoryInjectionBean) beanFactory.getBean("annotatedBean");
    assertThat(bean.stringRepository.toString()).isEqualTo("Repository<String>");
    assertThat(bean.integerRepository.toString()).isEqualTo("Repository<Integer>");
    assertThat(AopUtils.isCglibProxy(bean.stringRepository)).isTrue();
    assertThat(AopUtils.isCglibProxy(bean.integerRepository)).isTrue();
  }

  @Test
  void genericsBasedInjectionWithScopedProxyUsingAsm() {
    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
    bpp.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(bpp);
    BeanDefinition bd = new BeanDefinition(RepositoryInjectionBean.class.getName());
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    beanFactory.registerBeanDefinition("annotatedBean", bd);
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(ScopedProxyRepositoryConfiguration.class.getName()));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
//    beanFactory.freezeConfiguration();

    RepositoryInjectionBean bean = (RepositoryInjectionBean) beanFactory.getBean("annotatedBean");
    assertThat(bean.stringRepository.toString()).isEqualTo("Repository<String>");
    assertThat(bean.integerRepository.toString()).isEqualTo("Repository<Integer>");
    assertThat(AopUtils.isCglibProxy(bean.stringRepository)).isTrue();
    assertThat(AopUtils.isCglibProxy(bean.integerRepository)).isTrue();
  }

  @Test
  void genericsBasedInjectionWithImplTypeAtInjectionPoint() {
    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
    bpp.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(bpp);
    BeanDefinition bd = new BeanDefinition(SpecificRepositoryInjectionBean.class);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    beanFactory.registerBeanDefinition("annotatedBean", bd);
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(SpecificRepositoryConfiguration.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    beanFactory.preInstantiateSingletons();

    SpecificRepositoryInjectionBean bean = (SpecificRepositoryInjectionBean) beanFactory.getBean("annotatedBean");
    assertThat(bean.genericRepository).isSameAs(beanFactory.getBean("genericRepo"));
  }

  @Test
  void genericsBasedInjectionWithFactoryBean() {
    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
    bpp.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(bpp);
    BeanDefinition bd = new BeanDefinition(RepositoryFactoryBeanInjectionBean.class);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    beanFactory.registerBeanDefinition("annotatedBean", bd);
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RepositoryFactoryBeanConfiguration.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);
    beanFactory.preInstantiateSingletons();

    RepositoryFactoryBeanInjectionBean bean = (RepositoryFactoryBeanInjectionBean) beanFactory.getBean("annotatedBean");
    assertThat(bean.repositoryFactoryBean).isSameAs(beanFactory.getBean("&repoFactoryBean"));
    assertThat(bean.qualifiedRepositoryFactoryBean).isSameAs(beanFactory.getBean("&repoFactoryBean"));
    assertThat(bean.prefixQualifiedRepositoryFactoryBean).isSameAs(beanFactory.getBean("&repoFactoryBean"));
  }

  @Test
  void genericsBasedInjectionWithRawMatch() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RawMatchingConfiguration.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);

    assertThat(beanFactory.getBean("repoConsumer")).isSameAs(beanFactory.getBean("rawRepo"));
  }

  @Test
  void genericsBasedInjectionWithWildcardMatch() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(WildcardMatchingConfiguration.class));
    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    pp.postProcessBeanFactory(beanFactory);

    assertThat(beanFactory.getBean("repoConsumer")).isSameAs(beanFactory.getBean("genericRepo"));
  }

  @Test
  void genericsBasedInjectionWithWildcardWithExtendsMatch() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(WildcardWithExtendsConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);

    assertThat(beanFactory.getBean("repoConsumer")).isSameAs(beanFactory.getBean("stringRepo"));
  }

  @Test
  void genericsBasedInjectionWithWildcardWithGenericExtendsMatch() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(WildcardWithGenericExtendsConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);

    assertThat(beanFactory.getBean("repoConsumer")).isSameAs(beanFactory.getBean("genericRepo"));
  }

  @Test
  void genericsBasedInjectionWithEarlyGenericsMatching() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);

    String[] beanNames = beanFactory.getBeanNamesForType(Repository.class).toArray(new String[0]);
    assertThat(beanNames).contains("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");
  }

  @Test
  void genericsBasedInjectionWithLateGenericsMatching() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    beanFactory.preInstantiateSingletons();

    Set<String> beanNamesSet = beanFactory.getBeanNamesForType(Repository.class);
    String[] beanNames = beanNamesSet.toArray(String[]::new);
    assertThat(beanNames).contains("stringRepo");

    beanNamesSet = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class));
    beanNames = beanNamesSet.toArray(String[]::new);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNamesSet = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class));
    beanNames = beanNamesSet.toArray(String[]::new);

    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");
  }

  @Test
  void genericsBasedInjectionWithEarlyGenericsMatchingAndRawFactoryMethod() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RawFactoryMethodRepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);

    Set<String> beanNamesSet = beanFactory.getBeanNamesForType(Repository.class);
    String[] beanNames = beanNamesSet.toArray(String[]::new);

    assertThat(beanNames).contains("stringRepo");
    beanNamesSet = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class));
    ;

    beanNames = beanNamesSet.toArray(String[]::new);

    assertThat(beanNames.length).isEqualTo(0);

    beanNamesSet = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class));
    beanNames = beanNamesSet.toArray(String[]::new);
    assertThat(beanNames.length).isEqualTo(0);
  }

  @Test
  void genericsBasedInjectionWithLateGenericsMatchingAndRawFactoryMethod() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RawFactoryMethodRepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    beanFactory.preInstantiateSingletons();

    String[] beanNames = beanFactory.getBeanNamesForType(Repository.class)
            .toArray(new String[0]);
    assertThat(beanNames).contains("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class))
            .toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class))
            .toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");
  }

  @Test
  void genericsBasedInjectionWithEarlyGenericsMatchingAndRawInstance() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RawInstanceRepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);

    String[] beanNames = beanFactory.getBeanNamesForType(Repository.class)
            .toArray(new String[0]);
    assertThat(beanNames).contains("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class))
            .toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class))
            .toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");
  }

  @Test
  void genericsBasedInjectionWithLateGenericsMatchingAndRawInstance() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RawInstanceRepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    beanFactory.preInstantiateSingletons();

    String[] beanNames = beanFactory.getBeanNamesForType(Repository.class)
            .toArray(new String[0]);
    assertThat(beanNames).contains("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");
  }

  @Test
  void genericsBasedInjectionWithEarlyGenericsMatchingOnCglibProxy() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
    autoProxyCreator.setProxyTargetClass(true);
    autoProxyCreator.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(autoProxyCreator);
    beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));

    String[] beanNames = beanFactory.getBeanNamesForType(Repository.class).toArray(new String[0]);
    assertThat(beanNames).contains("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class))
            .toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class))
            .toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    assertThat(AopUtils.isCglibProxy(beanFactory.getBean("stringRepo"))).isTrue();
  }

  @Test
  void genericsBasedInjectionWithLateGenericsMatchingOnCglibProxy() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
    autoProxyCreator.setProxyTargetClass(true);
    autoProxyCreator.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(autoProxyCreator);
    beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
    beanFactory.preInstantiateSingletons();

    String[] beanNames = beanFactory.getBeanNamesForType(Repository.class).toArray(new String[0]);
    assertThat(beanNames).contains("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    assertThat(AopUtils.isCglibProxy(beanFactory.getBean("stringRepo"))).isTrue();
  }

  @Test
  void genericsBasedInjectionWithLateGenericsMatchingOnCglibProxyAndRawFactoryMethod() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RawFactoryMethodRepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
    autoProxyCreator.setProxyTargetClass(true);
    autoProxyCreator.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(autoProxyCreator);
    beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
    beanFactory.preInstantiateSingletons();

    String[] beanNames = beanFactory.getBeanNamesForType(Repository.class).toArray(new String[0]);
    assertThat(beanNames).contains("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    assertThat(AopUtils.isCglibProxy(beanFactory.getBean("stringRepo"))).isTrue();
  }

  @Test
  void genericsBasedInjectionWithLateGenericsMatchingOnCglibProxyAndRawInstance() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RawInstanceRepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
    autoProxyCreator.setProxyTargetClass(true);
    autoProxyCreator.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(autoProxyCreator);
    beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
    beanFactory.preInstantiateSingletons();

    String[] beanNames = beanFactory.getBeanNamesForType(Repository.class).toArray(new String[0]);
    assertThat(beanNames).contains("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(Repository.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    assertThat(AopUtils.isCglibProxy(beanFactory.getBean("stringRepo"))).isTrue();
  }

  @Test
  void genericsBasedInjectionWithEarlyGenericsMatchingOnJdkProxy() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
    autoProxyCreator.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(autoProxyCreator);
    beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));

    String[] beanNames = beanFactory.getBeanNamesForType(RepositoryInterface.class).toArray(new String[0]);
    assertThat(beanNames).contains("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(RepositoryInterface.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(RepositoryInterface.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    assertThat(AopUtils.isJdkDynamicProxy(beanFactory.getBean("stringRepo"))).isTrue();
  }

  @Test
  void genericsBasedInjectionWithLateGenericsMatchingOnJdkProxy() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
    autoProxyCreator.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(autoProxyCreator);
    beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
    beanFactory.preInstantiateSingletons();

    String[] beanNames = beanFactory.getBeanNamesForType(RepositoryInterface.class).toArray(new String[0]);
    assertThat(beanNames).contains("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(RepositoryInterface.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(RepositoryInterface.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    assertThat(AopUtils.isJdkDynamicProxy(beanFactory.getBean("stringRepo"))).isTrue();
  }

  @Test
  void genericsBasedInjectionWithLateGenericsMatchingOnJdkProxyAndRawFactoryMethod() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RawFactoryMethodRepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
    autoProxyCreator.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(autoProxyCreator);
    beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
    beanFactory.preInstantiateSingletons();

    String[] beanNames = beanFactory.getBeanNamesForType(RepositoryInterface.class).toArray(new String[0]);
    assertThat(beanNames).contains("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(RepositoryInterface.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(RepositoryInterface.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    assertThat(AopUtils.isJdkDynamicProxy(beanFactory.getBean("stringRepo"))).isTrue();
  }

  @Test
  void genericsBasedInjectionWithLateGenericsMatchingOnJdkProxyAndRawInstance() {
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(RawInstanceRepositoryConfiguration.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
    autoProxyCreator.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(autoProxyCreator);
    beanFactory.registerSingleton("traceInterceptor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
    beanFactory.preInstantiateSingletons();

    String[] beanNames = beanFactory.getBeanNamesForType(RepositoryInterface.class).toArray(new String[0]);
    assertThat(beanNames).contains("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(RepositoryInterface.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    beanNames = beanFactory.getBeanNamesForType(ResolvableType.fromClassWithGenerics(RepositoryInterface.class, String.class)).toArray(new String[0]);
    assertThat(beanNames.length).isEqualTo(1);
    assertThat(beanNames[0]).isEqualTo("stringRepo");

    assertThat(AopUtils.isJdkDynamicProxy(beanFactory.getBean("stringRepo"))).isTrue();
  }

  @Test
  void testSelfReferenceExclusionForFactoryMethodOnSameBean() {
    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
    bpp.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(bpp);
    InitDestroyAnnotationBeanPostProcessor beanPostProcessor = new InitDestroyAnnotationBeanPostProcessor();
    beanPostProcessor.setInitAnnotationType(PostConstruct.class);
    beanPostProcessor.setDestroyAnnotationType(PreDestroy.class);
    beanFactory.addBeanPostProcessor(beanPostProcessor);

    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(ConcreteConfig.class));
    beanFactory.registerBeanDefinition("serviceBeanProvider", new BeanDefinition(ServiceBeanProvider.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    beanFactory.preInstantiateSingletons();

    beanFactory.getBean(ServiceBean.class);
  }

  @Test
  void testConfigWithDefaultMethods() {
    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
    bpp.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(bpp);
    InitDestroyAnnotationBeanPostProcessor beanPostProcessor = new InitDestroyAnnotationBeanPostProcessor();
    beanPostProcessor.setInitAnnotationType(PostConstruct.class);
    beanPostProcessor.setDestroyAnnotationType(PreDestroy.class);
    beanFactory.addBeanPostProcessor(beanPostProcessor);
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(ConcreteConfigWithDefaultMethods.class));
    beanFactory.registerBeanDefinition("serviceBeanProvider", new BeanDefinition(ServiceBeanProvider.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    beanFactory.preInstantiateSingletons();

    beanFactory.getBean(ServiceBean.class);
  }

  @Test
  void testConfigWithDefaultMethodsUsingAsm() {
    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
    bpp.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(bpp);
    InitDestroyAnnotationBeanPostProcessor beanPostProcessor = new InitDestroyAnnotationBeanPostProcessor();
    beanPostProcessor.setInitAnnotationType(PostConstruct.class);
    beanPostProcessor.setDestroyAnnotationType(PreDestroy.class);
    beanFactory.addBeanPostProcessor(beanPostProcessor);
    beanFactory.registerBeanDefinition("configClass", new BeanDefinition(ConcreteConfigWithDefaultMethods.class.getName()));
    beanFactory.registerBeanDefinition("serviceBeanProvider", new BeanDefinition(ServiceBeanProvider.class.getName()));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    beanFactory.preInstantiateSingletons();

    beanFactory.getBean(ServiceBean.class);
  }

  @Test
  void testCircularDependency() {
    StandardDependenciesBeanPostProcessor bpp = new StandardDependenciesBeanPostProcessor();
    bpp.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(bpp);
    beanFactory.registerBeanDefinition("configClass1", new BeanDefinition(A.class));
    beanFactory.registerBeanDefinition("configClass2", new BeanDefinition(AStrich.class));
    new ConfigurationClassPostProcessor(loadingContext).postProcessBeanFactory(beanFactory);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
                    beanFactory::preInstantiateSingletons)
            .withMessageContaining("Circular reference");
  }

  @Test
  void testCircularDependencyWithApplicationContext() {
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    new StandardApplicationContext(A.class, AStrich.class))
            .withMessageContaining("Circular reference");
  }

  @Test
  void testPrototypeArgumentThroughBeanMethodCall() {
    ApplicationContext ctx = new StandardApplicationContext(BeanArgumentConfigWithPrototype.class);
    ctx.getBean(FooFactory.class).createFoo(new BarArgument());
  }

  @Test
  void testSingletonArgumentThroughBeanMethodCall() {
    ApplicationContext ctx = new StandardApplicationContext(BeanArgumentConfigWithSingleton.class);
    ctx.getBean(FooFactory.class).createFoo(new BarArgument());
  }

  @Test
  void testNullArgumentThroughBeanMethodCall() {
    ApplicationContext ctx = new StandardApplicationContext(BeanArgumentConfigWithNull.class);
    ctx.getBean("aFoo");
  }

  @Test
  void testInjectionPointMatchForNarrowTargetReturnType() {
    ApplicationContext ctx = new StandardApplicationContext(FooBarConfiguration.class);
    assertThat(ctx.getBean(FooImpl.class).bar).isSameAs(ctx.getBean(BarImpl.class));
  }

  @Test
  void testVarargOnBeanMethod() {
    ApplicationContext ctx = new StandardApplicationContext(VarargConfiguration.class, TestBean.class);
    VarargConfiguration bean = ctx.getBean(VarargConfiguration.class);
    assertThat(bean.testBeans).isNotNull();
    assertThat(bean.testBeans.length).isEqualTo(1);
    assertThat(bean.testBeans[0]).isSameAs(ctx.getBean(TestBean.class));
  }

  @Test
  void testEmptyVarargOnBeanMethod() {
    ApplicationContext ctx = new StandardApplicationContext(VarargConfiguration.class);
    VarargConfiguration bean = ctx.getBean(VarargConfiguration.class);
    assertThat(bean.testBeans).isNotNull();
    assertThat(bean.testBeans.length).isEqualTo(0);
  }

  @Test
  void testCollectionArgumentOnBeanMethod() {
    ApplicationContext ctx = new StandardApplicationContext(CollectionArgumentConfiguration.class, TestBean.class);
    CollectionArgumentConfiguration bean = ctx.getBean(CollectionArgumentConfiguration.class);
    assertThat(bean.testBeans).isNotNull();
    assertThat(bean.testBeans.size()).isEqualTo(1);
    assertThat(bean.testBeans.get(0)).isSameAs(ctx.getBean(TestBean.class));
  }

  @Test
  void testEmptyCollectionArgumentOnBeanMethod() {
    ApplicationContext ctx = new StandardApplicationContext(CollectionArgumentConfiguration.class);
    CollectionArgumentConfiguration bean = ctx.getBean(CollectionArgumentConfiguration.class);
    assertThat(bean.testBeans).isNotNull();
    assertThat(bean.testBeans.isEmpty()).isTrue();
  }

  @Test
  void testMapArgumentOnBeanMethod() {
    ApplicationContext ctx = new StandardApplicationContext(MapArgumentConfiguration.class, DummyRunnable.class);
    MapArgumentConfiguration bean = ctx.getBean(MapArgumentConfiguration.class);
    assertThat(bean.testBeans).isNotNull();
    assertThat(bean.testBeans.size()).isEqualTo(1);
    assertThat(bean.testBeans.values().iterator().next()).isSameAs(ctx.getBean(Runnable.class));
  }

  @Test
  void testEmptyMapArgumentOnBeanMethod() {
    ApplicationContext ctx = new StandardApplicationContext(MapArgumentConfiguration.class);
    MapArgumentConfiguration bean = ctx.getBean(MapArgumentConfiguration.class);
    assertThat(bean.testBeans).isNotNull();
    assertThat(bean.testBeans.isEmpty()).isTrue();
  }

  @Test
  void testCollectionInjectionFromSameConfigurationClass() {
    ApplicationContext ctx = new StandardApplicationContext(CollectionInjectionConfiguration.class);
    CollectionInjectionConfiguration bean = ctx.getBean(CollectionInjectionConfiguration.class);
    assertThat(bean.testBeans).isNotNull();
    assertThat(bean.testBeans.size()).isEqualTo(1);
    assertThat(bean.testBeans.get(0)).isSameAs(ctx.getBean(TestBean.class));
  }

  @Test
  void testMapInjectionFromSameConfigurationClass() {
    ApplicationContext ctx = new StandardApplicationContext(MapInjectionConfiguration.class);
    MapInjectionConfiguration bean = ctx.getBean(MapInjectionConfiguration.class);
    assertThat(bean.testBeans).isNotNull();
    assertThat(bean.testBeans.size()).isEqualTo(1);
    assertThat(bean.testBeans.get("testBean")).isSameAs(ctx.getBean(Runnable.class));
  }
//
//  @Test
//  void testBeanLookupFromSameConfigurationClass() {
//    ApplicationContext ctx = new StandardApplicationContext(BeanLookupConfiguration.class);
//    BeanLookupConfiguration bean = ctx.getBean(BeanLookupConfiguration.class);
//    assertThat(bean.getTestBean()).isNotNull();
//    assertThat(bean.getTestBean()).isSameAs(ctx.getBean(TestBean.class));
//  }

  @Test
  void testNameClashBetweenConfigurationClassAndBean() {
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() -> {
      ApplicationContext ctx = new StandardApplicationContext(MyTestBean.class);
      ctx.getBean("myTestBean", TestBean.class);
    });
  }

  @Test
  void testBeanDefinitionRegistryPostProcessorConfig() {
    ApplicationContext ctx = new StandardApplicationContext(BeanDefinitionRegistryPostProcessorConfig.class);
    boolean condition = ctx.getBean("myTestBean") instanceof TestBean;
    assertThat(condition).isTrue();
  }

  // -------------------------------------------------------------------------

  @Configuration
  @Order(1)
  static class SingletonBeanConfig {

    public @Bean
    Foo foo() {
      return new Foo();
    }

    public @Bean
    Bar bar() {
      return new Bar(foo());
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class NonEnhancedSingletonBeanConfig {

    public @Bean
    Foo foo() {
      return new Foo();
    }

    public @Bean
    Bar bar() {
      return new Bar(foo());
    }
  }

  @Configuration
  static class StaticSingletonBeanConfig {

    public static @Bean
    Foo foo() {
      return new Foo();
    }

    public static @Bean
    Bar bar() {
      return new Bar(foo());
    }
  }

  @Configuration
  @Order(2)
  static class OverridingSingletonBeanConfig {

    public @Bean
    ExtendedFoo foo() {
      return new ExtendedFoo();
    }

    public @Bean
    Bar bar() {
      return new Bar(foo());
    }
  }

  @Configuration
  static class OverridingAgainSingletonBeanConfig {

    public @Bean
    ExtendedAgainFoo foo() {
      return new ExtendedAgainFoo();
    }
  }

  @Configuration
  static class InvalidOverridingSingletonBeanConfig {

    public @Bean
    Foo foo() {
      return new Foo();
    }
  }

  @Configuration
  static class ConfigWithOrderedNestedClasses {

    @Configuration
    @Order(1)
    static class SingletonBeanConfig {

      public @Bean
      Foo foo() {
        return new Foo();
      }

      public @Bean
      Bar bar() {
        return new Bar(foo());
      }
    }

    @Configuration
    @Order(2)
    static class OverridingSingletonBeanConfig {

      public @Bean
      ExtendedFoo foo() {
        return new ExtendedFoo();
      }

      public @Bean
      Bar bar() {
        return new Bar(foo());
      }
    }
  }

  @Configuration
  static class ConfigWithOrderedInnerClasses {

    @Configuration
    @Order(1)
    class SingletonBeanConfig {

      public SingletonBeanConfig(ConfigWithOrderedInnerClasses other) {
      }

      public @Bean
      Foo foo() {
        return new Foo();
      }

      public @Bean
      Bar bar() {
        return new Bar(foo());
      }
    }

    @Configuration
    @Order(2)
    class OverridingSingletonBeanConfig {

      public OverridingSingletonBeanConfig(ObjectSupplier<SingletonBeanConfig> other) {
        other.get();
      }

      public @Bean
      ExtendedFoo foo() {
        return new ExtendedFoo();
      }

      public @Bean
      Bar bar() {
        return new Bar(foo());
      }
    }
  }

  static class Foo {
  }

  static class ExtendedFoo extends Foo {
  }

  static class ExtendedAgainFoo extends ExtendedFoo {
  }

  static class Bar {

    final Foo foo;

    public Bar(Foo foo) {
      this.foo = foo;
    }
  }

  @Configuration
  static class UnloadedConfig {

    public @Bean
    Foo foo() {
      return new Foo();
    }
  }

  @Configuration
  static class LoadedConfig {

    public @Bean
    Bar bar() {
      return new Bar(new Foo());
    }
  }

  @Configuration
  static class FirstConfiguration {

    @Bean
    SyncTaskExecutor taskExecutor() {
      return new SyncTaskExecutor();
    }
  }

  @Configuration
  static class SecondConfiguration {

    @Bean(name = { "applicationTaskExecutor", "taskExecutor" })
    SimpleAsyncTaskExecutor simpleAsyncTaskExecutor() {
      return new SimpleAsyncTaskExecutor();
    }
  }

  public static class ScopedProxyConsumer {

    @Autowired
    public ITestBean testBean;
  }

  @Configuration
  public static class ScopedProxyConfigurationClass {

    @Bean
    @Lazy
    @Scope/*(proxyMode = ScopedProxyMode.INTERFACES)*/
    public ITestBean scopedClass() {
      return new cn.taketoday.beans.factory.support.TestBean();
    }
  }

  public interface RepositoryInterface<T> {

    @Override
    String toString();
  }

  public static class Repository<T> implements RepositoryInterface<T> {
  }

  public static class GenericRepository<T> extends Repository<T> {
  }

  public static class RepositoryFactoryBean<T> implements FactoryBean<T> {

    @Override
    public T getObject() {
      throw new IllegalStateException();
    }

    @Override
    public Class<?> getObjectType() {
      return Object.class;
    }

    @Override
    public boolean isSingleton() {
      return false;
    }
  }

  public static class RepositoryInjectionBean {

    @Autowired
    public Repository<String> stringRepository;

    @Autowired
    public Repository<Integer> integerRepository;
  }

  @Configuration
  public static class RepositoryConfiguration {

    @Bean
    public Repository<String> stringRepo() {
      return new Repository<String>() {
        @Override
        public String toString() {
          return "Repository<String>";
        }
      };
    }

    @Bean
    public Repository<Integer> integerRepo() {
      return new Repository<Integer>() {
        @Override
        public String toString() {
          return "Repository<Integer>";
        }
      };
    }

    @Bean
    public Repository<?> genericRepo() {
      return new Repository<Object>() {
        @Override
        public String toString() {
          return "Repository<Object>";
        }
      };
    }
  }

  @Configuration
  public static class RawFactoryMethodRepositoryConfiguration {

    @SuppressWarnings("rawtypes") // intentionally a raw type
    @Bean
    public Repository stringRepo() {
      return new Repository<String>() {
        @Override
        public String toString() {
          return "Repository<String>";
        }
      };
    }
  }

  @Configuration
  public static class RawInstanceRepositoryConfiguration {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    public Repository<String> stringRepo() {
      return new Repository() {
        @Override
        public String toString() {
          return "Repository<String>";
        }
      };
    }
  }

  @Configuration
  public static class ScopedRepositoryConfiguration {

    @Bean
    @Scope("prototype")
    public Repository<String> stringRepo() {
      return new Repository<String>() {
        @Override
        public String toString() {
          return "Repository<String>";
        }
      };
    }

    @Bean
    @Scope("prototype")
    public Repository<Integer> integerRepo() {
      return new Repository<Integer>() {
        @Override
        public String toString() {
          return "Repository<Integer>";
        }
      };
    }

    @Bean
    @Scope("prototype")
    @SuppressWarnings("rawtypes")
    public Repository genericRepo() {
      return new Repository<Object>() {
        @Override
        public String toString() {
          return "Repository<Object>";
        }
      };
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Scope(scopeName = "prototype")
  public @interface PrototypeScoped {

//    ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;
  }

  @Configuration
  public static class ScopedProxyRepositoryConfiguration {

    @Bean
    @Scope(scopeName = "prototype"/*, proxyMode = ScopedProxyMode.TARGET_CLASS*/)
    public Repository<String> stringRepo() {
      return new Repository<String>() {
        @Override
        public String toString() {
          return "Repository<String>";
        }
      };
    }

    @Bean
    @PrototypeScoped
    public Repository<Integer> integerRepo() {
      return new Repository<Integer>() {
        @Override
        public String toString() {
          return "Repository<Integer>";
        }
      };
    }
  }

  public static class SpecificRepositoryInjectionBean {

    @Autowired
    public GenericRepository<?> genericRepository;
  }

  @Configuration
  public static class SpecificRepositoryConfiguration {

    @Bean
    public Repository<Object> genericRepo() {
      return new GenericRepository<>();
    }
  }

  public static class RepositoryFactoryBeanInjectionBean {

    @Autowired
    public RepositoryFactoryBean<?> repositoryFactoryBean;

    @Autowired
    @Qualifier("repoFactoryBean")
    public RepositoryFactoryBean<?> qualifiedRepositoryFactoryBean;

    @Autowired
    @Qualifier("&repoFactoryBean")
    public RepositoryFactoryBean<?> prefixQualifiedRepositoryFactoryBean;
  }

  @Configuration
  public static class RepositoryFactoryBeanConfiguration {

    @Bean
    public RepositoryFactoryBean<Object> repoFactoryBean() {
      return new RepositoryFactoryBean<>();
    }

    @Bean
    public FactoryBean<Object> nullFactoryBean() {
      return null;
    }
  }

  @Configuration
  public static class RawMatchingConfiguration {

    @Bean
    @SuppressWarnings("rawtypes")
    public Repository rawRepo() {
      return new Repository();
    }

    @Bean
    public Object repoConsumer(Repository<String> repo) {
      return repo;
    }
  }

  @Configuration
  public static class WildcardMatchingConfiguration {

    @Bean
    @SuppressWarnings("rawtypes")
    public Repository<?> genericRepo() {
      return new Repository();
    }

    @Bean
    public Object repoConsumer(Repository<String> repo) {
      return repo;
    }
  }

  @Configuration
  public static class WildcardWithExtendsConfiguration {

    @Bean
    public Repository<? extends String> stringRepo() {
      return new Repository<>();
    }

    @Bean
    public Repository<? extends Number> numberRepo() {
      return new Repository<>();
    }

    @Bean
    public Object repoConsumer(Repository<? extends String> repo) {
      return repo;
    }
  }

  @Configuration
  public static class WildcardWithGenericExtendsConfiguration {

    @Bean
    public Repository<? extends Object> genericRepo() {
      return new Repository<String>();
    }

    @Bean
    public Repository<? extends Number> numberRepo() {
      return new Repository<>();
    }

    @Bean
    public Object repoConsumer(Repository<String> repo) {
      return repo;
    }
  }

  @Configuration
  @ComponentScan(basePackages = "cn.taketoday.context.annotation.componentscan.simple")
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface ComposedConfiguration {
  }

  @ComposedConfiguration
  public static class ComposedConfigurationClass {
  }

  @Configuration
  @ComponentScan
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface ComposedConfigurationWithAttributeOverrides {

    String[] basePackages() default {};

    ComponentScan.Filter[] excludeFilters() default {};
  }

  @ComposedConfigurationWithAttributeOverrides(basePackages = "cn.taketoday.context.annotation.componentscan.simple")
  public static class ComposedConfigurationWithAttributeOverrideForBasePackage {
  }

  @ComposedConfigurationWithAttributeOverrides(basePackages = "cn.taketoday.context.annotation.componentscan.simple",
                                               excludeFilters = @ComponentScan.Filter(Component.class))
  public static class ComposedConfigurationWithAttributeOverrideForExcludeFilter {
  }

  @ComponentScan(basePackages = "cn.taketoday.context.annotation.componentscan.base", excludeFilters = {})
  public static class BaseConfigurationWithEmptyExcludeFilters {
  }

  @ComponentScan(basePackages = "cn.taketoday.context.annotation.componentscan.simple",
                 excludeFilters = @ComponentScan.Filter(Component.class))
  public static class ExtendedConfigurationWithAttributeOverrideForExcludeFilter extends BaseConfigurationWithEmptyExcludeFilters {
  }

  @ComposedConfigurationWithAttributeOverrides
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface ComposedComposedConfigurationWithAttributeOverrides {

    String[] basePackages() default {};
  }

  @ComposedComposedConfigurationWithAttributeOverrides(basePackages = "cn.taketoday.context.annotation.componentscan.simple")
  public static class ComposedComposedConfigurationWithAttributeOverridesClass {
  }

  @ComponentScan
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface MetaComponentScan {
  }

  @MetaComponentScan
  @Configuration
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface MetaComponentScanConfigurationWithAttributeOverrides {

    String[] basePackages() default {};
  }

  @MetaComponentScanConfigurationWithAttributeOverrides(basePackages = "cn.taketoday.context.annotation.componentscan.simple")
  public static class MetaComponentScanConfigurationWithAttributeOverridesClass {
  }

  @Configuration
  public static class SubMetaComponentScanConfigurationWithAttributeOverridesClass extends
                                                                                   MetaComponentScanConfigurationWithAttributeOverridesClass {
  }

  public static class ServiceBean {

    private final String parameter;

    public ServiceBean(String parameter) {
      this.parameter = parameter;
    }

    public String getParameter() {
      return parameter;
    }
  }

  @Configuration
  public static abstract class AbstractConfig {

    @Bean
    public ServiceBean serviceBean() {
      return provider().getServiceBean();
    }

    @Bean
    public ServiceBeanProvider provider() {
      return new ServiceBeanProvider();
    }
  }

  @Configuration
  public static class ConcreteConfig extends AbstractConfig {

    @Autowired
    private ServiceBeanProvider provider;

    @Bean
    @Override
    public ServiceBeanProvider provider() {
      return provider;
    }

    @PostConstruct
    public void validate() {
      Assert.notNull(provider, "No ServiceBeanProvider injected");
    }
  }

  public interface BaseInterface {

    ServiceBean serviceBean();
  }

  public interface BaseDefaultMethods extends BaseInterface {

    @Bean
    default ServiceBeanProvider provider() {
      return new ServiceBeanProvider();
    }

    @Bean
    @Override
    default ServiceBean serviceBean() {
      return provider().getServiceBean();
    }
  }

  public interface DefaultMethodsConfig extends BaseDefaultMethods {

  }

  @Configuration
  public static class ConcreteConfigWithDefaultMethods implements DefaultMethodsConfig {

    @Autowired
    private ServiceBeanProvider provider;

    @Bean
    @Override
    public ServiceBeanProvider provider() {
      return provider;
    }

    @PostConstruct
    public void validate() {
      Assert.notNull(provider, "No ServiceBeanProvider injected");
    }
  }

  @Primary
  public static class ServiceBeanProvider {

    public ServiceBean getServiceBean() {
      return new ServiceBean("message");
    }
  }

  @Configuration
  public static class A {

    @Autowired(required = true)
    Z z;

    @Bean
    public B b() {
      if (z == null) {
        throw new NullPointerException("z is null");
      }
      return new B(z);
    }
  }

  @Configuration
  public static class AStrich {

    @Autowired
    B b;

    @Bean
    public Z z() {
      return new Z();
    }
  }

  public static class B {

    public B(Z z) {
    }
  }

  public static class Z {
  }

  @Configuration
  static class BeanArgumentConfigWithPrototype {

    @Bean
    @Scope("prototype")
    public DependingFoo foo(BarArgument bar) {
      return new DependingFoo(bar);
    }

    @Bean
    public FooFactory fooFactory() {
      return new FooFactory() {
        @Override
        public DependingFoo createFoo(BarArgument bar) {
          return foo(bar);
        }
      };
    }
  }

  @Configuration
  static class BeanArgumentConfigWithSingleton {

    @Bean
    @Lazy
    public DependingFoo foo(BarArgument bar) {
      return new DependingFoo(bar);
    }

    @Bean
    public FooFactory fooFactory() {
      return new FooFactory() {
        @Override
        public DependingFoo createFoo(BarArgument bar) {
          return foo(bar);
        }
      };
    }
  }

  @Configuration
  static class BeanArgumentConfigWithNull {

    @Bean
    public DependingFoo aFoo() {
      return foo(null);
    }

    @Bean
    @Lazy
    public DependingFoo foo(BarArgument bar) {
      return new DependingFoo(bar);
    }

    @Bean
    public BarArgument bar() {
      return new BarArgument();
    }
  }

  static class BarArgument {
  }

  static class DependingFoo {

    DependingFoo(BarArgument bar) {
      Assert.notNull(bar, "No BarArgument injected");
    }
  }

  static abstract class FooFactory {

    abstract DependingFoo createFoo(BarArgument bar);
  }

  interface BarInterface {
  }

  static class BarImpl implements BarInterface {
  }

  static class FooImpl {

    @Autowired
    public BarImpl bar;
  }

  @Configuration
  static class FooBarConfiguration {

    @Bean
    public BarInterface bar() {
      return new BarImpl();
    }

    @Bean
    public FooImpl foo() {
      return new FooImpl();
    }
  }

  public static class DummyRunnable implements Runnable {

    @Override
    public void run() {
      /* no-op */
    }
  }

  @Configuration
  static class VarargConfiguration {

    TestBean[] testBeans;

    @Bean/*(autowireCandidate = false)*/
    public TestBean thing(TestBean... testBeans) {
      this.testBeans = testBeans;
      return new TestBean();
    }
  }

  @Configuration
  static class CollectionArgumentConfiguration {

    List<TestBean> testBeans;

    @Bean/*(autowireCandidate = false)*/
    public TestBean thing(List<TestBean> testBeans) {
      this.testBeans = testBeans;
      return new TestBean();
    }
  }

  @Configuration
  public static class MapArgumentConfiguration {

    @Autowired
    ConfigurableEnvironment env;

    Map<String, Runnable> testBeans;

    @Bean/*(autowireCandidate = false)*/
    Runnable testBean(Map<String, Runnable> testBeans,
                      @Qualifier("systemProperties") Map<String, String> sysprops,
                      @Qualifier("systemEnvironment") Map<String, String> sysenv) {
      this.testBeans = testBeans;
      assertThat(sysprops).isSameAs(env.getSystemProperties());
      assertThat(sysenv).isSameAs(env.getSystemEnvironment());
      return () -> { };
    }

    // Unrelated, not to be considered as a factory method
    @SuppressWarnings("unused")
    private boolean testBean(boolean param) {
      return param;
    }
  }

  @Configuration
  static class CollectionInjectionConfiguration {

    @Autowired(required = false)
    public List<TestBean> testBeans;

    @Bean
    public TestBean thing() {
      return new TestBean();
    }
  }

  @Configuration
  public static class MapInjectionConfiguration {

    @Autowired
    private Map<String, Runnable> testBeans;

    @Bean
    Runnable testBean() {
      return () -> { };
    }

    // Unrelated, not to be considered as a factory method
    @SuppressWarnings("unused")
    private boolean testBean(boolean param) {
      return param;
    }
  }

/*  @Configuration
  static abstract class BeanLookupConfiguration {

    @Bean
    public TestBean thing() {
      return new TestBean();
    }

    @Lookup
    public abstract TestBean getTestBean();
  }*/

  @Configuration
  static class BeanDefinitionRegistryPostProcessorConfig {

    @Bean
    public static BeanDefinitionRegistryPostProcessor bdrpp() {
      return new BeanDefinitionRegistryPostProcessor() {
        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
          registry.registerBeanDefinition("myTestBean", new BeanDefinition(TestBean.class));
        }

        @Override
        public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
        }
      };
    }
  }

}
