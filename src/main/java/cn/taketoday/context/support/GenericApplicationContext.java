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

package cn.taketoday.context.support;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionCustomizer;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.loader.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.loader.BeanDefinitionRegistrar;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceConsumer;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;

/**
 * Generic ApplicationContext implementation that holds a single internal
 * {@link cn.taketoday.beans.factory.support.StandardBeanFactory}
 * instance and does not assume a specific bean definition format. Implements
 * the {@link cn.taketoday.beans.factory.support.BeanDefinitionRegistry}
 * interface in order to allow for applying any bean definition readers to it.
 *
 * <p>Typical usage is to register a variety of bean definitions via the
 * {@link cn.taketoday.beans.factory.support.BeanDefinitionRegistry}
 * interface and then call {@link #refresh()} to initialize those beans
 * with application context semantics (handling
 * {@link cn.taketoday.context.aware.ApplicationContextAware}, auto-detecting
 * {@link cn.taketoday.beans.factory.BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * etc).
 *
 * <p>In contrast to other ApplicationContext implementations that create a new
 * internal BeanFactory instance for each refresh, the internal BeanFactory of
 * this context is available right from the start, to be able to register bean
 * definitions on it. {@link #refresh()} may only be called once.
 *
 * <p>Usage example:
 *
 * <pre class="code">
 * GenericApplicationContext ctx = new GenericApplicationContext();
 * XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
 * xmlReader.loadBeanDefinitions(new ClassPathResource("applicationContext.xml"));
 * PropertiesBeanDefinitionReader propReader = new PropertiesBeanDefinitionReader(ctx);
 * propReader.loadBeanDefinitions(new ClassPathResource("otherBeans.properties"));
 * ctx.refresh();
 *
 * MyBean myBean = (MyBean) ctx.getBean("myBean");
 * ...</pre>
 *
 * <p>For custom application context implementations that are supposed to read
 * special bean definition formats in a refreshable manner, consider deriving
 * from the {@link AbstractRefreshableApplicationContext} base class.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author TODAY 2021/10/1 16:25
 * @see #registerBeanDefinition
 * @see #refresh()
 * @since 4.0
 */
