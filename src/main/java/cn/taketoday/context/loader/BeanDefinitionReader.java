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

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.beans.Lazy;
import cn.taketoday.beans.Primary;
import cn.taketoday.beans.factory.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionCustomizer;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.DefaultAnnotatedBeanDefinition;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.SingletonBeanRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotationScopeMetadataResolver;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * read bean-definition
 *
 * @author TODAY 2021/10/1 16:46
 * @since 4.0
 */
public class BeanDefinitionReader implements BeanDefinitionRegistrar {

  private BeanDefinitionRegistry registry;

  @Nullable
  private ConditionEvaluator conditionEvaluator;

  private ApplicationContext context;

  @Nullable
  private List<BeanDefinitionCustomizer> customizers;

  /**
   * enable
   */
  private boolean enableConditionEvaluation = true;

  private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

  public BeanDefinitionReader() { }

  public BeanDefinitionReader(ApplicationContext context) {
    this.context = context;
  }

  public BeanDefinitionReader(BeanDefinitionRegistry registry) {
    this.registry = registry;
  }

  public BeanDefinitionReader(
          ApplicationContext context, BeanDefinitionRegistry registry) {
    this.context = context;
    this.registry = registry;
  }

  //---------------------------------------------------------------------
  // Implementation of BeanDefinitionRegistrar interface
  //---------------------------------------------------------------------

