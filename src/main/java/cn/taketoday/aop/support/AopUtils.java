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

package cn.taketoday.aop.support;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.AfterReturningAdvice;
import cn.taketoday.aop.AopInvocationException;
import cn.taketoday.aop.IntroductionAdvisor;
import cn.taketoday.aop.IntroductionAwareMethodMatcher;
import cn.taketoday.aop.MethodBeforeAdvice;
import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.PointcutAdvisor;
import cn.taketoday.aop.TargetClassAware;
import cn.taketoday.aop.ThrowsAdvice;
import cn.taketoday.aop.proxy.Advised;
import cn.taketoday.aop.proxy.AdvisedSupport;
import cn.taketoday.aop.proxy.AdvisorAdapter;
import cn.taketoday.aop.proxy.AopConfigException;
import cn.taketoday.aop.proxy.AopProxy;
import cn.taketoday.aop.proxy.AopProxyUtils;
import cn.taketoday.aop.proxy.CglibAopProxy;
import cn.taketoday.aop.proxy.JdkDynamicAopProxy;
import cn.taketoday.aop.proxy.StandardAopProxy;
import cn.taketoday.aop.proxy.StandardProxy;
import cn.taketoday.aop.proxy.UnknownAdviceTypeException;
import cn.taketoday.aop.support.annotation.AfterReturningMethodInterceptor;
import cn.taketoday.aop.support.annotation.AfterThrowingMethodInterceptor;
import cn.taketoday.aop.support.annotation.BeforeMethodInterceptor;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * Utility methods for AOP support code.
 *
 * <p>Mainly for internal use within AOP support.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author TODAY 2021/2/1 18:46
 * @see AopProxyUtils
 * @since 3.0
 */
public abstract class AopUtils {

  public static final MethodInterceptor[] EMPTY_INTERCEPTOR = new MethodInterceptor[0];

  private static final List<AdvisorAdapter> advisorAdapters = new ArrayList<>();

  static {
    addAdvisorAdapters(new BeforeAdvisorAdapter(),
                       new ThrowsAdviceAdvisorAdapter(),
                       new AfterReturningAdvisorAdapter());
  }

  /**
   * Check whether the given object is a JDK dynamic proxy or a CGLIB proxy.
   * <p>This method additionally checks if the given object is an instance
   * of {@link StandardProxy}.
   *
   * @param object
   *         the object to check
   *
   * @see #isJdkDynamicProxy
   * @see #isCglibProxy
   */
  public static boolean isAopProxy(Object object) {
    return (object instanceof StandardProxy
            && (Proxy.isProxyClass(object.getClass()) || object.getClass().getName().contains("$$")));
  }

  /**
   * Check whether the given object is a JDK dynamic proxy.
   * <p>This method goes beyond the implementation of
   * {@link Proxy#isProxyClass(Class)} by additionally checking if the
   * given object is an instance of {@link StandardProxy}.
   *
   * @param object
   *         the object to check
   *
   * @see java.lang.reflect.Proxy#isProxyClass
   */
  public static boolean isJdkDynamicProxy(Object object) {
    return (object instanceof StandardProxy && Proxy.isProxyClass(object.getClass()));
  }

  /**
   * Check whether the given object is a CGLIB proxy.
   * <p>This method goes beyond the implementation of
   * {@link ClassUtils#isCglibProxy(Object)} by additionally checking if
   * the given object is an instance of {@link StandardProxy}.
   *
   * @param object
   *         the object to check
   *
   * @see ClassUtils#isCglibProxy(Object)
   */
  public static boolean isCglibProxy(Object object) {
    return (object instanceof StandardProxy &&
            object.getClass().getName().contains("$$"));
  }

