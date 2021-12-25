/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.beans.factory.dependency.DependencyDescriptor;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.BeanReference;
import cn.taketoday.beans.factory.BeansException;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.NamedBeanHolder;
import cn.taketoday.beans.factory.PropertyValueRetriever;
import cn.taketoday.beans.support.PropertyValuesBinder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Helper class for use in bean factory implementations,
 * resolving values contained in bean definition objects
 * into the actual values applied to the target bean instance.
 *
 * <p>Operates on an {@link AbstractBeanFactory} and a plain
 * {@link BeanDefinition} object. Used by {@link AbstractAutowireCapableBeanFactory}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractAutowireCapableBeanFactory
 * @since 4.0 2021/12/25 13:47
 */
class BeanDefinitionValueResolver {

  private final String beanName;
  private final PropertyValuesBinder binder;
  private final BeanDefinition beanDefinition;
  private final AbstractAutowireCapableBeanFactory beanFactory;

  /**
   * Create a BeanDefinitionValueResolver for the given BeanFactory and BeanDefinition.
   *
   * @param beanFactory the BeanFactory to resolve against
   * @param beanDefinition the BeanDefinition of the bean that we work on
   */
  public BeanDefinitionValueResolver(
          AbstractAutowireCapableBeanFactory beanFactory,
          PropertyValuesBinder binder, BeanDefinition beanDefinition) {

    this.binder = binder;
    this.beanFactory = beanFactory;
    this.beanName = beanDefinition.getName();
    this.beanDefinition = beanDefinition;
  }

  /**
   * Given a PropertyValue, return a value, resolving any references to other
   * beans in the factory if necessary. The value could be:
   * <li>A BeanDefinition, which leads to the creation of a corresponding
   * new bean instance. Singleton flags and names of such "inner beans"
   * are always ignored: Inner beans are anonymous prototypes.
   * <li>A RuntimeBeanReference, which must be resolved.
   * <li>A ManagedList. This is a special collection that may contain
   * RuntimeBeanReferences or Collections that will need to be resolved.
   * <li>A ManagedSet. May also contain RuntimeBeanReferences or
   * Collections that will need to be resolved.
   * <li>A ManagedMap. In this case the value may be a RuntimeBeanReference
   * or Collection that will need to be resolved.
   * <li>An ordinary object or {@code null}, in which case it's left alone.
   *
   * @param argName the name of the argument that the value is defined for
   * @param value the value object to resolve
   * @return the resolved object
   * @see PropertyValueRetriever#DO_NOT_SET
   */
  @Nullable
  public Object resolveValueIfNecessary(Object argName, @Nullable Object value) {
    if (value instanceof PropertyValueRetriever retriever) {
      return retriever.retrieve((String) argName, binder, beanFactory);
    }
    else if (value instanceof BeanDefinitionHolder bdHolder) {
      // Resolve BeanDefinitionHolder: contains BeanDefinition with name and aliases.
      return resolveInnerBean(argName, bdHolder.getBeanName(), bdHolder.getBeanDefinition());
    }
    else if (value instanceof BeanDefinition bd) {
      // Resolve plain BeanDefinition, without contained name: use dummy name.
      String innerBeanName = "(inner bean)" + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR +
              ObjectUtils.getIdentityHexString(bd);
      return resolveInnerBean(argName, innerBeanName, bd);
    }
    else if (value instanceof DependencyDescriptor dependencyDescriptor) {
      Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
      Object result = this.beanFactory.resolveDependency(
              dependencyDescriptor, this.beanName, autowiredBeanNames);
      for (String autowiredBeanName : autowiredBeanNames) {
        if (this.beanFactory.containsBean(autowiredBeanName)) {
          this.beanFactory.registerDependentBean(autowiredBeanName, this.beanName);
        }
      }
      return result;
    }
    else if (value instanceof ManagedArray managedArray) {
      // May need to resolve contained runtime references.
      Class<?> elementType = managedArray.resolvedElementType;
      if (elementType == null) {
        String elementTypeName = managedArray.getElementTypeName();
        if (StringUtils.hasText(elementTypeName)) {
          try {
            elementType = ClassUtils.forName(elementTypeName, this.beanFactory.getBeanClassLoader());
            managedArray.resolvedElementType = elementType;
          }
          catch (Throwable ex) {
            // Improve the message by showing the context.
            throw new BeanCreationException(
                    this.beanDefinition.getResourceDescription(), this.beanName,
                    "Error resolving array type for " + argName, ex);
          }
        }
        else {
          elementType = Object.class;
        }
      }
      return resolveManagedArray(argName, (List<?>) value, elementType);
    }
    else if (value instanceof ManagedList<?> managedList) {
      // May need to resolve contained runtime references.
      return resolveManagedList(argName, managedList);
    }
    else if (value instanceof ManagedSet<?> managedSet) {
      // May need to resolve contained runtime references.
      return resolveManagedSet(argName, managedSet);
    }
    else if (value instanceof ManagedMap<?, ?> managedMap) {
      // May need to resolve contained runtime references.
      return resolveManagedMap(argName, managedMap);
    }
    else if (value instanceof ManagedProperties original) {
      // Properties original = managedProperties;
      Properties copy = new Properties();
      original.forEach((propKey, propValue) -> {
        if (propKey instanceof TypedStringValue typedStringValue) {
          propKey = evaluate(typedStringValue);
        }
        if (propValue instanceof TypedStringValue typedStringValue) {
          propValue = evaluate(typedStringValue);
        }
        if (propKey == null || propValue == null) {
          throw new BeanCreationException(
                  this.beanDefinition.getResourceDescription(), this.beanName,
                  "Error converting Properties key/value pair for " + argName + ": resolved to null");
        }
        copy.put(propKey, propValue);
      });
      return copy;
    }
    else if (value instanceof TypedStringValue typedStringValue) {
      // Convert value to target type here.
      Object valueObject = evaluate(typedStringValue);
      try {
        Class<?> resolvedTargetType = resolveTargetType(typedStringValue);
        if (resolvedTargetType != null) {
          return convertIfNecessary(valueObject, resolvedTargetType);
        }
        else {
          return valueObject;
        }
      }
      catch (Throwable ex) {
        // Improve the message by showing the context.
        throw new BeanCreationException(
                this.beanDefinition.getResourceDescription(), this.beanName,
                "Error converting typed String value for " + argName, ex);
      }
    }
    else if (value == null) {
      return null;
    }
    else {
      return evaluate(value);
    }
  }

