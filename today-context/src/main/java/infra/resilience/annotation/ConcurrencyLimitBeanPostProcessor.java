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
