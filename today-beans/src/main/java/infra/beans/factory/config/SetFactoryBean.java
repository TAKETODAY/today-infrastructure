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
package infra.beans.factory.config;

import java.util.LinkedHashSet;
import java.util.Set;

import infra.beans.BeanUtils;
import infra.beans.TypeConverter;
import infra.core.ResolvableType;
import infra.lang.Nullable;

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