  private Object convertIfNecessary(Object valueObject, Class<?> resolvedTargetType) {
    return null;
  }

  /**
   * Evaluate the given value as an expression, if necessary.
   *
   * @param value the candidate value (may be an expression)
   * @return the resolved value
   */
  @Nullable
  protected Object evaluate(TypedStringValue value) {
    Object result = doEvaluate(value.getValue());
    if (!ObjectUtils.nullSafeEquals(result, value.getValue())) {
      value.setDynamic();
    }
    return result;
  }

  /**
   * Evaluate the given value as an expression, if necessary.
   *
   * @param value the original value (may be an expression)
   * @return the resolved value if necessary, or the original value
   */
  @Nullable
  protected Object evaluate(@Nullable Object value) {
    if (value instanceof String str) {
      return doEvaluate(str);
    }
    else if (value instanceof String[] values) {
      boolean actuallyResolved = false;
      Object[] resolvedValues = new Object[values.length];
      for (int i = 0; i < values.length; i++) {
        String originalValue = values[i];
        Object resolvedValue = doEvaluate(originalValue);
        if (resolvedValue != originalValue) {
          actuallyResolved = true;
        }
        resolvedValues[i] = resolvedValue;
      }
      return (actuallyResolved ? resolvedValues : values);
    }
    else {
      return value;
    }
  }

  /**
   * Evaluate the given String value as an expression, if necessary.
   *
   * @param value the original value (may be an expression)
   * @return the resolved value if necessary, or the original String value
   */
  @Nullable
  private Object doEvaluate(@Nullable String value) {
    return this.beanFactory.evaluateBeanDefinitionString(value, this.beanDefinition);
  }

  /**
   * Resolve the target type in the given TypedStringValue.
   *
   * @param value the TypedStringValue to resolve
   * @return the resolved target type (or {@code null} if none specified)
   * @throws ClassNotFoundException if the specified type cannot be resolved
   * @see TypedStringValue#resolveTargetType
   */
  @Nullable
  protected Class<?> resolveTargetType(TypedStringValue value) throws ClassNotFoundException {
    if (value.hasTargetType()) {
      return value.getTargetType();
    }
    return value.resolveTargetType(this.beanFactory.getBeanClassLoader());
  }

  /**
   * Resolve a reference to another bean in the factory.
   */
  @Nullable
  private Object resolveReference(Object argName, BeanReference ref) {
    try {
      Object bean;
      Class<?> beanType = ref.getBeanType();
      String resolvedName;
      if (beanType != null) {
        NamedBeanHolder<?> namedBean = this.beanFactory.resolveNamedBean(beanType);
        bean = namedBean.getBeanInstance();
        resolvedName = namedBean.getBeanName();
      }
      else {
        resolvedName = String.valueOf(doEvaluate(ref.getBeanName()));
        bean = this.beanFactory.getBean(resolvedName);
      }
      this.beanFactory.registerDependentBean(resolvedName, this.beanName);
      return bean;
    }
    catch (BeansException ex) {
      throw new BeanCreationException(
              this.beanDefinition.getResourceDescription(), this.beanName,
              "Cannot resolve reference to bean '" + ref.getBeanName() + "' while setting " + argName, ex);
    }
  }

