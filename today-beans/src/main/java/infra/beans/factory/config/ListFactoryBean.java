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

import java.util.ArrayList;
import java.util.List;

import infra.beans.BeanUtils;
import infra.beans.TypeConverter;
import infra.core.ResolvableType;
import infra.lang.Nullable;

/**
 * Simple factory for shared List instances. Allows for central setup
 * of Lists via the "list" element in XML bean definitions.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SetFactoryBean
 * @see MapFactoryBean
 * @since 4.0 2021/11/30 13:57
 */
public class ListFactoryBean extends AbstractFactoryBean<List<Object>> {

  @Nullable
  private List<?> sourceList;

  @SuppressWarnings("rawtypes")
  @Nullable
  private Class<? extends List> targetListClass;

  /**
   * Set the source List, typically populated via XML "list" elements.
   */
  public void setSourceList(@Nullable List<?> sourceList) {
    this.sourceList = sourceList;
  }

  /**
   * Set the class to use for the target List. Can be populated with a fully
   * qualified class name when defined in a Framework application context.
   * <p>Default is a {@code java.util.ArrayList}.
   *
   * @see java.util.ArrayList
   */
  @SuppressWarnings("rawtypes")
  public void setTargetListClass(@Nullable Class<? extends List> targetListClass) {
    if (targetListClass == null) {
      throw new IllegalArgumentException("'targetListClass' is required");
    }
    if (!List.class.isAssignableFrom(targetListClass)) {
      throw new IllegalArgumentException("'targetListClass' must implement [java.util.List]");
    }
    this.targetListClass = targetListClass;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Class<List> getObjectType() {
    return List.class;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected List<Object> createBeanInstance() {
    if (this.sourceList == null) {
      throw new IllegalArgumentException("'sourceList' is required");
    }
    List<Object> result;
    if (this.targetListClass != null) {
      result = BeanUtils.newInstance(this.targetListClass);
    }
    else {
      result = new ArrayList<>(this.sourceList.size());
    }
    Class<?> valueType = null;
    if (this.targetListClass != null) {
      valueType = ResolvableType.forClass(this.targetListClass).asCollection().resolveGeneric();
    }
    if (valueType != null) {
      TypeConverter converter = getBeanTypeConverter();
      for (Object elem : this.sourceList) {
        result.add(converter.convertIfNecessary(elem, valueType));
      }
    }
    else {
      result.addAll(this.sourceList);
    }
    return result;
  }

}
