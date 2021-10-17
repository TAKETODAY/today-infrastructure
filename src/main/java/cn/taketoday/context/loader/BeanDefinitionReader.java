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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.taketoday.beans.Lazy;
import cn.taketoday.beans.Primary;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionCustomizer;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.DefaultBeanDefinition;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.beans.factory.SingletonBeanRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ContextUtils;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.aware.ImportAware;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Configuration;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * read bean-definition
 *
 * @author TODAY 2021/10/1 16:46
 * @since 4.0
 */
public class BeanDefinitionReader {
  private static final Logger log = LoggerFactory.getLogger(BeanDefinitionReader.class);

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
  // Implementation of BeanDefinitionLoader interface
  //---------------------------------------------------------------------

  public void register(BeanDefinition def) {
    obtainRegistry().registerBeanDefinition(def);
  }

  // import

  public void register(Class<?>... beans) {

    Assert.notNull(beans, "Cannot import null beans");

    BeanDefinitionBuilder builder = new BeanDefinitionBuilder(context);
    for (Class<?> bean : beans) {
      String beanName = createBeanName(bean);
      builder.beanClass(bean)
              .name(beanName);

      BeanDefinition def =  builder.build();
      importAnnotated(def);
      register(def);
      loadConfigurationBeans(def); // scan config bean
    }
  }

  public void importAnnotated(BeanDefinition annotated) {
    BeanDefinitionRegistry registry = obtainRegistry();
    for (AnnotationAttributes attr : AnnotationUtils.getAttributesArray(annotated, Import.class)) {
      for (Class<?> importClass : attr.getAttribute(Constant.VALUE, Class[].class)) {
        if (!registry.containsBeanDefinition(importClass, true)) {
          doImport(annotated, importClass);
        }
      }
    }
  }

  /**
   * Select import
   *
   * @param annotated Target {@link BeanDefinition}
   * @since 2.1.7
   */
  protected void doImport(BeanDefinition annotated, Class<?> importClass) {
    log.debug("Importing: [{}]", importClass);

    BeanDefinition importDef = BeanDefinitionBuilder.defaults(importClass);
    importDef.setAttribute(ImportAware.ImportAnnotatedMetadata, annotated); // @since 3.0
    register(importDef);
    loadConfigurationBeans(importDef); // scan config bean
    if (ImportSelector.class.isAssignableFrom(importClass)) {
      String[] imports = createImporter(importDef, ImportSelector.class).selectImports(annotated, obtainRegistry());
      if (ObjectUtils.isNotEmpty(imports)) {
        for (String select : imports) {
          Class<Object> beanClass = ClassUtils.load(select);
          if (beanClass == null) {
            throw new ConfigurationException("Bean class not in class-path: " + select);
          }
          register(BeanDefinitionBuilder.defaults(beanClass));
        }
      }
    }
    if (BeanDefinitionImporter.class.isAssignableFrom(importClass)) {
      createImporter(importDef, BeanDefinitionImporter.class)
              .registerBeanDefinitions(annotated, obtainRegistry());
    }
    if (ApplicationListener.class.isAssignableFrom(importClass)) {
      context.addApplicationListener(createImporter(importDef, ApplicationListener.class));
    }
  }

  /**
   * Load {@link Configuration} beans from input bean class
   *
   * @param declaringDef current {@link Configuration} bean
   * @since 2.1.7
   */
  protected void loadConfigurationBeans(BeanDefinition declaringDef) {
    for (Method method : ReflectionUtils.getDeclaredMethods(declaringDef.getBeanClass())) {
      AnnotationAttributes[] components = AnnotationUtils.getAttributesArray(method, Component.class);
      if (ObjectUtils.isEmpty(components)) {
        // detect missed bean
        AnnotationAttributes attributes = AnnotationUtils.getAttributes(MissingBean.class, method);
        if (isMissedBean(attributes, method)) {
          // register directly @since 3.0

          String defaultBeanName = method.getName();
          String declaringBeanName = createBeanName(method.getDeclaringClass()); // @since v2.1.7

          BeanDefinitionBuilder builder = new BeanDefinitionBuilder(context);
          builder.factoryMethod(method);
          builder.declaringName(declaringBeanName);
          builder.beanClass(method.getReturnType());

          builder.build(defaultBeanName, attributes, (attribute, definition) -> {
            // Missing BeanMetadata a flag to determine its a missed bean @since 3.0
            definition.setAttribute(MissingBean.MissingBeanMetadata, attribute);
            // register missed bean
            register(definition);
            // @since 3.0.5
            if (definition.isAnnotationPresent(Configuration.class)) {
              loadConfigurationBeans(definition);
            }
          });
        }
      } // is a Component
      else if (conditionEvaluator().passCondition(method)) { // pass the condition
        registerConfigurationBean(declaringDef, method, components);
      }
    }
  }