public class GenericApplicationContext
        extends AbstractApplicationContext implements BeanDefinitionRegistry, BeanDefinitionRegistrar {

  @Nullable
  private ResourceLoader resourceLoader;
  private boolean customClassLoader = false;

  protected final StandardBeanFactory beanFactory;
  private AnnotatedBeanDefinitionReader beanDefinitionReader;

  /**
   * Default Constructor
   *
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericApplicationContext() {
    this.beanFactory = new StandardBeanFactory();
  }

  /**
   * Create a new DefaultApplicationContext with the given StandardBeanFactory.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericApplicationContext(StandardBeanFactory beanFactory) {
    Assert.notNull(beanFactory, "BeanFactory must not be null");
    this.beanFactory = beanFactory;
  }

  /**
   * Create a new DefaultApplicationContext with the given parent.
   *
   * @param parent the parent application context
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericApplicationContext(@Nullable ApplicationContext parent) {
    this();
    setParent(parent);
  }

  /**
   * Create a new DefaultApplicationContext with the given StandardBeanFactory.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   * @param parent the parent application context
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericApplicationContext(StandardBeanFactory beanFactory, ApplicationContext parent) {
    this(beanFactory);
    setParent(parent);
  }

  /**
   * Set the parent of this application context, also setting
   * the parent of the internal BeanFactory accordingly.
   *
   * @see ConfigurableBeanFactory#setParentBeanFactory
   */
  @Override
  public void setParent(@Nullable ApplicationContext parent) {
    super.setParent(parent);
    this.beanFactory.setParentBeanFactory(getInternalParentBeanFactory());
  }

  @Override
  protected void refreshBeanFactory() throws BeansException, IllegalStateException { }

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
   * @see #getResourcesArray
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
  public Set<Resource> getResources(String locationPattern) throws IOException {
    if (this.resourceLoader instanceof PatternResourceLoader) {
      return ((PatternResourceLoader) this.resourceLoader).getResources(locationPattern);
    }
    return super.getResources(locationPattern);
  }

  /**
   * This implementation delegates to this context's ResourceLoader if it
   * implements the ResourcePatternResolver interface, falling back to the
   * default superclass behavior else.
   *
   * @see #setResourceLoader
   */
  @Override
  public void scan(String locationPattern, ResourceConsumer consumer) throws IOException {
    if (this.resourceLoader instanceof PatternResourceLoader) {
      ((PatternResourceLoader) this.resourceLoader).scan(locationPattern, consumer);
    }
    else {
      super.scan(locationPattern, consumer);
    }
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
  public String[] getBeanDefinitionNames() {
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

  //---------------------------------------------------------------------
  // Implementation of BeanDefinitionRegistrar Interface
  //---------------------------------------------------------------------

  /**
   * Register a bean with the given name and bean instance
   *
   * @param name bean name (must not be null)
   * @param obj bean instance (must not be null)
   */
  @Override
  public void registerSingleton(String name, Object obj) {
    getBeanDefinitionReader().registerSingleton(name, obj);
  }

  /**
   * Register a singleton bean with the underlying bean factory.
   * <p>For more advanced needs, register with the underlying BeanFactory directly.
   *
   * @see #getBeanFactory
   */
  public void registerSingleton(String name, Class<?> clazz) throws BeansException {
    BeanDefinition bd = new BeanDefinition(name, clazz);
    getBeanFactory().registerBeanDefinition(name, bd);
  }

  /**
   * Register a singleton bean with the underlying bean factory.
   * <p>For more advanced needs, register with the underlying BeanFactory directly.
   *
   * @see #getBeanFactory()
   */
  public void registerSingleton(String name, Class<?> clazz, PropertyValues pvs) throws BeansException {
    BeanDefinition bd = new BeanDefinition(name, clazz);
    bd.setPropertyValues(pvs);
    getBeanFactory().registerBeanDefinition(name, bd);
  }

  /**
   * Register a prototype bean with the underlying bean factory.
   * <p>For more advanced needs, register with the underlying BeanFactory directly.
   *
   * @see #getBeanFactory
   */
  public void registerPrototype(String name, Class<?> clazz) throws BeansException {
    BeanDefinition bd = new BeanDefinition(name, clazz);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    getBeanFactory().registerBeanDefinition(name, bd);
  }

  /**
   * Register a prototype bean with the underlying bean factory.
   * <p>For more advanced needs, register with the underlying BeanFactory directly.
   *
   * @see #getBeanFactory
   */
  public void registerPrototype(String name, Class<?> clazz, PropertyValues pvs) throws BeansException {
    BeanDefinition bd = new BeanDefinition(name, clazz);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    bd.setPropertyValues(pvs);
    getBeanFactory().registerBeanDefinition(name, bd);
  }

  @Override
  public void registerBean(Object obj) {
    getBeanDefinitionReader().registerBean(obj);
  }

  @Override
  public void registerBean(String name, Object obj) {
    getBeanDefinitionReader().registerBean(name, obj);
  }

  /**
   * Register a bean with the given type and instance supplier
   *
   * @param clazz bean class
   * @param supplier bean instance supplier
   * @param prototype register as prototype?
   * @param ignoreAnnotation ignore {@link Component} scanning
   * @throws BeanDefinitionStoreException If can't store a bean
   * @since 4.0
   */
  public <T> void registerBean(
          Class<T> clazz, Supplier<T> supplier,
          boolean prototype, boolean ignoreAnnotation) throws BeanDefinitionStoreException {
    getBeanDefinitionReader().registerBean(clazz, supplier, prototype, ignoreAnnotation);
  }

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
    beanFactory.registerBean(name, supplier);
  }

  /**
   * Register a bean from the given bean class, optionally providing explicit
   * constructor arguments for consideration in the autowiring process.
   *
   * @param beanName the name of the bean (may be {@code null})
   * @param beanClass the class of the bean
   * @param constructorArgs custom argument values to be fed into constructor
   * resolution algorithm, resolving either all arguments or just
   * specific ones, with the rest to be resolved through regular autowiring
   * (may be {@code null} or empty)
   */
  @Override
  public <T> void registerBean(
          @Nullable String beanName, Class<T> beanClass, Object... constructorArgs) {
    getBeanDefinitionReader().registerBean(beanName, beanClass, constructorArgs);
  }

  /**
   * Register a bean from the given bean class, using the given supplier for
   * obtaining a new instance (typically declared as a lambda expression or
   * method reference), optionally customizing its bean definition metadata
   * (again typically declared as a lambda expression).
   * <p>This method can be overridden to adapt the registration mechanism for
   * all {@code registerBean} methods (since they all delegate to this one).
   *
   * @param beanName the name of the bean (may be {@code null})
   * @param beanClass the class of the bean
   * @param supplier a callback for creating an instance of the bean (in case
   * of {@code null}, resolving a public constructor to be autowired instead)
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   */
  @Override
  public <T> void registerBean(
          @Nullable String beanName, Class<T> beanClass,
          @Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {
    getBeanDefinitionReader().registerBean(beanName, beanClass, supplier, customizers);
  }

  public final AnnotatedBeanDefinitionReader getBeanDefinitionReader() {
    if (beanDefinitionReader == null) {
      beanDefinitionReader = new AnnotatedBeanDefinitionReader(this, beanFactory);
    }
    return beanDefinitionReader;
  }

  //---------------------------------------------------------------------
  // Implementation of AliasRegistry Interface
  //---------------------------------------------------------------------

  @Override
  public void registerAlias(String name, String alias) {
    beanFactory.registerAlias(name, alias);
  }

  @Override
  public void removeAlias(String alias) {
    beanFactory.removeAlias(alias);
  }

  @Override
  public boolean isAlias(String name) {
    return beanFactory.isAlias(name);
  }

  @Override
  public List<String> getAliasList(String name) {
    return beanFactory.getAliasList(name);
  }

  /**
   * Set whether it should be allowed to override bean definitions by registering
   * a different definition with the same name, automatically replacing the former.
   * If not, an exception will be thrown. This also applies to overriding aliases.
   * <p>Default is "true".
   *
   * @see #registerBeanDefinition
   * @since 4.0
   */
  public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
    this.beanFactory.setAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding);
  }

  /**
   * Set whether to allow circular references between beans - and automatically
   * try to resolve them.
   * <p>Default is "true". Turn this off to throw an exception when encountering
   * a circular reference, disallowing them completely.
   *
   * @since 4.0
   */
  public void setAllowCircularReferences(boolean allowCircularReferences) {
    this.beanFactory.setAllowCircularReferences(allowCircularReferences);
  }

}
