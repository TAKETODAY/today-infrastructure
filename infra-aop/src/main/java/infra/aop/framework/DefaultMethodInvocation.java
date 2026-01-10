/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import infra.aop.ProxyMethodInvocation;
import infra.aop.support.AopUtils;
import infra.core.AttributeAccessorSupport;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;

import static infra.aop.InterceptorChainFactory.EMPTY_INTERCEPTOR;

/**
 * Default ProxyMethodInvocation
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-10 13:14
 */
public class DefaultMethodInvocation extends AttributeAccessorSupport implements ProxyMethodInvocation, Cloneable {

  private final Object proxy;

  protected Object[] args;

  @Nullable
  protected final Object target;

  protected final Method method;

  @Nullable
  protected final Class<?> targetClass;

  protected final MethodInterceptor[] advices;

  /**
   * a flag show that current index of advice
   */
  private int currentAdviceIndex = 0;

  private final int adviceLength;

  public DefaultMethodInvocation(Object proxy, Method method, Class<?> targetClass, Object[] arguments) {
    this(proxy, null, method, targetClass, arguments, EMPTY_INTERCEPTOR);
  }

  /**
   * @throws NullPointerException if advices is {@code null}
   */
  public DefaultMethodInvocation(Object proxy, @Nullable Object target,
          Method method, @Nullable Class<?> targetClass, Object[] arguments, MethodInterceptor[] advices) {
    this.proxy = proxy;
    this.target = target;
    this.method = method;
    this.targetClass = targetClass;
    this.args = ClassUtils.adaptArgumentsIfNecessary(method, arguments);
    this.advices = advices;
    this.adviceLength = advices.length;
  }

  /**
   * Return the proxy that this method invocation was made through.
   *
   * @return the original proxy object
   */
  @Override
  public Object getProxy() {
    return proxy;
  }

  /**
   * Basic logic. Proceeds to the next interceptor in the chain.
   * <p>
   * Subclasses can override this method to handle {@link Exception}
   * </p>
   *
   * @return see the children interfaces' proceed definition.
   * @throws Throwable if the join-point throws an exception.
   * @see CglibAopProxy.CglibMethodInvocation
   * @see DefaultMethodInvocation
   */
  @Nullable
  @Override
  public Object proceed() throws Throwable {
    if (currentAdviceIndex < adviceLength) {
      // It's an interceptor, so we just invoke it
      // runtime interceptor will automatically matches MethodInvocation
      return advices[currentAdviceIndex++].invoke(this);
    }
    // join-point
    return invokeJoinPoint();
  }

  /**
   * Invoke jon-point
   *
   * @return the result of the call to {@link MethodInvocation#proceed()}, might be
   * intercepted by the interceptor.
   */
  @Nullable
  protected Object invokeJoinPoint() throws Throwable {
    return AopUtils.invokeJoinpointUsingReflection(target, method, args);
  }

  /**
   * This implementation returns a shallow copy of this invocation object,
   * including an independent copy of the original arguments array.
   * <p>We want a shallow copy in this case: We want to use the same interceptor
   * chain and other object references, but we want an independent value for the
   * current interceptor index.
   *
   * @see java.lang.Object#clone()
   */
  @Override
  public MethodInvocation invocableClone() {
    Object[] cloneArguments = this.args;
    if (ObjectUtils.isNotEmpty(cloneArguments)) {
      // Build an independent copy of the arguments array.
      cloneArguments = cloneArguments.clone();
    }
    return invocableClone(cloneArguments);
  }

  /**
   * This implementation returns a shallow copy of this invocation object,
   * using the given arguments array for the clone.
   * <p>We want a shallow copy in this case: We want to use the same interceptor
   * chain and other object references, but we want an independent value for the
   * current interceptor index.
   *
   * @see java.lang.Object#clone()
   */
  @Override
  public MethodInvocation invocableClone(Object... arguments) {
    // Force initialization of the user attributes Map,
    // for having a shared Map reference in the clone.
    if (this.attributes == null) {
      this.attributes = new HashMap<>();
    }

    // Create the ProxyMethodInvocation clone.
    try {
      ProxyMethodInvocation clone = (ProxyMethodInvocation) clone();
      clone.setArguments(arguments);
      return clone;
    }
    catch (CloneNotSupportedException ex) {
      throw new IllegalStateException(
              "Should be able to clone object of type [%s]: %s".formatted(getClass(), ex));
    }
  }

  @Override
  public Method getMethod() {
    return method;
  }

  @Override
  public Object[] getArguments() {
    return args;
  }

  @Override
  public void setArguments(Object[] arguments) {
    args = arguments;
  }

  @Nullable
  @Override
  public Object getThis() {
    return target;
  }

  @Nullable
  @Override
  public Class<?> getTargetClass() {
    return targetClass;
  }

  @Override
  public AccessibleObject getStaticPart() {
    return method;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final DefaultMethodInvocation that))
      return false;
    if (!super.equals(o))
      return false;
    return currentAdviceIndex == that.currentAdviceIndex
            && adviceLength == that.adviceLength
            && Arrays.equals(args, that.args)
            && Objects.equals(target, that.target)
            && Objects.equals(method, that.method)
            && Arrays.equals(advices, that.advices);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(super.hashCode(), target, method);
    result = 31 * result + Arrays.hashCode(args);
    result = 31 * result + Arrays.hashCode(advices);
    return result;
  }

  @Override
  public String toString() {
    // Don't do toString on target, it may be proxied.
    StringBuilder sb = new StringBuilder("DefaultMethodInvocation: ");
    sb.append(this.method).append("; ");
    if (this.target == null) {
      sb.append("target is null");
    }
    else {
      sb.append("target is of class [").append(this.target.getClass().getName()).append(']');
    }
    return sb.toString();
  }

}
