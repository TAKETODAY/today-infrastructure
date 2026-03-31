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

package infra.test.context.bean.override.mockito;

import org.mockito.plugins.MockResolver;

import infra.aop.TargetSource;
import infra.aop.framework.Advised;
import infra.aop.support.AopUtils;
import infra.lang.Assert;
import infra.util.ClassUtils;

/**
 * A {@link MockResolver} for testing Infra applications with Mockito.
 *
 * <p>Resolves mocks by walking the Infra AOP proxy chain until the target or a
 * non-static proxy is found.
 *
 * @author Sam Brannen
 * @author Andy Wilkinson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class InfraMockResolver implements MockResolver {

  static final boolean INFRA_AOP_PRESENT = ClassUtils.isPresent(
          "infra.aop.framework.Advised", InfraMockResolver.class.getClassLoader());

  @Override
  public Object resolve(Object instance) {
    if (INFRA_AOP_PRESENT) {
      return getUltimateTargetObject(instance);
    }
    return instance;
  }

  /**
   * This is a modified version of
   * {@link infra.test.util.AopTestUtils#getUltimateTargetObject(Object)
   * AopTestUtils#getUltimateTargetObject()} which only checks static target sources.
   *
   * @param candidate the instance to check (potentially a Infra AOP proxy;
   * never {@code null})
   * @return the target object or the {@code candidate} (never {@code null})
   * @throws IllegalStateException if an error occurs while unwrapping a proxy
   * @see Advised#getTargetSource()
   * @see TargetSource#isStatic()
   */
  static Object getUltimateTargetObject(Object candidate) {
    Assert.notNull(candidate, "Candidate is required");
    try {
      if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised advised) {
        TargetSource targetSource = advised.getTargetSource();
        if (targetSource.isStatic()) {
          Object target = targetSource.getTarget();
          if (target != null) {
            return getUltimateTargetObject(target);
          }
        }
      }
    }
    catch (Throwable ex) {
      throw new IllegalStateException("Failed to unwrap proxied object", ex);
    }
    return candidate;
  }

  /**
   * Reject the supplied bean if it is not a supported candidate to spy on.
   * <p>Specifically, this method ensures that the bean is not a Infra AOP proxy
   * with a non-static {@link TargetSource}.
   *
   * @param beanName the name of the bean to spy on
   * @param bean the bean to spy on
   * @see #getUltimateTargetObject(Object)
   * @since 5.0
   */
  static void rejectUnsupportedSpyTarget(String beanName, Object bean) throws IllegalStateException {
    if (INFRA_AOP_PRESENT) {
      if (AopUtils.isAopProxy(bean) && bean instanceof Advised advised &&
              !advised.getTargetSource().isStatic()) {
        throw new IllegalStateException("""
                @MockitoSpyBean cannot be applied to bean '%s', because it is a Infra AOP proxy \
                with a non-static TargetSource. Perhaps you have attempted to spy on a scoped proxy, \
                which is not supported.""".formatted(beanName));
      }
    }
  }

}
