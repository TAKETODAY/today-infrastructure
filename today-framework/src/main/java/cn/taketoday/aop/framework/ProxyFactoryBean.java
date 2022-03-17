/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.aop.framework;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Interceptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.framework.adapter.AdvisorAdapterRegistry;
import cn.taketoday.aop.framework.adapter.DefaultAdvisorAdapterRegistry;
import cn.taketoday.aop.framework.adapter.UnknownAdviceTypeException;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.FactoryBeanNotInitializedException;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * {@link FactoryBean} implementation that builds an
 * AOP proxy based on beans in {@link cn.taketoday.beans.factory.BeanFactory}.
 *
 * <p>{@link org.aopalliance.intercept.MethodInterceptor MethodInterceptors} and
 * {@link cn.taketoday.aop.Advisor Advisors} are identified by a list of bean
 * names in the current bean factory, specified through the "interceptorNames" property.
 * The last entry in the list can be the name of a target bean or a
 * {@link cn.taketoday.aop.TargetSource}; however, it is normally preferable
 * to use the "targetName"/"target"/"targetSource" properties instead.
 *
 * <p>Global interceptors and advisors can be added at the factory level. The specified
 * ones are expanded in an interceptor list where an "xxx*" entry is included in the
 * list, matching the given prefix with the bean names (e.g. "global*" would match
 * both "globalBean1" and "globalBean2", "*" all defined interceptors). The matching
 * interceptors get applied according to their returned order value, if they implement
 * the {@link cn.taketoday.core.Ordered} interface.
 *
 * <p>Creates a JDK proxy when proxy interfaces are given, and a CGLIB proxy for the
 * actual target class if not. Note that the latter will only work if the target class
 * does not have final methods, as a dynamic subclass will be created at runtime.
 *
 * <p>It's possible to cast a proxy obtained from this factory to {@link Advised},
 * or to obtain the ProxyFactoryBean reference and programmatically manipulate it.
 * This won't work for existing prototype references, which are independent. However,
 * it will work for prototypes subsequently obtained from the factory. Changes to
 * interception will work immediately on singletons (including existing references).
 * However, to change interfaces or target it's necessary to obtain a new instance
 * from the factory. This means that singleton instances obtained from the factory
 * do not have the same object identity. However, they do have the same interceptors
 * and target, and changing any reference will change all objects.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setInterceptorNames
 * @see #setProxyInterfaces
 * @see org.aopalliance.intercept.MethodInterceptor
 * @see Advisor
 * @see Advised
 */
