/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.DefaultBeanDefinition;
import cn.taketoday.beans.factory.FactoryMethodBeanDefinition;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Environment;
import cn.taketoday.context.loader.AutowiredPropertyResolver;
import cn.taketoday.context.loader.PropertyResolvingContext;
import cn.taketoday.context.loader.PropertyValueResolverComposite;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Constant;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

import static cn.taketoday.core.Constant.VALUE;
import static cn.taketoday.util.ReflectionUtils.makeAccessible;

/**
 * @author TODAY 2021/10/2 22:45
 * @since 4.0
 */
public class BeanDefinitionBuilder {

  public static final Class<? extends Annotation>
          PostConstruct = ClassUtils.load("javax.annotation.PostConstruct");

  private final Environment environment;
  private final PropsReader propsReader;
  private final ApplicationContext context;
  private final PropertyResolvingContext resolvingContext;
  private final PropertyValueResolverComposite propertyValueResolver;

  private String name;
  private Class<?> beanClass;
  private String scope;
  private String[] initMethods;
  private String[] destroyMethods;
  private AnnotationAttributes component;

  public BeanDefinitionBuilder(ApplicationContext context) {
    this.context = context;
    this.environment = context.getEnvironment();
    this.propsReader = new PropsReader(environment);
    this.resolvingContext = new PropertyResolvingContext(context, propsReader);
    this.propertyValueResolver = new PropertyValueResolverComposite();
  }

  public BeanDefinition buildDefault(
          String name, Class<?> beanClass, @Nullable AnnotationAttributes attributes) {
    return defaults(name, beanClass, attributes);
  }

  public BeanDefinition build(String beanName, Class<?> beanClass) {
    BeanDefinition defaults = BeanDefinitionBuilder.defaults(beanName, beanClass, null);
    defaults.setPropertyValues(resolvePropertyValue(beanClass));
    // fix missing @Props injection
    resolveProps(defaults);
    return defaults;
  }

  public void resolveProps(BeanDefinition def) {
    List<PropertySetter> resolvedProps = propsReader.read(def);
    def.addPropertySetter(resolvedProps);
  }

  public BeanDefinitionBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public BeanDefinitionBuilder withScope(String scope) {
    if (StringUtils.isNotEmpty(scope)) {
      this.scope = scope;
    }
    return this;
  }

  public BeanDefinitionBuilder withInitMethods(String... initMethods) {
    if (ObjectUtils.isNotEmpty(initMethods)) {
      this.initMethods = initMethods;
    }
    return this;
  }

  public BeanDefinitionBuilder withDestroyMethods(String... destroyMethods) {
    if (ObjectUtils.isNotEmpty(destroyMethods)) {
      this.destroyMethods = destroyMethods;
    }
    return this;
  }

  public void withAttributes (AnnotationAttributes component) {
    if (component != null) {
      this.component = component;
      this.scope = component.getString(BeanDefinition.SCOPE);

      component.getStringArray(BeanDefinition.INIT_METHODS);
      String[] destroyMethods = component.getStringArray(BeanDefinition.DESTROY_METHODS);

      if (ObjectUtils.isNotEmpty(destroyMethods)) {
        this.destroyMethods = destroyMethods;
      }
    }
  }

  public BeanDefinition build() {
    BeanDefinition defaults = defaults(name, beanClass);

  }

  public BeanDefinition buildWithFactoryMethod(Method factoryMethod) {
    FactoryMethodBeanDefinition factoryMethodBeanDefinition = factoryMethod(factoryMethod);

    resolveProps(factoryMethodBeanDefinition);

    if (component != null) {
      final String scope = component.getString(BeanDefinition.SCOPE);
      final String[] initMethods = component.getStringArray(BeanDefinition.INIT_METHODS);
      final String[] destroyMethods = component.getStringArray(BeanDefinition.DESTROY_METHODS);

    }

    return factoryMethodBeanDefinition;
  }

  public Environment getEnvironment() {
    return environment;
  }

  //---------------------------------------------------------------------
  // PropertyValue (PropertySetter) resolving @since 3.0
  //---------------------------------------------------------------------

  /**
   * Process bean's property (field)
   *
   * @param beanClass
   *         Bean class
   *
   * @since 3.0
   */
  public PropertySetter[] resolvePropertyValue(Class<?> beanClass) {
    LinkedHashSet<PropertySetter> propertySetters = new LinkedHashSet<>(32);
    ReflectionUtils.doWithFields(beanClass, field -> {
      // if property is required and PropertyValue is null will throw ex in PropertyValueResolver
      PropertySetter created = createPropertyValue(makeAccessible(field));
      // not required
      if (created != null) {
        propertySetters.add(created);
      }
    });

    return propertySetters.isEmpty()
           ? BeanDefinition.EMPTY_PROPERTY_SETTER
           : propertySetters.toArray(new PropertySetter[propertySetters.size()]);
  }