  /**
   * Resolve an inner bean definition.
   *
   * @param argName the name of the argument that the inner bean is defined for
   * @param innerBeanName the name of the inner bean
   * @param innerBd the bean definition for the inner bean
   * @return the resolved inner bean instance
   */
  @Nullable
  private Object resolveInnerBean(Object argName, String innerBeanName, BeanDefinition innerBd) {
    BeanDefinition mbd = null;
    try {
      mbd = this.beanFactory.getMergedBeanDefinition(innerBeanName, innerBd, this.beanDefinition);
      // Check given bean name whether it is unique. If not already unique,
      // add counter - increasing the counter until the name is unique.
      String actualInnerBeanName = innerBeanName;
      if (mbd.isSingleton()) {
        actualInnerBeanName = adaptInnerBeanName(innerBeanName);
      }
      this.beanFactory.registerContainedBean(actualInnerBeanName, this.beanName);
      // Guarantee initialization of beans that the inner bean depends on.
      String[] dependsOn = mbd.getDependsOn();
      if (dependsOn != null) {
        for (String dependsOnBean : dependsOn) {
          this.beanFactory.registerDependentBean(dependsOnBean, actualInnerBeanName);
          this.beanFactory.getBean(dependsOnBean);
        }
      }
      // Actually create the inner bean instance now...
      Object innerBean = this.beanFactory.createBean(actualInnerBeanName, mbd, null);
      if (innerBean instanceof FactoryBean<?> factoryBean) {
        boolean synthetic = mbd.isSynthetic();
        innerBean = this.beanFactory.getObjectFromFactoryBean(factoryBean, actualInnerBeanName, !synthetic);
      }
      return innerBean;
    }
    catch (BeansException ex) {
      throw new BeanCreationException(
              this.beanDefinition.getResourceDescription(), this.beanName,
              "Cannot create inner bean '" + innerBeanName + "' " +
                      (mbd != null && mbd.getBeanClassName() != null ? "of type [" + mbd.getBeanClassName() + "] " : "") +
                      "while setting " + argName, ex);
    }
  }

  /**
   * Checks the given bean name whether it is unique. If not already unique,
   * a counter is added, increasing the counter until the name is unique.
   *
   * @param innerBeanName the original name for the inner bean
   * @return the adapted name for the inner bean
   */
  private String adaptInnerBeanName(String innerBeanName) {
    String actualInnerBeanName = innerBeanName;
    int counter = 0;
    String prefix = innerBeanName + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;
    while (this.beanFactory.isBeanNameInUse(actualInnerBeanName)) {
      counter++;
      actualInnerBeanName = prefix + counter;
    }
    return actualInnerBeanName;
  }

  /**
   * For each element in the managed array, resolve reference if necessary.
   */
  private Object resolveManagedArray(Object argName, List<?> ml, Class<?> elementType) {
    Object resolved = Array.newInstance(elementType, ml.size());
    for (int i = 0; i < ml.size(); i++) {
      Array.set(resolved, i, resolveValueIfNecessary(new KeyedArgName(argName, i), ml.get(i)));
    }
    return resolved;
  }

  /**
   * For each element in the managed list, resolve reference if necessary.
   */
  private List<?> resolveManagedList(Object argName, List<?> ml) {
    List<Object> resolved = new ArrayList<>(ml.size());
    for (int i = 0; i < ml.size(); i++) {
      resolved.add(resolveValueIfNecessary(new KeyedArgName(argName, i), ml.get(i)));
    }
    return resolved;
  }

  /**
   * For each element in the managed set, resolve reference if necessary.
   */
  private Set<?> resolveManagedSet(Object argName, Set<?> ms) {
    Set<Object> resolved = new LinkedHashSet<>(ms.size());
    int i = 0;
    for (Object m : ms) {
      resolved.add(resolveValueIfNecessary(new KeyedArgName(argName, i), m));
      i++;
    }
    return resolved;
  }

  /**
   * For each element in the managed map, resolve reference if necessary.
   */
  private Map<?, ?> resolveManagedMap(Object argName, Map<?, ?> mm) {
    Map<Object, Object> resolved = CollectionUtils.newLinkedHashMap(mm.size());
    for (Map.Entry<?, ?> entry : mm.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();
      Object resolvedKey = resolveValueIfNecessary(argName, key);
      Object resolvedValue = resolveValueIfNecessary(new KeyedArgName(argName, key), value);
      resolved.put(resolvedKey, resolvedValue);
    }
    return resolved;
  }

  /**
   * Holder class used for delayed toString building.
   */
  private record KeyedArgName(Object argName, Object key) {

    @Override
    public String toString() {
      return this.argName + " with key [" + this.key + "]";
    }
  }

}
