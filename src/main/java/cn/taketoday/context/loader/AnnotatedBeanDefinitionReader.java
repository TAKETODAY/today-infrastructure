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

package cn.taketoday.context.loader;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.function.Supplier;

import cn.taketoday.beans.Primary;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanNamePopulator;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.beans.factory.SingletonBeanRegistry;
import cn.taketoday.beans.factory.annotation.DisableDependencyInjection;
import cn.taketoday.beans.factory.support.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.support.AutowireCandidateQualifier;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanDefinitionCustomizer;
import cn.taketoday.beans.factory.support.BeanDefinitionCustomizers;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.ConstructorArgumentValues;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotationBeanNamePopulator;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.annotation.AnnotationScopeMetadataResolver;
import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionEvaluator;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.DependsOn;
import cn.taketoday.context.annotation.Description;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * read bean-definition
 *
 * @author TODAY 2021/10/1 16:46
 * @since 4.0
 */
public class AnnotatedBeanDefinitionReader extends BeanDefinitionCustomizers implements BeanDefinitionRegistrar {

  private BeanDefinitionRegistry registry;

  @Nullable
  private ConditionEvaluator conditionEvaluator;

  private ApplicationContext context;

  /**
   * enable
   */
  private boolean enableConditionEvaluation = true;

  private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();
  private BeanNamePopulator beanNamePopulator = AnnotationBeanNamePopulator.INSTANCE;

  public AnnotatedBeanDefinitionReader() { }

  public AnnotatedBeanDefinitionReader(ApplicationContext context) {
    this.context = context;
    if (context instanceof BeanDefinitionRegistry registry) {
      this.registry = registry;
    }
    AnnotationConfigUtils.registerAnnotationConfigProcessors(registry);
  }

