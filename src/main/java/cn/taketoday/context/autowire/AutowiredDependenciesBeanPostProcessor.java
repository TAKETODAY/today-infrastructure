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

import java.util.ArrayList;
import java.util.List;

<<<<<<< HEAD:src/main/java/cn/taketoday/context/autowire/AutowiredDependencyCollector.java
import cn.taketoday.beans.dependency.DependencyCollectingContext;
import cn.taketoday.beans.dependency.DependencyCollector;
=======
>>>>>>> refactor-di:src/main/java/cn/taketoday/context/autowire/AutowiredDependenciesBeanPostProcessor.java
import cn.taketoday.beans.dependency.DependencySetter;
import cn.taketoday.beans.dependency.InjectableMethodDependencySetter;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.DependenciesBeanPostProcessor;
import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ReflectionUtils;

/**
 * this class is a dependency collecting entrance, process every bean class
 *
 * @author TODAY 2021/10/23 22:59
 * @see cn.taketoday.lang.Autowired
 * @see DependencySetter
 * @since 4.0
 */
<<<<<<< HEAD:src/main/java/cn/taketoday/context/autowire/AutowiredDependencyCollector.java
public class AutowiredDependencyCollector implements DependencyCollector {
  private static final Logger log = LoggerFactory.getLogger(AutowiredDependencyCollector.class);
=======
public class AutowiredDependenciesBeanPostProcessor implements DependenciesBeanPostProcessor {
  private static final Logger log = LoggerFactory.getLogger(AutowiredDependenciesBeanPostProcessor.class);
>>>>>>> refactor-di:src/main/java/cn/taketoday/context/autowire/AutowiredDependenciesBeanPostProcessor.java

  private final ApplicationContext context;

  @Nullable
  private PropsReader propsReader;

  @Nullable
  private PropertyResolvingContext resolvingContext;

  @Nullable
  private PropertyValueResolverComposite resolvingStrategies;

<<<<<<< HEAD:src/main/java/cn/taketoday/context/autowire/AutowiredDependencyCollector.java
  public AutowiredDependencyCollector(ApplicationContext context) {
=======
  public AutowiredDependenciesBeanPostProcessor(ApplicationContext context) {
>>>>>>> refactor-di:src/main/java/cn/taketoday/context/autowire/AutowiredDependenciesBeanPostProcessor.java
    this.context = context;
  }

  //---------------------------------------------------------------------
  // Implementation of DependenciesBeanPostProcessor interface
  //---------------------------------------------------------------------

  @Override
<<<<<<< HEAD:src/main/java/cn/taketoday/context/autowire/AutowiredDependencyCollector.java
  public void collectDependencies(DependencyCollectingContext collectingContext) {
    Class<?> beanClass = collectingContext.getBeanClass();
    resolvePropertyValues(collectingContext, beanClass);
  }

  //---------------------------------------------------------------------
  // PropertyValue (PropertySetter) resolving @since 3.0
  //---------------------------------------------------------------------

  /**
   * Process bean's property (field)
   *
   * @param resolvingContext resolving context
   * @param beanClass Bean class
   * @since 3.0
   */
  public void resolvePropertyValues(
          DependencyCollectingContext resolvingContext, Class<?> beanClass) {
=======
  public void postProcessDependencies(Object bean, BeanDefinition definition, ConfigurableBeanFactory beanFactory) {
    Class<?> beanClass = bean.getClass();
>>>>>>> refactor-di:src/main/java/cn/taketoday/context/autowire/AutowiredDependenciesBeanPostProcessor.java
    BeanMetadata beanMetadata = BeanMetadata.ofClass(beanClass);
    for (BeanProperty beanProperty : beanMetadata) {
      if (!beanProperty.isReadOnly()) {
        // if property is required and PropertyValue is null will throw ex in PropertyValueResolver
        DependencySetter created = resolveProperty(beanProperty);
        if (created != null) {
<<<<<<< HEAD:src/main/java/cn/taketoday/context/autowire/AutowiredDependencyCollector.java
          resolvingContext.addDependency(created);
=======
          created.applyTo(bean, beanFactory);
>>>>>>> refactor-di:src/main/java/cn/taketoday/context/autowire/AutowiredDependenciesBeanPostProcessor.java
        }
      }
    }

    // process methods
    ReflectionUtils.doWithMethods(beanClass, method -> {
      if (AutowiredPropertyResolver.isInjectable(method)) {
<<<<<<< HEAD:src/main/java/cn/taketoday/context/autowire/AutowiredDependencyCollector.java
        resolvingContext.addDependency(new InjectableMethodDependencySetter(method));
=======
        InjectableMethodDependencySetter created = new InjectableMethodDependencySetter(method);
        created.applyTo(bean, beanFactory);
>>>>>>> refactor-di:src/main/java/cn/taketoday/context/autowire/AutowiredDependenciesBeanPostProcessor.java
      }
    }, ReflectionUtils.USER_DECLARED_METHODS);
  }

  //---------------------------------------------------------------------
  // PropertyValue (PropertySetter) resolving @since 3.0
  //---------------------------------------------------------------------

  /**
   * Create property value
   *
   * @param property Property
   * @return A new {@link DependencySetter}
   */
  @Nullable
  public DependencySetter resolveProperty(BeanProperty property) {
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
    // @formatter:on

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
