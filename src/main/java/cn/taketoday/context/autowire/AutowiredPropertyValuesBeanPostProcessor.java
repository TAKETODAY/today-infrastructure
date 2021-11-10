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

package cn.taketoday.context.autowire;

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.factory.AbstractBeanFactory;
import cn.taketoday.beans.factory.InstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author TODAY 2021/10/23 22:59
 * @see cn.taketoday.lang.Autowired
 * @see PropertySetter
 * @since 4.0
 */
public class AutowiredPropertyValuesBeanPostProcessor implements InstantiationAwareBeanPostProcessor {
  private static final Logger log = LoggerFactory.getLogger(AutowiredPropertyValuesBeanPostProcessor.class);

  private final ApplicationContext context;

  @Nullable
  private PropsReader propsReader;

  @Nullable
  private PropertyResolvingContext resolvingContext;

  @Nullable
  private PropertyValueResolverComposite resolvingStrategies;

  public AutowiredPropertyValuesBeanPostProcessor(ApplicationContext context) {
    this.context = context;
  }

  //---------------------------------------------------------------------
  // Implementation of InstantiationAwareBeanPostProcessor interface
  //---------------------------------------------------------------------

  @Nullable
  @Override
  public Set<PropertySetter> postProcessPropertyValues(Object bean, String beanName) {

    Class<?> beanClass = bean.getClass();
    LinkedHashSet<PropertySetter> propertySetters = resolvePropertyValues(beanClass);

    // fix missing @Props injection
    List<PropertySetter> resolvedProps = propsReader().read(beanClass);
    propertySetters.addAll(resolvedProps);

    return propertySetters;
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
    BeanMetadata beanMetadata = BeanMetadata.ofClass(beanClass);
    for (BeanProperty beanProperty : beanMetadata) {
      if (!beanProperty.isReadOnly()) {
        // if property is required and PropertyValue is null will throw ex in PropertyValueResolver
        PropertySetter created = resolveProperty(beanProperty);
        // not required
        if (created != null) {
          propertySetters.add(created);
        }
      }
    }

    // process methods
    ReflectionUtils.doWithMethods(beanClass, method -> {
      MergedAnnotations annotations = MergedAnnotations.from(method);
      MergedAnnotation<Autowired> autowired = annotations.get(Autowired.class);
      if (autowired.isPresent()) {
        propertySetters.add(new PropertySetter0(method));
      }
    }, ReflectionUtils.USER_DECLARED_METHODS);

    return propertySetters;
  }

  static class PropertySetter0 implements PropertySetter {
    private final Method method;

    PropertySetter0(Method method) {
      this.method = method;
    }

    @Override
    public void applyTo(Object bean, AbstractBeanFactory beanFactory) {
      ArgumentsResolver argumentsResolver = beanFactory.getArgumentsResolver();
      Object[] args = argumentsResolver.resolve(method);
      ReflectionUtils.invokeMethod(method, bean, args);
    }
  }

  /**
   * Create property value
   *
   * @param property Property
   * @return A new {@link PropertySetter}
   */
  @Nullable
  public PropertySetter resolveProperty(BeanProperty property) {
    if (resolvingStrategies == null) {
      resolvingStrategies = new PropertyValueResolverComposite();
      initResolvingStrategies(resolvingStrategies);
    }
    if (resolvingContext == null) {
      resolvingContext = new PropertyResolvingContext(context, propsReader());
    }
    return resolvingStrategies.resolveProperty(resolvingContext, property);
  }

  public PropsReader propsReader() {
    if (propsReader == null) {
      Assert.state(context != null, "No Application Context");
      propsReader = new PropsReader(context.getEnvironment());
    }
    return propsReader;
  }

  private void initResolvingStrategies(PropertyValueResolverComposite resolvingStrategies) {
    log.debug("initialize property-setter-resolvers");
    ArrayList<PropertyValueResolver> resolvers = resolvingStrategies.getResolvers();
    resolvers.add(new ValuePropertyResolver());
    resolvers.add(new PropsPropertyResolver());
    resolvers.add(new ObjectSupplierPropertyResolver());
    resolvers.add(new AutowiredPropertyResolver());

    try { // @formatter:off
      resolvers.add(new JSR330InjectPropertyResolver());
      log.debug("Add JSR-330 annotation '@Inject,@Named' supports");
    }
    catch (Exception ignored) {}
    try {
      resolvers.add(new JSR250ResourcePropertyValueResolver());
      log.debug("Add JSR-250 annotation '@Resource' supports");
    }
    catch (Exception ignored) {}
    // formatter:on

    List<PropertyValueResolver> strategies =
            TodayStrategies.getDetector().getStrategies(PropertyValueResolver.class, context);

    // un-ordered
    resolvers.addAll(strategies); // @since 4.0
    AnnotationAwareOrderComparator.sort(resolvers);
  }

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