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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionCustomizer;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceConsumer;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
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
 * {@link BeanFactoryPostProcessor BeanFactoryPostProcessors},
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
        extends AbstractApplicationContext implements BeanDefinitionRegistry {

  protected final StandardBeanFactory beanFactory;

  @Nullable
  private ResourceLoader resourceLoader;

  private boolean customClassLoader = false;

  private final AtomicBoolean refreshed = new AtomicBoolean();

  /**
   * Create a new GenericApplicationContext.
   *
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericApplicationContext() {
    this.beanFactory = new StandardBeanFactory();
  }

  /**
   * Create a new GenericApplicationContext with the given StandardBeanFactory.
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
   * Create a new GenericApplicationContext with the given parent.
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
   * Create a new GenericApplicationContext with the given StandardBeanFactory.
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

  @Override
  protected BootstrapContext createBootstrapContext() {
    return new BootstrapContext(beanFactory, this);
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

  /**
   * Do nothing: We hold a single internal BeanFactory and rely on callers
   * to register beans through our public methods (or the BeanFactory's).
   *
   * @see #registerBeanDefinition
   */
  @Override
  protected final void refreshBeanFactory() throws IllegalStateException {
    if (!this.refreshed.compareAndSet(false, true)) {
      throw new IllegalStateException(
              "GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once");
    }
    this.beanFactory.setSerializationId(getId());
  }

  @Override
  protected void cancelRefresh(Exception ex) {
    this.beanFactory.setSerializationId(null);
    super.cancelRefresh(ex);
  }

  /**
   * Not much to do: We hold a single internal BeanFactory that will never
   * get released.
   */
  @Override
  protected final void closeBeanFactory() {
    this.beanFactory.setSerializationId(null);
  }

  @Override
  public StandardBeanFactory getBeanFactory() {
    return beanFactory;
  }

  @Override
  public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
    assertBeanFactoryActive();
    return this.beanFactory;
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
  // Convenient methods for registering individual beans
  //---------------------------------------------------------------------

  /**
   * Register a bean with the given name and bean instance
   *
   * @param name bean name (must not be null)
   * @param obj bean instance (must not be null)
   */
  public void registerSingleton(String name, Object obj) {
    getBeanFactory().registerSingleton(name, obj);
  }

  /**
   * Register a singleton bean with the underlying bean factory.
   * <p>For more advanced needs, register with the underlying BeanFactory directly.
   *
   * @see #getBeanFactory
   */
  public void registerSingleton(String name, Class<?> clazz) throws BeansException {
    BeanDefinition bd = new RootBeanDefinition(clazz);
    getBeanFactory().registerBeanDefinition(name, bd);
  }

  /**
   * Register a singleton bean with the underlying bean factory.
   * <p>For more advanced needs, register with the underlying BeanFactory directly.
   *
   * @see #getBeanFactory()
   */
  public void registerSingleton(String name, Class<?> clazz, @Nullable PropertyValues pvs) throws BeansException {
    RootBeanDefinition bd = new RootBeanDefinition(clazz);
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
    RootBeanDefinition bd = new RootBeanDefinition(clazz);
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
    RootBeanDefinition bd = new RootBeanDefinition(clazz);
    bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    bd.setPropertyValues(pvs);
    getBeanFactory().registerBeanDefinition(name, bd);
  }

  public void registerSingleton(Object obj) {
    getBeanFactory().registerSingleton(obj);
  }

  /**
   * Register a bean from the given bean class, optionally providing explicit
   * constructor arguments for consideration in the autowiring process.
   *
   * @param beanClass the class of the bean
   * @param constructorArgs custom argument values to be fed into Framework's
   * constructor resolution algorithm, resolving either all arguments or just
   * specific ones, with the rest to be resolved through regular autowiring
   * (may be {@code null} or empty)
   */
  public <T> void registerBean(Class<T> beanClass, Object... constructorArgs) {
    registerBean(null, beanClass, constructorArgs);
  }

  /**
   * Register a bean from the given bean class, optionally providing explicit
   * constructor arguments for consideration in the autowiring process.
   *
   * @param beanName the name of the bean (may be {@code null})
   * @param beanClass the class of the bean
   * @param constructorArgs custom argument values to be fed into Framework's
   * constructor resolution algorithm, resolving either all arguments or just
   * specific ones, with the rest to be resolved through regular autowiring
   * (may be {@code null} or empty)
   */
  public <T> void registerBean(@Nullable String beanName, Class<T> beanClass, Object... constructorArgs) {
    registerBean(beanName, beanClass, (Supplier<T>) null,
            bd -> {
              for (Object arg : constructorArgs) {
                bd.getConstructorArgumentValues().addGenericArgumentValue(arg);
              }
            });
  }

  /**
   * Register a bean from the given bean class, optionally customizing its
   * bean definition metadata (typically declared as a lambda expression).
   *
   * @param beanClass the class of the bean (resolving a public constructor
   * to be autowired, possibly simply the default constructor)
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
   */
  public final <T> void registerBean(Class<T> beanClass, BeanDefinitionCustomizer... customizers) {
    registerBean(null, beanClass, null, customizers);
  }

  /**
   * Register a bean from the given bean class, optionally customizing its
   * bean definition metadata (typically declared as a lambda expression).
   *
   * @param beanName the name of the bean (may be {@code null})
   * @param beanClass the class of the bean (resolving a public constructor
   * to be autowired, possibly simply the default constructor)
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
   */
  public final <T> void registerBean(
          @Nullable String beanName, Class<T> beanClass, BeanDefinitionCustomizer... customizers) {

    registerBean(beanName, beanClass, null, customizers);
  }

  /**
   * Register a bean from the given bean class, using the given supplier for
   * obtaining a new instance (typically declared as a lambda expression or
   * method reference), optionally customizing its bean definition metadata
   * (again typically declared as a lambda expression).
   *
   * @param beanClass the class of the bean
   * @param supplier a callback for creating an instance of the bean
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
   */
  public final <T> void registerBean(
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
   * @param beanName the name of the bean (may be {@code null})
   * @param beanClass the class of the bean
   * @param supplier a callback for creating an instance of the bean (in case
   * of {@code null}, resolving a public constructor to be autowired instead)
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   */
  public <T> void registerBean(
          @Nullable String beanName, Class<T> beanClass,
          @Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

    RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass);
    if (supplier != null) {
      beanDefinition.setInstanceSupplier(supplier);
    }

    for (BeanDefinitionCustomizer customizer : customizers) {
      customizer.customize(beanDefinition);
    }

    String nameToUse = beanName != null ? beanName : beanClass.getName();
    registerBeanDefinition(nameToUse, beanDefinition);
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
