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

package cn.taketoday.context;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionCustomizer;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.DefaultBeanDefinition;
import cn.taketoday.beans.factory.StandardBeanFactory;
import cn.taketoday.context.loader.BeanDefinitionReader;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * ApplicationContext default implementation
 *
 * @author TODAY 2021/10/1 16:25
 * @since 4.0
 */
public class DefaultApplicationContext
        extends AbstractApplicationContext implements BeanDefinitionRegistry {

  @Nullable
  private ResourceLoader resourceLoader;
  private boolean customClassLoader = false;

  protected final StandardBeanFactory beanFactory;
  protected final BeanDefinitionReader beanDefinitionReader;

  /**
   * Default Constructor
   *
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public DefaultApplicationContext() {
    this.beanFactory = new StandardBeanFactory();
    this.beanDefinitionReader = new BeanDefinitionReader(this, beanFactory);
  }

  /**
   * Create a new DefaultApplicationContext with the given StandardBeanFactory.
   *
   * @param beanFactory
   *         the StandardBeanFactory instance to use for this context
   *
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public DefaultApplicationContext(StandardBeanFactory beanFactory) {
    Assert.notNull(beanFactory, "BeanFactory must not be null");
    this.beanFactory = beanFactory;
    this.beanDefinitionReader = new BeanDefinitionReader(this, beanFactory);
  }

  /**
   * Create a new DefaultApplicationContext with the given parent.
   *
   * @param parent
   *         the parent application context
   *
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public DefaultApplicationContext(@Nullable ApplicationContext parent) {
    this();
    setParent(parent);
  }

  /**
   * Create a new DefaultApplicationContext with the given StandardBeanFactory.
   *
   * @param beanFactory
   *         the StandardBeanFactory instance to use for this context
   * @param parent
   *         the parent application context
   *
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public DefaultApplicationContext(StandardBeanFactory beanFactory, ApplicationContext parent) {
    this(beanFactory);
    setParent(parent);
  }

  /**
   * Set the parent of this application context, also setting
   * the parent of the internal BeanFactory accordingly.
   *
   * @see cn.taketoday.beans.factory.ConfigurableBeanFactory#setParentBeanFactory
   */
  @Override
  public void setParent(@Nullable ApplicationContext parent) {
    super.setParent(parent);
    this.beanFactory.setParentBeanFactory(getInternalParentBeanFactory());
  }

  @Override
  public StandardBeanFactory getBeanFactory() {
    return beanFactory;
  }

  /**
   * Set a ResourceLoader to use for this context. If set, the context will
   * delegate all {@code getResource} calls to the given ResourceLoader.
   * If not set, default resource loading will apply.
   * <p>The main reason to specify a custom ResourceLoader is to resolve
   * resource paths (without URL prefix) in a specific fashion.
   * The default behavior is to resolve such paths as class path locations.
   * To resolve resource paths as file system locations, specify a
   * FileSystemResourceLoader here.
   * <p>You can also pass in a full ResourcePatternResolver, which will
   * be autodetected by the context and used for {@code getResources}
   * calls as well. Else, default resource pattern matching will apply.
   *
   * @see #getResource
   * @see DefaultResourceLoader
   * @see PatternResourceLoader
   * @see #getResources
   */
  public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  //---------------------------------------------------------------------
  // ResourceLoader / ResourcePatternResolver override if necessary
  //---------------------------------------------------------------------

  /**
   * This implementation delegates to this context's ResourceLoader if set,
   * falling back to the default superclass behavior else.
   *
   * @see #setResourceLoader
   */
  @Override
  public Resource getResource(String location) {
    if (this.resourceLoader != null) {
      return this.resourceLoader.getResource(location);
    }
    return super.getResource(location);
  }

  /**
   * This implementation delegates to this context's ResourceLoader if it
   * implements the ResourcePatternResolver interface, falling back to the
   * default superclass behavior else.
   *
   * @see #setResourceLoader
   */
  @Override
  public Resource[] getResources(String locationPattern) throws IOException {
    if (this.resourceLoader instanceof PatternResourceLoader) {
      return ((PatternResourceLoader) this.resourceLoader).getResources(locationPattern);
    }
    return super.getResources(locationPattern);
  }

  @Override
  public void setClassLoader(@Nullable ClassLoader classLoader) {
    super.setClassLoader(classLoader);
    this.customClassLoader = true;
  }

  @Override
  @Nullable
  public ClassLoader getClassLoader() {
    if (this.resourceLoader != null && !this.customClassLoader) {
      return this.resourceLoader.getClassLoader();
    }
    return super.getClassLoader();
  }

  //---------------------------------------------------------------------
  // Implementation of BeanDefinitionRegistry
  //---------------------------------------------------------------------

  @Override
  public void registerBeanDefinition(String name, BeanDefinition def) {
    beanFactory.registerBeanDefinition(name, def);
  }

  @Override
  public void removeBeanDefinition(String beanName) {
    beanFactory.removeBeanDefinition(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(Class<?> beanClass) {
    return beanFactory.getBeanDefinition(beanClass);
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type) {
    return beanFactory.containsBeanDefinition(type);
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type, boolean equals) {
    return beanFactory.containsBeanDefinition(type, equals);
  }

  @Override
  public boolean isBeanNameInUse(String beanName) {
    return beanFactory.isBeanNameInUse(beanName);
  }

  @Override
  public boolean isAllowBeanDefinitionOverriding() {
    return beanFactory.isAllowBeanDefinitionOverriding();
  }

  @Override
  public int getBeanDefinitionCount() {
    return beanFactory.getBeanDefinitionCount();
  }

  @Override
  public Set<String> getBeanDefinitionNames() {
    return beanFactory.getBeanDefinitionNames();
  }

  @Override
  public Iterator<String> getBeanNamesIterator() {
    return beanFactory.getBeanNamesIterator();
  }

  @Override
  public boolean containsBeanDefinition(String beanName) {
    return beanFactory.containsBeanDefinition(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(String beanName) {
    return beanFactory.getBeanDefinition(beanName);
  }

  @Override
  public Iterator<BeanDefinition> iterator() {
    return beanFactory.iterator();
  }

  //---------------------------------------------------------------------
  // Convenient methods for registering individual beans
  //---------------------------------------------------------------------

  /**
   * register a bean with the given bean class
   *
   * @since 3.0
   */
  public void registerBean(Class<?> clazz) {
    beanDefinitionReader.registerBean(clazz);
  }

  public void registerBean(Class<?>... candidates) {
    beanDefinitionReader.registerBean(candidates);
  }

  /**
   * @since 4.0
   */
  public void registerBean(Set<Class<?>> candidates) {
    beanDefinitionReader.registerBean(candidates);
  }

  public void registerBean(String name, Class<?> clazz) {
    beanDefinitionReader.registerBean(name, clazz);
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
    getBeanFactory().registerSingleton(name, obj);
  }

  /**
   * Register a bean with the given type and instance supplier
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
    beanDefinitionReader.registerBean(clazz, supplier, prototype, ignoreAnnotation);
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
    beanDefinitionReader.registerBean(name, supplier);
  }

  /**
   * Register a bean from the given bean class, optionally providing explicit
   * constructor arguments for consideration in the autowiring process.
   *
   * @param beanClass
   *         the class of the bean
   * @param constructorArgs
   *         custom argument values to be fed into Spring's
   *         constructor resolution algorithm, resolving either all arguments or just
   *         specific ones, with the rest to be resolved through regular autowiring
   *         (may be {@code null} or empty)
   */
  public <T> void registerBean(Class<T> beanClass, Object... constructorArgs) {
    registerBean(null, beanClass, constructorArgs);
  }

  /**
   * Register a bean from the given bean class, optionally providing explicit
   * constructor arguments for consideration in the autowiring process.
   *
   * @param beanName
   *         the name of the bean (may be {@code null})
   * @param beanClass
   *         the class of the bean
   * @param constructorArgs
   *         custom argument values to be fed into Spring's
   *         constructor resolution algorithm, resolving either all arguments or just
   *         specific ones, with the rest to be resolved through regular autowiring
   *         (may be {@code null} or empty)
   */
  public <T> void registerBean(
          @Nullable String beanName, Class<T> beanClass, Object... constructorArgs) {
    registerBean(beanName, beanClass, (Supplier<T>) null,
                 (a, bd) -> bd.setSupplier(() -> bd.newInstance(beanFactory, constructorArgs)));
  }

  /**
   * Register a bean from the given bean class, optionally customizing its
   * bean definition metadata (typically declared as a lambda expression).
   *
   * @param beanClass
   *         the class of the bean (resolving a public constructor
   *         to be autowired, possibly simply the default constructor)
   * @param customizers
   *         one or more callbacks for customizing the factory's
   *         {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   *
   * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
   */
  public final <T> void registerBean(Class<T> beanClass, BeanDefinitionCustomizer... customizers) {
    registerBean(null, beanClass, null, customizers);
  }

  /**
   * Register a bean from the given bean class, optionally customizing its
   * bean definition metadata (typically declared as a lambda expression).
   *
   * @param beanName
   *         the name of the bean (may be {@code null})
   * @param beanClass
   *         the class of the bean (resolving a public constructor
   *         to be autowired, possibly simply the default constructor)
   * @param customizers
   *         one or more callbacks for customizing the factory's
   *         {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   *
   * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
   */
  public <T> void registerBean(
          @Nullable String beanName, Class<T> beanClass, BeanDefinitionCustomizer... customizers) {
    registerBean(beanName, beanClass, null, customizers);
  }

  /**
   * Register a bean from the given bean class, using the given supplier for
   * obtaining a new instance (typically declared as a lambda expression or
   * method reference), optionally customizing its bean definition metadata
   * (again typically declared as a lambda expression).
   *
   * @param beanClass
   *         the class of the bean
   * @param supplier
   *         a callback for creating an instance of the bean
   * @param customizers
   *         one or more callbacks for customizing the factory's
   *         {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   *
   * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
   */
  public <T> void registerBean(
          Class<T> beanClass, Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {
    registerBean(null, beanClass, supplier, customizers);
  }

  /**
   * Register a bean from the given bean class, using the given supplier for
   * obtaining a new instance (typically declared as a lambda expression or
   * method reference), optionally customizing its bean definition metadata
   * (again typically declared as a lambda expression).
   * <p>This method can be overridden to adapt the registration mechanism for
   * all {@code registerBean} methods (since they all delegate to this one).
   *
   * @param beanName
   *         the name of the bean (may be {@code null})
   * @param beanClass
   *         the class of the bean
   * @param supplier
   *         a callback for creating an instance of the bean (in case
   *         of {@code null}, resolving a public constructor to be autowired instead)
   * @param customizers
   *         one or more callbacks for customizing the factory's
   *         {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   */
  public <T> void registerBean(
          @Nullable String beanName, Class<T> beanClass,
          @Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

    String nameToUse = beanName != null ? beanName : createBeanName(beanClass);
    DefaultBeanDefinition definition = new DefaultBeanDefinition(nameToUse, beanClass);
    definition.setSupplier(supplier);

    if (ObjectUtils.isNotEmpty(customizers)) {
      for (BeanDefinitionCustomizer customizer : customizers) {
        customizer.customize(null, definition);
      }
    }

    registerBeanDefinition(nameToUse, definition);
  }

}
