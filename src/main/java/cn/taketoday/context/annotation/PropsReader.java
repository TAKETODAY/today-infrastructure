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

package cn.taketoday.context.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.DefaultPropertySetter;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.beans.support.BeanFactoryAwareBeanInstantiator;
import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.DefaultProps;
import cn.taketoday.context.expression.ExpressionEvaluator;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.env.IterablePropertyResolver;
import cn.taketoday.core.env.PropertiesPropertyResolver;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.PropertyResolverComposite;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2021/10/3 13:41
 * @see Props
 * @since 4.0
 */
public class PropsReader {
  private static final Logger log = LoggerFactory.getLogger(PropsReader.class);

  private final PropertyResolver propertyResolver;

  @Nullable
  private ExpressionEvaluator expressionEvaluator;

  @Nullable
  private BeanFactoryAwareBeanInstantiator beanInstantiator;

  @Nullable
  private BeanFactory beanFactory;

  private ResourceLoader resourceLoader;

  private ConversionService conversionService;

  public PropsReader() {
    this.propertyResolver = new StandardEnvironment();
  }

  public PropsReader(ApplicationContext context) {
    Assert.notNull(context, "ApplicationContext must not be null");
    this.resourceLoader = context;
    this.beanFactory = context.getBeanFactory();
    this.propertyResolver = context.getEnvironment();
    this.expressionEvaluator = context.getExpressionEvaluator();
    this.beanInstantiator = new BeanFactoryAwareBeanInstantiator(context);
  }

  public PropsReader(PropertyResolver propertyResolver) {
    Assert.notNull(propertyResolver, "PropertyResolver must not be null");
    this.propertyResolver = propertyResolver;
  }

  public List<PropertySetter> read(AnnotatedElement annotated) {
    Assert.notNull(annotated, "AnnotatedElement must not be null");
    AnnotationAttributes attributes = AnnotationUtils.getAttributes(Props.class, annotated);
    if (attributes == null) {
      return Collections.emptyList();
    }

    Class<?> type = getBeanClass(annotated);
    if (log.isDebugEnabled()) {
      log.debug("Loading Properties For: [{}]", type.getName());
    }

    DefaultProps defaultProps = new DefaultProps(attributes);
    PropertyResolver propertyResolver = getResolver(defaultProps);

    ArrayList<PropertySetter> propertySetters = new ArrayList<>();
    for (BeanProperty property : BeanMetadata.ofClass(type)) {
      Object converted = read(property, defaultProps, propertyResolver);
      if (converted != null) {
        propertySetters.add(new DefaultPropertySetter(converted, property));
      }
    }
    propertySetters.trimToSize();
    return propertySetters;
  }

  public static Class<?> getBeanClass(AnnotatedElement annotated) {
    if (annotated instanceof Class) {
      return (Class<?>) annotated;
    }
    if (annotated instanceof BeanDefinition) {
      return ((BeanDefinition) annotated).getBeanClass();
    }
    if (annotated instanceof Method) {
      return ((Method) annotated).getReturnType();
    }
    throw new ConfigurationException("Not support annotated element: [" + annotated + "]");
  }

  public Object read(Field declaredField, Props props) {
    return read(BeanProperty.valueOf(declaredField), props);
  }

  public Object read(BeanProperty property, Props props) {
    PropertyResolver propertyResolver = getResolver(props);
    return read(property, props, propertyResolver);
  }

  /**
   * resolve nestedly
   */
  public Object read(BeanProperty property, Props props, PropertyResolver propertyResolver) {
    Class<?> fieldType = property.getType();
    boolean debugEnabled = log.isDebugEnabled();

    boolean isSimpleType = ClassUtils.isSimpleType(fieldType);

    String[] prefixs = props.prefix();
    if (ObjectUtils.isEmpty(prefixs)) {
      prefixs = new String[] { Constant.BLANK };
    }

    for (String prefix : prefixs) {// maybe a default value: ""
      String key;
      if (StringUtils.isEmpty(prefix)) {
        key = property.getPropertyName();
      }
      else {
        key = prefix.concat(property.getPropertyName());
      }
      Object value;
      if (isSimpleType) {
        value = propertyResolver.getProperty(key, fieldType);
      }
      else {
        value = propertyResolver.getProperty(key);
      }
      if (value == null) { // just null not include empty
        // inject nested Props
        DefaultProps nestedProps;
        if (property.isAnnotationPresent(Props.class)) {
          nestedProps = new DefaultProps(property.getAnnotation(Props.class));
        }
        else {
          if (!ObjectUtils.containsElement(props.nested(), fieldType)) {
            continue;
          }
          nestedProps = new DefaultProps();
        }
        boolean replace = nestedProps.replace();
        String[] prefixsToUse = nestedProps.prefix();
        for (int i = 0; i < prefixsToUse.length; i++) {
          if (StringUtils.isEmpty(prefixsToUse[i])) {
            prefixsToUse[i] = key.concat(".");
          }
          else if (!replace) { // don't replace the parent prefix
            prefixsToUse[i] = key.concat(prefixsToUse[i]);
          }
        }
        value = read(nestedProps.setPrefix(prefixsToUse), fieldType, propertyResolver);
      }
      if (debugEnabled) {
        log.debug("Found Property: [{}] = [{}]", key, value);
      }
      if (value instanceof String) {
        return resolveValue((String) value, fieldType, propertyResolver);
      }
      if (value != null) {
        return convertIfNecessary(value, property);
      }
    }
    return null;
  }

