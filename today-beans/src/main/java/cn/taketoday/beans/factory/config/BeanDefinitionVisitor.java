/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.factory.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Visitor class for traversing {@link BeanDefinition} objects, in particular
 * the property values and constructor argument values contained in them,
 * resolving bean metadata values.
 *
 * <p>Used by {@link PlaceholderConfigurerSupport} to parse all String values
 * contained in a BeanDefinition, resolving any placeholders found.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanDefinition
 * @see BeanDefinition#getPropertyValues
 * @see PlaceholderConfigurerSupport
 * @since 4.0 2021/12/12 14:42
 */
public class BeanDefinitionVisitor {

  @Nullable
  private StringValueResolver valueResolver;

  /**
   * Create a new BeanDefinitionVisitor, applying the specified
   * value resolver to all bean metadata values.
   *
   * @param valueResolver the StringValueResolver to apply
   */
  public BeanDefinitionVisitor(StringValueResolver valueResolver) {
    Assert.notNull(valueResolver, "StringValueResolver is required");
    this.valueResolver = valueResolver;
  }

  /**
   * Create a new BeanDefinitionVisitor for subclassing.
   * Subclasses need to override the {@link #resolveStringValue} method.
   */
  protected BeanDefinitionVisitor() { }

  /**
   * Traverse the given BeanDefinition object and the PropertyValues
   * and ConstructorArgumentValues contained in them.
   *
   * @param beanDefinition the BeanDefinition object to traverse
   * @see #resolveStringValue(String)
   */
  public void visitBeanDefinition(BeanDefinition beanDefinition) {
    visitParentName(beanDefinition);
    visitBeanClassName(beanDefinition);
    visitFactoryBeanName(beanDefinition);
    visitFactoryMethodName(beanDefinition);
    visitScope(beanDefinition);

    if (beanDefinition.hasPropertyValues()) {
      visitPropertyValues(beanDefinition.getPropertyValues());
    }

    if (beanDefinition.hasConstructorArgumentValues()) {
      ConstructorArgumentValues cas = beanDefinition.getConstructorArgumentValues();
      visitIndexedArgumentValues(cas.getIndexedArgumentValues());
      visitGenericArgumentValues(cas.getGenericArgumentValues());
    }
  }

  protected void visitParentName(BeanDefinition beanDefinition) {
    String parentName = beanDefinition.getParentName();
    if (parentName != null) {
      String resolvedName = resolveStringValue(parentName);
      if (!parentName.equals(resolvedName)) {
        beanDefinition.setParentName(resolvedName);
      }
    }
  }

  protected void visitBeanClassName(BeanDefinition beanDefinition) {
    String beanClassName = beanDefinition.getBeanClassName();
    if (beanClassName != null) {
      String resolvedName = resolveStringValue(beanClassName);
      if (!beanClassName.equals(resolvedName)) {
        beanDefinition.setBeanClassName(resolvedName);
      }
    }
  }

  protected void visitFactoryBeanName(BeanDefinition beanDefinition) {
    String factoryBeanName = beanDefinition.getFactoryBeanName();
    if (factoryBeanName != null) {
      String resolvedName = resolveStringValue(factoryBeanName);
      if (!factoryBeanName.equals(resolvedName)) {
        beanDefinition.setFactoryBeanName(resolvedName);
      }
    }
  }

  protected void visitFactoryMethodName(BeanDefinition beanDefinition) {
    String factoryMethodName = beanDefinition.getFactoryMethodName();
    if (factoryMethodName != null) {
      String resolvedName = resolveStringValue(factoryMethodName);
      if (!factoryMethodName.equals(resolvedName)) {
        beanDefinition.setFactoryMethodName(resolvedName);
      }
    }
  }

  protected void visitScope(BeanDefinition beanDefinition) {
    String scope = beanDefinition.getScope();
    if (scope != null) {
      String resolvedScope = resolveStringValue(scope);
      if (!scope.equals(resolvedScope)) {
        beanDefinition.setScope(resolvedScope);
      }
    }
  }

  protected void visitPropertyValues(PropertyValues pvs) {
    for (PropertyValue pv : pvs.toArray()) {
      Object newVal = resolveValue(pv.getValue());
      if (!ObjectUtils.nullSafeEquals(newVal, pv.getValue())) {
        pvs.add(pv.getName(), newVal);
      }
    }
  }

  protected void visitIndexedArgumentValues(Map<Integer, ConstructorArgumentValues.ValueHolder> ias) {
    for (ConstructorArgumentValues.ValueHolder valueHolder : ias.values()) {
      Object newVal = resolveValue(valueHolder.getValue());
      if (!ObjectUtils.nullSafeEquals(newVal, valueHolder.getValue())) {
        valueHolder.setValue(newVal);
      }
    }
  }

