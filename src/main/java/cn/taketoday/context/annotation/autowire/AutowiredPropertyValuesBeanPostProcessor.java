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

package cn.taketoday.context.annotation.autowire;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.InstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.context.loader.PropertyResolvingContext;
import cn.taketoday.context.loader.PropertyValueResolverComposite;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author TODAY 2021/10/23 22:59
 * @see cn.taketoday.lang.Autowired
 * @see PropertySetter
 * @since 4.0
 */
public class AutowiredPropertyValuesBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

  @Nullable
  private ApplicationContext context;

  @Nullable
  private PropsReader propsReader;

  @Nullable
  private PropertyResolvingContext resolvingContext;

  @Nullable
  private PropertyValueResolverComposite resolvingStrategies;

  //---------------------------------------------------------------------
  // Implementation of InstantiationAwareBeanPostProcessor interface
  //---------------------------------------------------------------------

  @Nullable
  @Override
  public Set<PropertySetter> postProcessPropertyValues(Object bean, String beanName) {

    LinkedHashSet<PropertySetter> propertySetters = resolvePropertyValues(bean.getClass());

    // fix missing @Props injection
//    List<PropertySetter> resolvedProps = propsReader().read(definition);
//    propertySetters.addAll(resolvedProps);

    return null;
  }

  //---------------------------------------------------------------------
  // PropertyValue (PropertySetter) resolving @since 3.0
  //---------------------------------------------------------------------

  /**
   * Process bean's property (field)
   *
   * @param beanClass Bean class
   * @since 3.0
   */
  public LinkedHashSet<PropertySetter> resolvePropertyValues(Class<?> beanClass) {
    LinkedHashSet<PropertySetter> propertySetters = new LinkedHashSet<>(32);
    ReflectionUtils.doWithFields(beanClass, field -> {
      // if property is required and PropertyValue is null will throw ex in PropertyValueResolver
      PropertySetter created = resolveProperty(field);
      // not required
      if (created != null) {
        propertySetters.add(created);
      }
    });

    return propertySetters;
  }

  /**
   * Create property value
   *
   * @param field Property
   * @return A new {@link PropertySetter}
   */
  @Nullable
  public PropertySetter resolveProperty(Field field) {
    if (resolvingStrategies == null) {
      resolvingStrategies = new PropertyValueResolverComposite();
    }
    if (resolvingContext == null) {
      resolvingContext = new PropertyResolvingContext(context, propsReader());
    }
    return resolvingStrategies.resolveProperty(resolvingContext, field);
  }

  public PropsReader propsReader() {
    if (propsReader == null) {
      Assert.state(context != null, "No Application Context");
      propsReader = new PropsReader(context.getEnvironment());
    }
    return propsReader;
  }

  //

  @Nullable
  public ApplicationContext getContext() {
    return context;
  }

  @Nullable
  public PropsReader getPropsReader() {
    return propsReader;
  }

  @Nullable
  public PropertyResolvingContext getResolvingContext() {
    return resolvingContext;
  }

  @Nullable
  public PropertyValueResolverComposite getResolvingStrategies() {
    return resolvingStrategies;
  }

}