  /**
   * Create property value
   *
   * @param field
   *         Property
   *
   * @return A new {@link PropertySetter}
   */
  @Nullable
  public PropertySetter createPropertyValue(Field field) {
    return propertyValueResolver.resolveProperty(resolvingContext, field);
  }

  public void reset() {
    this.scope = null;
    this.initMethods = null;
    this.destroyMethods = null;
  }

  //---------------------------------------------------------------------
  // static utils
  //---------------------------------------------------------------------

  /**
   * Find bean names
   *
   * @param defaultName
   *         Default bean name
   * @param names
   *         Annotation values
   *
   * @return Bean names
   */
  public static String[] determineName(String defaultName, String... names) {
    if (ObjectUtils.isEmpty(names)) {
      return new String[] { defaultName }; // default name
    }
    return names;
  }

  /**
   * @param beanClass
   *         Bean class
   * @param initMethods
   *         Init Method s
   *
   * @since 2.1.2
   */
  public static Method[] resolveInitMethod(Class<?> beanClass, String... initMethods) {
    return resolveInitMethod(initMethods, beanClass);
  }

  /**
   * Add a method which annotated with {@link javax.annotation.PostConstruct}
   * or {@link  cn.taketoday.context.annotation.Autowired}
   *
   * @param beanClass
   *         Bean class
   * @param initMethods
   *         Init Method name
   *
   * @see AutowiredPropertyResolver#isInjectable(AnnotatedElement)
   * @since 2.1.7
   */
  public static Method[] resolveInitMethod(@Nullable String[] initMethods, Class<?> beanClass) {
    ArrayList<Method> methods = new ArrayList<>(2);
    boolean initMethodsNotEmpty = ObjectUtils.isNotEmpty(initMethods);
    // @since 4.0 use ReflectionUtils.doWithMethods
    ReflectionUtils.doWithMethods(beanClass, method -> {
      if (AnnotationUtils.isPresent(method, PostConstruct)
              || AutowiredPropertyResolver.isInjectable(method)) { // method Injection
        methods.add(method);
      }
      else if (initMethodsNotEmpty) {
        String name = method.getName();
        for (String initMethod : initMethods) {
          if (initMethod.equals(name)) { // equals
            methods.add(method);
          }
        }
      }
    });

    if (methods.isEmpty()) {
      return BeanDefinition.EMPTY_METHOD;
    }
    AnnotationAwareOrderComparator.sort(methods);
    return methods.toArray(new Method[methods.size()]);
  }

  public static BeanDefinition empty() {
    return new DefaultBeanDefinition();
  }

  public static BeanDefinition defaults(Class<?> candidate) {
    Assert.notNull(candidate, "bean-class must not be null");
    String defaultBeanName = ClassUtils.getShortName(candidate);
    return defaults(defaultBeanName, candidate, null);
  }

  public static BeanDefinition defaults(String name, Class<?> beanClass) {
    return defaults(name, beanClass, null);
  }

  public static BeanDefinition defaults(
          String name, Class<?> beanClass, @Nullable AnnotationAttributes attributes) {
    Assert.notNull(name, "bean-name must not be null");
    Assert.notNull(beanClass, "bean-class must not be null");

    DefaultBeanDefinition def = new DefaultBeanDefinition(name, beanClass);
    if (attributes == null) {
      def.setDestroyMethods(Constant.EMPTY_STRING_ARRAY);
      def.setInitMethods(resolveInitMethod(null, beanClass));
    }
    else {
      def.setScope(attributes.getString(BeanDefinition.SCOPE));
      def.setDestroyMethods(attributes.getStringArray(BeanDefinition.DESTROY_METHODS));
      def.setInitMethods(resolveInitMethod(attributes.getStringArray(BeanDefinition.INIT_METHODS), beanClass));
    }
    return def;
  }

  public static FactoryMethodBeanDefinition factoryMethod(Method factoryMethod) {
    FactoryMethodBeanDefinition stdDef = new FactoryMethodBeanDefinition(factoryMethod);

    return stdDef;
  }

  public static List<BeanDefinition> from(Class<?> candidate) {
    Assert.notNull(candidate, "bean-class must not be null");

    String defaultBeanName = ClassUtils.getShortName(candidate);
    AnnotationAttributes[] annotationAttributes =
            AnnotationUtils.getAttributesArray(candidate, Component.class);
    // has Component
    if (ObjectUtils.isNotEmpty(annotationAttributes)) {
      ArrayList<BeanDefinition> definitions = new ArrayList<>(2);
      for (AnnotationAttributes attributes : annotationAttributes) {
        String[] determineName = BeanDefinitionBuilder.determineName(
                defaultBeanName, attributes.getStringArray(VALUE));
        for (String beanName : determineName) {
          BeanDefinition defaults = defaults(beanName, candidate, attributes);
          definitions.add(defaults);
        }
      }
      return definitions;
    }
    else {
      BeanDefinition defaults = defaults(defaultBeanName, candidate, null);
      return Collections.singletonList(defaults);
    }
  }
}