  /**
   * Register a bean with the given bean name and instance supplier
   *
   * <p>
   * register as singleton or prototype defined in your supplier
   * </p>
   *
   * @param name bean name
   * @param supplier bean instance supplier
   * @throws BeanDefinitionStoreException If can't store a bean
   * @since 4.0
   */
  public <T> void registerBean(String name, Supplier<T> supplier) throws BeanDefinitionStoreException {
    BeanDefinition definition = new BeanDefinition(name, (Class<?>) null);
    definition.setInstanceSupplier(supplier);
    definition.setSynthetic(true);
    register(definition);
  }

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
                 (a, bd) -> bd.setConstructorArgs(constructorArgs));
  }

  @Override
  public <T> void registerBean(
          @Nullable String beanName, Class<T> beanClass,
          @Nullable Supplier<T> supplier, @Nullable BeanDefinitionCustomizer... customizers) {
    Assert.notNull(beanClass, "bean-class must not be null");

    if (shouldSkip(beanClass)) {
      return;
    }

    DefaultAnnotatedBeanDefinition definition = new DefaultAnnotatedBeanDefinition(beanClass);
    ScopeMetadata scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(definition);
    definition.setScope(scopeMetadata.getScopeName());

    String defaultName = createBeanName(beanClass);
    definition.setInstanceSupplier(supplier);
    definition.setName(defaultName);

    doRegisterWithAnnotationMetadata(definition.getMetadata(), definition, customizers);
  }

  // BeanDefinitionRegistrar end

  public void register(BeanDefinition def) {
    obtainRegistry().registerBeanDefinition(def);
  }

  //

  /**
   * default is use {@link ClassUtils#getShortName(Class)}
   *
   * <p>
   * sub-classes can overriding this method to provide a strategy to create bean name
   * </p>
   *
   * @param type type
   * @return bean name
   * @see ClassUtils#getShortName(Class)
   */
  protected String createBeanName(Class<?> type) {
    return BeanDefinitionBuilder.defaultBeanName(type);
  }

  //---------------------------------------------------------------------
  // register name -> Class
  //---------------------------------------------------------------------

  /**
   * Register a bean with the bean instance
   * <p>
   * just register to {@code SingletonBeanRegistry}
   *
   * @param obj bean instance
   */
  @Override
  public void registerSingleton(Object obj) {
    registerSingleton(createBeanName(obj.getClass()), obj);
  }

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
  public void registerBean(Object obj) {
    registerBean(createBeanName(obj.getClass()), obj);
  }

  /**
   * Register a bean with the given name and bean instance
   *
   * @param name bean name (must not be null)
   * @param obj bean instance (must not be null)
   * @throws BeanDefinitionStoreException If can't store a bean
   */
  @Override
  public void registerBean(String name, Object obj) {
    Assert.notNull(name, "bean-name must not be null");
    Assert.notNull(obj, "bean-instance must not be null");
    SingletonBeanRegistry singletonRegistry = context.unwrapFactory(SingletonBeanRegistry.class);

    BeanDefinition definition = new BeanDefinition(name, obj.getClass());
    definition.setSynthetic(true);
    definition.setInitialized(true);
    register(definition);

    singletonRegistry.registerSingleton(name, obj);
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
      DefaultAnnotatedBeanDefinition definition = new DefaultAnnotatedBeanDefinition(clazz);
      ScopeMetadata scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(definition);
      definition.setScope(scopeMetadata.getScopeName());

      String defaultName = createBeanName(clazz);
      definition.setInstanceSupplier(supplier);
      definition.setName(defaultName);

      if (ignoreAnnotation) {
        register(definition);
      }
      else {
        doRegisterWithAnnotationMetadata(definition.getMetadata(), definition, null);
      }
    }
  }

  public static void applyAnnotationMetadata(AnnotatedBeanDefinition definition) {
    AnnotationMetadata metadata = definition.getMetadata();
    MergedAnnotations annotations = metadata.getAnnotations();
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
      definition.setLazyInit(lazyMergedAnnotation.getBoolean(MergedAnnotation.VALUE));
    }

    MergedAnnotation<Role> roleMergedAnnotation = annotations.get(Role.class);
    if (roleMergedAnnotation.isPresent()) {
      definition.setRole(roleMergedAnnotation.getInt(MergedAnnotation.VALUE));
    }
  }

  private void doRegisterWithAnnotationMetadata(
          AnnotationMetadata metadata, BeanDefinition definition, @Nullable BeanDefinitionCustomizer[] dynamicCustomizers) {
    MergedAnnotations annotations = metadata.getAnnotations();
    applyAnnotationMetadata(annotations, definition);

    AnnotationAttributes attributes = null;
    MergedAnnotation<Component> annotation = annotations.get(Component.class);
    if (annotation.isPresent()) {
      attributes = annotation.asAnnotationAttributes();
    }

    // dynamic customize
    if (ObjectUtils.isNotEmpty(dynamicCustomizers)) {
      for (BeanDefinitionCustomizer dynamicCustomizer : dynamicCustomizers) {
        dynamicCustomizer.customize(attributes, definition);
      }
    }

    // static customize
    if (CollectionUtils.isNotEmpty(customizers)) {
      for (BeanDefinitionCustomizer customizer : customizers) {
        customizer.customize(attributes, definition);
      }
    }

    if (CollectionUtils.isNotEmpty(attributes)) {
      // compute bean names, maybe register multiple beans
      String[] candidateNames = annotation.getStringArray(MergedAnnotation.VALUE);
      String[] realNames = BeanDefinitionBuilder.determineName(definition.getName(), candidateNames);
      if (realNames.length == 1) {
        register(definition);
      }
      else {
        for (String name : realNames) {
          BeanDefinition clone = definition.cloneDefinition();
          clone.setName(name);
          register(clone);
        }
      }
    }
    else {
      register(definition);
    }
  }

  @Nullable
  public List<BeanDefinitionCustomizer> getCustomizers() {
    return customizers;
  }

  public void addCustomizers(@Nullable BeanDefinitionCustomizer... customizers) {
    if (ObjectUtils.isNotEmpty(customizers)) {
      CollectionUtils.addAll(customizers(), customizers);
    }
  }

  public void addCustomizers(@Nullable List<BeanDefinitionCustomizer> customizers) {
    if (CollectionUtils.isNotEmpty(customizers)) {
      CollectionUtils.addAll(customizers(), customizers);
    }
  }

  /**
   * clear exist customizers and set
   *
   * @param customizers new customizers
   */
  public void setCustomizers(@Nullable BeanDefinitionCustomizer... customizers) {
    if (ObjectUtils.isNotEmpty(customizers)) {
      CollectionUtils.addAll(customizers(), customizers);
    }
    else {
      // clear
      if (this.customizers != null) {
        this.customizers.clear();
      }
    }
  }

  /**
   * set customizers
   *
   * @param customizers new customizers
   */
  public void setCustomizers(@Nullable List<BeanDefinitionCustomizer> customizers) {
    this.customizers = customizers;
  }

  public boolean isEnableConditionEvaluation() {
    return enableConditionEvaluation;
  }

  /**
   * set the flag to enable condition-evaluation
   *
   * @param enableConditionEvaluation enableConditionEvaluation flag
   * @see ConditionEvaluator
   * @see cn.taketoday.context.Condition
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
  private List<BeanDefinitionCustomizer> customizers() {
    if (customizers == null) {
      customizers = new ArrayList<>();
    }
    return customizers;
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

}