  /**
   * Create {@link Configuration} bean definition, and register it
   *
   * @param method factory method
   * @param components {@link AnnotationAttributes}
   */
  protected void registerConfigurationBean(
          BeanDefinition declaringDef, Method method, AnnotationAttributes[] components
  ) {
    String defaultBeanName = method.getName(); // @since v2.1.7
    String declaringBeanName = declaringDef.getName(); // @since v2.1.7

    BeanDefinitionBuilder builder = new BeanDefinitionBuilder(context);
    builder.factoryMethod(method);
    builder.declaringName(declaringBeanName);
    builder.beanClass(method.getReturnType());
    builder.build(defaultBeanName, components, (component, definition) -> {
      register(definition);
      if (definition.isAnnotationPresent(Configuration.class)) {
        loadConfigurationBeans(definition);
      }
    });
  }

  /**
   * Load missing beans, default beans
   *
   * @param candidates candidate class set
   */
  public void loadMissingBean(Collection<Class<?>> candidates) {
    log.debug("Loading lost beans");

    BeanDefinitionBuilder builder = new BeanDefinitionBuilder(context);
    for (Class<?> beanClass : candidates) {
      AnnotationAttributes attributes = AnnotationUtils.getAttributes(MissingBean.class, beanClass);
      if (isMissedBean(attributes, beanClass)) {
        String beanName = createBeanName(beanClass);
        builder.build(beanName, attributes, this::registerMissing);
      }
    }
  }

  protected void registerMissing(AnnotationAttributes missingBean, BeanDefinition def) {
    // Missing BeanMetadata a flag to determine its a missed bean @since 3.0
    def.setAttribute(MissingBean.MissingBeanMetadata, missingBean);
    // register missed bean
    register(def);
  }

  /**
   * Is a context missed bean?
   *
   * @param missingBean The {@link Annotation} declared on the class or a method
   * @param annotated Missed bean class or method
   * @return If the bean is missed in context
   * @since 3.0
   */
  private boolean isMissedBean(AnnotationAttributes missingBean, AnnotatedElement annotated) {
    if (missingBean != null && conditionEvaluator().passCondition(annotated)) {
      // find by bean name
      String beanName = missingBean.getString(Constant.VALUE);
      if (StringUtils.isNotEmpty(beanName) && obtainRegistry().containsBeanDefinition(beanName)) {
        return false;
      }
      // find by type
      Class<?> type = missingBean.getClass("type");
      if (type != void.class) {
        return !obtainRegistry().containsBeanDefinition(type, missingBean.getBoolean("equals"));
      }
      else {
        return !obtainRegistry().containsBeanDefinition(PropsReader.getBeanClass(annotated));
      }
    }
    return false;
  }

  /**
   * Resolve bean from META-INF/beans
   *
   * @see Constant#META_INFO_beans
   * @since 2.1.6
   */
  public Set<Class<?>> loadMetaInfoBeans() {
    log.debug("Loading META-INF/beans");

    // Load the META-INF/beans @since 2.1.6
    // ---------------------------------------------------
    Set<Class<?>> beans = ContextUtils.loadFromMetaInfo(Constant.META_INFO_beans);
    // @since 4.0 load from StrategiesLoader strategy file
    beans.addAll(TodayStrategies.getDetector().getTypes(MissingBean.class));

    BeanDefinitionBuilder builder = new BeanDefinitionBuilder(context);

    for (Class<?> beanClass : beans) {
      AnnotationAttributes missingBean = AnnotationUtils.getAttributes(MissingBean.class, beanClass);
      if (missingBean != null) {
        if (isMissedBean(missingBean, beanClass)) {
          // MissingBean in 'META-INF/beans' @since 3.0
          String name = createBeanName(beanClass);
          builder.build(name, missingBean, this::registerMissing);
        }
        else {
          log.info("@MissingBean -> '{}' cannot pass the condition " +
                           "or contains its bean definition, dont register to the map", beanClass);
        }
      }
      else {
        if (conditionEvaluator().passCondition(beanClass)) {
          // can't be a missed bean. MissingBean load after normal loading beans
          List<BeanDefinition> defs = BeanDefinitionBuilder.from(beanClass);
          for (BeanDefinition def : defs) {
            register(def);
          }
        }
      }

    }
    return beans;
  }

