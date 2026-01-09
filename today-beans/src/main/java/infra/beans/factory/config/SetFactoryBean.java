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

package infra.beans.factory.config;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

import infra.beans.BeanUtils;
import infra.beans.TypeConverter;
import infra.core.ResolvableType;

/**
 * Simple factory for shared Set instances. Allows for central setup
 * of Sets via the "set" element in XML bean definitions.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ListFactoryBean
 * @see MapFactoryBean
 * @since 4.0 2021/11/30 13:56
 */
public class SetFactoryBean extends AbstractFactoryBean<Set<Object>> {

  @Nullable
  private Set<?> sourceSet;

  @SuppressWarnings("rawtypes")
  @Nullable
  private Class<? extends Set> targetSetClass;

  /**
   * Set the source Set, typically populated via XML "set" elements.
   */
  public void setSourceSet(@Nullable Set<?> sourceSet) {
    this.sourceSet = sourceSet;
  }

  /**
   * Set the class to use for the target Set. Can be populated with a fully
   * qualified class name when defined in a Framework application context.
   * <p>Default is a linked HashSet, keeping the registration order.
   *
   * @see java.util.LinkedHashSet
   */
  @SuppressWarnings("rawtypes")
  public void setTargetSetClass(@Nullable Class<? extends Set> targetSetClass) {
    if (targetSetClass == null) {
      throw new IllegalArgumentException("'targetSetClass' is required");
    }
    if (!Set.class.isAssignableFrom(targetSetClass)) {
      throw new IllegalArgumentException("'targetSetClass' must implement [java.util.Set]");
    }
    this.targetSetClass = targetSetClass;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Class<Set> getObjectType() {
    return Set.class;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Set<Object> createBeanInstance() {
    if (this.sourceSet == null) {
      throw new IllegalArgumentException("'sourceSet' is required");
    }
    Set<Object> result;
    if (this.targetSetClass != null) {
      result = BeanUtils.newInstance(this.targetSetClass);
    }
    else {
      result = new LinkedHashSet<>(this.sourceSet.size());
    }
    Class<?> valueType = null;
    if (this.targetSetClass != null) {
      valueType = ResolvableType.forClass(this.targetSetClass).asCollection().resolveGeneric();
    }
    if (valueType != null) {
      TypeConverter converter = getBeanTypeConverter();
      for (Object elem : this.sourceSet) {
        result.add(converter.convertIfNecessary(elem, valueType));
      }
    }
    else {
      result.addAll(this.sourceSet);
    }
    return result;
  }

}
