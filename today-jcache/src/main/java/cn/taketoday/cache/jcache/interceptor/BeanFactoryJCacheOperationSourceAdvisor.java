/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.cache.jcache.interceptor;

import java.io.Serial;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.support.AbstractBeanFactoryPointcutAdvisor;

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
   * Set the {@link cn.taketoday.aop.ClassFilter} to use for this pointcut.
   * Default is {@link cn.taketoday.aop.ClassFilter#TRUE}.
   */
  public void setClassFilter(ClassFilter classFilter) {
    this.pointcut.setClassFilter(classFilter);
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

}