  @NonNull
  private PropertyResolver getResolver(Props props) {
    String[] fileNames = props.value();
    if (ObjectUtils.isNotEmpty(fileNames)) {
      Properties propertiesToUse = new Properties();

      if (resourceLoader == null) {
        resourceLoader = new DefaultResourceLoader();
      }

      for (String fileName : fileNames) {
        String resourceLocation = PropertiesUtils.checkPropertiesName(fileName);
        Resource resource = resourceLoader.getResource(resourceLocation);
        try {
          PropertiesUtils.fillProperties(propertiesToUse, resource);
        }
        catch (IOException e) {
          throw new ApplicationContextException("IO exception occurred : properties file '" + resource + "' load failed", e);
        }
      }
      PropertiesPropertyResolver resolver = new PropertiesPropertyResolver(propertiesToUse);
      return new PropertyResolverComposite(resolver, propertyResolver);
    }
    return propertyResolver;
  }

  private Object resolveValue(String value, Class<?> expectedType) {
    return resolveValue(value, expectedType, propertyResolver);
  }

  private Object resolveValue(
          String value, Class<?> expectedType, PropertyResolver propertyResolver) {
    if (expressionEvaluator == null) {
      expressionEvaluator = new ExpressionEvaluator(propertyResolver);
    }
    return expressionEvaluator.evaluate(value, expectedType);
  }

  private Object convertIfNecessary(Object value, BeanProperty property) {
    if (property.isInstance(value)) {
      return value;
    }
    if (conversionService == null) {
      conversionService = DefaultConversionService.getSharedInstance();
    }
    return conversionService.convert(value, TypeDescriptor.fromProperty(property));
  }

  /**
   * Resolve target object with {@link Props} and target object's class
   *
   * @param props
   *         {@link Props}
   * @param beanClass
   *         Target class, must have default {@link Constructor}
   *
   * @since 2.1.5
   */
  public <T> T read(Props props, Class<T> beanClass) {
    PropertyResolver propertyResolver = getResolver(props);
    return read(props, beanClass, propertyResolver);
  }

  public <T> T read(Props props, Class<T> beanClass, PropertyResolver propertyResolver) {
    if (beanInstantiator == null) {
      beanInstantiator = new BeanFactoryAwareBeanInstantiator(beanFactory);
    }
    return read(props, beanInstantiator.instantiate(beanClass), propertyResolver);
  }

  /**
   * Resolve target object with {@link Props} and target object's instance
   *
   * @param bean
   *         Bean instance
   *
   * @since 2.1.5
   */
  public <T> T read(Props props, T bean) {
    PropertyResolver propertyResolver = getResolver(props);
    return read(props, bean, propertyResolver);
  }

  public <T> T read(Props props, T bean, PropertyResolver propertyResolver) {
    for (BeanProperty property : BeanMetadata.ofObject(bean)) {
      Object converted = read(property, props, propertyResolver);
      if (converted != null) {
        property.setValue(bean, converted);
      }
    }
    return bean;
  }

  /**
   * Load {@link Properties} from {@link Props} {@link Annotation}
   *
   * @param props
   *         {@link Props}
   *
   * @since 2.1.5
   */
  public Properties readMap(Props props) {
    Properties ret = new Properties();
    // process -----------------

    String[] prefixs = props.prefix();
    boolean replace = props.replace();

    PropertyResolver propertyResolver = getResolver(props);

    if (propertyResolver instanceof IterablePropertyResolver) {
      for (String key : ((IterablePropertyResolver) propertyResolver)) {
        for (String prefix : prefixs) {
          if (Constant.BLANK.equals(prefix) || key.startsWith(prefix)) { // start with prefix
            if (replace) {
              // replace the prefix
              key = key.replaceFirst(prefix, Constant.BLANK);
            }
            String value = propertyResolver.getProperty(key);
            if (value != null) { // fix only support String
              ret.put(key, resolveValue(value, Object.class));
            }
          }
        }
      }
    }
    return ret;
  }

  //

  public void setBeanFactory(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  public void setExpressionEvaluator(@Nullable ExpressionEvaluator expressionEvaluator) {
    this.expressionEvaluator = expressionEvaluator;
  }

  public void setBeanInstantiator(@Nullable BeanFactoryAwareBeanInstantiator beanInstantiator) {
    this.beanInstantiator = beanInstantiator;
  }

  @Nullable
  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public PropertyResolver getPropertyResolver() {
    return propertyResolver;
  }

  @Nullable
  public ExpressionEvaluator getExpressionEvaluator() {
    return expressionEvaluator;
  }

  @Nullable
  public BeanFactoryAwareBeanInstantiator getBeanInstantiator() {
    return beanInstantiator;
  }

  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  public ResourceLoader getResourceLoader() {
    return resourceLoader;
  }

}

