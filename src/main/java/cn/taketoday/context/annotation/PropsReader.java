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

import java.io.IOException;
import java.io.InputStream;
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
import cn.taketoday.context.ExpressionEvaluator;
import cn.taketoday.context.Props;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Assert;
import cn.taketoday.core.ConcurrentProperties;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Constant;
import cn.taketoday.core.NonNull;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.conversion.ConversionUtils;
import cn.taketoday.core.env.IterablePropertyResolver;
import cn.taketoday.core.env.PropertiesPropertyResolver;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.PropertyResolverComposite;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2021/10/3 13:41
 * @see cn.taketoday.context.Props
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

  public PropsReader() {
    this.propertyResolver = new StandardEnvironment();
  }

  public PropsReader(ApplicationContext context) {
    Assert.notNull(context, "ApplicationContext must not be null");
    this.propertyResolver = context.getEnvironment();
    this.expressionEvaluator = new ExpressionEvaluator(context);
    this.beanInstantiator = new BeanFactoryAwareBeanInstantiator(context);
    this.beanFactory = context.getBeanFactory();
  }

  public PropsReader(PropertyResolver propertyResolver) {
    Assert.notNull(propertyResolver, "PropertyResolver must not be null");
    this.propertyResolver = propertyResolver;
    this.expressionEvaluator = new ExpressionEvaluator(propertyResolver);
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
    ArrayList<PropertySetter> propertySetters = new ArrayList<>();
    for (BeanProperty property : BeanMetadata.ofClass(type)) {
      Object converted = read(property, defaultProps);
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

    for (String prefix : props.prefix()) {// maybe a default value: ""

      String key = prefix.concat(property.getName());
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
          String str = prefixsToUse[i];
          if (StringUtils.isEmpty(str)) {
            prefixsToUse[i] = key.concat(".");
          }
          else if (!replace) { // don't replace the parent prefix
            prefixsToUse[i] = prefix.concat(str);
          }
        }
        value = read(nestedProps.setPrefix(prefixsToUse), fieldType);
      }
      if (debugEnabled) {
        log.debug("Found Property: [{}] = [{}]", key, value);
      }
      if (value instanceof String) {
        return resolveValue((String) value, fieldType, propertyResolver);
      }
      if (value != null) {
        return ConversionUtils.convert(value, fieldType);
      }
    }
    return null;
  }

  @NonNull
  private PropertyResolver getResolver(Props props) {
    String[] fileNames = props.value();
    if (ObjectUtils.isNotEmpty(fileNames)) {
      Properties propertiesToUse = new Properties();
      for (String fileName : fileNames) {
        String resourceLocation = StringUtils.checkPropertiesName(fileName);
        try (InputStream inputStream = ResourceUtils.getResourceAsStream(resourceLocation)) {
          propertiesToUse.load(inputStream);
        }
        catch (IOException e) {
          throw new ApplicationContextException("IO exception occurred", e);
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
    if (beanInstantiator == null) {
      beanInstantiator = new BeanFactoryAwareBeanInstantiator(beanFactory);
    }
    return read(props, beanInstantiator.instantiate(beanClass));
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
    Properties ret = new ConcurrentProperties();
    // process -----------------

    String[] prefixs = props.prefix();
    boolean replace = props.replace();

    PropertyResolver propertyResolver = getResolver(props);

    if (propertyResolver instanceof IterablePropertyResolver) {
      for (String key : ((IterablePropertyResolver) propertyResolver)) {

        String blank = Constant.BLANK;
        for (String prefix : prefixs) {
          if (blank.equals(prefix) || key.startsWith(prefix)) { // start with prefix
            if (replace) {
              // replace the prefix
              key = key.replaceFirst(prefix, blank);
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

}

