/**
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
package cn.taketoday.context.factory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.aware.ImportAware;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.event.LoadingMissingBeanEvent;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.loader.AutowiredPropertyResolver;
import cn.taketoday.context.loader.BeanDefinitionImporter;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.loader.CandidateComponentScanner;
import cn.taketoday.context.loader.ImportSelector;
import cn.taketoday.context.loader.ObjectSupplierPropertyResolver;
import cn.taketoday.context.loader.PropertyValueResolver;
import cn.taketoday.context.loader.PropsPropertyResolver;
import cn.taketoday.context.loader.ValuePropertyResolver;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.ReflectionUtils;
import cn.taketoday.context.utils.StringUtils;

import static cn.taketoday.context.Constant.VALUE;
import static cn.taketoday.context.exception.ConfigurationException.nonNull;
import static cn.taketoday.context.utils.ClassUtils.getAnnotationAttributes;
import static cn.taketoday.context.utils.ClassUtils.getAnnotationAttributesArray;
import static cn.taketoday.context.utils.ContextUtils.conditional;
import static cn.taketoday.context.utils.ContextUtils.findNames;
import static cn.taketoday.context.utils.ContextUtils.resolveInitMethod;
import static cn.taketoday.context.utils.ContextUtils.resolveProps;
import static cn.taketoday.context.utils.ReflectionUtils.makeAccessible;
import static java.util.Objects.requireNonNull;

/**
 * Standard {@link BeanFactory} implementation
 *
 * @author TODAY <br>
 * 2019-03-23 15:00
 */