  /**
   * Determine the target class of the given bean instance which might be an AOP proxy.
   * <p>Returns the target class for an AOP proxy or the plain class otherwise.
   *
   * @param candidate
   *         the instance to check (might be an AOP proxy)
   *
   * @return the target class (or the plain class of the given object as fallback;
   * never {@code null})
   *
   * @see TargetClassAware#getTargetClass()
   * @see AopProxyUtils#ultimateTargetClass(Object)
   */
  public static Class<?> getTargetClass(Object candidate) {
    Assert.notNull(candidate, "Candidate object must not be null");
    Class<?> result = null;
    if (candidate instanceof TargetClassAware) {
      result = ((TargetClassAware) candidate).getTargetClass();
    }
    if (result == null) {
      result = (isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
    }
    return result;
  }

  /**
   * Determine the target class of the given invocation.
   * <p>Returns the target class for an AOP proxy or the plain class otherwise.
   *
   * @param invocation
   *         the instance to check
   *
   * @return the target class (or the plain class of the given object as fallback;
   * never {@code null})
   *
   * @see TargetClassAware#getTargetClass()
   */
  public static Class<?> getTargetClass(MethodInvocation invocation) {
    Assert.notNull(invocation, "MethodInvocation must not be null");
    Class<?> result = null;
    if (invocation instanceof TargetClassAware) {
      result = ((TargetClassAware) invocation).getTargetClass();
    }
    if (result == null) {
      result = getTargetClass(invocation.getThis());
    }
    return result;
  }

  /**
   * Can the given pointcut apply at all on the given class?
   * <p>This is an important test as it can be used to optimize
   * out a pointcut for a class.
   *
   * @param pc
   *         the static or dynamic pointcut to check
   * @param targetClass
   *         the class to test
   *
   * @return whether the pointcut can apply on any method
   */
  public static boolean canApply(Pointcut pc, Class<?> targetClass) {
    return canApply(pc, targetClass, false);
  }

  /**
   * Can the given pointcut apply at all on the given class?
   * <p>This is an important test as it can be used to optimize
   * out a pointcut for a class.
   *
   * @param pc
   *         the static or dynamic pointcut to check
   * @param targetClass
   *         the class to test
   * @param hasIntroductions
   *         whether or not the advisor chain
   *         for this bean includes any introductions
   *
   * @return whether the pointcut can apply on any method
   */
  public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
    Assert.notNull(pc, "Pointcut must not be null");
    if (!pc.getClassFilter().matches(targetClass)) {
      return false;
    }

    MethodMatcher methodMatcher = pc.getMethodMatcher();
    if (methodMatcher == MethodMatcher.TRUE) {
      // No need to iterate the methods if we're matching any method anyway...
      return true;
    }

    IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
    if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
      introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
    }

    LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
    if (!Proxy.isProxyClass(targetClass)) {
      classes.add(ClassUtils.getUserClass(targetClass));
    }
    classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));

    for (Class<?> clazz : classes) {
      Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
      for (Method method : methods) {
        if (introductionAwareMethodMatcher != null ?
            introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions) :
            methodMatcher.matches(method, targetClass)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Can the given advisor apply at all on the given class?
   * This is an important test as it can be used to optimize
   * out a advisor for a class.
   *
   * @param advisor
   *         the advisor to check
   * @param targetClass
   *         class we're testing
   *
   * @return whether the pointcut can apply on any method
   */
  public static boolean canApply(Advisor advisor, Class<?> targetClass) {
    return canApply(advisor, targetClass, false);
  }

  /**
   * Can the given advisor apply at all on the given class?
   * <p>This is an important test as it can be used to optimize out a advisor for a class.
   * This version also takes into account introductions (for IntroductionAwareMethodMatchers).
   *
   * @param advisor
   *         the advisor to check
   * @param targetClass
   *         class we're testing
   * @param hasIntroductions
   *         whether or not the advisor chain for this bean includes
   *         any introductions
   *
   * @return whether the pointcut can apply on any method
   */
  public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
    if (advisor instanceof IntroductionAdvisor) {
      return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
    }
    else if (advisor instanceof PointcutAdvisor) {
      PointcutAdvisor pca = (PointcutAdvisor) advisor;
      return canApply(pca.getPointcut(), targetClass, hasIntroductions);
    }
    else {
      // It doesn't have a pointcut so we assume it applies.
      return true;
    }
  }

  /**
   * Determine the sublist of the {@code candidateAdvisors} list
   * that is applicable to the given class.
   *
   * @param candidateAdvisors
   *         the Advisors to evaluate
   * @param clazz
   *         the target class
   *
   * @return sublist of Advisors that can apply to an object of the given class
   * (may be the incoming List as-is)
   */
  public static List<Advisor> filterAdvisors(List<Advisor> candidateAdvisors, Class<?> clazz) {
    if (candidateAdvisors.isEmpty()) {
      return candidateAdvisors;
    }
    List<Advisor> eligibleAdvisors = new ArrayList<>();
    for (Advisor candidate : candidateAdvisors) {
      if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
        eligibleAdvisors.add(candidate);
      }
    }
    boolean hasIntroductions = !eligibleAdvisors.isEmpty();
    for (Advisor candidate : candidateAdvisors) {
      if (candidate instanceof IntroductionAdvisor) {
        // already processed
        continue;
      }
      if (canApply(candidate, clazz, hasIntroductions)) {
        eligibleAdvisors.add(candidate);
      }
    }
    return eligibleAdvisors;
  }

  /**
   * Invoke the given target via reflection, as part of an AOP method invocation.
   *
   * @param target
   *         the target object
   * @param method
   *         the method to invoke
   * @param args
   *         the arguments for the method
   *
   * @return the invocation result, if any
   *
   * @throws Throwable
   *         if thrown by the target method
   * @throws AopInvocationException
   *         in case of a reflection error
   */
  public static Object invokeJoinpointUsingReflection(Object target, Method method, Object[] args)
          throws Throwable {

    // Use reflection to invoke the method.
    try {
      ReflectionUtils.makeAccessible(method);
      return method.invoke(target, args);
    }
    catch (InvocationTargetException ex) {
      // Invoked method threw a checked exception.
      // We must rethrow it. The client won't see the interceptor.
      throw ex.getTargetException();
    }
    catch (IllegalArgumentException ex) {
      throw new AopInvocationException(
              "AOP configuration seems to be invalid: tried calling method [" +
                      method + "] on target [" + target + "]", ex);
    }
    catch (IllegalAccessException ex) {
      throw new AopInvocationException("Could not access method [" + method + "]", ex);
    }
  }

  //

  /**
   * get un-ordered {@link MethodInterceptor} array
   */
  public static MethodInterceptor[] getInterceptorsArray(Advised config, Method method, Class<?> targetClass) {
    final List<MethodInterceptor> interceptors = getInterceptors(config, method, targetClass);
    if (interceptors.isEmpty()) {
      return EMPTY_INTERCEPTOR;
    }
    return interceptors.toArray(new MethodInterceptor[interceptors.size()]);
  }

  /**
   * get un-ordered {@link MethodInterceptor} list
   */
  public static List<MethodInterceptor> getInterceptors(Advised config, Method method, Class<?> targetClass) {

    // This is somewhat tricky... We have to process introductions first,
    // but we need to preserve order in the ultimate list.
    Advisor[] advisors = config.getAdvisors();
    ArrayList<MethodInterceptor> ret = new ArrayList<>(advisors.length);
    Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
    Boolean hasIntroductions = null;

    for (Advisor advisor : advisors) {
      if (advisor instanceof PointcutAdvisor) {
        // Add it conditionally.
        PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
        if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
          MethodMatcher matcher = pointcutAdvisor.getPointcut().getMethodMatcher();
          boolean match;
          if (matcher instanceof IntroductionAwareMethodMatcher) {
            if (hasIntroductions == null) {
              hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
            }
            match = ((IntroductionAwareMethodMatcher) matcher).matches(method, actualClass, hasIntroductions);
          }
          else {
            match = matcher.matches(method, actualClass);
          }
          if (match) {
            MethodInterceptor[] interceptors = getInterceptors(advisor);
            if (matcher.isRuntime()) {
              // Creating a new object instance in the getInterceptors() method
              // isn't a problem as we normally cache created chains.
              for (MethodInterceptor interceptor : interceptors) {
                ret.add(new RuntimeMethodInterceptor(interceptor, matcher));
              }
            }
            else {
              ret.addAll(Arrays.asList(interceptors));
            }
          }
        }
      }
      else if (advisor instanceof IntroductionAdvisor) {
        IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
        if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
          MethodInterceptor[] interceptors = getInterceptors(advisor);
          ret.addAll(Arrays.asList(interceptors));
        }
      }
      else {
        MethodInterceptor[] interceptors = getInterceptors(advisor);
        ret.addAll(Arrays.asList(interceptors));
      }
    }

    return ret;
  }

  public static Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
    if (adviceObject instanceof Advisor) {
      return (Advisor) adviceObject;
    }
    if (adviceObject instanceof Advice) {
      Advice advice = (Advice) adviceObject;
      if (advice instanceof MethodInterceptor) {
        // So well-known it doesn't even need an adapter.
        return new DefaultPointcutAdvisor(advice);
      }
      for (AdvisorAdapter adapter : advisorAdapters) {
        // Check that it is supported.
        if (adapter.supportsAdvice(advice)) {
          return new DefaultPointcutAdvisor(advice);
        }
      }
    }
    throw new UnknownAdviceTypeException(adviceObject);
  }

  public static MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
    List<MethodInterceptor> interceptors = new ArrayList<>(3);
    Advice advice = advisor.getAdvice();
    if (advice instanceof MethodInterceptor) {
      interceptors.add((MethodInterceptor) advice);
    }
    for (AdvisorAdapter adapter : advisorAdapters) {
      if (adapter.supportsAdvice(advice)) {
        interceptors.add(adapter.getInterceptor(advisor));
      }
    }
    if (interceptors.isEmpty()) {
      throw new UnknownAdviceTypeException(advisor.getAdvice());
    }
    return interceptors.toArray(new MethodInterceptor[0]);
  }

  /**
   * Determine whether the Advisors contain matching introductions.
   */
  private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
    for (Advisor advisor : advisors) {
      if (advisor instanceof IntroductionAdvisor) {
        IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
        if (ia.getClassFilter().matches(actualClass)) {
          return true;
        }
      }
    }
    return false;
  }

  // AopProxy

  /**
   * Create an {@link AopProxy} for the given AOP configuration.
   *
   * @param config
   *         the AOP configuration in the form of an
   *         AdvisedSupport object
   *
   * @return the corresponding AOP proxy
   *
   * @throws AopConfigException
   *         if the configuration is invalid
   */
  public static AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
    if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
      Class<?> targetClass = config.getTargetClass();
      if (targetClass == null) {
        throw new AopConfigException(
                "TargetSource cannot determine target class: " +
                        "Either an interface or a target is required for proxy creation.");
      }
      if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
        return new JdkDynamicAopProxy(config);
      }

      if (config.isUsingCglib()) {
        return new CglibAopProxy(config);
      }
      return new StandardAopProxy(config);
    }
    else {
      return new JdkDynamicAopProxy(config);
    }
  }

  /**
   * Determine whether the supplied {@link AdvisedSupport} has only the
   * {@link cn.taketoday.aop.proxy.StandardProxy} interface specified
   * (or no proxy interfaces specified at all).
   */
  private static boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
    Class<?>[] ifcs = config.getProxiedInterfaces();
    return (ifcs.length == 0 || (ifcs.length == 1 && StandardProxy.class.isAssignableFrom(ifcs[0])));
  }

  // AdvisorAdapter

  /**
   * Add {@link AdvisorAdapter} to {@link #advisorAdapters} and sort them
   *
   * @param adapters
   *         new AdvisorAdapters
   */
  public static void addAdvisorAdapters(AdvisorAdapter... adapters) {
    Collections.addAll(advisorAdapters, adapters);
    OrderUtils.reversedSort(advisorAdapters);
  }

  static class BeforeAdvisorAdapter implements AdvisorAdapter {

    @Override
    public boolean supportsAdvice(Advice advice) {
      return advice instanceof MethodBeforeAdvice;
    }

    @Override
    public MethodInterceptor getInterceptor(Advisor advisor) {
      final MethodBeforeAdvice advice = (MethodBeforeAdvice) advisor.getAdvice();
      class Interceptor implements MethodInterceptor, Ordered {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
          advice.before(invocation);
          return invocation.proceed();
        }

        @Override
        public int getOrder() {
          return BeforeMethodInterceptor.DEFAULT_ORDER;
        }
      }
      return new Interceptor();
    }
  }

  static class AfterReturningAdvisorAdapter implements AdvisorAdapter {

    @Override
    public boolean supportsAdvice(Advice advice) {
      return advice instanceof AfterReturningAdvice;
    }

    @Override
    public MethodInterceptor getInterceptor(Advisor advisor) {
      final AfterReturningAdvice advice = (AfterReturningAdvice) advisor.getAdvice();

      class Interceptor implements MethodInterceptor, Ordered {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
          final Object returnValue = invocation.proceed();
          advice.afterReturning(returnValue, invocation);
          return returnValue;
        }

        @Override
        public int getOrder() {
          return AfterReturningMethodInterceptor.DEFAULT_ORDER;
        }
      }
      return new Interceptor();
    }
  }

  static class ThrowsAdviceAdvisorAdapter implements AdvisorAdapter {

    @Override
    public boolean supportsAdvice(Advice advice) {
      return advice instanceof ThrowsAdvice;
    }

    @Override
    public MethodInterceptor getInterceptor(Advisor advisor) {
      final ThrowsAdvice advice = (ThrowsAdvice) advisor.getAdvice();

      class Interceptor implements MethodInterceptor, Ordered {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
          try {
            return invocation.proceed();
          }
          catch (Throwable ex) {
            return advice.afterThrowing(ex, invocation);
          }
        }

        @Override
        public int getOrder() {
          return AfterThrowingMethodInterceptor.DEFAULT_ORDER;
        }
      }

      return new Interceptor();
    }
  }

}
