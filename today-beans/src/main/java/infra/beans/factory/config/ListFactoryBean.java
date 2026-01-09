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

import java.util.ArrayList;
import java.util.List;

import infra.beans.BeanUtils;
import infra.beans.TypeConverter;
import infra.core.ResolvableType;

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