  public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
    this.registry = registry;
  }

  public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, boolean enableConditionEvaluation) {
    this.registry = registry;
    this.enableConditionEvaluation = enableConditionEvaluation;
  }

  public AnnotatedBeanDefinitionReader(
          ApplicationContext context, BeanDefinitionRegistry registry) {
    this.context = context;
    this.registry = registry;
  }

  //---------------------------------------------------------------------
  // Implementation of BeanDefinitionRegistrar interface
  //---------------------------------------------------------------------

  /**
   * this method requires application-context {@code obtainContext()}
   *
   * @param beanName the name of the bean (may be {@code null})
   * @param beanClass the class of the bean
   * @param constructorArgs custom argument values to be fed into constructor
   * resolution algorithm, resolving either all arguments or just
   * specific ones, with the rest to be resolved through regular autowiring
   */
  @Override
  public <T> void registerBean(@Nullable String beanName, Class<T> beanClass, Object... constructorArgs) {
    registerBean(beanName, beanClass, (Supplier<T>) null,
            bd -> {
              ConstructorArgumentValues argumentValues = bd.getConstructorArgumentValues();
              for (Object arg : constructorArgs) {
                argumentValues.addGenericArgumentValue(arg);
              }
            });
  }

  @Override
  public <T> void registerBean(
          @Nullable String beanName, Class<T> beanClass,
          @Nullable Supplier<T> supplier, @Nullable BeanDefinitionCustomizer... customizers) {
    doRegisterBean(beanClass, beanName, null, supplier, customizers);
  }

  /**
   * Register one or more component classes to be processed.
   * <p>Calls to {@code register} are idempotent; adding the same
   * component class more than once has no additional effect.
   *
   * @param componentClasses one or more component classes,
   * e.g. {@link Configuration @Configuration} classes
   */
  public void register(Class<?>... componentClasses) {
    for (Class<?> componentClass : componentClasses) {
      registerBean(componentClass);
    }
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations.
   *
   * @param beanClass the class of the bean
   */
  public void registerBean(Class<?> beanClass) {
    doRegisterBean(beanClass, null, null, null, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations.
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean
   * (or {@code null} for generating a default bean name)
   */
  public void registerBean(Class<?> beanClass, @Nullable String name) {
    doRegisterBean(beanClass, name, null, null, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations.
   *
   * @param beanClass the class of the bean
   * @param qualifiers specific qualifier annotations to consider,
   * in addition to qualifiers at the bean class level
   */
  @SuppressWarnings("unchecked")
  public void registerBean(Class<?> beanClass, Class<? extends Annotation>... qualifiers) {
    doRegisterBean(beanClass, null, qualifiers, null, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations.
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean
   * (or {@code null} for generating a default bean name)
   * @param qualifiers specific qualifier annotations to consider,
   * in addition to qualifiers at the bean class level
   */
  @SuppressWarnings("unchecked")
  public void registerBean(
          Class<?> beanClass, @Nullable String name,
          Class<? extends Annotation>... qualifiers) {

    doRegisterBean(beanClass, name, qualifiers, null, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations, using the given supplier for obtaining a new
   * instance (possibly declared as a lambda expression or method reference).
   *
   * @param beanClass the class of the bean
   * @param supplier a callback for creating an instance of the bean
   * (may be {@code null})
   */
  public <T> void registerBean(Class<T> beanClass, @Nullable Supplier<T> supplier) {
    doRegisterBean(beanClass, null, null, supplier, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations, using the given supplier for obtaining a new
   * instance (possibly declared as a lambda expression or method reference).
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean
   * (or {@code null} for generating a default bean name)
   * @param supplier a callback for creating an instance of the bean
   * (may be {@code null})
   */
  public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier) {
    doRegisterBean(beanClass, name, null, supplier, null);
  }

  /**
   * Register a bean from the given bean class, deriving its metadata from
   * class-declared annotations.
   *
   * @param beanClass the class of the bean
   * @param name an explicit name for the bean
   * @param qualifiers specific qualifier annotations to consider, if any,
   * in addition to qualifiers at the bean class level
   * @param supplier a callback for creating an instance of the bean
   * (may be {@code null})
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   */
  private <T> void doRegisterBean(
          Class<T> beanClass, @Nullable String name,
          @Nullable Class<? extends Annotation>[] qualifiers,
          @Nullable Supplier<T> supplier, @Nullable BeanDefinitionCustomizer[] customizers) {

    if (shouldSkip(beanClass)) {
      return;
    }

    AnnotatedBeanDefinition definition = new AnnotatedBeanDefinition(beanClass);
    ScopeMetadata scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(definition);
    definition.setScope(scopeMetadata.getScopeName());

    if (qualifiers != null) {
      for (Class<? extends Annotation> qualifier : qualifiers) {
        if (Primary.class == qualifier) {
          definition.setPrimary(true);
        }
        else if (Lazy.class == qualifier) {
          definition.setLazyInit(true);
        }
        else {
          definition.addQualifier(new AutowireCandidateQualifier(qualifier));
        }
      }
    }

    if (StringUtils.hasText(name)) {
      definition.setBeanName(name);
    }
    else {
      beanNamePopulator.populateName(definition, registry);
    }
    definition.setInstanceSupplier(supplier);
    doRegisterWithAnnotationMetadata(definition.getMetadata(), definition, customizers);
  }

  // BeanDefinitionRegistrar end

  public void register(BeanDefinition def) {
    obtainRegistry().registerBeanDefinition(def);
  }

  //---------------------------------------------------------------------
  // register name -> Class
  //---------------------------------------------------------------------

  /**
   * Register a bean with the given name and bean instance
   * <p>
   * just register to {@code SingletonBeanRegistry}
   *
   * @param name bean name (must not be null)
   * @param obj bean instance (must not be null)
   */
  @Override
  public void registerSingleton(String name, Object obj) {
    Assert.notNull(name, "bean-name must not be null");
    Assert.notNull(obj, "bean-instance must not be null");
    obtainContext().unwrapFactory(SingletonBeanRegistry.class).registerSingleton(name, obj);
  }

  //---------------------------------------------------------------------
  // register name -> object singleton
  //---------------------------------------------------------------------

  /**
   * Register a bean with the bean instance
   * <p>
   *
   * register a BeanDefinition and its' singleton
   *
   * @param obj bean instance
   * @throws BeanDefinitionStoreException If can't store a bean
   */
  @Override
  public void registerSingleton(Object obj) {
    Assert.notNull(obj, "bean-instance must not be null");
    BeanDefinition definition = new BeanDefinition(obj.getClass());
    String name = beanNamePopulator.populateName(definition, registry);

    definition.setSynthetic(true);

    register(definition);
    getSingletonRegistry().registerSingleton(name, obj);
  }

  private SingletonBeanRegistry getSingletonRegistry() {
    if (context != null) {
      return context.unwrapFactory(SingletonBeanRegistry.class);
    }
    if (registry instanceof SingletonBeanRegistry) {
      return (SingletonBeanRegistry) registry;
    }
    throw new IllegalStateException("cannot determine a SingletonBeanRegistry");
  }

  //---------------------------------------------------------------------
  // register Class -> Supplier
  //---------------------------------------------------------------------

  /**
   * Register a bean with the given type and instance supplier
   *
   * @param clazz bean class
   * @param supplier bean instance supplier
   * @param prototype register as prototype?
   * @throws BeanDefinitionStoreException If can't store a bean
   * @see #enableConditionEvaluation
   * @since 4.0
   */
  @Override
  public <T> void registerBean(
          Class<T> clazz, Supplier<T> supplier, boolean prototype) throws BeanDefinitionStoreException {
    registerBean(clazz, supplier, prototype, true);
  }

  /**
   * Register a bean with the given type and instance supplier
   * <p>
   * If the provided bean class annotated {@link Component} annotation will
   * register beans with given {@link Component} metadata.
   * <p>
   *
   * @param clazz bean class
   * @param supplier bean instance supplier
   * @param prototype register as prototype?
   * @param ignoreAnnotation ignore {@link Component} scanning
   * @throws BeanDefinitionStoreException If BeanDefinition could not be store
   * @see #enableConditionEvaluation
   * @since 4.0
   */
  @Override
  public <T> void registerBean(
          Class<T> clazz, @Nullable Supplier<T> supplier, boolean prototype, boolean ignoreAnnotation)
          throws BeanDefinitionStoreException //
  {
    Assert.notNull(clazz, "bean-class must not be null");
    if (!shouldSkip(clazz)) {
      AnnotatedBeanDefinition definition = new AnnotatedBeanDefinition(clazz);
      if (prototype) {
        definition.setScope(Scope.PROTOTYPE);
      }

      if (ignoreAnnotation && beanNamePopulator instanceof AnnotationBeanNamePopulator) {
        definition.setBeanName(BeanDefinitionBuilder.defaultBeanName(clazz));
      }
      else {
        beanNamePopulator.populateName(definition, this.registry);
      }

      definition.setInstanceSupplier(supplier);

      if (ignoreAnnotation) {
        register(definition);
      }
      else {
        doRegisterWithAnnotationMetadata(definition.getMetadata(), definition, null);
      }
    }
  }

  public static void applyAnnotationMetadata(AnnotatedBeanDefinition definition) {
    MergedAnnotations annotations;
    MethodMetadata factoryMethodMetadata = definition.getFactoryMethodMetadata();
    if (factoryMethodMetadata != null) {
      annotations = factoryMethodMetadata.getAnnotations();
    }
    else {
      AnnotationMetadata metadata = definition.getMetadata();
      annotations = metadata.getAnnotations();
    }
    applyAnnotationMetadata(annotations, definition);
  }

  public static void applyAnnotationMetadata(AnnotatedElement annotated, BeanDefinition definition) {
    MergedAnnotations annotations = MergedAnnotations.from(annotated);
    applyAnnotationMetadata(annotations, definition);
  }

  public static void applyAnnotationMetadata(MergedAnnotations annotations, BeanDefinition definition) {
    if (annotations.isPresent(Primary.class)) {
      definition.setPrimary(true);
    }

    MergedAnnotation<Lazy> lazyMergedAnnotation = annotations.get(Lazy.class);
    if (lazyMergedAnnotation.isPresent()) {
      definition.setLazyInit(lazyMergedAnnotation.getBooleanValue());
    }
    else if (definition instanceof AnnotatedBeanDefinition annotated) {
      AnnotationMetadata metadata = annotated.getMetadata();
      lazyMergedAnnotation = metadata.getAnnotation(Lazy.class);

      if (lazyMergedAnnotation.isPresent()) {
        definition.setLazyInit(lazyMergedAnnotation.getBooleanValue());
      }
    }

    MergedAnnotation<Role> roleMergedAnnotation = annotations.get(Role.class);
    if (roleMergedAnnotation.isPresent()) {
      definition.setRole(roleMergedAnnotation.getIntValue());
    }

    MergedAnnotation<DependsOn> dependsOn = annotations.get(DependsOn.class);
    if (dependsOn.isPresent()) {
      definition.setDependsOn(dependsOn.getStringValueArray());
    }

    MergedAnnotation<Description> description = annotations.get(Description.class);
    if (description.isPresent()) {
      definition.setDescription(description.getStringValue());
    }

    // DisableDependencyInjection
    if (annotations.isPresent(DisableDependencyInjection.class)) {
      definition.setEnableDependencyInjection(false);
    }
  }

  private void doRegisterWithAnnotationMetadata(
          AnnotationMetadata metadata, BeanDefinition definition,
          @Nullable BeanDefinitionCustomizer[] dynamicCustomizers) {
    MergedAnnotations annotations = metadata.getAnnotations();
    applyAnnotationMetadata(annotations, definition);

    // dynamic customize
    if (ObjectUtils.isNotEmpty(dynamicCustomizers)) {
      for (BeanDefinitionCustomizer dynamicCustomizer : dynamicCustomizers) {
        dynamicCustomizer.customize(definition);
      }
    }

    // static customize
    if (CollectionUtils.isNotEmpty(customizers)) {
      for (BeanDefinitionCustomizer customizer : customizers) {
        customizer.customize(definition);
      }
    }

    register(definition);
  }

  public boolean isEnableConditionEvaluation() {
    return enableConditionEvaluation;
  }

  /**
   * set the flag to enable condition-evaluation
   *
   * @param enableConditionEvaluation enableConditionEvaluation flag
   * @see ConditionEvaluator
   * @see Condition
   * @see Conditional
   */
  public void setEnableConditionEvaluation(boolean enableConditionEvaluation) {
    this.enableConditionEvaluation = enableConditionEvaluation;
  }

  public BeanDefinitionRegistry getRegistry() {
    return registry;
  }

  public ApplicationContext getContext() {
    return context;
  }

  public void setContext(ApplicationContext context) {
    this.context = context;
  }

  public void setRegistry(BeanDefinitionRegistry registry) {
    this.registry = registry;
  }

  public void setConditionEvaluator(@Nullable ConditionEvaluator conditionEvaluator) {
    this.conditionEvaluator = conditionEvaluator;
  }

  @Nullable
  public ConditionEvaluator getConditionEvaluator() {
    return conditionEvaluator;
  }

  // private

  /**
   * @param annotated should AnnotatedElement skip register to registry?
   */
  protected boolean shouldSkip(Class<?> annotated) {
    return enableConditionEvaluation && !conditionEvaluator().passCondition(annotated);
  }

  @NonNull
  protected BeanDefinitionRegistry obtainRegistry() {
    Assert.state(registry != null, "No registry");
    return registry;
  }

  @NonNull
  protected ApplicationContext obtainContext() {
    Assert.state(context != null, "No ApplicationContext");
    return context;
  }

  @NonNull
  protected ConditionEvaluator conditionEvaluator() {
    if (conditionEvaluator == null) {
      conditionEvaluator = new ConditionEvaluator(obtainContext(), obtainRegistry());
    }
    return conditionEvaluator;
  }

  public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
    Assert.notNull(scopeMetadataResolver, "ScopeMetadataResolver must not be null");
    this.scopeMetadataResolver = scopeMetadataResolver;
  }

  public ScopeMetadataResolver getScopeMetadataResolver() {
    return scopeMetadataResolver;
  }

  /**
   * Set the {@code Environment} to use when evaluating whether
   * {@link Conditional @Conditional}-annotated component classes should be registered.
   * <p>The default is a {@link StandardEnvironment}.
   *
   * @see #registerBean(Class, String, Class...)
   */
  public void setEnvironment(Environment environment) {
    this.conditionEvaluator = new ConditionEvaluator(environment, context, registry);
  }

  /**
   * Set the {@code BeanNamePopulator} to use for detected bean classes.
   * <p>The default is a {@link AnnotationBeanNamePopulator}.
   */
  public void setBeanNamePopulator(@Nullable BeanNamePopulator beanNamePopulator) {
    this.beanNamePopulator =
            (beanNamePopulator != null ? beanNamePopulator : AnnotationBeanNamePopulator.INSTANCE);
  }

}
