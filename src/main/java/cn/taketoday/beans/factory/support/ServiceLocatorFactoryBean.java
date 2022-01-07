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
package cn.taketoday.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.BeansException;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * A {@link FactoryBean} implementation that takes an interface which must have one or more
 * methods with the signatures {@code MyType xxx()} or {@code MyType xxx(MyIdType id)}
 * (typically, {@code MyService getService()} or {@code MyService getService(String id)})
 * and creates a dynamic proxy which implements that interface, delegating to an
 * underlying {@link BeanFactory}.
 *
 * <p>Such service locators permit the decoupling of calling code from
 * the {@link BeanFactory} API, by using an
 * appropriate custom locator interface. They will typically be used for
 * <b>prototype beans</b>, i.e. for factory methods that are supposed to
 * return a new instance for each call. The client receives a reference to the
 * service locator via setter or constructor injection, to be able to invoke
 * the locator's factory methods on demand. <b>For singleton beans, direct
 * setter or constructor injection of the target bean is preferable.</b>
 *
 * <p>On invocation of the no-arg factory method, or the single-arg factory
 * method with a String id of {@code null} or empty String, if exactly
 * <b>one</b> bean in the factory matches the return type of the factory
 * method, that bean is returned, otherwise a {@link NoSuchBeanDefinitionException}
 * is thrown.
 *
 * <p>On invocation of the single-arg factory method with a non-null (and
 * non-empty) argument, the proxy returns the result of a
 * {@link BeanFactory#getBean(String)} call, using a stringified version
 * of the passed-in id as bean name.
 *
 * <p>A factory method argument will usually be a String, but can also be an
 * int or a custom enumeration type, for example, stringified via
 * {@code toString}. The resulting String can be used as bean name as-is,
 * provided that corresponding beans are defined in the bean factory.
 * Alternatively, {@linkplain #setServiceMappings(java.util.Properties) a custom
 * mapping} between service IDs and bean names can be defined.
 *
 * <p>By way of an example, consider the following service locator interface.
 * Note that this interface is not dependent on any Framework APIs.
 *
 * <pre class="code">package a.b.c;
 *
 * public interface ServiceFactory {
 *
 *    public MyService getService();
 * }</pre>
 *
 * <p>A sample config in an XML-based {@link BeanFactory} might look as follows:
 *
 * <pre class="code">&lt;beans&gt;
 *
 *   &lt;!-- Prototype bean since we have state --&gt;
 *   &lt;bean id="myService" class="a.b.c.MyService" singleton="false"/&gt;
 *
 *   &lt;!-- will lookup the above 'myService' bean by *TYPE* --&gt;
 *   &lt;bean id="myServiceFactory"
 *            class="cn.taketoday.beans.factory.support.ServiceLocatorFactoryBean"&gt;
 *     &lt;property name="serviceLocatorInterface" value="a.b.c.ServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean"&gt;
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 * &lt;/beans&gt;</pre>
 *
 * <p>The attendant {@code MyClientBean} class implementation might then
 * look something like this:
 *
 * <pre class="code">package a.b.c;
 *
 * public class MyClientBean {
 *
 *    private ServiceFactory myServiceFactory;
 *
 *    // actual implementation provided by the IoC
 *    public void setServiceFactory(ServiceFactory myServiceFactory) {
 *        this.myServiceFactory = myServiceFactory;
 *    }
 *
 *    public void someBusinessMethod() {
 *        // get a 'fresh', brand new MyService instance
 *        MyService service = this.myServiceFactory.getService();
 *        // use the service object to effect the business logic...
 *    }
 * }</pre>
 *
 * <p>By way of an example that looks up a bean <b>by name</b>, consider
 * the following service locator interface. Again, note that this
 * interface is not dependent on any Framework APIs.
 *
 * <pre class="code">package a.b.c;
 *
 * public interface ServiceFactory {
 *
 *    public MyService getService (String serviceName);
 * }</pre>
 *
 * <p>A sample config in an XML-based {@link BeanFactory} might look as follows:
 *
 * <pre class="code">&lt;beans&gt;
 *
 *   &lt;!-- Prototype beans since we have state (both extend MyService) --&gt;
 *   &lt;bean id="specialService" class="a.b.c.SpecialService" singleton="false"/&gt;
 *   &lt;bean id="anotherService" class="a.b.c.AnotherService" singleton="false"/&gt;
 *
 *   &lt;bean id="myServiceFactory"
 *            class="cn.taketoday.beans.factory.support.ServiceLocatorFactoryBean"&gt;
 *     &lt;property name="serviceLocatorInterface" value="a.b.c.ServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean"&gt;
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 * &lt;/beans&gt;</pre>
 *
 * <p>The attendant {@code MyClientBean} class implementation might then
 * look something like this:
 *
 * <pre class="code">package a.b.c;
 *
 * public class MyClientBean {
 *
 *    private ServiceFactory myServiceFactory;
 *
 *    // actual implementation provided by the IoC
 *    public void setServiceFactory(ServiceFactory myServiceFactory) {
 *        this.myServiceFactory = myServiceFactory;
 *    }
 *
 *    public void someBusinessMethod() {
 *        // get a 'fresh', brand new MyService instance
 *        MyService service = this.myServiceFactory.getService("specialService");
 *        // use the service object to effect the business logic...
 *    }
 *
 *    public void anotherBusinessMethod() {
 *        // get a 'fresh', brand new MyService instance
 *        MyService service = this.myServiceFactory.getService("anotherService");
 *        // use the service object to effect the business logic...
 *    }
 * }</pre>
 *
 * <p>See {@link SupplierFactoryCreatingFactoryBean} for an alternate approach.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setServiceLocatorInterface
 * @see #setServiceMappings
 * @see SupplierFactoryCreatingFactoryBean
 * @since 4.0 2021/11/30 14:26
 */
public class ServiceLocatorFactoryBean implements FactoryBean<Object>, BeanFactoryAware, InitializingBean {

  @Nullable
  private Class<?> serviceLocatorInterface;

  @Nullable
  private Constructor<Exception> serviceLocatorExceptionConstructor;

  @Nullable
  private Properties serviceMappings;

  @Nullable
  private BeanFactory beanFactory;

  @Nullable
  private Object proxy;

  /**
   * Set the service locator interface to use, which must have one or more methods with
   * the signatures {@code MyType xxx()} or {@code MyType xxx(MyIdType id)}
   * (typically, {@code MyService getService()} or {@code MyService getService(String id)}).
   * See the {@link ServiceLocatorFactoryBean class-level Javadoc} for
   * information on the semantics of such methods.
   */
  public void setServiceLocatorInterface(@Nullable Class<?> interfaceType) {
    this.serviceLocatorInterface = interfaceType;
  }

  /**
   * Set the exception class that the service locator should throw if service
   * lookup failed. The specified exception class must have a constructor
   * with one of the following parameter types: {@code (String, Throwable)}
   * or {@code (Throwable)} or {@code (String)}.
   * <p>If not specified, subclasses of  BeansException will be thrown,
   * for example NoSuchBeanDefinitionException. As those are unchecked, the
   * caller does not need to handle them, so it might be acceptable that
   * Framework exceptions get thrown as long as they are just handled generically.
   *
   * @see #determineServiceLocatorExceptionConstructor
   * @see #createServiceLocatorException
   */
  public void setServiceLocatorExceptionClass(Class<? extends Exception> serviceLocatorExceptionClass) {
    this.serviceLocatorExceptionConstructor =
            determineServiceLocatorExceptionConstructor(serviceLocatorExceptionClass);
  }

  /**
   * Set mappings between service ids (passed into the service locator)
   * and bean names (in the bean factory). Service ids that are not defined
   * here will be treated as bean names as-is.
   * <p>The empty string as service id key defines the mapping for {@code null} and
   * empty string, and for factory methods without parameter. If not defined,
   * a single matching bean will be retrieved from the bean factory.
   *
   * @param serviceMappings mappings between service ids and bean names,
   * with service ids as keys as bean names as values
   */
  public void setServiceMappings(@Nullable Properties serviceMappings) {
    this.serviceMappings = serviceMappings;
  }

  @Override
  public void setBeanFactory(@Nullable BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  @Override
  public void afterPropertiesSet() {
    if (this.serviceLocatorInterface == null) {
      throw new IllegalArgumentException("Property 'serviceLocatorInterface' is required");
    }

    // Create service locator proxy.
    this.proxy = Proxy.newProxyInstance(
            this.serviceLocatorInterface.getClassLoader(),
            new Class<?>[] { this.serviceLocatorInterface },
            new ServiceLocatorInvocationHandler());
  }

  /**
   * Determine the constructor to use for the given service locator exception
   * class. Only called in case of a custom service locator exception.
   * <p>The default implementation looks for a constructor with one of the
   * following parameter types: {@code (String, Throwable)}
   * or {@code (Throwable)} or {@code (String)}.
   *
   * @param exceptionClass the exception class
   * @return the constructor to use
   * @see #setServiceLocatorExceptionClass
   */
  @SuppressWarnings("unchecked")
  protected Constructor<Exception> determineServiceLocatorExceptionConstructor(Class<? extends Exception> exceptionClass) {
    try {
      return (Constructor<Exception>) exceptionClass.getConstructor(String.class, Throwable.class);
    }
    catch (NoSuchMethodException ex) {
      try {
        return (Constructor<Exception>) exceptionClass.getConstructor(Throwable.class);
      }
      catch (NoSuchMethodException ex2) {
        try {
          return (Constructor<Exception>) exceptionClass.getConstructor(String.class);
        }
        catch (NoSuchMethodException ex3) {
          throw new IllegalArgumentException(
                  "Service locator exception [" + exceptionClass.getName() +
                          "] neither has a (String, Throwable) constructor nor a (String) constructor");
        }
      }
    }
  }

  /**
   * Create a service locator exception for the given cause.
   * Only called in case of a custom service locator exception.
   * <p>The default implementation can handle all variations of
   * message and exception arguments.
   *
   * @param exceptionConstructor the constructor to use
   * @param cause the cause of the service lookup failure
   * @return the service locator exception to throw
   * @see #setServiceLocatorExceptionClass
   */
  protected Exception createServiceLocatorException(Constructor<Exception> exceptionConstructor, BeansException cause) {
    Class<?>[] paramTypes = exceptionConstructor.getParameterTypes();
    Object[] args = new Object[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      if (String.class == paramTypes[i]) {
        args[i] = cause.getMessage();
      }
      else if (paramTypes[i].isInstance(cause)) {
        args[i] = cause;
      }
    }
    return BeanUtils.newInstance(exceptionConstructor, args);
  }

  @Override
  @Nullable
  public Object getObject() {
    return this.proxy;
  }

  @Override
  public Class<?> getObjectType() {
    return this.serviceLocatorInterface;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  /**
   * Invocation handler that delegates service locator calls to the bean factory.
   */
  private class ServiceLocatorInvocationHandler implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (ReflectionUtils.isEqualsMethod(method)) {
        // Only consider equal when proxies are identical.
        return (proxy == args[0]);
      }
      else if (ReflectionUtils.isHashCodeMethod(method)) {
        // Use hashCode of service locator proxy.
        return System.identityHashCode(proxy);
      }
      else if (ReflectionUtils.isToStringMethod(method)) {
        return "Service locator: " + serviceLocatorInterface;
      }
      else {
        return invokeServiceLocatorMethod(method, args);
      }
    }

    private Object invokeServiceLocatorMethod(Method method, Object[] args) throws Exception {
      Class<?> serviceLocatorMethodReturnType = getServiceLocatorMethodReturnType(method);
      try {
        String beanName = tryGetBeanName(args);
        Assert.state(beanFactory != null, "No BeanFactory available");
        if (StringUtils.isNotEmpty(beanName)) {
          // Service locator for a specific bean name
          return BeanFactoryUtils.requiredBean(beanFactory, beanName, serviceLocatorMethodReturnType);
        }
        else {
          // Service locator for a bean type
          return BeanFactoryUtils.requiredBean(beanFactory, serviceLocatorMethodReturnType);
        }
      }
      catch (BeansException ex) {
        if (serviceLocatorExceptionConstructor != null) {
          throw createServiceLocatorException(serviceLocatorExceptionConstructor, ex);
        }
        throw ex;
      }
    }

    /**
     * Check whether a service id was passed in.
     */
    private String tryGetBeanName(@Nullable Object[] args) {
      String beanName = Constant.BLANK;
      if (args != null && args.length == 1 && args[0] != null) {
        beanName = args[0].toString();
      }
      // Look for explicit serviceId-to-beanName mappings.
      if (serviceMappings != null) {
        String mappedName = serviceMappings.getProperty(beanName);
        if (mappedName != null) {
          beanName = mappedName;
        }
      }
      return beanName;
    }

    private Class<?> getServiceLocatorMethodReturnType(Method method) throws NoSuchMethodException {
      Assert.state(serviceLocatorInterface != null, "No service locator interface specified");
      Class<?>[] paramTypes = method.getParameterTypes();
      Method interfaceMethod = serviceLocatorInterface.getMethod(method.getName(), paramTypes);
      Class<?> serviceLocatorReturnType = interfaceMethod.getReturnType();

      // Check whether the method is a valid service locator.
      if (paramTypes.length > 1 || void.class == serviceLocatorReturnType) {
        throw new UnsupportedOperationException(
                "May only call methods with signature '<type> xxx()' or '<type> xxx(<idtype> id)' " +
                        "on factory interface, but tried to call: " + interfaceMethod);
      }
      return serviceLocatorReturnType;
    }
  }

}
