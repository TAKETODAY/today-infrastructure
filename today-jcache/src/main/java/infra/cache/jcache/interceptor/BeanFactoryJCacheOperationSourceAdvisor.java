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

package infra.cache.jcache.interceptor;

import java.io.Serial;

import infra.aop.ClassFilter;
import infra.aop.Pointcut;
import infra.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * Advisor driven by a {@link JCacheOperationSource}, used to include a
 * cache advice bean for methods that are cacheable.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BeanFactoryJCacheOperationSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

  @Serial
  private static final long serialVersionUID = 1L;

  private final JCacheOperationSourcePointcut pointcut = new JCacheOperationSourcePointcut();

  /**
   * Set the cache operation attribute source which is used to find cache
   * attributes. This should usually be identical to the source reference
   * set on the cache interceptor itself.
   *
   * @see JCacheInterceptor#setCacheOperationSource
   */
  public void setCacheOperationSource(JCacheOperationSource cacheOperationSource) {
    this.pointcut.setCacheOperationSource(cacheOperationSource);
  }

  /**
   * Set the {@link infra.aop.ClassFilter} to use for this pointcut.
   * Default is {@link infra.aop.ClassFilter#TRUE}.
   */
  public void setClassFilter(ClassFilter classFilter) {
    this.pointcut.setClassFilter(classFilter);
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

}
