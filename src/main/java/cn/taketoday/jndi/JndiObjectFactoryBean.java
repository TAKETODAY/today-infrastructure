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

package cn.taketoday.jndi;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.naming.Context;
import javax.naming.NamingException;

import cn.taketoday.aop.proxy.ProxyFactory;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * {@link cn.taketoday.beans.factory.FactoryBean} that looks up a
 * JNDI object. Exposes the object found in JNDI for bean references,
 * e.g. for data access object's "dataSource" property in case of a
 * {@link javax.sql.DataSource}.
 *
 * <p>The typical usage will be to register this as singleton factory
 * (e.g. for a certain JNDI-bound DataSource) in an application context,
 * and give bean references to application services that need it.
 *
 * <p>The default behavior is to look up the JNDI object on startup and cache it.
 * This can be customized through the "lookupOnStartup" and "cache" properties,
 * using a {@link JndiObjectTargetSource} underneath. Note that you need to specify
 * a "proxyInterface" in such a scenario, since the actual JNDI object type is not
 * known in advance.
 *
 * <p>Of course, bean classes in a Framework environment may lookup e.g. a DataSource
 * from JNDI themselves. This class simply enables central configuration of the
 * JNDI name, and easy switching to non-JNDI alternatives. The latter is
 * particularly convenient for test setups, reuse in standalone clients, etc.
 *
 * <p>Note that switching to e.g. DriverManagerDataSource is just a matter of
 * configuration: Simply replace the definition of this FactoryBean with a
 * {@link cn.taketoday.jdbc.datasource.DriverManagerDataSource} definition!
 *
 * @author Juergen Hoeller
 * @see #setProxyInterface
 * @see #setLookupOnStartup
 * @see #setCache
 * @see JndiObjectTargetSource
 * @since 22.05.2003
 */
