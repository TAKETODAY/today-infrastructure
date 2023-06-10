/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.cache.jcache.interceptor;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.support.AbstractBeanFactoryPointcutAdvisor;
import cn.taketoday.aop.support.StaticMethodMatcherPointcut;
import cn.taketoday.lang.Nullable;

/**
 * Advisor driven by a {@link JCacheOperationSource}, used to include a
 * cache advice bean for methods that are cacheable.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BeanFactoryJCacheOperationSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

  @Serial
  private static final long serialVersionUID = 1L;

  private final JCacheOperationSourcePointcut pointcut = new JCacheOperationSourcePointcut();

  /**
   * Set the cache operation attribute source which is used to find cache
   * attributes. This should usually be identical to the source reference
   * set on the cache interceptor itself.
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

  private static class JCacheOperationSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

    @Nullable
    private JCacheOperationSource cacheOperationSource;

    public void setCacheOperationSource(@Nullable JCacheOperationSource cacheOperationSource) {
      this.cacheOperationSource = cacheOperationSource;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
      return (this.cacheOperationSource == null ||
              this.cacheOperationSource.getCacheOperation(method, targetClass) != null);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return (this == other || (other instanceof JCacheOperationSourcePointcut otherPc &&
              Objects.equals(this.cacheOperationSource, otherPc.cacheOperationSource)));
    }

    @Override
    public int hashCode() {
      return JCacheOperationSourcePointcut.class.hashCode();
    }

    @Override
    public String toString() {
      return getClass().getName() + ": " + this.cacheOperationSource;
    }
  }

}