public class StandardBeanFactory
        extends AbstractBeanFactory implements ConfigurableBeanFactory, BeanDefinitionLoader {

  private static final Logger log = LoggerFactory.getLogger(StandardBeanFactory.class);

  private final ConfigurableApplicationContext applicationContext;
  private final HashSet<Method> missingMethods = new HashSet<>(32);
  private final LinkedList<AnnotatedElement> componentScanned = new LinkedList<>();

  /**
   * @since 3.0 Resolve {@link PropertyValue}
   */
  private final LinkedList<PropertyValueResolver> propertyResolvers = new LinkedList<>();

  /**
   * @since 2.1.7 Preventing repeated initialization of beans(Prevent duplicate
   * initialization) , Prevent Cycle Dependency
   */
  private final HashSet<String> currentInitializingBeanName = new HashSet<>();

  public StandardBeanFactory(ConfigurableApplicationContext applicationContext) {
    Assert.notNull(applicationContext, "applicationContext must not be null");
    this.applicationContext = applicationContext;
  }

  @Override
  protected void awareInternal(final Object bean, final BeanDefinition def) {
    super.awareInternal(bean, def);

    if (bean instanceof ApplicationContextAware) {
      ((ApplicationContextAware) bean).setApplicationContext(getApplicationContext());
    }

    if (bean instanceof EnvironmentAware) {
      ((EnvironmentAware) bean).setEnvironment(getApplicationContext().getEnvironment());
    }
  }

  @Override
  public Object initializeBean(final Object bean, final BeanDefinition def) {
    final String name = def.getName();
    if (currentInitializingBeanName.contains(name)) {
      return bean;
    }
    currentInitializingBeanName.add(name);
    final Object initializingBean = super.initializeBean(bean, def);
    currentInitializingBeanName.remove(name);
    return initializingBean;
  }

  // -----------------------------------------

  /**
   * Resolve bean from a class which annotated with @{@link Configuration}
   */
  public void loadConfigurationBeans() {
    log.debug("Loading Configuration Beans");

    for (final Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      if (entry.getValue().isAnnotationPresent(Configuration.class)) {
        // @Configuration bean
        loadConfigurationBeans(entry.getValue());
      }
    }
  }

  /**
   * Load {@link Configuration} beans from input bean class
   *
   * @param def
   *         current {@link Configuration} bean
   *
   * @since 2.1.7
   */
  protected void loadConfigurationBeans(final BeanDefinition def) {

    final Collection<Method> missingMethods = this.missingMethods;
    final ConfigurableApplicationContext context = getApplicationContext();

    for (final Method method : ReflectionUtils.getDeclaredMethods(def.getBeanClass())) {
      final AnnotationAttributes[] components = getAnnotationAttributesArray(method, Component.class);
      if (ObjectUtils.isEmpty(components)) {
        if (method.isAnnotationPresent(MissingBean.class) && conditional(method, context)) {
          missingMethods.add(method);
        }
      }
      else if (conditional(method, context)) { // pass the condition
        registerConfigurationBean(def, method, components);
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
          final BeanDefinition def, final Method method, final AnnotationAttributes[] components
  ) {
    final Class<?> returnType = method.getReturnType();

    final ConfigurableEnvironment environment = getApplicationContext().getEnvironment();
    final Properties properties = environment.getProperties();
    //final String defaultBeanName = beanNameCreator.create(returnType); // @Deprecated in v2.1.7, use method name instead
    final String defaultBeanName = method.getName(); // @since v2.1.7
    final String declaringBeanName = def.getName(); // @since v2.1.7

    for (final AnnotationAttributes component : components) {
      final String scope = component.getString(Constant.SCOPE);
      final String[] initMethods = component.getStringArray(Constant.INIT_METHODS);
      final String[] destroyMethods = component.getStringArray(Constant.DESTROY_METHODS);

      for (final String name : findNames(defaultBeanName, component.getStringArray(VALUE))) {

        // register
        final StandardBeanDefinition stdDef = new StandardBeanDefinition(name, returnType);

        stdDef.setScope(scope);
        stdDef.setDestroyMethods(destroyMethods);
        stdDef.setInitMethods(resolveInitMethod(initMethods, returnType));
        // fix Configuration bean shouldn't auto apply properties
        // def.setPropertyValues(ContextUtils.resolvePropertyValue(returnType));
        stdDef.setDeclaringName(declaringBeanName)
                .setFactoryMethod(method);
        // resolve @Props on a bean
        stdDef.addPropertyValue(resolveProps(stdDef, properties));
        register(name, stdDef);
      }
    }
  }

  /**
   * Load missing beans, default beans
   *
   * @param beanClasses
   *         Class set
   */
  public void loadMissingBean(final Collection<Class<?>> beanClasses) {
    log.debug("Loading lost beans");

    final ConfigurableApplicationContext context = getApplicationContext();
    context.publishEvent(new LoadingMissingBeanEvent(context, beanClasses));

    for (final Class<?> beanClass : beanClasses) {

      final MissingBean missingBean = beanClass.getAnnotation(MissingBean.class);

      if (ContextUtils.isMissedBean(missingBean, beanClass, this)) {
        registerMissingBean(missingBean, new DefaultBeanDefinition(getBeanName(missingBean, beanClass), beanClass));
      }
    }

    final BeanNameCreator beanNameCreator = getBeanNameCreator();

    for (final Method method : missingMethods) {
      final MissingBean missingBean = method.getAnnotation(MissingBean.class);

      if (ContextUtils.isMissedBean(missingBean, method, this)) {

        final Class<?> beanClass = method.getReturnType();
        StandardBeanDefinition beanDefinition = // @Configuration use default bean name
                new StandardBeanDefinition(getBeanName(missingBean, beanClass), beanClass)//
                        .setFactoryMethod(method)//
                        .setDeclaringName(beanNameCreator.create(method.getDeclaringClass()));

        if (method.isAnnotationPresent(Props.class)) {
          // @Props on method
          final List<PropertyValue> props = resolveProps(method, context.getEnvironment().getProperties());
          beanDefinition.addPropertyValue(props);
        }
        registerMissingBean(missingBean, beanDefinition);
      }
    }
    missingMethods.clear();
  }

  /**
   * Register {@link MissingBean}
   *
   * @param missingBean
   *         {@link MissingBean} metadata
   * @param def
   *         Target {@link BeanDefinition}
   */
  protected void registerMissingBean(final MissingBean missingBean, final BeanDefinition def) {
    final Class<?> beanClass = def.getBeanClass();

    def.setScope(missingBean.scope())
            .setDestroyMethods(missingBean.destroyMethods())
            .setInitMethods(resolveInitMethod(missingBean.initMethods(), beanClass))
            .setPropertyValues(resolvePropertyValue(beanClass));

    resolveProps(def, getApplicationContext().getEnvironment());

    // register missed bean
    register(def.getName(), def);
  }

  /**
   * Get bean name
   *
   * @param missingBean
   *         {@link MissingBean}
   * @param beanClass
   *         Bean class
   *
   * @return Bean name
   */
  protected String getBeanName(final MissingBean missingBean, final Class<?> beanClass) {
    String beanName = missingBean.value();
    if (StringUtils.isEmpty(beanName)) {
      beanName = getBeanNameCreator().create(beanClass);
    }
    return beanName;
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
    final Set<Class<?>> beans = ContextUtils.loadFromMetaInfo(Constant.META_INFO_beans);
    final BeanNameCreator beanNameCreator = getBeanNameCreator();
    for (final Class<?> beanClass : beans) {

      if (conditional(beanClass)
              && !ClassUtils.isAnnotationPresent(beanClass, MissingBean.class)) {
        // can't be a missed bean. MissingBean load after normal loading beans
        ContextUtils.createBeanDefinitions(beanNameCreator.create(beanClass), beanClass, this)
                .forEach(this::register);
      }
    }
    return beans;
  }

  @Override
  public void importBeans(final Class<?>... beans) {
    for (final Class<?> bean : requireNonNull(beans)) {
      final BeanDefinition def = createBeanDefinition(bean);
      importBeans(def);
      register(def);
      loadConfigurationBeans(def); // scan config bean
    }
  }

  @Override
  public void importBeans(final Set<BeanDefinition> defs) {

    for (final BeanDefinition def : defs) {
      importBeans(def);
    }
  }

  @Override
  public void importBeans(final BeanDefinition def) {

    for (final AnnotationAttributes attr : getAnnotationAttributesArray(def, Import.class)) {
      for (final Class<?> importClass : attr.getAttribute(VALUE, Class[].class)) {
        if (!containsBeanDefinition(importClass, true)) {
          selectImport(def, importClass);
        }
      }
    }
  }

  /**
   * Select import
   *
   * @since 2.1.7
   */
  protected void selectImport(final BeanDefinition def, final Class<?> importClass) {
    log.debug("Importing: [{}]", importClass);

    BeanDefinition importDef = createBeanDefinition(importClass);
    register(importDef);
    loadConfigurationBeans(importDef); // scan config bean
    if (ImportSelector.class.isAssignableFrom(importClass)) {
      for (final String select : createImporter(importDef, ImportSelector.class).selectImports(def)) {
        register(createBeanDefinition(ClassUtils.loadClass(select)));
      }
    }
    if (BeanDefinitionImporter.class.isAssignableFrom(importClass)) {
      createImporter(importDef, BeanDefinitionImporter.class).registerBeanDefinitions(def, this);
    }
    if (ApplicationListener.class.isAssignableFrom(importClass)) {
      getApplicationContext().addApplicationListener(createImporter(importDef, ApplicationListener.class));
    }
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
      final Object bean = getBean(importDef);
      if (bean instanceof ImportAware) {
        ((ImportAware) bean).setImportBeanDefinition(importDef);
      }
      return target.cast(bean);
    }
    catch (Throwable e) {
      throw new BeanDefinitionStoreException("Can't initialize a target: [" + importDef + "]");
    }
  }

  @Override
  protected BeanNameCreator createBeanNameCreator() {
    return getApplicationContext().getEnvironment().getBeanNameCreator();
  }

  @Override
  public final BeanDefinitionLoader getBeanDefinitionLoader() {
    return this;
  }

  // BeanDefinitionLoader @since 2.1.7
  // ---------------------------------------------

  @Override
  public void loadBeanDefinition(final Class<?> candidate) {
    // don't load abstract class
    if (!Modifier.isAbstract(candidate.getModifiers()) && conditional(candidate, applicationContext)) {
      register(candidate);
    }
  }

  @Override
  public void loadBeanDefinitions(final Collection<Class<?>> beans) {
    for (Class<?> clazz : beans) {
      loadBeanDefinition(clazz);
    }
  }

  @Override
  public void loadBeanDefinition(final String name, final Class<?> beanClass) {
    final AnnotationAttributes[] annotationAttributes = getAnnotationAttributesArray(beanClass, Component.class);

    if (ObjectUtils.isEmpty(annotationAttributes)) {
      register(name, createBeanDefinition(name, beanClass, null));
    }
    else {
      for (final AnnotationAttributes attributes : annotationAttributes) {
        register(name, createBeanDefinition(name, beanClass, attributes));
      }
    }
  }

  @Override
  public void loadBeanDefinition(final String... locations) {
    loadBeanDefinitions(new CandidateComponentScanner().scan(locations));
  }

  @Override
  public void register(final Class<?> candidate) {
    final AnnotationAttributes[] annotationAttributes = getAnnotationAttributesArray(candidate, Component.class);
    if (ObjectUtils.isNotEmpty(annotationAttributes)) {
      final String defaultBeanName = getBeanNameCreator().create(candidate);
      for (final AnnotationAttributes attributes : annotationAttributes) {
        for (final String name : findNames(defaultBeanName, attributes.getStringArray(VALUE))) {
          register(name, createBeanDefinition(name, candidate, attributes));
        }
      }
    }
  }

  /**
   * Register bean definition with given name
   *
   * @param name
   *         Bean name
   * @param def
   *         Bean definition
   *
   * @throws BeanDefinitionStoreException
   *         If can't store bean
   */
  @Override
  public void register(final String name, final BeanDefinition def) {
    ContextUtils.validateBeanDefinition(def);

    String nameToUse = name;
    final Class<?> beanClass = def.getBeanClass();

    try {
      if (containsBeanDefinition(name)) {
        final BeanDefinition existBeanDefinition = getBeanDefinition(name);
        Class<?> existClass = existBeanDefinition.getBeanClass();
        log.info("=====================|START|=====================");
        log.info("There is already a bean called: [{}], its bean Definition: [{}].", name, existBeanDefinition);

        if (beanClass.equals(existClass)) {
          log.warn("They have same bean class: [{}]. We will override it.", beanClass);
        }
        else {
          nameToUse = beanClass.getName();
          def.setName(nameToUse);
          log.warn("Current bean class: [{}]. You are supposed to change your bean name creator or bean name.", beanClass);
          log.warn("Current bean definition: [{}] will be registed as: [{}].", def, nameToUse);
        }
        log.info("======================|END|======================");
      }

      if (FactoryBean.class.isAssignableFrom(beanClass)) { // process FactoryBean
        registerFactoryBean(nameToUse, def);
      }
      else {
        registerBeanDefinition(nameToUse, def);
      }
      postProcessRegisterBeanDefinition(def);
    }
    catch (Throwable ex) {
      ex = ExceptionUtils.unwrapThrowable(ex);
      throw new BeanDefinitionStoreException(
              "An Exception Occurred When Register Bean Definition: [" + name + "]", ex);
    }
  }

  /**
   * Process after register {@link BeanDefinition}
   *
   * @param targetDef
   *         Target {@link BeanDefinition}
   */
  protected void postProcessRegisterBeanDefinition(final BeanDefinition targetDef) {
    if (targetDef.isAnnotationPresent(Import.class)) { // @since 2.1.7
      importBeans(targetDef);
    }
    if (targetDef.isAnnotationPresent(ComponentScan.class)) {
      componentScan(targetDef);
    }
    // application listener @since 2.1.7
    if (ApplicationListener.class.isAssignableFrom(targetDef.getBeanClass())) {
      Object listener = getSingleton(targetDef.getName());
      if (listener == null) {
        listener = createBeanIfNecessary(targetDef);
        applicationContext.addApplicationListener((ApplicationListener<?>) listener);
      }
    }
  }

  /**
   * Import beans from given package locations
   *
   * @param source
   *         {@link BeanDefinition} that annotated {@link ComponentScan}
   */
  protected void componentScan(final AnnotatedElement source) {
    if (!componentScanned.contains(source)) {
      componentScanned.add(source);
      for (final AnnotationAttributes attribute : getAnnotationAttributesArray(source, ComponentScan.class)) {
        loadBeanDefinition(attribute.getStringArray(VALUE));
      }
    }
  }

  /**
   * Register {@link FactoryBeanDefinition} to the {@link BeanFactory}
   *
   * @param oldBeanName
   *         Target old bean name
   * @param factoryDef
   *         {@link FactoryBean} Bean definition
   */
  protected void registerFactoryBean(final String oldBeanName, final BeanDefinition factoryDef) {

    final FactoryBeanDefinition<?> def = //
            factoryDef instanceof FactoryBeanDefinition
            ? (FactoryBeanDefinition<?>) factoryDef
            : new FactoryBeanDefinition<>(factoryDef, this);

    registerBeanDefinition(oldBeanName, def);
  }

  @Override
  public BeanDefinitionRegistry getRegistry() {
    return this;
  }

  @Override
  public BeanDefinition createBeanDefinition(final Class<?> beanClass) {
    return createBeanDefinition(getBeanNameCreator().create(beanClass),
                                beanClass,
                                getAnnotationAttributes(Component.class, beanClass));
  }

  @Override
  public BeanDefinition createBeanDefinition(
          final String beanName, final Class<?> beanClass, final AnnotationAttributes attributes
  ) {

    final DefaultBeanDefinition ret = new DefaultBeanDefinition(beanName, beanClass);

    if (attributes == null) {
      ret.setDestroyMethods(Constant.EMPTY_STRING_ARRAY)
              .setInitMethods(resolveInitMethod(null, beanClass));
    }
    else {
      ret.setScope(attributes.getString(Constant.SCOPE))
              .setDestroyMethods(attributes.getStringArray(Constant.DESTROY_METHODS))
              .setInitMethods(resolveInitMethod(attributes.getStringArray(Constant.INIT_METHODS), beanClass));
    }

    ret.setPropertyValues(resolvePropertyValue(beanClass));
    // fix missing @Props injection
    resolveProps(ret, getApplicationContext().getEnvironment());
    return ret;
  }

  @Override
  public ConfigurableApplicationContext getApplicationContext() {
    return applicationContext;
  }

  // PropertyValue    @since 3.0

  /**
   * Process bean's property (field)
   *
   * @param beanClass
   *         Bean class
   *
   * @since 3.0
   */
  public PropertyValue[] resolvePropertyValue(final Class<?> beanClass) {

    final LinkedHashSet<PropertyValue> propertyValues = new LinkedHashSet<>(32);
    for (final Field field : ReflectionUtils.getFields(beanClass)) {
      // if property is required and PropertyValue is null will throw ex in PropertyValueResolver
      final PropertyValue created = createPropertyValue(makeAccessible(field));
      // not required
      if (created != null) {
        propertyValues.add(created);
      }
    }

    return propertyValues.isEmpty()
           ? BeanDefinition.EMPTY_PROPERTY_VALUE
           : propertyValues.toArray(new PropertyValue[propertyValues.size()]);
  }

  /**
   * Create property value
   *
   * @param field
   *         Property
   *
   * @return A new {@link PropertyValue}
   */
  public PropertyValue createPropertyValue(final Field field) {

    for (final PropertyValueResolver propertyValueResolver : getPropertyValueResolvers()) {
      if (propertyValueResolver.supportsProperty(field)) {
        return propertyValueResolver.resolveProperty(field);
      }
    }
    return null;
  }

  /**
   * @see Constant#META_INFO_property_resolvers
   * @since 3.0
   */
  public LinkedList<PropertyValueResolver> getPropertyValueResolvers() {
    if (propertyResolvers.isEmpty()) {
      final ConfigurableApplicationContext context = getApplicationContext();
      final Set<PropertyValueResolver> objects = ContextUtils.loadBeansFromMetaInfo(Constant.META_INFO_property_resolvers, this);
      // un-ordered
      propertyResolvers.addAll(objects);

      addPropertyValueResolvers(new ValuePropertyResolver(context),
                                new PropsPropertyResolver(context),
                                new ObjectSupplierPropertyResolver(),
                                new AutowiredPropertyResolver(context));

    }
    return propertyResolvers;
  }

  /**
   * @since 3.0
   */
  public void setPropertyValueResolvers(PropertyValueResolver... resolvers) {
    nonNull(resolvers, "PropertyValueResolver must not be null");

    propertyResolvers.clear();
    Collections.addAll(propertyResolvers, OrderUtils.reversedSort(resolvers));
  }

  /**
   * Add {@link PropertyValueResolver} to {@link #propertyResolvers}
   *
   * @param resolvers
   *         {@link PropertyValueResolver} object
   *
   * @since 3.0
   */
  public void addPropertyValueResolvers(final PropertyValueResolver... resolvers) {
    if (ObjectUtils.isNotEmpty(resolvers)) {
      Collections.addAll(propertyResolvers, resolvers);
      OrderUtils.reversedSort(propertyResolvers);
    }
  }

}
