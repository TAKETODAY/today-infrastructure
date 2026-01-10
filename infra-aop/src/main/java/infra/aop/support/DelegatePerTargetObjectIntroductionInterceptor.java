/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop.support;

import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.WeakHashMap;

import infra.aop.DynamicIntroductionAdvice;
import infra.aop.IntroductionInterceptor;
import infra.aop.ProxyMethodInvocation;
import infra.util.ReflectionUtils;

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
public class DelegatePerTargetObjectIntroductionInterceptor extends IntroductionInfoSupport implements IntroductionInterceptor {

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
  @Nullable
  @SuppressWarnings("NullAway")
  public Object invoke(MethodInvocation mi) throws Throwable {
    if (isMethodOnIntroducedInterface(mi)) {
      Object delegate = getIntroductionDelegateFor(mi.getThis());

      // Using the following method rather than direct reflection,
      // we get correct handling of InvocationTargetException
      // if the introduced method throws an exception.

      Object retVal = AopUtils.invokeJoinpointUsingReflection(delegate, mi.getMethod(), mi.getArguments());

      // Massage return value if possible: if the delegate returned itself,
      // we really want to return the proxy.
      if (retVal == delegate && mi instanceof ProxyMethodInvocation) {
        retVal = ((ProxyMethodInvocation) mi).getProxy();
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
  @Nullable
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
      throw new IllegalArgumentException("Cannot create default implementation for '%s' mixin (%s): %s"
              .formatted(this.interfaceType.getName(), this.defaultImplType.getName(), ex));
    }
  }

}