  protected void visitGenericArgumentValues(List<ConstructorArgumentValues.ValueHolder> gas) {
    for (ConstructorArgumentValues.ValueHolder valueHolder : gas) {
      Object newVal = resolveValue(valueHolder.getValue());
      if (!ObjectUtils.nullSafeEquals(newVal, valueHolder.getValue())) {
        valueHolder.setValue(newVal);
      }
    }
  }

  @SuppressWarnings("rawtypes")
  @Nullable
  protected Object resolveValue(@Nullable Object value) {
    if (value instanceof BeanDefinition) {
      visitBeanDefinition((BeanDefinition) value);
    }
    else if (value instanceof BeanDefinitionHolder) {
      visitBeanDefinition(((BeanDefinitionHolder) value).getBeanDefinition());
    }
    else if (value instanceof RuntimeBeanReference ref) {
      String newBeanName = resolveStringValue(ref.getBeanName());
      if (newBeanName == null) {
        return null;
      }
      if (!newBeanName.equals(ref.getBeanName())) {
        return new RuntimeBeanReference(newBeanName);
      }
    }
    else if (value instanceof RuntimeBeanNameReference ref) {
      String newBeanName = resolveStringValue(ref.getBeanName());
      if (newBeanName == null) {
        return null;
      }
      if (!newBeanName.equals(ref.getBeanName())) {
        return new RuntimeBeanNameReference(newBeanName);
      }
    }
    else if (value instanceof Object[]) {
      visitArray((Object[]) value);
    }
    else if (value instanceof List) {
      visitList((List) value);
    }
    else if (value instanceof Set) {
      visitSet((Set) value);
    }
    else if (value instanceof Map) {
      visitMap((Map) value);
    }
    else if (value instanceof TypedStringValue typedStringValue) {
      String stringValue = typedStringValue.getValue();
      if (stringValue != null) {
        String visitedString = resolveStringValue(stringValue);
        typedStringValue.setValue(visitedString);
      }
    }
    else if (value instanceof String) {
      return resolveStringValue((String) value);
    }
    return value;
  }

  protected void visitArray(Object[] arrayVal) {
    for (int i = 0; i < arrayVal.length; i++) {
      Object elem = arrayVal[i];
      Object newVal = resolveValue(elem);
      if (!ObjectUtils.nullSafeEquals(newVal, elem)) {
        arrayVal[i] = newVal;
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected void visitList(List listVal) {
    for (int i = 0; i < listVal.size(); i++) {
      Object elem = listVal.get(i);
      Object newVal = resolveValue(elem);
      if (!ObjectUtils.nullSafeEquals(newVal, elem)) {
        listVal.set(i, newVal);
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected void visitSet(Set setVal) {
    Set newContent = new LinkedHashSet();
    boolean entriesModified = false;
    for (Object elem : setVal) {
      int elemHash = (elem != null ? elem.hashCode() : 0);
      Object newVal = resolveValue(elem);
      int newValHash = (newVal != null ? newVal.hashCode() : 0);
      newContent.add(newVal);
      entriesModified = entriesModified || (newVal != elem || newValHash != elemHash);
    }
    if (entriesModified) {
      setVal.clear();
      setVal.addAll(newContent);
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected void visitMap(Map<?, ?> mapVal) {
    Map newContent = new LinkedHashMap();
    boolean entriesModified = false;
    for (Map.Entry entry : mapVal.entrySet()) {
      Object key = entry.getKey();
      int keyHash = (key != null ? key.hashCode() : 0);
      Object newKey = resolveValue(key);
      int newKeyHash = (newKey != null ? newKey.hashCode() : 0);
      Object val = entry.getValue();
      Object newVal = resolveValue(val);
      newContent.put(newKey, newVal);
      entriesModified = entriesModified || (newVal != val || newKey != key || newKeyHash != keyHash);
    }
    if (entriesModified) {
      mapVal.clear();
      mapVal.putAll(newContent);
    }
  }

  /**
   * Resolve the given String value, for example parsing placeholders.
   *
   * @param strVal the original String value
   * @return the resolved String value
   */
  @Nullable
  protected String resolveStringValue(String strVal) {
    if (this.valueResolver == null) {
      throw new IllegalStateException("No StringValueResolver specified - pass a resolver " +
              "object into the constructor or override the 'resolveStringValue' method");
    }
    String resolvedValue = this.valueResolver.resolveStringValue(strVal);
    // Return original String if not modified.
    return strVal.equals(resolvedValue) ? strVal : resolvedValue;
  }

}
