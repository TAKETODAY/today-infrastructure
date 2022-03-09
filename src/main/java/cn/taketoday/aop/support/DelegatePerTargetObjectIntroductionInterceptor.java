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

package cn.taketoday.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import java.io.Serial;
import java.util.WeakHashMap;

import cn.taketoday.aop.DynamicIntroductionAdvice;
import cn.taketoday.aop.IntroductionInterceptor;
import cn.taketoday.aop.framework.AbstractMethodInvocation;
import cn.taketoday.util.ReflectionUtils;

/**
 * Convenient implementation of the {@link IntroductionInterceptor} interface.
 *
 * <p>This differs from {@link DelegatingIntroductionInterceptor} in that a single
 * instance of this class can be used to advise multiple target objects, and each target
 * object will have its <i>own</i> delegate (whereas DelegatingIntroductionInterceptor
 * shares the same delegate, and hence the same state across all targets).
 *
 * <p>The {@code suppressInterface} method can be used to suppress interfaces
 * implemented by the delegate class but which should not be introduced to the
 * owning AOP proxy.
 *
 * <p>An instance of this class is serializable if the delegates are.
 *
 * <p><i>Note: There are some implementation similarities between this class and
 * {@link DelegatingIntroductionInterceptor} that suggest a possible refactoring
 * to extract a common ancestor class in the future.</i>
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author TODAY 2021/3/8 22:21
 * @see #suppressInterface
 * @see DelegatingIntroductionInterceptor
 * @since 3.0
 */
public class DelegatePerTargetObjectIntroductionInterceptor
        extends IntroductionInfoSupport implements IntroductionInterceptor {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Hold weak references to keys as we don't want to interfere with garbage collection..
   */
  private final WeakHashMap<Object, Object> delegateMap = new WeakHashMap<>();

  private final Class<?> interfaceType;
  private final Class<?> defaultImplType;

  public DelegatePerTargetObjectIntroductionInterceptor(Class<?> defaultImplType, Class<?> interfaceType) {
    this.interfaceType = interfaceType;
    this.defaultImplType = defaultImplType;
    // Create a new delegate now (but don't store it in the map).
    // We do this for two reasons:
    // 1) to fail early if there is a problem instantiating delegates
    // 2) to populate the interface map once and once only
    Object delegate = createNewDelegate();
    implementInterfacesOnObject(delegate);
    suppressInterface(IntroductionInterceptor.class);
    suppressInterface(DynamicIntroductionAdvice.class);
  }

  /**
   * Subclasses may need to override this if they want to perform custom
   * behaviour in around advice. However, subclasses should invoke this
   * method, which handles introduced interfaces and forwarding to the target.
   */
  @Override
  public Object invoke(MethodInvocation mi) throws Throwable {
    if (isMethodOnIntroducedInterface(mi)) {
      Object delegate = getIntroductionDelegateFor(mi.getThis());

      // Using the following method rather than direct reflection,
      // we get correct handling of InvocationTargetException
      // if the introduced method throws an exception.

      Object retVal = AopUtils.invokeJoinpointUsingReflection(delegate, mi.getMethod(), mi.getArguments());

      // Massage return value if possible: if the delegate returned itself,
      // we really want to return the proxy.
      if (retVal == delegate && mi instanceof AbstractMethodInvocation) {
        retVal = ((AbstractMethodInvocation) mi).getProxy();
      }
      return retVal;
    }

    return doProceed(mi);
  }

  /**
   * Proceed with the supplied {@link org.aopalliance.intercept.MethodInterceptor}.
   * Subclasses can override this method to intercept method invocations on the
   * target object which is useful when an introduction needs to monitor the object
   * that it is introduced into. This method is <strong>never</strong> called for
   * {@link MethodInvocation MethodInvocations} on the introduced interfaces.
   */
  protected Object doProceed(MethodInvocation mi) throws Throwable {
    // If we get here, just pass the invocation on.
    return mi.proceed();
  }

  private Object getIntroductionDelegateFor(Object targetObject) {
    synchronized(this.delegateMap) {
      if (this.delegateMap.containsKey(targetObject)) {
        return this.delegateMap.get(targetObject);
      }
      else {
        Object delegate = createNewDelegate();
        this.delegateMap.put(targetObject, delegate);
        return delegate;
      }
    }
  }

  private Object createNewDelegate() {
    try {
      return ReflectionUtils.accessibleConstructor(this.defaultImplType).newInstance();
    }
    catch (Throwable ex) {
      throw new IllegalArgumentException(
              "Cannot create default implementation for '" +
                      this.interfaceType.getName() + "' mixin (" + this.defaultImplType.getName() + "): " + ex);
    }
  }

}
