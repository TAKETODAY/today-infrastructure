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

package infra.beans.factory.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.Set;

import infra.beans.TypeConverter;
import infra.beans.factory.config.DependencyDescriptor;
import infra.core.style.ToStringBuilder;

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

    public final @Nullable Set<String> dependentBeans;

    public final @Nullable TypeConverter typeConverter;

    public final @Nullable String requestingBeanName;

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
      return ToStringBuilder.forInstance(this)
              .append("dependentBeans", dependentBeans)
              .append("typeConverter", typeConverter)
              .append("requestingBeanName", requestingBeanName)
              .toString();
    }

  }

}
