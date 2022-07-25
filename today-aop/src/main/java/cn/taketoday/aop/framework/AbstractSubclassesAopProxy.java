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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import cn.taketoday.bytecode.core.CodeGenerationException;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author TODAY 2021/3/7 11:45
 * @since 3.0
 */
public abstract class AbstractSubclassesAopProxy implements AopProxy {
  private static final Logger log = LoggerFactory.getLogger(AbstractSubclassesAopProxy.class);

  /** Keeps track of the Classes that we have validated for final methods. */
  private static final Map<Class<?>, Boolean> validatedClasses = new WeakHashMap<>();

  /** The configuration used to configure this proxy. */
  final AdvisedSupport config;

  protected Object[] constructorArgs;
  protected Class<?>[] constructorArgTypes;

  /**
   * Create a new AopProxy for the given AOP configuration.
   *
   * @param config the AOP configuration as AdvisedSupport object
   * @throws AopConfigException if the config is invalid. We try to throw an informative
   * exception in this case, rather than let a mysterious failure
   * happen later.
   */
  public AbstractSubclassesAopProxy(AdvisedSupport config) {
    Assert.notNull(config, "AdvisedSupport must not be null");
    if (config.getAdvisors().length == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
      throw new AopConfigException("No advisors and no TargetSource specified");
    }
    this.config = config;
  }

  /**
   * Set constructor arguments to use for creating the proxy.
   *
   * @param constructorArgs the constructor argument values
   * @param constructorArgTypes the constructor argument types
   */
  public void setConstructorArguments(Object[] constructorArgs, Class<?>[] constructorArgTypes) {
    if (constructorArgs == null || constructorArgTypes == null) {
      throw new IllegalArgumentException("Both 'constructorArgs' and 'constructorArgTypes' need to be specified");
    }
    if (constructorArgs.length != constructorArgTypes.length) {
      throw new IllegalArgumentException(
              "Number of 'constructorArgs' (" + constructorArgs.length +
                      ") must match number of 'constructorArgTypes' (" + constructorArgTypes.length + ")");
    }
    this.constructorArgs = constructorArgs;
    this.constructorArgTypes = constructorArgTypes;
  }

  @Override
  public Object getProxy() {
    return getProxy(null);
  }

  @Override
  public Object getProxy(ClassLoader classLoader) {
    try {
      Class<?> rootClass = config.getTargetClass();
      Assert.state(rootClass != null, "Target class must be available for creating a CGLIB proxy");

      Class<?> proxySuperClass = getProxySuperClass(rootClass);

      // Validate the class, writing log messages as necessary.
      validateClassIfNecessary(proxySuperClass, classLoader);

      return getProxyInternal(proxySuperClass, classLoader);
    }
    catch (CodeGenerationException | IllegalArgumentException ex) {
      throw new AopConfigException(
              "Could not generate subclass of " + config.getTargetClass() +
                      ": Common causes of this problem include using a final class or a non-visible class",
              ex);
    }
    catch (Throwable ex) {
      // TargetSource.getTarget() failed
      throw new AopConfigException("Unexpected AOP exception", ex);
    }
  }

  protected Class<?> getProxySuperClass(Class<?> rootClass) {
    Class<?> proxySuperClass = rootClass;
    if (rootClass.getName().contains("$$")) {
      proxySuperClass = rootClass.getSuperclass();
      Class<?>[] additionalInterfaces = rootClass.getInterfaces();
      for (Class<?> additionalInterface : additionalInterfaces) {
        this.config.addInterface(additionalInterface);
      }
    }
    return proxySuperClass;
  }

  protected abstract Object getProxyInternal(
          Class<?> proxySuperClass, ClassLoader loader) throws Exception;

  /**
   * Checks to see whether the supplied {@code Class} has already been validated
   * and validates it if not.
   */
  void validateClassIfNecessary(Class<?> proxySuperClass, ClassLoader proxyClassLoader) {
    if (!this.config.isOptimize() && log.isInfoEnabled()) {
      synchronized(validatedClasses) {
        if (!validatedClasses.containsKey(proxySuperClass)) {
          doValidateClass(proxySuperClass, proxyClassLoader,
                  ClassUtils.getAllInterfacesForClassAsSet(proxySuperClass));
          validatedClasses.put(proxySuperClass, Boolean.TRUE);
        }
      }
    }
  }

  /**
   * Checks for final methods on the given {@code Class}, as well as
   * package-visible methods across ClassLoaders, and writes warnings to the log
   * for each one found.
   */
  void doValidateClass(Class<?> proxySuperClass, ClassLoader proxyClassLoader, Set<Class<?>> ifcs) {
    if (proxySuperClass != Object.class) {
      Method[] methods = proxySuperClass.getDeclaredMethods();
      for (Method method : methods) {
        int mod = method.getModifiers();
        if (!Modifier.isStatic(mod) && !Modifier.isPrivate(mod)) {
          if (Modifier.isFinal(mod)) {
            if (log.isInfoEnabled() && implementsInterface(method, ifcs)) {
              log.info("Unable to proxy interface-implementing method [{}] because " +
                      "it is marked as final: Consider using interface-based JDK proxies instead!", method);
            }
            if (log.isDebugEnabled()) {
              log.debug("Final method [{}] cannot get proxied via CGLIB: " +
                      "Calls to this method will NOT be routed to the target instance and " +
                      "might lead to NPEs against uninitialized fields in the proxy instance.", method);
            }
          }
          else if (log.isDebugEnabled() && !Modifier.isPublic(mod) && !Modifier.isProtected(mod)
                  && proxyClassLoader != null && proxySuperClass.getClassLoader() != proxyClassLoader) {
            log.debug("Method [{}] is package-visible across different ClassLoaders " +
                    "and cannot get proxied via CGLIB: Declare this method as public or protected " +
                    "if you need to support invocations through the proxy.", method);
          }
        }
      }
      doValidateClass(proxySuperClass.getSuperclass(), proxyClassLoader, ifcs);
    }
  }

  /**
   * Check whether the given method is declared on any of the given interfaces.
   */
  static boolean implementsInterface(Method method, Set<Class<?>> ifcs) {
    for (Class<?> ifc : ifcs) {
      if (ReflectionUtils.hasMethod(ifc, method)) {
        return true;
      }
    }
    return false;
  }

}
