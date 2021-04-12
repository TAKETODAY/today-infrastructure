/*
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

package cn.taketoday.aop.proxy;

import org.aopalliance.aop.Advice;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.AopInvocationException;
import cn.taketoday.aop.PointcutAdvisor;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.context.cglib.proxy.Callback;
import cn.taketoday.context.cglib.proxy.CallbackFilter;
import cn.taketoday.context.cglib.proxy.Dispatcher;
import cn.taketoday.context.cglib.proxy.Enhancer;
import cn.taketoday.context.cglib.proxy.Factory;
import cn.taketoday.context.cglib.proxy.MethodInterceptor;
import cn.taketoday.context.cglib.proxy.MethodProxy;
import cn.taketoday.context.cglib.proxy.NoOp;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * CGLIB-based {@link AopProxy} implementation for the AOP framework.
 *
 * <p>
 * Objects of this type should be obtained through proxy factories, configured
 * by an {@link AdvisedSupport} object. This class is internal to AOP framework
 * and need not be used directly by client code.
 *
 * <p>
 * {@link AopUtils} will automatically create CGLIB-based proxies if necessary,
 * for example in case of proxying a target class (see the
 * {@link AopUtils#createAopProxy(AdvisedSupport)} attendant javadoc} for
 * details).
 *
 * <p>
 * Proxies created using this class are thread-safe if the underlying (target)
 * class is thread-safe.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @author Dave Syer
 * @author TODAY 2021/2/1 21:47
 * @see AdvisedSupport#setProxyTargetClass
 * @since 3.0
 */
public class CglibAopProxy extends AbstractSubclassesAopProxy implements AopProxy, Serializable {
  private static final long serialVersionUID = 1L;

  protected static final Logger log = LoggerFactory.getLogger(CglibAopProxy.class);

  // Constants for CGLIB callback array indices
  private static final int AOP_PROXY = 0;
  private static final int INVOKE_TARGET = 1;
  private static final int NO_OVERRIDE = 2;
  private static final int DISPATCH_TARGET = 3;
  private static final int DISPATCH_ADVISED = 4;
  private static final int INVOKE_EQUALS = 5;
  private static final int INVOKE_HASHCODE = 6;

  private transient Map<Method, Integer> fixedInterceptorMap = Collections.emptyMap();

  private transient int fixedInterceptorOffset;

  /**
   * Create a new CglibAopProxy for the given AOP configuration.
   *
   * @param config
   *         the AOP configuration as AdvisedSupport object
   *
   * @throws AopConfigException
   *         if the config is invalid. We try to throw an informative
   *         exception in this case, rather than let a mysterious failure
   *         happen later.
   */
  public CglibAopProxy(AdvisedSupport config) {
    super(config);
  }

  @Override
  public Object getProxy(ClassLoader classLoader, Function<Constructor<?>, Object[]> argsFunction) {
    if (log.isTraceEnabled()) {
      log.trace("Creating CGLIB proxy: {}", config.getTargetSource());
    }
    return super.getProxy(classLoader, argsFunction);
  }

  @Override
  protected Object getProxyInternal(
          Class<?> proxySuperClass, ClassLoader classLoader,
          Function<Constructor<?>, Object[]> argsFunction) throws Exception {

    final Class<?> rootClass = config.getTargetClass();

    // Configure CGLIB Enhancer...
    Enhancer enhancer = createEnhancer();
    if (classLoader != null) {
      enhancer.setClassLoader(classLoader);
    }
    enhancer.setSuperclass(proxySuperClass);
    enhancer.setInterfaces(AopProxyUtils.completeProxiedInterfaces(config));

    Callback[] callbacks = getCallbacks(rootClass);
    Class<?>[] types = new Class<?>[callbacks.length];
    for (int x = 0; x < types.length; x++) {
      types[x] = callbacks[x].getClass();
    }
    // fixedInterceptorMap only populated at this point, after getCallbacks call above
    enhancer.setCallbackFilter(
            new ProxyCallbackFilter(
                    config.getConfigurationOnlyCopy(),
                    fixedInterceptorMap,
                    fixedInterceptorOffset
            )
    );
    enhancer.setCallbackTypes(types);

    computeConstructorArguments(argsFunction, proxySuperClass);

    // Generate the proxy class and create a proxy instance.
    return createProxyClassAndInstance(enhancer, callbacks);
  }

  protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
    enhancer.setInterceptDuringConstruction(false);
    enhancer.setCallbacks(callbacks);
    return constructorArgs != null && constructorArgTypes != null
           ? enhancer.create(constructorArgTypes, constructorArgs)
           : enhancer.create();
  }

  /**
   * Creates the CGLIB {@link Enhancer}. Subclasses may wish to override this to
   * return a custom {@link Enhancer} implementation.
   */
  protected Enhancer createEnhancer() {
    return new Enhancer();
  }

  Callback[] getCallbacks(Class<?> rootClass) throws Exception {
    // Parameters used for optimization choices...
    final AdvisedSupport config = this.config;
    boolean exposeProxy = config.isExposeProxy();
    boolean isFrozen = config.isFrozen();
    boolean isStatic = config.getTargetSource().isStatic();

    // Choose an "aop" interceptor (used for AOP calls).
    Callback aopInterceptor = new DynamicAdvisedInterceptor(config);

    // Choose a "straight to target" interceptor. (used for calls that are
    // unadvised but can return this). May be required to expose the proxy.
    Callback targetInterceptor;
    if (exposeProxy) {
      targetInterceptor = (isStatic ? new StaticUnadvisedExposedInterceptor(config.getTargetSource().getTarget())
                                    : new DynamicUnadvisedExposedInterceptor(config.getTargetSource()));
    }
    else {
      targetInterceptor = (isStatic ? new StaticUnadvisedInterceptor(config.getTargetSource().getTarget())
                                    : new DynamicUnadvisedInterceptor(config.getTargetSource()));
    }

    // Choose a "direct to target" dispatcher (used for
    // unadvised calls to static targets that cannot return this).
    Callback targetDispatcher = (isStatic
                                 ? new StaticDispatcher(config.getTargetSource().getTarget())
                                 : new SerializableNoOp());

    Callback[] mainCallbacks = new Callback[] {
            aopInterceptor, // for normal advice
            targetInterceptor, // invoke target without considering advice, if optimized
            new SerializableNoOp(), // no override for methods mapped to this
            targetDispatcher,
            new AdvisedDispatcher(config),
            new EqualsInterceptor(config),
            new HashCodeInterceptor(config)
    };

    Callback[] callbacks = mainCallbacks;

    // If the target is a static one and the advice chain is frozen,
    // then we can make some optimizations by sending the AOP calls
    // direct to the target using the fixed chain for that method.
    if (isStatic && isFrozen) {
      Method[] methods = rootClass.getMethods();
      Callback[] fixedCallbacks = new Callback[methods.length];
      this.fixedInterceptorMap = new HashMap<>(methods.length);

      // TODO: small memory optimization here (can skip creation for methods with no advice)
      for (int x = 0; x < methods.length; x++) {
        Method method = methods[x];
        org.aopalliance.intercept.MethodInterceptor[] chain = config.getInterceptors(method, rootClass);

        fixedCallbacks[x] = new FixedChainStaticTargetInterceptor(chain, config);
        this.fixedInterceptorMap.put(method, x);
      }

      // Now copy both the callbacks from mainCallbacks
      // and fixedCallbacks into the callbacks array.
      callbacks = new Callback[mainCallbacks.length + fixedCallbacks.length];
      System.arraycopy(mainCallbacks, 0, callbacks, 0, mainCallbacks.length);
      System.arraycopy(fixedCallbacks, 0, callbacks, mainCallbacks.length, fixedCallbacks.length);
      this.fixedInterceptorOffset = mainCallbacks.length;
    }
    return callbacks;
  }

  @Override
  public boolean equals(Object other) {
    return (this == other || (other instanceof CglibAopProxy &&
            AopProxyUtils.equalsInProxy(config, ((CglibAopProxy) other).config)));
  }

  @Override
  public int hashCode() {
    return CglibAopProxy.class.hashCode() * 13 + config.getTargetSource().hashCode();
  }

  /**
   * Serializable replacement for CGLIB's NoOp interface. Public to allow use
   * elsewhere in the framework.
   */
  public static class SerializableNoOp implements NoOp, Serializable { }

  /**
   * Method interceptor used for static targets with no advice chain. The call is
   * passed directly back to the target. Used when the proxy needs to be exposed
   * and it can't be determined that the method won't return {@code this}.
   */
  static class StaticUnadvisedInterceptor implements MethodInterceptor, Serializable {
    final Object target;

    public StaticUnadvisedInterceptor(Object target) {
      this.target = target;
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
      return methodProxy.invoke(this.target, args);
    }
  }

  /**
   * Method interceptor used for static targets with no advice chain, when the
   * proxy is to be exposed.
   */
  static class StaticUnadvisedExposedInterceptor implements MethodInterceptor, Serializable {
    final Object target;

    public StaticUnadvisedExposedInterceptor(Object target) {
      this.target = target;
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
      Object oldProxy = null;
      try {
        oldProxy = AopContext.setCurrentProxy(proxy);
        return methodProxy.invoke(this.target, args);
      }
      finally {
        AopContext.setCurrentProxy(oldProxy);
      }
    }
  }

  /**
   * Interceptor used to invoke a dynamic target without creating a method
   * invocation or evaluating an advice chain. (We know there was no advice for
   * this method.)
   */
  static class DynamicUnadvisedInterceptor implements MethodInterceptor, Serializable {
    final TargetSource targetSource;

    public DynamicUnadvisedInterceptor(TargetSource targetSource) {
      this.targetSource = targetSource;
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
      Object target = this.targetSource.getTarget();
      try {
        return methodProxy.invoke(target, args);
      }
      finally {
        if (target != null) {
          this.targetSource.releaseTarget(target);
        }
      }
    }
  }

  /**
   * Interceptor for unadvised dynamic targets when the proxy needs exposing.
   */
  static class DynamicUnadvisedExposedInterceptor implements MethodInterceptor, Serializable {
    final TargetSource targetSource;

    public DynamicUnadvisedExposedInterceptor(TargetSource targetSource) {
      this.targetSource = targetSource;
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
      Object oldProxy = null;
      Object target = this.targetSource.getTarget();
      try {
        oldProxy = AopContext.setCurrentProxy(proxy);
        return methodProxy.invoke(target, args);
      }
      finally {
        AopContext.setCurrentProxy(oldProxy);
        if (target != null) {
          this.targetSource.releaseTarget(target);
        }
      }
    }
  }

  /**
   * Dispatcher for a static target. Dispatcher is much faster than interceptor.
   * This will be used whenever it can be determined that a method definitely does
   * not return "this"
   */
  static class StaticDispatcher implements Dispatcher, Serializable {
    final Object target;

    public StaticDispatcher(Object target) {
      this.target = target;
    }

    @Override
    public Object loadObject() {
      return this.target;
    }
  }

  /**
   * Dispatcher for any methods declared on the Advised class.
   */
  static class AdvisedDispatcher implements Dispatcher, Serializable {
    final AdvisedSupport advised;

    public AdvisedDispatcher(AdvisedSupport advised) {
      this.advised = advised;
    }

    @Override
    public Object loadObject() {
      return this.advised;
    }
  }

  /**
   * Dispatcher for the {@code equals} method. Ensures that the method call is
   * always handled by this class.
   */
  static class EqualsInterceptor implements MethodInterceptor, Serializable {
    final AdvisedSupport advised;

    public EqualsInterceptor(AdvisedSupport advised) {
      this.advised = advised;
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) {
      Object other = args[0];
      if (proxy == other) {
        return true;
      }
      if (other instanceof Factory) {
        Callback callback = ((Factory) other).getCallback(INVOKE_EQUALS);
        if (!(callback instanceof EqualsInterceptor)) {
          return false;
        }
        AdvisedSupport otherAdvised = ((EqualsInterceptor) callback).advised;
        return AopProxyUtils.equalsInProxy(this.advised, otherAdvised);
      }
      else {
        return false;
      }
    }
  }

  /**
   * Dispatcher for the {@code hashCode} method. Ensures that the method call is
   * always handled by this class.
   */
  static class HashCodeInterceptor implements MethodInterceptor, Serializable {
    final AdvisedSupport advised;

    public HashCodeInterceptor(AdvisedSupport advised) {
      this.advised = advised;
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) {
      return CglibAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
    }
  }

  /**
   * Interceptor used specifically for advised methods on a frozen, static proxy.
   */
  static class FixedChainStaticTargetInterceptor implements MethodInterceptor, Serializable {
    final Object target;
    final Class<?> targetClass;
    final org.aopalliance.intercept.MethodInterceptor[] adviceChain;

    public FixedChainStaticTargetInterceptor(
            org.aopalliance.intercept.MethodInterceptor[] adviceChain, AdvisedSupport config) throws Exception {
      this.adviceChain = adviceChain;
      this.targetClass = config.getTargetClass();
      this.target = config.getTargetSource().getTarget();
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
      return new CglibMethodInvocation(target, method, targetClass, methodProxy, args, adviceChain).proceed();
    }
  }

  /**
   * General purpose AOP callback. Used when the target is dynamic or when the
   * proxy is not frozen.
   */
  static class DynamicAdvisedInterceptor implements MethodInterceptor, Serializable {
    final AdvisedSupport advised;

    public DynamicAdvisedInterceptor(AdvisedSupport advised) {
      this.advised = advised;
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
      Object oldProxy = null;
      boolean restore = false;
      Object target = null;

      final AdvisedSupport advised = this.advised;
      final TargetSource targetSource = advised.getTargetSource();
      try {
        if (advised.isExposeProxy()) {
          // Make invocation available if necessary.
          oldProxy = AopContext.setCurrentProxy(proxy);
          restore = true;
        }

        // Get as late as possible to minimize the time we "own" the target, in case it comes from a pool...
        target = targetSource.getTarget();
        Class<?> targetClass = (target != null ? target.getClass() : null);
        org.aopalliance.intercept.MethodInterceptor[] chain = advised.getInterceptors(method, targetClass);
        final Object retVal;
        // Check whether we only have one InvokerInterceptor: that is,
        // no real advice, but just reflective invocation of the target.
        if (ObjectUtils.isEmpty(chain) && Modifier.isPublic(method.getModifiers())) {
          // We can skip creating a MethodInvocation: just invoke the target directly.
          // Note that the final invoker must be an InvokerInterceptor, so we know
          // it does nothing but a reflective operation on the target, and no hot
          // swapping or fancy proxying.
          Object[] argsToUse = ClassUtils.adaptArgumentsIfNecessary(method, args);
          retVal = methodProxy.invoke(target, argsToUse);
        }
        else {
          // We need to create a CglibMethodInvocation...
          retVal = new CglibMethodInvocation(target, method, targetClass, methodProxy, args, chain).proceed();
        }
        return processReturnValue(proxy, target, method, retVal);
      }
      finally {
        if (target != null && !targetSource.isStatic()) {
          targetSource.releaseTarget(target);
        }
        if (restore) {
          // Restore old proxy.
          AopContext.setCurrentProxy(oldProxy);
        }
      }
    }

    @Override
    public boolean equals(Object other) {
      return (this == other ||
              (other instanceof DynamicAdvisedInterceptor &&
                      this.advised.equals(((DynamicAdvisedInterceptor) other).advised)));
    }

    /**
     * CGLIB uses this to drive proxy creation.
     */
    @Override
    public int hashCode() {
      return this.advised.hashCode();
    }
  }

  /**
   * Process a return value. Wraps a return of {@code this} if necessary to be the
   * {@code proxy} and also verifies that {@code null} is not returned as a primitive.
   */
  private static Object processReturnValue(Object proxy, Object target, Method method, Object retVal) {
    // Massage return value if necessary
    if (retVal != null && retVal == target) {
      // Special case: it returned "this". Note that we can't help
      // if the target sets a reference to itself in another returned object.
      retVal = proxy;
    }
    final Class<?> returnType;
    if (retVal == null && (returnType = method.getReturnType()) != Void.TYPE && returnType.isPrimitive()) {
      throw new AopInvocationException(
              "Null return value from advice does not match primitive return type for: " + method);
    }
    return retVal;
  }

  /**
   * Implementation of AOP Alliance MethodInvocation used by this AOP proxy.
   */
  static class CglibMethodInvocation extends DefaultMethodInvocation {
    final MethodProxy proxy;

    public CglibMethodInvocation(Object target, Method method, Class<?> targetClass,
                                 MethodProxy proxy, Object[] arguments,
                                 org.aopalliance.intercept.MethodInterceptor[] advices) {
      super(target, method, targetClass, arguments, advices);
      this.proxy = proxy;
    }

    @Override
    protected Object invokeJoinPoint() throws Throwable {
      return proxy.invoke(target, args);
    }

    @Override
    public Object proceed() throws Throwable {
      try {
        return super.proceed();
      }
      catch (RuntimeException ex) {
        throw ex;
      }
      catch (Exception ex) {
        if (ReflectionUtils.declaresException(getMethod(), ex.getClass())) {
          throw ex;
        }
        else {
          throw new UndeclaredThrowableException(ex);
        }
      }
    }
  }

  /**
   * CallbackFilter to assign Callbacks to methods.
   */
  static class ProxyCallbackFilter implements CallbackFilter {
    final AdvisedSupport advised;
    final int fixedInterceptorOffset;
    final Map<Method, Integer> fixedInterceptorMap;

    public ProxyCallbackFilter(
            AdvisedSupport advised, Map<Method, Integer> fixedInterceptorMap, int fixedInterceptorOffset) {

      this.advised = advised;
      this.fixedInterceptorMap = fixedInterceptorMap;
      this.fixedInterceptorOffset = fixedInterceptorOffset;
    }

    /**
     * Implementation of CallbackFilter.accept() to return the index of the callback
     * we need.
     * <p>
     * The callbacks for each proxy are built up of a set of fixed callbacks for
     * general use and then a set of callbacks that are specific to a method for use
     * on static targets with a fixed advice chain.
     * <p>
     * The callback used is determined thus:
     * <dl>
     * <dt>For exposed proxies</dt>
     * <dd>Exposing the proxy requires code to execute before and after the
     * method/chain invocation. This means we must use DynamicAdvisedInterceptor,
     * since all other interceptors can avoid the need for a try/catch block</dd>
     * <dt>For Object.finalize():</dt>
     * <dd>No override for this method is used.</dd>
     * <dt>For equals():</dt>
     * <dd>The EqualsInterceptor is used to redirect equals() calls to a special
     * handler to this proxy.</dd>
     * <dt>For methods on the Advised class:</dt>
     * <dd>the AdvisedDispatcher is used to dispatch the call directly to the
     * target</dd>
     * <dt>For advised methods:</dt>
     * <dd>If the target is static and the advice chain is frozen then a
     * FixedChainStaticTargetInterceptor specific to the method is used to invoke
     * the advice chain. Otherwise a DynamicAdvisedInterceptor is used.</dd>
     * <dt>For non-advised methods:</dt>
     * <dd>Where it can be determined that the method will not return {@code this}
     * or when {@code ProxyFactory.getExposeProxy()} returns {@code false}, then a
     * Dispatcher is used. For static targets, the StaticDispatcher is used; and for
     * dynamic targets, a DynamicUnadvisedInterceptor is used. If it possible for
     * the method to return {@code this} then a StaticUnadvisedInterceptor is used
     * for static targets - the DynamicUnadvisedInterceptor already considers
     * this.</dd>
     * </dl>
     */
    @Override
    public int accept(Method method) {
      if (ReflectionUtils.isFinalizeMethod(method)) {
        log.trace("Found finalize() method - using NO_OVERRIDE");
        return NO_OVERRIDE;
      }
      final AdvisedSupport advised = this.advised;
      if (!advised.isOpaque()
              && method.getDeclaringClass().isInterface()
              && method.getDeclaringClass().isAssignableFrom(Advised.class)) {
        if (log.isTraceEnabled()) {
          log.trace("Method is declared on Advised interface: {}", method);
        }
        return DISPATCH_ADVISED;
      }
      // We must always proxy equals, to direct calls to this.
      if (ReflectionUtils.isEqualsMethod(method)) {
        if (log.isTraceEnabled()) {
          log.trace("Found 'equals' method: {}", method);
        }
        return INVOKE_EQUALS;
      }
      // We must always calculate hashCode based on the proxy.
      if (ReflectionUtils.isHashCodeMethod(method)) {
        if (log.isTraceEnabled()) {
          log.trace("Found 'hashCode' method: {}", method);
        }
        return INVOKE_HASHCODE;
      }
      Class<?> targetClass = advised.getTargetClass();
      // Proxy is not yet available, but that shouldn't matter.
      org.aopalliance.intercept.MethodInterceptor[] chain = advised.getInterceptors(method, targetClass);
      boolean haveAdvice = ObjectUtils.isNotEmpty(chain);
      boolean exposeProxy = advised.isExposeProxy();
      boolean isStatic = advised.getTargetSource().isStatic();
      boolean isFrozen = advised.isFrozen();
      if (haveAdvice || !isFrozen) {
        // If exposing the proxy, then AOP_PROXY must be used.
        if (exposeProxy) {
          if (log.isTraceEnabled()) {
            log.trace("Must expose proxy on advised method: {}", method);
          }
          return AOP_PROXY;
        }
        // Check to see if we have fixed interceptor to serve this method.
        // Else use the AOP_PROXY.
        if (isStatic && isFrozen && this.fixedInterceptorMap.containsKey(method)) {
          if (log.isTraceEnabled()) {
            log.trace("Method has advice and optimizations are enabled: {}", method);
          }
          // We know that we are optimizing so we can use the FixedStaticChainInterceptors.
          int index = this.fixedInterceptorMap.get(method);
          return (index + this.fixedInterceptorOffset);
        }
        else {
          if (log.isTraceEnabled()) {
            log.trace("Unable to apply any optimizations to advised method: {}", method);
          }
          return AOP_PROXY;
        }
      }
      else {
        // See if the return type of the method is outside the class hierarchy of the target type.
        // If so we know it never needs to have return type massage and can use a dispatcher.
        // If the proxy is being exposed, then must use the interceptor the correct one is already
        // configured. If the target is not static, then we cannot use a dispatcher because the
        // target needs to be explicitly released after the invocation.
        if (exposeProxy || !isStatic) {
          return INVOKE_TARGET;
        }
        Class<?> returnType = method.getReturnType();
        if (targetClass != null && returnType.isAssignableFrom(targetClass)) {
          if (log.isTraceEnabled()) {
            log.trace("Method return type is assignable from target type and " +
                              "may therefore return 'this' - using INVOKE_TARGET: {}", method);
          }
          return INVOKE_TARGET;
        }
        else {
          if (log.isTraceEnabled()) {
            log.trace("Method return type ensures 'this' cannot be returned - using DISPATCH_TARGET: {}", method);
          }
          return DISPATCH_TARGET;
        }
      }
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof ProxyCallbackFilter)) {
        return false;
      }
      final AdvisedSupport advised = this.advised;
      ProxyCallbackFilter otherCallbackFilter = (ProxyCallbackFilter) other;
      AdvisedSupport otherAdvised = otherCallbackFilter.advised;
      if (advised.isFrozen() != otherAdvised.isFrozen()) {
        return false;
      }
      if (advised.isExposeProxy() != otherAdvised.isExposeProxy()) {
        return false;
      }
      if (advised.getTargetSource().isStatic() != otherAdvised.getTargetSource().isStatic()) {
        return false;
      }
      if (!AopProxyUtils.equalsProxiedInterfaces(advised, otherAdvised)) {
        return false;
      }
      // Advice instance identity is unimportant to the proxy class:
      // All that matters is type and ordering.
      Advisor[] thisAdvisors = advised.getAdvisors();
      Advisor[] thatAdvisors = otherAdvised.getAdvisors();
      if (thisAdvisors.length != thatAdvisors.length) {
        return false;
      }
      for (int i = 0; i < thisAdvisors.length; i++) {
        Advisor thisAdvisor = thisAdvisors[i];
        Advisor thatAdvisor = thatAdvisors[i];
        if (!equalsAdviceClasses(thisAdvisor, thatAdvisor)) {
          return false;
        }
        if (!equalsPointcuts(thisAdvisor, thatAdvisor)) {
          return false;
        }
      }
      return true;
    }

    static boolean equalsAdviceClasses(Advisor a, Advisor b) {
      return (a.getAdvice().getClass() == b.getAdvice().getClass());
    }

    static boolean equalsPointcuts(Advisor a, Advisor b) {
      // If only one of the advisor (but not both) is PointcutAdvisor, then it is a mismatch.
      // Takes care of the situations where an IntroductionAdvisor is used (see SPR-3959).
      return (!(a instanceof PointcutAdvisor) ||
              (b instanceof PointcutAdvisor &&
                      Objects.equals(((PointcutAdvisor) a).getPointcut(), ((PointcutAdvisor) b).getPointcut())));
    }

    @Override
    public int hashCode() {
      int hashCode = 0;
      final AdvisedSupport advised = this.advised;
      Advisor[] advisors = advised.getAdvisors();
      for (Advisor advisor : advisors) {
        Advice advice = advisor.getAdvice();
        hashCode = 13 * hashCode + advice.getClass().hashCode();
      }
      hashCode = 13 * hashCode + (advised.isFrozen() ? 1 : 0);
      hashCode = 13 * hashCode + (advised.isExposeProxy() ? 1 : 0);
      hashCode = 13 * hashCode + (advised.isOptimize() ? 1 : 0);
      hashCode = 13 * hashCode + (advised.isOpaque() ? 1 : 0);
      return hashCode;
    }
  }

}
