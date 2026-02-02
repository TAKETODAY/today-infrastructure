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

package infra.resilience.annotation;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.aop.Pointcut;
import infra.aop.ProxyMethodInvocation;
import infra.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import infra.aop.interceptor.ConcurrencyThrottleInterceptor;
import infra.aop.support.ComposablePointcut;
import infra.aop.support.DefaultPointcutAdvisor;
import infra.aop.support.annotation.AnnotationMatchingPointcut;
import infra.context.expression.EmbeddedValueResolverAware;
import infra.core.StringValueResolver;
import infra.core.annotation.AnnotatedElementUtils;
import infra.lang.Assert;
import infra.resilience.InvocationRejectedException;
import infra.util.ClassUtils;
import infra.util.StringUtils;

/**
 * A convenient {@link infra.beans.factory.config.BeanPostProcessor
 * BeanPostProcessor} that applies a concurrency interceptor to all bean methods
 * annotated with {@link ConcurrencyLimit @ConcurrencyLimit}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class ConcurrencyLimitBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor implements EmbeddedValueResolverAware {

  private @Nullable StringValueResolver embeddedValueResolver;

  public ConcurrencyLimitBeanPostProcessor() {
    setBeforeExistingAdvisors(true);

    Pointcut cpc = new AnnotationMatchingPointcut(ConcurrencyLimit.class, true);
    Pointcut mpc = new AnnotationMatchingPointcut(null, ConcurrencyLimit.class, true);
    this.advisor = new DefaultPointcutAdvisor(
            new ComposablePointcut(cpc).union(mpc),
            new ConcurrencyLimitInterceptor());
  }

  @Override
  public void setEmbeddedValueResolver(StringValueResolver resolver) {
    this.embeddedValueResolver = resolver;
  }

  private final class ConcurrencyLimitInterceptor implements MethodInterceptor {

    private final Map<Object, ConcurrencyThrottleHolder> holderPerInstance =
            Collections.synchronizedMap(new IdentityHashMap<>(16));

    @Override
    public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
      Method method = invocation.getMethod();
      Object instance = invocation.getThis();
      Class<?> targetClass = (instance != null ? instance.getClass() : method.getDeclaringClass());
      if (invocation instanceof ProxyMethodInvocation methodInvocation) {
        // Apply concurrency throttling at the AOP proxy level (independent of target instance)
        instance = methodInvocation.getProxy();
      }
      Assert.state(instance != null, "Unique instance required - use a ProxyMethodInvocation");

      // Build unique ConcurrencyThrottleHolder instance per target object
      ConcurrencyThrottleHolder holder = this.holderPerInstance.computeIfAbsent(instance,
              k -> new ConcurrencyThrottleHolder());

      // Determine method-specific interceptor instance with isolated concurrency count
      MethodInterceptor interceptor = holder.methodInterceptors.get(method);
      if (interceptor == null) {
        synchronized(holder) {
          interceptor = holder.methodInterceptors.get(method);
          if (interceptor == null) {
            boolean perMethod = false;
            ConcurrencyLimit annotation = AnnotatedElementUtils.findMergedAnnotation(method, ConcurrencyLimit.class);
            if (annotation != null) {
              perMethod = true;
            }
            else {
              interceptor = holder.classInterceptor;
              if (interceptor == null) {
                annotation = AnnotatedElementUtils.findMergedAnnotation(targetClass, ConcurrencyLimit.class);
              }
            }
            if (interceptor == null) {
              Assert.state(annotation != null, "No @ConcurrencyLimit annotation found");
              int concurrencyLimit = parseInt(annotation.limit(), annotation.limitString());
              if (concurrencyLimit < -1) {
                throw new IllegalStateException(annotation + " must be configured with a valid limit");
              }
              String name = (perMethod ? ClassUtils.getQualifiedMethodName(method) : targetClass.getName());
              interceptor = (annotation.policy() == ConcurrencyLimit.ThrottlePolicy.REJECT ?
                      new RejectingConcurrencyThrottleInterceptor(concurrencyLimit, name, instance) :
                      new ResilienceConcurrencyThrottleInterceptor(concurrencyLimit, name, instance));
              if (!perMethod) {
                holder.classInterceptor = interceptor;
              }
            }
            holder.methodInterceptors.put(method, interceptor);
          }
        }
      }
      return interceptor.invoke(invocation);
    }

    private int parseInt(int value, String stringValue) {
      if (StringUtils.hasText(stringValue)) {
        if (embeddedValueResolver != null) {
          stringValue = embeddedValueResolver.resolveStringValue(stringValue);
        }
        if (StringUtils.hasText(stringValue)) {
          return Integer.parseInt(stringValue);
        }
      }
      return value;
    }
  }

  private static class ConcurrencyThrottleHolder {

    final ConcurrentHashMap<Method, MethodInterceptor> methodInterceptors = new ConcurrentHashMap<>();

    @Nullable MethodInterceptor classInterceptor;
  }

  private static class ResilienceConcurrencyThrottleInterceptor extends ConcurrencyThrottleInterceptor {

    private final String identifier;

    private final Object target;

    public ResilienceConcurrencyThrottleInterceptor(int concurrencyLimit, String identifier, Object target) {
      super(concurrencyLimit);
      this.identifier = identifier;
      this.target = target;
    }

    @Override
    protected void onAccessRejected(String msg) {
      throw new InvocationRejectedException(msg + " " + this.identifier, this.target);
    }
  }

  private static class RejectingConcurrencyThrottleInterceptor extends ResilienceConcurrencyThrottleInterceptor {

    public RejectingConcurrencyThrottleInterceptor(int concurrencyLimit, String identifier, Object target) {
      super(concurrencyLimit, identifier, target);
    }

    @Override
    protected void onLimitReached() {
      onAccessRejected("Concurrency limit reached: " + getConcurrencyLimit() + " - not allowed to enter");
    }
  }

}
