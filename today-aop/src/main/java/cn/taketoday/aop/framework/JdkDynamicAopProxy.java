/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.aopalliance.intercept.MethodInterceptor;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import cn.taketoday.aop.AopInvocationException;
import cn.taketoday.aop.RawTargetAccess;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.core.DecoratingProxy;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * JDK-based {@link AopProxy} implementation for the AOP framework,
 * based on JDK {@link java.lang.reflect.Proxy dynamic proxies}.
 *
 * <p>Creates a dynamic proxy, implementing the interfaces exposed by
 * the AopProxy. Dynamic proxies <i>cannot</i> be used to proxy methods
 * defined in classes, rather than interfaces.
 *
 * <p>Objects of this type should be obtained through proxy factories,
 * configured by an {@link AdvisedSupport} class. This class is internal
 * to AOP framework and need not be used directly by client code.
 *
 * <p>Proxies created using this class will be thread-safe if the
 * underlying (target) class is thread-safe.
 *
 * <p>Proxies are serializable so long as all Advisors (including Advices
 * and Pointcuts) and the TargetSource are serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author TODAY 2021/2/1 22:42
 * @see java.lang.reflect.Proxy
 * @see AdvisedSupport
 * @see ProxyFactory
 * @since 3.0
 */
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /*
   * NOTE: We could avoid the code duplication between this class and the CGLIB
   * proxies by refactoring "invoke" into a template method. However, this approach
   * adds at least 10% performance overhead versus a copy-paste solution, so we sacrifice
   * elegance for performance. (We have a good test suite to ensure that the different
   * proxies behave the same :-)
   * This way, we can also more easily take advantage of minor optimizations in each class.
   */

  /** We use a static Log to avoid serialization issues. */
  private static final Logger logger = LoggerFactory.getLogger(JdkDynamicAopProxy.class);

  /** Config used to configure this proxy. */
  private final AdvisedSupport advised;

  /**
   * Is the {@link #equals} method defined on the proxied interfaces?
   */
  private boolean equalsDefined;

  /**
   * Is the {@link #hashCode} method defined on the proxied interfaces?
   */
  private boolean hashCodeDefined;

  private final Class<?>[] proxiedInterfaces;

  /**
   * Construct a new JdkDynamicAopProxy for the given AOP configuration.
   *
   * @param config the AOP configuration as AdvisedSupport object
   * @throws AopConfigException if the config is invalid. We try to throw an informative
   * exception in this case, rather than let a mysterious failure happen later.
   */
  public JdkDynamicAopProxy(AdvisedSupport config) {
    Assert.notNull(config, "AdvisedSupport is required");
    this.advised = config;
    this.proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
    findDefinedEqualsAndHashCodeMethods(this.proxiedInterfaces);
  }

  @Override
  public Object getProxy() {
    return getProxy(ClassUtils.getDefaultClassLoader());
  }

  @Override
  public Object getProxy(@Nullable ClassLoader classLoader) {
    if (logger.isTraceEnabled()) {
      logger.trace("Creating JDK dynamic proxy: {}", this.advised.getTargetSource());
    }

    return Proxy.newProxyInstance(determineClassLoader(classLoader), proxiedInterfaces, this);
  }

  @SuppressWarnings("deprecation")
  @Override
  public Class<?> getProxyClass(@Nullable ClassLoader classLoader) {
    return Proxy.getProxyClass(determineClassLoader(classLoader), this.proxiedInterfaces);
  }

  /**
   * Determine whether the JDK bootstrap or platform loader has been suggested ->
   * use higher-level loader which can see infrastructure classes instead.
   */
  private ClassLoader determineClassLoader(@Nullable ClassLoader classLoader) {
    if (classLoader == null) {
      // JDK bootstrap loader -> use today-aop ClassLoader instead.
      return getClass().getClassLoader();
    }
    if (classLoader.getParent() == null) {
      // Potentially the JDK platform loader on JDK 9+
      ClassLoader aopClassLoader = getClass().getClassLoader();
      ClassLoader aopParent = aopClassLoader.getParent();
      while (aopParent != null) {
        if (classLoader == aopParent) {
          // Suggested ClassLoader is ancestor of today-aop ClassLoader
          // -> use today-aop ClassLoader itself instead.
          return aopClassLoader;
        }
        aopParent = aopParent.getParent();
      }
    }
    // Regular case: use suggested ClassLoader as-is.
    return classLoader;
  }

  /**
   * Finds any {@link #equals} or {@link #hashCode} method that may be defined
   * on the supplied set of interfaces.
   *
   * @param proxiedInterfaces the interfaces to introspect
   */
  void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
    for (Class<?> proxiedInterface : proxiedInterfaces) {
      Method[] methods = proxiedInterface.getDeclaredMethods();
      for (Method method : methods) {
        if (ReflectionUtils.isEqualsMethod(method)) {
          this.equalsDefined = true;
        }
        if (ReflectionUtils.isHashCodeMethod(method)) {
          this.hashCodeDefined = true;
        }
        if (this.equalsDefined && this.hashCodeDefined) {
          return;
        }
      }
    }
  }

  /**
   * Implementation of {@code InvocationHandler.invoke}.
   * <p>Callers will see exactly the exception thrown by the target,
   * unless a hook method throws an exception.
   */
  @Override
  @Nullable
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Object oldProxy = null;
    boolean setProxyContext = false;

    TargetSource targetSource = advised.getTargetSource();
    Object target = null;

    try {
      if (!equalsDefined && ReflectionUtils.isEqualsMethod(method)) {
        // The target does not implement the equals(Object) method itself.
        return equals(args[0]);
      }
      else if (!hashCodeDefined && ReflectionUtils.isHashCodeMethod(method)) {
        // The target does not implement the hashCode() method itself.
        return hashCode();
      }
      else {
        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass == DecoratingProxy.class) {
          // There is only getDecoratedClass() declared -> dispatch to proxy config.
          return AopProxyUtils.ultimateTargetClass(advised);
        }
        else if (!advised.isOpaque()) {
          if (declaringClass.isInterface() && declaringClass.isAssignableFrom(Advised.class)) {
            // Service invocations on ProxyConfig with the proxy config...
            return AopUtils.invokeJoinpointUsingReflection(advised, method, args);
          }
        }
      }

      if (advised.isExposeProxy()) {
        // Make invocation available if necessary.
        oldProxy = AopContext.setCurrentProxy(proxy);
        setProxyContext = true;
      }

      // Get as late as possible to minimize the time we "own" the target,
      // in case it comes from a pool.
      target = targetSource.getTarget();
      Class<?> targetClass = (target != null ? target.getClass() : null);

      // Get the interception chain for this method.
      MethodInterceptor[] chain = advised.getInterceptors(method, targetClass);

      Object retVal;
      // Check whether we have any advice. If we don't, we can fallback on direct
      // reflective invocation of the target, and avoid creating a MethodInvocation.
      if (ObjectUtils.isEmpty(chain)) {
        // We can skip creating a MethodInvocation: just invoke the target directly
        // Note that the final invoker must be an InvokerInterceptor so we know it does
        // nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
        Object[] argsToUse = ClassUtils.adaptArgumentsIfNecessary(method, args);
        retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
      }
      else {
        // Proceed to the join-point through the interceptor chain.
        retVal = new DefaultMethodInvocation(proxy, target, method, targetClass, args, chain)
                .proceed();
      }
      // Massage return value if necessary
      Class<?> returnType;
      if (retVal != null
              && retVal == target
              && (returnType = method.getReturnType()) != Object.class
              && returnType.isInstance(proxy)
              && !RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
        // Special case: it returned "this" and the return type of the method
        // is type-compatible. Note that we can't help if the target sets
        // a reference to itself in another returned object.
        retVal = proxy;
      }

      if (retVal == null) {
        returnType = method.getReturnType();
        if (returnType != Void.TYPE && returnType.isPrimitive()) {
          throw new AopInvocationException(
                  "Null return value from advice does not match primitive return type for: " + method);
        }
      }
      return retVal;
    }
    finally {
      if (target != null && !targetSource.isStatic()) {
        // Must have come from TargetSource.
        targetSource.releaseTarget(target);
      }
      if (setProxyContext) {
        // Restore old proxy.
        AopContext.setCurrentProxy(oldProxy);
      }
    }
  }

  /**
   * Equality means interfaces, advisors and TargetSource are equal.
   * <p>The compared object may be a JdkDynamicAopProxy instance itself
   * or a dynamic proxy wrapping a JdkDynamicAopProxy instance.
   */
  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (other == null) {
      return false;
    }

    JdkDynamicAopProxy otherProxy;
    if (other instanceof JdkDynamicAopProxy) {
      otherProxy = (JdkDynamicAopProxy) other;
    }
    else if (Proxy.isProxyClass(other.getClass())) {
      InvocationHandler ih = Proxy.getInvocationHandler(other);
      if (!(ih instanceof JdkDynamicAopProxy)) {
        return false;
      }
      otherProxy = (JdkDynamicAopProxy) ih;
    }
    else {
      // Not a valid comparison...
      return false;
    }

    // If we get here, otherProxy is the other AopProxy.
    return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
  }

  /**
   * Proxy uses the hash code of the TargetSource.
   */
  @Override
  public int hashCode() {
    return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
  }

}