@SuppressWarnings("serial")
public class ProxyFactoryBean extends ProxyCreatorSupport
        implements FactoryBean<Object>, BeanClassLoaderAware, BeanFactoryAware {

  private static final Logger logger = LoggerFactory.getLogger(ProxyFactoryBean.class);

  /**
   * This suffix in a value in an interceptor list indicates to expand globals.
   */
  public static final String GLOBAL_SUFFIX = "*";

  @Nullable
  private String[] interceptorNames;

  @Nullable
  private String targetName;

  private boolean autodetectInterfaces = true;

  private boolean singleton = true;

  private AdvisorAdapterRegistry advisorAdapterRegistry = DefaultAdvisorAdapterRegistry.getInstance();

  private boolean freezeProxy = false;

  @Nullable
  private transient ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

  private transient boolean classLoaderConfigured = false;

  @Nullable
  private transient BeanFactory beanFactory;

  /** Whether the advisor chain has already been initialized. */
  private boolean advisorChainInitialized = false;

  /** If this is a singleton, the cached singleton proxy instance. */
  @Nullable
  private Object singletonInstance;

  /**
   * Set the names of the interfaces we're proxying. If no interface
   * is given, a CGLIB for the actual class will be created.
   * <p>This is essentially equivalent to the "setInterfaces" method,
   * but mirrors TransactionProxyFactoryBean's "setProxyInterfaces".
   *
   * @see #setInterfaces
   * @see AbstractSingletonProxyFactoryBean#setProxyInterfaces
   */
  public void setProxyInterfaces(Class<?>[] proxyInterfaces) {
    setInterfaces(proxyInterfaces);
  }

  /**
   * Set the list of Advice/Advisor bean names. This must always be set
   * to use this factory bean in a bean factory.
   * <p>The referenced beans should be of type Interceptor, Advisor or Advice
   * The last entry in the list can be the name of any bean in the factory.
   * If it's neither an Advice nor an Advisor, a new SingletonTargetSource
   * is added to wrap it. Such a target bean cannot be used if the "target"
   * or "targetSource" or "targetName" property is set, in which case the
   * "interceptorNames" array must contain only Advice/Advisor bean names.
   * <p><b>NOTE: Specifying a target bean as final name in the "interceptorNames"
   * list is deprecated and will be removed in a future version.</b>
   * Use the {@link #setTargetName "gtargetName"} property instead.
   *
   * @see org.aopalliance.intercept.MethodInterceptor
   * @see cn.taketoday.aop.Advisor
   * @see org.aopalliance.aop.Advice
   * @see cn.taketoday.aop.target.SingletonTargetSource
   */
  public void setInterceptorNames(String... interceptorNames) {
    this.interceptorNames = interceptorNames;
  }

  /**
   * Set the name of the target bean. This is an alternative to specifying
   * the target name at the end of the "interceptorNames" array.
   * <p>You can also specify a target object or a TargetSource object
   * directly, via the "target"/"targetSource" property, respectively.
   *
   * @see #setInterceptorNames(String[])
   * @see #setTarget(Object)
   * @see #setTargetSource(cn.taketoday.aop.TargetSource)
   */
  public void setTargetName(@Nullable String targetName) {
    this.targetName = targetName;
  }

  /**
   * Set whether to autodetect proxy interfaces if none specified.
   * <p>Default is "true". Turn this flag off to create a CGLIB
   * proxy for the full target class if no interfaces specified.
   *
   * @see #setProxyTargetClass
   */
  public void setAutodetectInterfaces(boolean autodetectInterfaces) {
    this.autodetectInterfaces = autodetectInterfaces;
  }

  /**
   * Set the value of the singleton property. Governs whether this factory
   * should always return the same proxy instance (which implies the same target)
   * or whether it should return a new prototype instance, which implies that
   * the target and interceptors may be new instances also, if they are obtained
   * from prototype bean definitions. This allows for fine control of
   * independence/uniqueness in the object graph.
   */
  public void setSingleton(boolean singleton) {
    this.singleton = singleton;
  }

  /**
   * Specify the AdvisorAdapterRegistry to use.
   * Default is the global AdvisorAdapterRegistry.
   *
   * @see DefaultAdvisorAdapterRegistry
   */
  public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
    this.advisorAdapterRegistry = advisorAdapterRegistry;
  }

  @Override
  public void setFrozen(boolean frozen) {
    this.freezeProxy = frozen;
  }

  /**
   * Set the ClassLoader to generate the proxy class in.
   * <p>Default is the bean ClassLoader, i.e. the ClassLoader used by the
   * containing BeanFactory for loading all bean classes. This can be
   * overridden here for specific proxies.
   */
  public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
    this.proxyClassLoader = classLoader;
    this.classLoaderConfigured = classLoader != null;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    if (!this.classLoaderConfigured) {
      this.proxyClassLoader = classLoader;
    }
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    checkInterceptorNames();
  }

  /**
   * Return a proxy. Invoked when clients obtain beans from this factory bean.
   * Create an instance of the AOP proxy to be returned by this factory.
   * The instance will be cached for a singleton, and create on each call to
   * {@code getObject()} for a proxy.
   *
   * @return a fresh AOP proxy reflecting the current state of this factory
   */
  @Override
  @Nullable
  public Object getObject() throws BeansException {
    initializeAdvisorChain();
    if (isSingleton()) {
      return getSingletonInstance();
    }
    else {
      if (targetName == null) {
        logger.info("Using non-singleton proxies with singleton targets is often undesirable. " +
                "Enable prototype proxies by setting the 'targetName' property.");
      }
      return newPrototypeInstance();
    }
  }

  /**
   * Return the type of the proxy. Will check the singleton instance if
   * already created, else fall back to the proxy interface (in case of just
   * a single one), the target bean type, or the TargetSource's target class.
   *
   * @see cn.taketoday.aop.TargetSource#getTargetClass
   */
  @Override
  public Class<?> getObjectType() {
    synchronized(this) {
      if (singletonInstance != null) {
        return singletonInstance.getClass();
      }
    }
    Class<?>[] ifcs = getProxiedInterfaces();
    if (ifcs.length == 1) {
      return ifcs[0];
    }
    else if (ifcs.length > 1) {
      return createCompositeInterface(ifcs);
    }
    else if (targetName != null && beanFactory != null) {
      return beanFactory.getType(targetName);
    }
    else {
      return getTargetClass();
    }
  }

  @Override
  public boolean isSingleton() {
    return this.singleton;
  }

  /**
   * Create a composite interface Class for the given interfaces,
   * implementing the given interfaces in one single Class.
   * <p>The default implementation builds a JDK proxy class for the
   * given interfaces.
   *
   * @param interfaces the interfaces to merge
   * @return the merged interface as Class
   * @see java.lang.reflect.Proxy#getProxyClass
   */
  protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
    return ClassUtils.createCompositeInterface(interfaces, this.proxyClassLoader);
  }

  /**
   * Return the singleton instance of this class's proxy object,
   * lazily creating it if it hasn't been created already.
   *
   * @return the shared singleton proxy
   */
  private synchronized Object getSingletonInstance() {
    if (singletonInstance == null) {
      this.targetSource = freshTargetSource();
      if (autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
        // Rely on AOP infrastructure to tell us what interfaces to proxy.
        Class<?> targetClass = getTargetClass();
        if (targetClass == null) {
          throw new FactoryBeanNotInitializedException("Cannot determine target class for proxy");
        }
        setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, proxyClassLoader));
      }
      // Initialize the shared singleton instance.
      super.setFrozen(freezeProxy);
      this.singletonInstance = getProxy(createAopProxy());
    }
    return singletonInstance;
  }

  /**
   * Create a new prototype instance of this class's created proxy object,
   * backed by an independent AdvisedSupport configuration.
   *
   * @return a totally independent proxy, whose advice we may manipulate in isolation
   */
  private synchronized Object newPrototypeInstance() {
    // In the case of a prototype, we need to give the proxy
    // an independent instance of the configuration.
    // In this case, no proxy will have an instance of this object's configuration,
    // but will have an independent copy.
    ProxyCreatorSupport copy = new ProxyCreatorSupport(getAopProxyFactory());

    // The copy needs a fresh advisor chain, and a fresh TargetSource.
    TargetSource targetSource = freshTargetSource();
    copy.copyConfigurationFrom(this, targetSource, freshAdvisorChain());
    if (autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
      // Rely on AOP infrastructure to tell us what interfaces to proxy.
      Class<?> targetClass = targetSource.getTargetClass();
      if (targetClass != null) {
        copy.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, proxyClassLoader));
      }
    }
    copy.setFrozen(freezeProxy);

    return getProxy(copy.createAopProxy());
  }

  /**
   * Return the proxy object to expose.
   * <p>The default implementation uses a {@code getProxy} call with
   * the factory's bean class loader. Can be overridden to specify a
   * custom class loader.
   *
   * @param aopProxy the prepared AopProxy instance to get the proxy from
   * @return the proxy object to expose
   * @see AopProxy#getProxy(ClassLoader)
   */
  protected Object getProxy(AopProxy aopProxy) {
    return aopProxy.getProxy(proxyClassLoader);
  }

  /**
   * Check the interceptorNames list whether it contains a target name as final element.
   * If found, remove the final name from the list and set it as targetName.
   */
  private void checkInterceptorNames() {
    if (ObjectUtils.isNotEmpty(interceptorNames)) {
      String finalName = interceptorNames[interceptorNames.length - 1];
      if (targetName == null && targetSource == EMPTY_TARGET_SOURCE
              // The last name in the chain may be an Advisor/Advice or a target/TargetSource.
              // Unfortunately we don't know; we must look at type of the bean.
              && !finalName.endsWith(GLOBAL_SUFFIX) && !isNamedBeanAnAdvisorOrAdvice(finalName)) {
        // The target isn't an interceptor.
        this.targetName = finalName;
        if (logger.isDebugEnabled()) {
          logger.debug("Bean with name '{}' concluding interceptor chain " +
                  "is not an advisor class: treating it as a target or TargetSource", finalName);
        }
        this.interceptorNames = Arrays.copyOf(interceptorNames, interceptorNames.length - 1);
      }
    }
  }

  /**
   * Look at bean factory metadata to work out whether this bean name,
   * which concludes the interceptorNames list, is an Advisor or Advice,
   * or may be a target.
   *
   * @param beanName bean name to check
   * @return {@code true} if it's an Advisor or Advice
   */
  private boolean isNamedBeanAnAdvisorOrAdvice(String beanName) {
    Assert.state(beanFactory != null, "No BeanFactory set");
    Class<?> namedBeanClass = beanFactory.getType(beanName);
    if (namedBeanClass != null) {
      return Advisor.class.isAssignableFrom(namedBeanClass)
              || Advice.class.isAssignableFrom(namedBeanClass);
    }
    // Treat it as an target bean if we can't tell.
    if (logger.isDebugEnabled()) {
      logger.debug("Could not determine type of bean with name " +
              "'{}' - assuming it is neither an Advisor nor an Advice", beanName);
    }
    return false;
  }

  /**
   * Create the advisor (interceptor) chain. Advisors that are sourced
   * from a BeanFactory will be refreshed each time a new prototype instance
   * is added. Interceptors added programmatically through the factory API
   * are unaffected by such changes.
   */
  private synchronized void initializeAdvisorChain() throws AopConfigException, BeansException {
    if (this.advisorChainInitialized) {
      return;
    }

    if (ObjectUtils.isNotEmpty(interceptorNames)) {
      if (beanFactory == null) {
        throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
                "- cannot resolve interceptor names " + Arrays.asList(this.interceptorNames));
      }

      // Globals can't be last unless we specified a targetSource using the property...
      if (interceptorNames[interceptorNames.length - 1].endsWith(GLOBAL_SUFFIX)
              && targetName == null && targetSource == EMPTY_TARGET_SOURCE) {
        throw new AopConfigException("Target required after globals");
      }

      // Materialize interceptor chain from bean names.
      for (String name : interceptorNames) {
        if (name.endsWith(GLOBAL_SUFFIX)) {
          addGlobalAdvisors(beanFactory, name.substring(0, name.length() - GLOBAL_SUFFIX.length()));
        }
        else {
          // If we get here, we need to add a named interceptor.
          // We must check if it's a singleton or prototype.
          Object advice;
          if (singleton || beanFactory.isSingleton(name)) {
            // Add the real Advisor/Advice to the chain.
            advice = beanFactory.getBean(name);
          }
          else {
            // It's a prototype Advice or Advisor: replace with a prototype.
            // Avoid unnecessary creation of prototype bean just for advisor chain initialization.
            advice = new PrototypePlaceholderAdvisor(name);
          }
          addAdvisorOnChainCreation(advice);
        }
      }
    }

    this.advisorChainInitialized = true;
  }

  /**
   * Return an independent advisor chain.
   * We need to do this every time a new prototype instance is returned,
   * to return distinct instances of prototype Advisors and Advices.
   */
  private List<Advisor> freshAdvisorChain() {
    Advisor[] advisors = getAdvisors();
    ArrayList<Advisor> freshAdvisors = new ArrayList<>(advisors.length);
    for (Advisor advisor : advisors) {
      if (advisor instanceof PrototypePlaceholderAdvisor pa) {
        if (logger.isDebugEnabled()) {
          logger.debug("Refreshing bean named '{}'", pa.getBeanName());
        }
        // Replace the placeholder with a fresh prototype instance resulting from a getBean lookup
        if (beanFactory == null) {
          throw new IllegalStateException("No BeanFactory available anymore (probably due to " +
                  "serialization) - cannot resolve prototype advisor '" + pa.getBeanName() + "'");
        }
        Object bean = beanFactory.getBean(pa.getBeanName());
        Advisor refreshedAdvisor = namedBeanToAdvisor(bean);
        freshAdvisors.add(refreshedAdvisor);
      }
      else {
        // Add the shared instance.
        freshAdvisors.add(advisor);
      }
    }
    return freshAdvisors;
  }

  /**
   * Add all global interceptors and pointcuts.
   */
  private void addGlobalAdvisors(BeanFactory beanFactory, String prefix) {
    Set<String> globalAdvisorNames =
            BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Advisor.class);
    Set<String> globalInterceptorNames =
            BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Interceptor.class);
    if (globalAdvisorNames.size() > 0 || globalInterceptorNames.size() > 0) {
      ArrayList<Object> beans = new ArrayList<>(globalAdvisorNames.size() + globalInterceptorNames.size());
      for (String name : globalAdvisorNames) {
        if (name.startsWith(prefix)) {
          beans.add(beanFactory.getBean(name));
        }
      }
      for (String name : globalInterceptorNames) {
        if (name.startsWith(prefix)) {
          beans.add(beanFactory.getBean(name));
        }
      }
      AnnotationAwareOrderComparator.sort(beans);
      for (Object bean : beans) {
        addAdvisorOnChainCreation(bean);
      }
    }
  }

  /**
   * Invoked when advice chain is created.
   * <p>Add the given advice, advisor or object to the interceptor list.
   * Because of these three possibilities, we can't type the signature
   * more strongly.
   *
   * @param next advice, advisor or target object
   */
  private void addAdvisorOnChainCreation(Object next) {
    // We need to convert to an Advisor if necessary so that our source reference
    // matches what we find from superclass interceptors.
    addAdvisor(namedBeanToAdvisor(next));
  }

  /**
   * Return a TargetSource to use when creating a proxy. If the target was not
   * specified at the end of the interceptorNames list, the TargetSource will be
   * this class's TargetSource member. Otherwise, we get the target bean and wrap
   * it in a TargetSource if necessary.
   */
  private TargetSource freshTargetSource() {
    if (targetName == null) {
      // Not refreshing target: bean name not specified in 'interceptorNames'
      return targetSource;
    }
    else {
      if (beanFactory == null) {
        throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
                "- cannot resolve target with name '" + targetName + "'");
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Refreshing target with name '{}'", targetName);
      }
      Object target = beanFactory.getBean(targetName);
      return target instanceof TargetSource ? (TargetSource) target : new SingletonTargetSource(target);
    }
  }

  /**
   * Convert the following object sourced from calling getBean() on a name in the
   * interceptorNames array to an Advisor or TargetSource.
   */
  private Advisor namedBeanToAdvisor(Object next) {
    try {
      return advisorAdapterRegistry.wrap(next);
    }
    catch (UnknownAdviceTypeException ex) {
      // We expected this to be an Advisor or Advice,
      // but it wasn't. This is a configuration error.
      throw new AopConfigException("Unknown advisor type " + next.getClass() +
              "; can only include Advisor or Advice type beans in interceptorNames chain " +
              "except for last entry which may also be target instance or TargetSource", ex);
    }
  }

  /**
   * Blow away and recache singleton on an advice change.
   */
  @Override
  protected void adviceChanged() {
    super.adviceChanged();
    if (singleton) {
      logger.debug("Advice has changed; re-caching singleton instance");
      synchronized(this) {
        this.singletonInstance = null;
      }
    }
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization; just initialize state after deserialization.
    ois.defaultReadObject();

    // Initialize transient fields.
    this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
  }

  /**
   * Used in the interceptor chain where we need to replace a bean with a prototype
   * on creating a proxy.
   */
  private static class PrototypePlaceholderAdvisor implements Advisor, Serializable {

    private final String message;
    private final String beanName;

    public PrototypePlaceholderAdvisor(String beanName) {
      this.beanName = beanName;
      this.message = "Placeholder for prototype Advisor/Advice with bean name '" + beanName + "'";
    }

    public String getBeanName() {
      return this.beanName;
    }

    @Override
    public Advice getAdvice() {
      throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
    }

    @Override
    public boolean isPerInstance() {
      throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
    }

    @Override
    public String toString() {
      return this.message;
    }
  }

}
