/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.beans.factory.SingletonBeanRegistry;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.ContextUtils;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.context.aware.ImportAware;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Assert;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Constant;
import cn.taketoday.core.TodayStrategies;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

import static cn.taketoday.core.Constant.VALUE;
import static cn.taketoday.core.annotation.AnnotationUtils.getAttributesArray;

/**
 * read bean-definition
 *
 * @author TODAY 2021/10/1 16:46
 * @since 4.0
 */
public class BeanDefinitionReader {

  private static final Logger log = LoggerFactory.getLogger(BeanDefinitionReader.class);

  private final ConfigurableApplicationContext context;

  private final BeanDefinitionRegistry registry;
  private final ConditionEvaluator conditionEvaluator;

  private final ArrayList<AnnotatedElement> componentScanned = new ArrayList<>();

  public BeanDefinitionReader(
          ConfigurableApplicationContext context, BeanDefinitionRegistry registry) {
    this.context = context;
    this.registry = registry;
    this.conditionEvaluator = new ConditionEvaluator(context, registry);
  }

  //---------------------------------------------------------------------
  // Implementation of BeanDefinitionLoader interface
  //---------------------------------------------------------------------

  public BeanDefinitionRegistry getRegistry() {
    return registry;
  }

  public void register(BeanDefinition def) {
    registry.registerBeanDefinition(def);
  }
  // import

  public void importBeans(Class<?>... beans) {
    Assert.notNull(beans, "Cannot import null beans");

    for (Class<?> bean : beans) {
      BeanDefinition def = BeanDefinitionBuilder.defaults(bean);
      importAnnotated(def);
      register(def);
      loadConfigurationBeans(def); // scan config bean
    }
  }

  public void importBeans(Set<BeanDefinition> defs) {

    for (BeanDefinition def : defs) {
      importAnnotated(def);
    }
  }

  public void importAnnotated(BeanDefinition annotated) {
    for (AnnotationAttributes attr : getAttributesArray(annotated, Import.class)) {
      for (Class<?> importClass : attr.getAttribute(VALUE, Class[].class)) {
        if (!registry.containsBeanDefinition(importClass, true)) {
          doImport(annotated, importClass);
        }
      }
    }
  }

  /**
   * Select import
   *
   * @param annotated
   *         Target {@link BeanDefinition}
   *
   * @since 2.1.7
   */
  protected void doImport(BeanDefinition annotated, Class<?> importClass) {
    log.debug("Importing: [{}]", importClass);

    BeanDefinition importDef = BeanDefinitionBuilder.defaults(importClass);
    importDef.setAttribute(ImportAware.ImportAnnotatedMetadata, annotated); // @since 3.0
    register(importDef);
    loadConfigurationBeans(importDef); // scan config bean
    if (ImportSelector.class.isAssignableFrom(importClass)) {
      String[] imports = createImporter(importDef, ImportSelector.class).selectImports(annotated);
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
              .registerBeanDefinitions(annotated, registry);
    }
    if (ApplicationListener.class.isAssignableFrom(importClass)) {
      context.addApplicationListener(createImporter(importDef, ApplicationListener.class));
    }
  }

  /**
   * Resolve bean from a class which annotated with @{@link Configuration}
   */
  public void loadConfigurationBeans() {
    log.debug("Loading Configuration Beans");

    for (Map.Entry<String, BeanDefinition> entry : registry.getBeanDefinitions().entrySet()) {
      if (entry.getValue().isAnnotationPresent(Configuration.class)) {
        // @Configuration bean
        loadConfigurationBeans(entry.getValue());
      }
    }
  }

