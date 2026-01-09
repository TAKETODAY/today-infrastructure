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

  @Nullable
  private StringValueResolver embeddedValueResolver;

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

    private final Map<Object, ConcurrencyThrottleCache> cachePerInstance =
            Collections.synchronizedMap(new IdentityHashMap<>(16));

    @Nullable
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      Method method = invocation.getMethod();
      Object target = invocation.getThis();
      Class<?> targetClass = (target != null ? target.getClass() : method.getDeclaringClass());
      if (target == null && invocation instanceof ProxyMethodInvocation methodInvocation) {
        // Allow validation for AOP proxy without a target
        target = methodInvocation.getProxy();
      }
      Assert.state(target != null, "Target is required");

      ConcurrencyThrottleCache cache = cachePerInstance.computeIfAbsent(target, k -> new ConcurrencyThrottleCache());
      MethodInterceptor interceptor = cache.methodInterceptors.get(method);
      if (interceptor == null) {
        synchronized(cache) {
          interceptor = cache.methodInterceptors.get(method);
          if (interceptor == null) {
            boolean perMethod = false;
            ConcurrencyLimit anno = AnnotatedElementUtils.getMergedAnnotation(method, ConcurrencyLimit.class);
            if (anno != null) {
              perMethod = true;
            }
            else {
              interceptor = cache.classInterceptor;
              if (interceptor == null) {
                anno = AnnotatedElementUtils.getMergedAnnotation(targetClass, ConcurrencyLimit.class);
              }
            }
            if (interceptor == null) {
              Assert.state(anno != null, "No @ConcurrencyLimit annotation found");
              int concurrencyLimit = parseInt(anno.limit(), anno.limitString());
              if (concurrencyLimit < -1) {
                throw new IllegalStateException(anno + " must be configured with a valid limit");
              }
              interceptor = new ConcurrencyThrottleInterceptor(concurrencyLimit);
              if (!perMethod) {
                cache.classInterceptor = interceptor;
              }
            }
            cache.methodInterceptors.put(method, interceptor);
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

  private static final class ConcurrencyThrottleCache {

    public final ConcurrentHashMap<Method, MethodInterceptor> methodInterceptors = new ConcurrentHashMap<>();

    @Nullable
    public MethodInterceptor classInterceptor;
  }

}
