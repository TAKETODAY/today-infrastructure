/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import infra.aop.Advisor;
import infra.aop.AopInvocationException;
import infra.aop.IntroductionAdvisor;
import infra.aop.IntroductionAwareMethodMatcher;
import infra.aop.MethodMatcher;
import infra.aop.Pointcut;
import infra.aop.PointcutAdvisor;
import infra.aop.TargetClassAware;
import infra.aop.framework.AopProxyUtils;
import infra.aop.framework.StandardProxy;
import infra.core.BridgeMethodResolver;
import infra.core.MethodIntrospector;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.ReflectionUtils;

/**
 * Utility methods for AOP support code.
 *
 * <p>Mainly for internal use within AOP support.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AopProxyUtils
 * @since 3.0 2021/2/1 18:46
 */
public abstract class AopUtils {

  /**
   * Check whether the given object is a JDK dynamic proxy or a CGLIB proxy.
   * <p>This method additionally checks if the given object is an instance
   * of {@link StandardProxy}.
   *
   * @param object the object to check
   * @see #isJdkDynamicProxy
   * @see #isCglibProxy
   */
  public static boolean isAopProxy(Object object) {
    return (object instanceof StandardProxy
            && (Proxy.isProxyClass(object.getClass()) || object.getClass().getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)));
  }

  /**
   * Check whether the given object is a JDK dynamic proxy.
   * <p>This method goes beyond the implementation of
   * {@link Proxy#isProxyClass(Class)} by additionally checking if the
   * given object is an instance of {@link StandardProxy}.
   *
   * @param object the object to check
   * @see java.lang.reflect.Proxy#isProxyClass
   */
  public static boolean isJdkDynamicProxy(Object object) {
    return (object instanceof StandardProxy && Proxy.isProxyClass(object.getClass()));
  }

  /**
   * Check whether the given object is a CGLIB proxy.
   *
   * @param object the object to check
   */
  public static boolean isCglibProxy(Object object) {
    return object instanceof StandardProxy
            && object.getClass().getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR);
  }

  /**
   * Determine the target class of the given bean instance which might be an AOP proxy.
   * <p>Returns the target class for an AOP proxy or the plain class otherwise.
   *
   * @param candidate the instance to check (might be an AOP proxy)
   * @return the target class (or the plain class of the given object as fallback;
   * never {@code null})
   * @see TargetClassAware#getTargetClass()
   * @see AopProxyUtils#ultimateTargetClass(Object)
   */
  public static Class<?> getTargetClass(Object candidate) {
    Assert.notNull(candidate, "Candidate object is required");
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
   * @param invocation the instance to check
   * @return the target class (or the plain class of the given object as fallback;
   * never {@code null})
   * @see TargetClassAware#getTargetClass()
   */
  public static Class<?> getTargetClass(MethodInvocation invocation) {
    Assert.notNull(invocation, "MethodInvocation is required");
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
   * Given a method, which may come from an interface, and a target class used
   * in the current AOP invocation, find the corresponding target method if there
   * is one. E.g. the method may be {@code IFoo.bar()} and the target class
   * may be {@code DefaultFoo}. In this case, the method may be
   * {@code DefaultFoo.bar()}. This enables attributes on that method to be found.
   * <p><b>NOTE:</b> In contrast to {@link ReflectionUtils#getMostSpecificMethod},
   * this method resolves bridge methods in order to retrieve attributes from
   * the <i>original</i> method definition.
   *
   * @param method the method to be invoked, which may come from an interface
   * @param targetClass the target class for the current invocation.
   * May be {@code null} or may not even implement the method.
   * @return the specific target method, or the original method if the
   * {@code targetClass} doesn't implement it or is {@code null}
   * @see ReflectionUtils#getMostSpecificMethod
   * @see BridgeMethodResolver#getMostSpecificMethod
   * @since 4.0
   */
  public static Method getMostSpecificMethod(Method method, @Nullable Class<?> targetClass) {
    Class<?> specificTargetClass = (targetClass != null ? ClassUtils.getUserClass(targetClass) : null);
    return BridgeMethodResolver.getMostSpecificMethod(method, specificTargetClass);
  }

  /**
   * Can the given pointcut apply at all on the given class?
   * <p>This is an important test as it can be used to optimize
   * out a pointcut for a class.
   *
   * @param pc the static or dynamic pointcut to check
   * @param targetClass the class to test
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
   * @param pc the static or dynamic pointcut to check
   * @param targetClass the class to test
   * @param hasIntroductions whether or not the advisor chain
   * for this bean includes any introductions
   * @return whether the pointcut can apply on any method
   */
  public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
    Assert.notNull(pc, "Pointcut is required");
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
   * @param advisor the advisor to check
   * @param targetClass class we're testing
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
   * @param advisor the advisor to check
   * @param targetClass class we're testing
   * @param hasIntroductions whether or not the advisor chain for this bean includes
   * any introductions
   * @return whether the pointcut can apply on any method
   */
  public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
    if (advisor instanceof IntroductionAdvisor) {
      return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
    }
    else if (advisor instanceof PointcutAdvisor pca) {
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
   * @param candidateAdvisors the Advisors to evaluate
   * @param clazz the target class
   * @return sublist of Advisors that can apply to an object of the given class
   * (may be the incoming List as-is)
   */
  public static List<Advisor> filterAdvisors(List<Advisor> candidateAdvisors, Class<?> clazz) {
    if (candidateAdvisors.isEmpty()) {
      return candidateAdvisors;
    }
    ArrayList<Advisor> eligibleAdvisors = new ArrayList<>();
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
   * @param target the target object
   * @param method the method to invoke
   * @param args the arguments for the method
   * @return the invocation result, if any
   * @throws Throwable if thrown by the target method
   * @throws AopInvocationException in case of a reflection error
   */
  public static Object invokeJoinpointUsingReflection(@Nullable Object target, Method method, Object[] args)
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
              "AOP configuration seems to be invalid: tried calling method [%s] on target [%s]".formatted(method, target), ex);
    }
    catch (IllegalAccessException | InaccessibleObjectException ex) {
      throw new AopInvocationException("Could not access method [%s]".formatted(method), ex);
    }
  }

  /**
   * Select an invocable method on the target type: either the given method itself
   * if actually exposed on the target type, or otherwise a corresponding method
   * on one of the target type's interfaces or on the target type itself.
   *
   * @param method the method to check
   * @param targetType the target type to search methods on (typically an AOP proxy)
   * @return a corresponding invocable method on the target type
   * @throws IllegalStateException if the given method is not invocable on the given
   * target type (typically due to a proxy mismatch)
   * @see MethodIntrospector#selectInvocableMethod(Method, Class)
   * @since 4.0
   */
  public static Method selectInvocableMethod(Method method, @Nullable Class<?> targetType) {
    if (targetType == null) {
      return method;
    }
    Method methodToUse = MethodIntrospector.selectInvocableMethod(method, targetType);
    if (Modifier.isPrivate(methodToUse.getModifiers())
            && !Modifier.isStatic(methodToUse.getModifiers())
            && StandardProxy.class.isAssignableFrom(targetType)) {
      throw new IllegalStateException(String.format(
              "Need to invoke method '%s' found on proxy for target class '%s' but cannot " +
                      "be delegated to target bean. Switch its visibility to package or protected.",
              method.getName(), method.getDeclaringClass().getSimpleName()));
    }
    return methodToUse;
  }
}