  /**
   * Create {@link ImportSelector} ,or {@link BeanDefinitionImporter},
   * {@link ApplicationListener} object
   *
   * @param target Must be {@link ImportSelector} ,or {@link BeanDefinitionImporter}
   * @return {@link ImportSelector} object
   */
  protected <T> T createImporter(BeanDefinition importDef, Class<T> target) {
    try {
      Object bean = context.getBean(importDef);
      if (bean instanceof ImportAware) {
        ((ImportAware) bean).setImportBeanDefinition(importDef);
      }
      return target.cast(bean);
    }
    catch (Throwable e) {
      throw new BeanDefinitionStoreException("Can't initialize a target: [" + importDef + "]");
    }
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

  public BeanDefinition registerBean(String name, BeanDefinition beanDefinition) {
    return register(name, beanDefinition);
  }

  //---------------------------------------------------------------------
  // register name -> Class
  //---------------------------------------------------------------------

  /**
   * register a bean with the given bean class
   *
   * @see #enableConditionEvaluation
   * @since 3.0
   */
  public void registerBean(Class<?> clazz) {
    if (!shouldSkip(clazz)) {
      registerBean(createBeanName(clazz), clazz);
    }
  }

  /**
   * @see #enableConditionEvaluation
   */
  public void registerBean(Class<?>... candidates) {
    for (Class<?> candidate : candidates) {
      registerBean(candidate);
    }
  }

  /**
   * @see #enableConditionEvaluation
   */
  public BeanDefinition registerBean(String name, Class<?> clazz) {
    return getRegistered(name, clazz, null);
  }

  //---------------------------------------------------------------------
  // register name -> object singleton
  //---------------------------------------------------------------------

  /**
   * Register a bean with the bean instance
   * <p>
   *
   * @param obj bean instance
   * @throws BeanDefinitionStoreException If can't store a bean
   */
  public void registerBean(Object obj) {
    registerBean(createBeanName(obj.getClass()), obj);
  }

  public void registerBean(Set<Class<?>> candidates) {
    for (Class<?> candidate : candidates) {
      registerBean(candidate);
    }
  }

  /**
   * Register a bean with the given name and bean instance
   *
   * @param name bean name (must not be null)
   * @param obj bean instance (must not be null)
   * @throws BeanDefinitionStoreException If can't store a bean
   */
  public void registerBean(String name, Object obj) {
    Assert.notNull(name, "bean-name must not be null");
    Assert.notNull(obj, "bean-instance must not be null");
    SingletonBeanRegistry singletonRegistry = context.unwrapFactory(SingletonBeanRegistry.class);
    List<BeanDefinition> loaded = load(name, obj.getClass());
    for (BeanDefinition def : loaded) {
      if (def.isSingleton()) {
        singletonRegistry.registerSingleton(name, obj);
      }
    }
  }

  //---------------------------------------------------------------------
  // register Class -> Supplier
  //---------------------------------------------------------------------

  /**
   * Register a bean with the given type and instance supplier
   * <p>
   * default register as singleton
   * </p>
   *
   * @param clazz bean class
   * @param supplier bean instance supplier
   * @throws BeanDefinitionStoreException If can't store a bean
   * @see #enableConditionEvaluation
   * @since 4.0
   */
  public <T> void registerBean(Class<T> clazz, Supplier<T> supplier) throws BeanDefinitionStoreException {
    registerBean(clazz, supplier, false);
  }

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
  public <T> void registerBean(
          Class<T> clazz, Supplier<T> supplier, boolean prototype, boolean ignoreAnnotation)
          throws BeanDefinitionStoreException //
  {
    Assert.notNull(clazz, "bean-class must not be null");
    Assert.notNull(supplier, "bean-instance-supplier must not be null");

    if (shouldSkip(clazz)) {
      return;
    }

    String defaultName = createBeanName(clazz);
    BeanDefinitionBuilder builder = new BeanDefinitionBuilder(context);
    builder.instanceSupplier(supplier);

    if (prototype) {
      builder.prototype();
    }
    if (AnnotationUtils.isPresent(clazz, Primary.class)) {
      builder.primary(true);
    }

    AnnotationAttributes lazy = AnnotationUtils.getAttributes(Lazy.class, clazz);
    if (lazy != null) {
      builder.lazyInit(lazy.getBoolean(Constant.VALUE));
    }

    AnnotationAttributes role = AnnotationUtils.getAttributes(Role.class, clazz);
    if (role != null) {
      builder.role(role.getNumber(Constant.VALUE));
    }

    if (ignoreAnnotation) {
      BeanDefinition definition = builder.build();
      register(definition);
    }
    else {
      builder.build(defaultName, clazz, this::doRegister);
    }
  }

  private void doRegister(@Nullable AnnotationAttributes attributes, BeanDefinition definition) {
    if (CollectionUtils.isNotEmpty(customizers)) {
      for (BeanDefinitionCustomizer customizer : customizers) {
        customizer.customize(attributes, definition);
      }
    }
    register(definition);
  }

  //---------------------------------------------------------------------
  // register name -> Supplier
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
    DefaultBeanDefinition definition = new DefaultBeanDefinition(name, (Class<?>) null);
    definition.setSupplier(supplier);
    definition.setSynthetic(true);
    register(definition);
  }