public class JndiObjectFactoryBean extends JndiObjectLocator
        implements FactoryBean<Object>, BeanFactoryAware, BeanClassLoaderAware {

  @Nullable
  private Class<?>[] proxyInterfaces;

  private boolean lookupOnStartup = true;

  private boolean cache = true;

  private boolean exposeAccessContext = false;

  @Nullable
  private Object defaultObject;

  @Nullable
  private ConfigurableBeanFactory beanFactory;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  @Nullable
  private Object jndiObject;

  /**
   * Specify the proxy interface to use for the JNDI object.
   * <p>Typically used in conjunction with "lookupOnStartup"=false and/or "cache"=false.
   * Needs to be specified because the actual JNDI object type is not known
   * in advance in case of a lazy lookup.
   *
   * @see #setProxyInterfaces
   * @see #setLookupOnStartup
   * @see #setCache
   */
  public void setProxyInterface(Class<?> proxyInterface) {
    this.proxyInterfaces = new Class<?>[] { proxyInterface };
  }

  /**
   * Specify multiple proxy interfaces to use for the JNDI object.
   * <p>Typically used in conjunction with "lookupOnStartup"=false and/or "cache"=false.
   * Note that proxy interfaces will be autodetected from a specified "expectedType",
   * if necessary.
   *
   * @see #setExpectedType
   * @see #setLookupOnStartup
   * @see #setCache
   */
  public void setProxyInterfaces(Class<?>... proxyInterfaces) {
    this.proxyInterfaces = proxyInterfaces;
  }

  /**
   * Set whether to look up the JNDI object on startup. Default is "true".
   * <p>Can be turned off to allow for late availability of the JNDI object.
   * In this case, the JNDI object will be fetched on first access.
   * <p>For a lazy lookup, a proxy interface needs to be specified.
   *
   * @see #setProxyInterface
   * @see #setCache
   */
  public void setLookupOnStartup(boolean lookupOnStartup) {
    this.lookupOnStartup = lookupOnStartup;
  }

  /**
   * Set whether to cache the JNDI object once it has been located.
   * Default is "true".
   * <p>Can be turned off to allow for hot redeployment of JNDI objects.
   * In this case, the JNDI object will be fetched for each invocation.
   * <p>For hot redeployment, a proxy interface needs to be specified.
   *
   * @see #setProxyInterface
   * @see #setLookupOnStartup
   */
  public void setCache(boolean cache) {
    this.cache = cache;
  }

  /**
   * Set whether to expose the JNDI environment context for all access to the target
   * object, i.e. for all method invocations on the exposed object reference.
   * <p>Default is "false", i.e. to only expose the JNDI context for object lookup.
   * Switch this flag to "true" in order to expose the JNDI environment (including
   * the authorization context) for each method invocation, as needed by WebLogic
   * for JNDI-obtained factories (e.g. JDBC DataSource, JMS ConnectionFactory)
   * with authorization requirements.
   */
  public void setExposeAccessContext(boolean exposeAccessContext) {
    this.exposeAccessContext = exposeAccessContext;
  }

  /**
   * Specify a default object to fall back to if the JNDI lookup fails.
   * Default is none.
   * <p>This can be an arbitrary bean reference or literal value.
   * It is typically used for literal values in scenarios where the JNDI environment
   * might define specific config settings but those are not required to be present.
   * <p>Note: This is only supported for lookup on startup.
   * If specified together with {@link #setExpectedType}, the specified value
   * needs to be either of that type or convertible to it.
   *
   * @see #setLookupOnStartup
   * @see ConfigurableBeanFactory#getConversionService()
   * @see DefaultConversionService
   */
  public void setDefaultObject(Object defaultObject) {
    this.defaultObject = defaultObject;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (beanFactory instanceof ConfigurableBeanFactory) {
      // Just optional - for getting a specifically configured TypeConverter if needed.
      // We'll simply fall back to a SimpleTypeConverter if no specific one available.
      this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    }
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  /**
   * Look up the JNDI object and store it.
   */
  @Override
  public void afterPropertiesSet() throws IllegalArgumentException, NamingException {
    super.afterPropertiesSet();

    if (this.proxyInterfaces != null || !this.lookupOnStartup || !this.cache || this.exposeAccessContext) {
      // We need to create a proxy for this...
      if (this.defaultObject != null) {
        throw new IllegalArgumentException(
                "'defaultObject' is not supported in combination with 'proxyInterface'");
      }
      // We need a proxy and a JndiObjectTargetSource.
      this.jndiObject = JndiObjectProxyFactory.createJndiObjectProxy(this);
    }
    else {
      if (this.defaultObject != null && getExpectedType() != null
              && !getExpectedType().isInstance(this.defaultObject)) {
        ConversionService conversionService = getConversionService();
        try {
          this.defaultObject = conversionService.convert(this.defaultObject, getExpectedType());
        }
        catch (ConversionException ex) {
          throw new IllegalArgumentException("Default object [" + this.defaultObject + "] of type [" +
                  this.defaultObject.getClass().getName() + "] is not of expected type [" +
                  getExpectedType().getName() + "] and cannot be converted either", ex);
        }
      }
      // Locate specified JNDI object.
      this.jndiObject = lookupWithFallback();
    }
  }

  private ConversionService getConversionService() {
    ConversionService conversionService = null;
    if (beanFactory != null) {
      conversionService = this.beanFactory.getConversionService();
    }
    if (conversionService == null) {
      conversionService = DefaultConversionService.getSharedInstance();
    }
    return conversionService;
  }

  /**
   * Lookup variant that returns the specified "defaultObject"
   * (if any) in case of lookup failure.
   *
   * @return the located object, or the "defaultObject" as fallback
   * @throws NamingException in case of lookup failure without fallback
   * @see #setDefaultObject
   */
  protected Object lookupWithFallback() throws NamingException {
    ClassLoader originalClassLoader = ClassUtils.overrideThreadContextClassLoader(this.beanClassLoader);
    try {
      return lookup();
    }
    catch (TypeMismatchNamingException ex) {
      // Always let TypeMismatchNamingException through -
      // we don't want to fall back to the defaultObject in this case.
      throw ex;
    }
    catch (NamingException ex) {
      if (this.defaultObject != null) {
        if (logger.isTraceEnabled()) {
          logger.trace("JNDI lookup failed - returning specified default object instead", ex);
        }
        else if (logger.isDebugEnabled()) {
          logger.debug("JNDI lookup failed - returning specified default object instead: {}", ex.toString());
        }
        return this.defaultObject;
      }
      throw ex;
    }
    finally {
      if (originalClassLoader != null) {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
      }
    }
  }

  /**
   * Return the singleton JNDI object.
   */
  @Override
  @Nullable
  public Object getObject() {
    return this.jndiObject;
  }

  @Override
  public Class<?> getObjectType() {
    if (this.proxyInterfaces != null) {
      if (this.proxyInterfaces.length == 1) {
        return this.proxyInterfaces[0];
      }
      else if (this.proxyInterfaces.length > 1) {
        return createCompositeInterface(this.proxyInterfaces);
      }
    }
    if (this.jndiObject != null) {
      return this.jndiObject.getClass();
    }
    else {
      return getExpectedType();
    }
  }

  @Override
  public boolean isSingleton() {
    return true;
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
  @SuppressWarnings("deprecation")
  protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
    return ClassUtils.createCompositeInterface(interfaces, this.beanClassLoader);
  }

  /**
   * Inner class to just introduce an AOP dependency when actually creating a proxy.
   */
  private static class JndiObjectProxyFactory {

    private static Object createJndiObjectProxy(JndiObjectFactoryBean jof) throws NamingException {
      // Create a JndiObjectTargetSource that mirrors the JndiObjectFactoryBean's configuration.
      JndiObjectTargetSource targetSource = new JndiObjectTargetSource();
      targetSource.setJndiTemplate(jof.getJndiTemplate());
      String jndiName = jof.getJndiName();
      Assert.state(jndiName != null, "No JNDI name specified");
      targetSource.setJndiName(jndiName);
      targetSource.setExpectedType(jof.getExpectedType());
      targetSource.setResourceRef(jof.isResourceRef());
      targetSource.setLookupOnStartup(jof.lookupOnStartup);
      targetSource.setCache(jof.cache);
      targetSource.afterPropertiesSet();

      // Create a proxy with JndiObjectFactoryBean's proxy interface and the JndiObjectTargetSource.
      ProxyFactory proxyFactory = new ProxyFactory();
      if (jof.proxyInterfaces != null) {
        proxyFactory.setInterfaces(jof.proxyInterfaces);
      }
      else {
        Class<?> targetClass = targetSource.getTargetClass();
        if (targetClass == null) {
          throw new IllegalStateException(
                  "Cannot deactivate 'lookupOnStartup' without specifying a 'proxyInterface' or 'expectedType'");
        }
        Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(targetClass, jof.beanClassLoader);
        for (Class<?> ifc : ifcs) {
          if (Modifier.isPublic(ifc.getModifiers())) {
            proxyFactory.addInterface(ifc);
          }
        }
      }
      if (jof.exposeAccessContext) {
        proxyFactory.addAdvice(new JndiContextExposingInterceptor(jof.getJndiTemplate()));
      }
      proxyFactory.setTargetSource(targetSource);
      return proxyFactory.getProxy(jof.beanClassLoader);
    }
  }

  /**
   * Interceptor that exposes the JNDI context for all method invocations,
   * according to JndiObjectFactoryBean's "exposeAccessContext" flag.
   */
  private record JndiContextExposingInterceptor(JndiTemplate jndiTemplate) implements MethodInterceptor {

    @Override
    @Nullable
    public Object invoke(MethodInvocation invocation) throws Throwable {
      Context ctx = isEligible(invocation.getMethod()) ? this.jndiTemplate.getContext() : null;
      try {
        return invocation.proceed();
      }
      finally {
        this.jndiTemplate.releaseContext(ctx);
      }
    }

    private boolean isEligible(Method method) {
      return Object.class != method.getDeclaringClass();
    }
  }

}
