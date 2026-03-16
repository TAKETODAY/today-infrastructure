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

import java.util.Set;

import infra.beans.TypeConverter;
import infra.beans.factory.config.DependencyDescriptor;
import infra.core.style.ToStringBuilder;

/**
 * Strategy interface for resolving dependencies during bean creation.
 * <p>Implementations define how to resolve a dependency described by a
 * {@link DependencyDescriptor} within a given resolution {@link Context}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface DependencyResolvingStrategy {

  /**
   * Resolve a dependency described by the given {@link DependencyDescriptor} within the
   * provided resolution {@link Context}.
   *
   * @param descriptor the dependency descriptor containing metadata about the dependency to resolve
   * @param context the resolution context providing access to the requesting bean name,
   * dependent beans, and type converter
   * @return the resolved dependency object, or {@code null} if no matching dependency is found
   */
  @Nullable
  Object resolveDependency(DependencyDescriptor descriptor, Context context);

  /**
   * Context holder for dependency resolution, providing access to the requesting bean name,
   * dependent beans, and type converter during the resolution process.
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
     * Adds the specified bean name to the set of dependent beans.
     * <p>This method has no effect if the {@code dependentBeans} set is {@code null}.
     *
     * @param beanName the name of the bean to add as a dependent
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