  /**
   * Load {@link Configuration} beans from input bean class
   *
   * @param declaringDef
   *         current {@link Configuration} bean
   *
   * @since 2.1.7
   */
  protected void loadConfigurationBeans(BeanDefinition declaringDef) {
    for (Method method : ReflectionUtils.getDeclaredMethods(declaringDef.getBeanClass())) {
      AnnotationAttributes[] components = getAttributesArray(method, Component.class);
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
          builder.build(defaultBeanName, attributes, (attribute, definition) -> {
            // Missing BeanMetadata a flag to determine its a missed bean @since 3.0
            definition.setAttribute(MissingBeanMetadata, attribute);
            // register missed bean
            register(definition);
            // @since 3.0.5
            if (definition.isAnnotationPresent(Configuration.class)) {
              loadConfigurationBeans(definition);
            }
          });
        }
      } // is a Component
      else if (conditionEvaluator.passCondition(method)) { // pass the condition
        registerConfigurationBean(declaringDef, method, components);
      }
    }
  }

  /**
   * Create {@link Configuration} bean definition, and register it
   *
   * @param method
   *         factory method
   * @param components
   *         {@link AnnotationAttributes}
   */
  protected void registerConfigurationBean(
          BeanDefinition declaringDef, Method method, AnnotationAttributes[] components
  ) {
    String defaultBeanName = method.getName(); // @since v2.1.7
    String declaringBeanName = declaringDef.getName(); // @since v2.1.7

    BeanDefinitionBuilder builder = new BeanDefinitionBuilder(context);
    builder.factoryMethod(method);
    builder.declaringName(declaringBeanName);
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
   * @param candidates
   *         candidate class set
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
    def.setAttribute(MissingBeanMetadata, missingBean);
    // register missed bean
    register(def);
  }

  /**
   * Is a context missed bean?
   *
   * @param missingBean
   *         The {@link Annotation} declared on the class or a method
   * @param annotated
   *         Missed bean class or method
   *
   * @return If the bean is missed in context
   *
   * @since 3.0
   */
  private boolean isMissedBean(AnnotationAttributes missingBean, AnnotatedElement annotated) {
    if (missingBean != null && conditionEvaluator.passCondition(annotated)) {
      // find by bean name
      String beanName = missingBean.getString(VALUE);
      if (StringUtils.isNotEmpty(beanName) && registry.containsBeanDefinition(beanName)) {
        return false;
      }
      // find by type
      Class<?> type = missingBean.getClass("type");
      if (type != void.class) {
        return !registry.containsBeanDefinition(type, missingBean.getBoolean("equals"));
      }
      else {
        return !registry.containsBeanDefinition(PropsReader.getBeanClass(annotated));
      }
    }
    return false;
  }

  /**
   * Register {@link MissingBean}
   *
   * @param missingBean
   *         {@link MissingBean} metadata
   * @param def
   *         Target {@link BeanDefinition}
   */

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
        if (conditionEvaluator.passCondition(beanClass)) {
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
   * @param target
   *         Must be {@link ImportSelector} ,or {@link BeanDefinitionImporter}
   *
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
   * @param type
   *         type
   *
   * @return bean name
   *
   * @see ClassUtils#getShortName(Class)
   */
  protected String createBeanName(Class<?> type) {
    return ClassUtils.getShortName(type);
  }

  /**
   * register a bean with the given bean class
   *
   * @since 3.0
   */
  public void registerBean(Class<?> clazz) {
    registerBean(createBeanName(clazz), clazz);
  }

  public void registerBean(Set<Class<?>> candidates) {
    for (Class<?> candidate : candidates) {
      registerBean(createBeanName(candidate), candidate);
    }
  }

  public BeanDefinition registerBean(String name, Class<?> clazz) {
    return getRegistered(name, clazz, null);
  }

  public BeanDefinition registerBean(String name, BeanDefinition beanDefinition) {
    return register(name, beanDefinition);
  }

  /**
   * Register a bean with the bean instance
   * <p>
   *
   * @param obj
   *         bean instance
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   */
  public void registerBean(Object obj) {
    registerBean(createBeanName(obj.getClass()), obj);
  }

  /**
   * Register a bean with the given name and bean instance
   *
   * @param name
   *         bean name (must not be null)
   * @param obj
   *         bean instance (must not be null)
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   */
  public void registerBean(String name, Object obj) {
    Assert.notNull(name, "bean-name must not be null");
    Assert.notNull(obj, "bean-instance must not be null");
    SingletonBeanRegistry singletonRegistry = context.unwrap(SingletonBeanRegistry.class);
    List<BeanDefinition> loaded = load(name, obj.getClass());
    for (BeanDefinition def : loaded) {
      if (def.isSingleton()) {
        singletonRegistry.registerSingleton(name, obj);
      }
    }
  }

  /**
   * Register a bean with the given type and instance supplier
   * <p>
   * default register as singleton
   * </p>
   *
   * @param clazz
   *         bean class
   * @param supplier
   *         bean instance supplier
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   * @since 4.0
   */
  public <T> void registerBean(Class<T> clazz, Supplier<T> supplier) throws BeanDefinitionStoreException {
    registerBean(clazz, supplier, false);
  }

  /**
   * Register a bean with the given type and instance supplier
   *
   * @param clazz
   *         bean class
   * @param supplier
   *         bean instance supplier
   * @param prototype
   *         register as prototype?
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   * @since 4.0
   */
  public <T> void registerBean(
          Class<T> clazz, Supplier<T> supplier, boolean prototype) throws BeanDefinitionStoreException {
    registerBean(clazz, supplier, prototype, true);
  }

  /**
   * Register a bean with the given type and instance supplier
   *
   * @param clazz
   *         bean class
   * @param supplier
   *         bean instance supplier
   * @param prototype
   *         register as prototype?
   * @param ignoreAnnotation
   *         ignore {@link Component} scanning
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   * @since 4.0
   */
  public <T> void registerBean(
          Class<T> clazz, Supplier<T> supplier, boolean prototype, boolean ignoreAnnotation)
          throws BeanDefinitionStoreException //
  {
    Assert.notNull(clazz, "bean-class must not be null");
    Assert.notNull(supplier, "bean-instance-supplier must not be null");
    String defaultName = createBeanName(clazz);
    List<BeanDefinition> loaded = load(defaultName, clazz, ignoreAnnotation);

    if (conditionEvaluator.passCondition()) {

    }

    if (CollectionUtils.isNotEmpty(loaded)) {
      for (BeanDefinition def : loaded) {
        def.setSupplier(supplier);
        if (prototype) {
          def.setScope(Scope.PROTOTYPE);
        }
      }
    }
  }

  /**
   * Register a bean with the given bean name and instance supplier
   *
   * <p>
   * register as singleton or prototype defined in your supplier
   * </p>
   *
   * @param name
   *         bean name
   * @param supplier
   *         bean instance supplier
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   * @since 4.0
   */
  public <T> void registerBean(String name, Supplier<T> supplier) throws BeanDefinitionStoreException {

  }

  //

}
