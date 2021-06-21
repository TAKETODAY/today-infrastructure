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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.IgnoreDuplicates;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.aware.ImportAware;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.LoadingMissingBeanEvent;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
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
import static cn.taketoday.context.utils.ClassUtils.getAnnotationAttributes;
import static cn.taketoday.context.utils.ClassUtils.getAnnotationAttributesArray;
import static cn.taketoday.context.utils.ContextUtils.findNames;
import static cn.taketoday.context.utils.ContextUtils.resolveInitMethod;
import static cn.taketoday.context.utils.ContextUtils.resolveProps;
import static cn.taketoday.context.utils.ReflectionUtils.makeAccessible;

/**
 * Standard {@link BeanFactory} implementation
 *
 * @author TODAY <br>
 * 2019-03-23 15:00
 */
public class StandardBeanFactory
        extends AbstractBeanFactory implements ConfigurableBeanFactory, BeanDefinitionLoader {

  private static final Logger log = LoggerFactory.getLogger(StandardBeanFactory.class);
  // @since 3.0
  static final String MissingBeanMetadata = MissingBean.class.getName() + "-Metadata";
  static final String ImportAnnotatedMetadata = Import.class.getName() + "-Metadata"; // @since 3.0

  private final ConfigurableApplicationContext context;
  private final ArrayList<AnnotatedElement> componentScanned = new ArrayList<>();

  /**
   * @since 3.0 Resolve {@link PropertySetter}
   */
  private final ArrayList<PropertyValueResolver> propertyResolvers = new ArrayList<>(4);

  /**
   * @since 2.1.7 Preventing repeated initialization of beans(Prevent duplicate
   * initialization) , Prevent Cycle Dependency
   */
  private final HashSet<String> currentInitializingBeanName = new HashSet<>();

  public StandardBeanFactory(ConfigurableApplicationContext context) {
    Assert.notNull(context, "applicationContext must not be null");
    this.context = context;
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
    if (bean instanceof ImportAware) { // @since 3.0
      final Object attribute = def.getAttribute(ImportAnnotatedMetadata);
      if (attribute instanceof BeanDefinition) {
        ((ImportAware) bean).setImportBeanDefinition((BeanDefinition) attribute);
      }
    }
  }

  /**
   * Preventing Cycle Dependency expected {@link cn.taketoday.context.annotation.Prototype} beans
   */
  @Override
  public Object initializeBean(final Object bean, final BeanDefinition def) {
    if (def.isPrototype()) {
      return super.initializeBean(bean, def);
    }

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

    final ConfigurableApplicationContext context = getApplicationContext();
    final BeanNameCreator beanNameCreator = getBeanNameCreator();

    for (final Method method : ReflectionUtils.getDeclaredMethods(def.getBeanClass())) {
      final AnnotationAttributes[] components = getAnnotationAttributesArray(method, Component.class);
      if (ObjectUtils.isEmpty(components)) {
        // detect missed bean
        final AnnotationAttributes attributes = getAnnotationAttributes(MissingBean.class, method);
        if (isMissedBean(attributes, method, context)) {
          // register directly @since 3.0
          final Class<?> beanClass = method.getReturnType();
          String name = attributes.getString(VALUE);
          if (StringUtils.isEmpty(name)) {
            name = method.getName();
          }
          StandardBeanDefinition stdDef = // @Configuration use default bean name
                  new StandardBeanDefinition(name, beanClass)
                          .setFactoryMethod(method)
                          .setDeclaringName(beanNameCreator.create(method.getDeclaringClass()));

          registerMissingBean(attributes, stdDef);
        }
      }
      else if (ContextUtils.passCondition(method, context)) { // pass the condition
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
        stdDef.addPropertySetter(resolveProps(stdDef, properties));
        register(name, stdDef);
      }
    }
  }

  /**
   * Load missing beans, default beans
   *
   * @param candidates
   *         candidate class set
   */
  public void loadMissingBean(final Collection<Class<?>> candidates) {
    log.debug("Loading lost beans");

    final ConfigurableApplicationContext context = getApplicationContext();
    final BeanNameCreator nameCreator = getBeanNameCreator();

    context.publishEvent(new LoadingMissingBeanEvent(context, candidates));

    for (final Class<?> beanClass : candidates) {
      final AnnotationAttributes attributes = getAnnotationAttributes(MissingBean.class, beanClass);
      if (isMissedBean(attributes, beanClass, context)) {
        String beanName = attributes.getString(VALUE);
        if (StringUtils.isEmpty(beanName)) {
          beanName = nameCreator.create(beanClass);
        }

        final DefaultBeanDefinition def = new DefaultBeanDefinition(beanName, beanClass);
        registerMissingBean(attributes, def);
      }
    }
  }

  /**
   * Is a context missed bean?
   *
   * @param missingBean
   *         The {@link Annotation} declared on the class or a method
   * @param annotated
   *         Missed bean class or method
   * @param context
   *         Application context
   *
   * @return If the bean is missed in context
   *
   * @since 3.0
   */
  boolean isMissedBean(final AnnotationAttributes missingBean,
                       final AnnotatedElement annotated, final ApplicationContext context) {
    if (missingBean == null || !ContextUtils.passCondition(annotated, context)) { // use current application context
      return false;
    }

    final String beanName = missingBean.getString(VALUE);
    if (StringUtils.isNotEmpty(beanName) && containsBeanDefinition(beanName)) {
      return false;
    }
    final Class<?> type = missingBean.getClass("type");
    if (type != void.class) {
      return !containsBeanDefinition(type, missingBean.getBoolean("equals"));
    }
    else {
      return !containsBeanDefinition(ContextUtils.getBeanClass(annotated));
    }
  }

  /**
   * Register {@link MissingBean}
   *
   * @param missingBean
   *         {@link MissingBean} metadata
   * @param def
   *         Target {@link BeanDefinition}
   */
  protected void registerMissingBean(final AnnotationAttributes missingBean, final BeanDefinition def) {
    final Class<?> beanClass = def.getBeanClass();

    def.setScope(missingBean.getString("scope"))
            .setDestroyMethods(missingBean.getStringArray("destroyMethods"))
            .setInitMethods(resolveInitMethod(missingBean.getStringArray("initMethods"), beanClass))
            .setPropertyValues(resolvePropertyValue(beanClass));

    // Missing BeanMetadata a flag to determine its a missed bean @since 3.0
    def.setAttribute(MissingBeanMetadata, missingBean);

    resolveProps(def, getApplicationContext().getEnvironment());

    // register missed bean
    register(def.getName(), def);
  }

  /**
   * Resolve bean from META-INF/beans
   *
   * @see Constant#META_INFO_beans
   * @since 2.1.6
   */
  public Set<Class<?>> loadMetaInfoBeans() {
    log.debug("Loading META-INF/beans");
    final ConfigurableApplicationContext context = getApplicationContext();

    // Load the META-INF/beans @since 2.1.6
    // ---------------------------------------------------
    final Set<Class<?>> beans = ContextUtils.loadFromMetaInfo(Constant.META_INFO_beans);
    final BeanNameCreator beanNameCreator = getBeanNameCreator();
    for (final Class<?> beanClass : beans) {
      final AnnotationAttributes missingBean = getAnnotationAttributes(MissingBean.class, beanClass);
      if (missingBean != null) {
        if (isMissedBean(missingBean, beanClass, context)) {
          // MissingBean in 'META-INF/beans' @since 3.0
          final BeanDefinition def = createBeanDefinition(beanClass);
          final String name = missingBean.getString(VALUE);
          if (StringUtils.isNotEmpty(name)) {
            def.setName(name); // refresh bean name
          }
          registerMissingBean(missingBean, def);
        }
        else {
          log.info("@MissingBean -> '{}' cannot pass the condition " +
                           "or contains its bean definition, dont register to the map", beanClass);
        }
      }
      else {
        if (ContextUtils.passCondition(beanClass, context)) {
          // can't be a missed bean. MissingBean load after normal loading beans
          final List<BeanDefinition> defs =
                  ContextUtils.createBeanDefinitions(beanNameCreator.create(beanClass), beanClass, this);
          for (final BeanDefinition def : defs) {
            register(def);
          }
        }
      }

    }
    return beans;
  }

  @Override
  public void importBeans(final Class<?>... beans) {
    for (final Class<?> bean : Objects.requireNonNull(beans)) {
      final BeanDefinition def = createBeanDefinition(bean);
      importAnnotated(def);
      register(def);
      loadConfigurationBeans(def); // scan config bean
    }
  }

  @Override
  public void importBeans(final Set<BeanDefinition> defs) {

    for (final BeanDefinition def : defs) {
      importAnnotated(def);
    }
  }

  @Override
  public void importAnnotated(final BeanDefinition annotated) {

    for (final AnnotationAttributes attr : getAnnotationAttributesArray(annotated, Import.class)) {
      for (final Class<?> importClass : attr.getAttribute(VALUE, Class[].class)) {
        if (!containsBeanDefinition(importClass, true)) {
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
  protected void doImport(final BeanDefinition annotated, final Class<?> importClass) {
    log.debug("Importing: [{}]", importClass);

    BeanDefinition importDef = createBeanDefinition(importClass);
    importDef.setAttribute(ImportAnnotatedMetadata, annotated); // @since 3.0
    register(importDef);
    loadConfigurationBeans(importDef); // scan config bean
    if (ImportSelector.class.isAssignableFrom(importClass)) {
      final String[] imports = createImporter(importDef, ImportSelector.class).selectImports(annotated);
      if (StringUtils.isArrayNotEmpty(imports)) {
        for (final String select : imports) {
          final Class<Object> beanClass = ClassUtils.loadClass(select);
          if (beanClass == null) {
            throw new ConfigurationException("Bean class not in class-path: " + select);
          }
          register(createBeanDefinition(beanClass));
        }
      }
    }
    if (BeanDefinitionImporter.class.isAssignableFrom(importClass)) {
      createImporter(importDef, BeanDefinitionImporter.class)
              .registerBeanDefinitions(annotated, this);
    }
    if (ApplicationListener.class.isAssignableFrom(importClass)) {
      getApplicationContext()
              .addApplicationListener(createImporter(importDef, ApplicationListener.class));
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
    if (!Modifier.isAbstract(candidate.getModifiers()) && ContextUtils.passCondition(candidate, context)) {
      register(candidate);
    }
  }

  @Override
  public void loadBeanDefinitions(final Collection<Class<?>> candidates) {
    for (Class<?> clazz : candidates) {
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
  public void register(final String name, BeanDefinition def) {
    def = transformBeanDefinition(name, def);
    if (def == null) {
      return;
    }

    ContextUtils.validateBeanDefinition(def);
    String nameToUse = name;
    final Class<?> beanClass = def.getBeanClass();

    if (containsBeanDefinition(name) && !def.hasAttribute(MissingBeanMetadata)) {
      // has same name
      final BeanDefinition existBeanDef = getBeanDefinition(name);
      final Class<?> existClass = existBeanDef.getBeanClass();
      if (beanClass == existClass && existBeanDef.isAnnotationPresent(IgnoreDuplicates.class)) { // @since 3.0.2
        return; // ignore registering
      }

      log.info("=====================|repeat bean definition START|=====================");
      log.info("There is already a bean called: [{}], its bean definition: [{}].", name, existBeanDef);
      if (beanClass == existClass) {
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

    try {
      if (FactoryBean.class.isAssignableFrom(beanClass)) { // process FactoryBean
        registerFactoryBean(nameToUse, def);
      }
      else {
        registerBeanDefinition(nameToUse, def);
      }
    }
    catch (Throwable ex) {
      ex = ExceptionUtils.unwrapThrowable(ex);
      throw new BeanDefinitionStoreException(
              "An Exception Occurred When Register Bean Definition: [" + def + "]", ex);
    }
  }

  /**
   * @since 3.0
   */
  protected BeanDefinition transformBeanDefinition(String name, BeanDefinition def) {
    final Class<?> beanClass = def.getBeanClass();

    BeanDefinition missedDef = null;
    if (containsBeanDefinition(name)) {
      missedDef = getBeanDefinition(name);
    }
    else if (containsBeanDefinition(beanClass)) {
      missedDef = getBeanDefinition(beanClass);
    }

    if (missedDef != null
            && missedDef.hasAttribute(MissingBeanMetadata)) { // Have a corresponding missed bean
      // copy all state
      def.copy(missedDef);
      def.setName(name); // fix bean name update error
    }
    // nothing
    return def;
  }

  @Override
  protected void postProcessRegisterBeanDefinition(final BeanDefinition targetDef) {
    super.postProcessRegisterBeanDefinition(targetDef);

    // import beans
    if (targetDef.isAnnotationPresent(Import.class)) { // @since 2.1.7
      importAnnotated(targetDef);
    }
    // scan components
    if (targetDef.isAnnotationPresent(ComponentScan.class)) {
      componentScan(targetDef);
    }
    // load application listener @since 2.1.7
    if (ApplicationListener.class.isAssignableFrom(targetDef.getBeanClass())) {
      Object listener = getSingleton(targetDef.getName());
      if (listener == null) {
        listener = createBeanIfNecessary(targetDef);
        context.addApplicationListener((ApplicationListener<?>) listener);
      }
    }
    // apply lazy init @since 3.0
    applyLazyInit(targetDef);
  }

  protected void applyLazyInit(BeanDefinition def) {
    final Lazy lazy = def.getAnnotation(Lazy.class);
    if (lazy != null) {
      def.setLazyInit(lazy.value());
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
    return context;
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
  public PropertySetter[] resolvePropertyValue(final Class<?> beanClass) {

    final LinkedHashSet<PropertySetter> propertySetters = new LinkedHashSet<>(32);
    for (final Field field : ReflectionUtils.getFields(beanClass)) {
      // if property is required and PropertyValue is null will throw ex in PropertyValueResolver
      final PropertySetter created = createPropertyValue(makeAccessible(field));
      // not required
      if (created != null) {
        propertySetters.add(created);
      }
    }

    return propertySetters.isEmpty()
           ? BeanDefinition.EMPTY_PROPERTY_VALUE
           : propertySetters.toArray(new PropertySetter[propertySetters.size()]);
  }

  /**
   * Create property value
   *
   * @param field
   *         Property
   *
   * @return A new {@link PropertySetter}
   */
  public PropertySetter createPropertyValue(final Field field) {

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
  public ArrayList<PropertyValueResolver> getPropertyValueResolvers() {
    if (propertyResolvers.isEmpty()) {
      final ConfigurableApplicationContext context = getApplicationContext();
      final Set<PropertyValueResolver> objects =
              ContextUtils.loadBeansFromMetaInfo(Constant.META_INFO_property_resolvers, this);
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
    Assert.notNull(resolvers, "PropertyValueResolver must not be null");

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
