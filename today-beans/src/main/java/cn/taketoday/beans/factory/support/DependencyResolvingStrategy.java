/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Parameter;
import java.util.Set;

import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Nullable;

/**
 * resolve dependency
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 22:36</a>
 * @since 4.0
 */
@FunctionalInterface
public interface DependencyResolvingStrategy {

  /**
   * Resolve dependency from DependencyDescriptor
   *
   * @param descriptor Target method {@link Parameter} or a {@link java.lang.reflect.Field}
   * @param context resolving context never {@code null}
   */
  @Nullable
  Object resolveDependency(DependencyDescriptor descriptor, Context context);

  /**
   * context
   */
  class Context {

    @Nullable
    public final Set<String> dependentBeans;

    @Nullable
    public final TypeConverter typeConverter;

    @Nullable
    public final String requestingBeanName;

    public Context(@Nullable String requestingBeanName,
            @Nullable Set<String> dependentBeans, @Nullable TypeConverter typeConverter) {
      this.typeConverter = typeConverter;
      this.dependentBeans = dependentBeans;
      this.requestingBeanName = requestingBeanName;
    }

    /**
     * add dependent bean
     */
    public void addDependentBean(String beanName) {
      if (dependentBeans != null) {
        dependentBeans.add(beanName);
      }
    }

    @Override
    public String toString() {
      return ToStringBuilder.from(this)
              .append("dependentBeans", dependentBeans)
              .append("typeConverter", typeConverter)
              .append("requestingBeanName", requestingBeanName)
              .toString();
    }

  }

}