  /**
   * Load bean definition with given bean class and bean name.
   * <p>
   * If the provided bean class annotated {@link Component} annotation will
   * register beans with given {@link Component} metadata.
   * <p>
   * Otherwise register a bean will given default metadata: use the default bean
   * name creator create the default bean name, use default bean scope
   * {@link Scope#SINGLETON} , empty initialize method ,empty property value and
   * empty destroy method.
   *
   * @param name Bean name
   * @param beanClass Bean class
   * @return returns a new BeanDefinition
   * @throws BeanDefinitionStoreException If BeanDefinition could not be store
   * @since 4.0
   */
  public List<BeanDefinition> load(String name, Class<?> beanClass) {
    return Collections.singletonList(getRegistered(name, beanClass, null));
  }

  private BeanDefinition getRegistered(
          String name, Class<?> beanClass, @Nullable AnnotationAttributes attributes) {
    BeanDefinition newDef = BeanDefinitionBuilder.defaults(name, beanClass, attributes);
    return register(name, newDef);
  }

  private BeanDefinition register(String name, BeanDefinition newDef) {
    obtainRegistry().registerBeanDefinition(name, newDef);
    return newDef;
  }

  public List<BeanDefinition> register(Class<?> candidate) {
    ArrayList<BeanDefinition> defs = new ArrayList<>();
    doRegister(candidate, defs::add);
    return defs;
  }

  private void doRegister(Class<?> candidate, Consumer<BeanDefinition> registeredConsumer) {
    AnnotationAttributes[] annotationAttributes = AnnotationUtils.getAttributesArray(candidate, Component.class);
    if (ObjectUtils.isNotEmpty(annotationAttributes)) {
      String defaultBeanName = createBeanName(candidate);
      for (AnnotationAttributes attributes : annotationAttributes) {
        doRegister(candidate, defaultBeanName, attributes, registeredConsumer);
      }
    }
  }

  private void doRegister(
          Class<?> candidate, String defaultBeanName,
          AnnotationAttributes attributes, Consumer<BeanDefinition> registeredConsumer) {
    for (String name : BeanDefinitionBuilder.determineName(
            defaultBeanName, attributes.getStringArray(Constant.VALUE))) {
      BeanDefinition registered = getRegistered(name, candidate, attributes);
      if (registered != null && registeredConsumer != null) { // none null BeanDefinition
        registeredConsumer.accept(registered);
      }
    }
  }

  //

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
  protected boolean shouldSkip(AnnotatedElement annotated) {
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

}
