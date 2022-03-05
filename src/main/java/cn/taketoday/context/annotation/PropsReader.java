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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Properties;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.support.DependencyInjectorAwareInstantiator;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.expression.ExpressionEvaluator;
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
@Deprecated
public class PropsReader {
  private static final Logger log = LoggerFactory.getLogger(PropsReader.class);

  private final PropertyResolver propertyResolver;

  @Nullable
  private ExpressionEvaluator expressionEvaluator;

  @Nullable
  private DependencyInjectorAwareInstantiator beanInstantiator;

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
    this.beanInstantiator = DependencyInjectorAwareInstantiator.from(context);
  }

  public PropsReader(PropertyResolver propertyResolver) {
    Assert.notNull(propertyResolver, "PropertyResolver must not be null");
    this.propertyResolver = propertyResolver;
  }

  public Object read(Field declaredField, Props props) {
    return read(BeanProperty.valueOf(declaredField), props);
  }

  public Object read(BeanProperty property, Props props) {
    PropertyResolver propertyResolver = getResolver(props);
    return readProperty(property, props, propertyResolver);
  }

  /**
   * resolve nested-ly
   */
  public Object readProperty(BeanProperty property, Props props, PropertyResolver propertyResolver) {
    Class<?> fieldType = property.getType();
    boolean debugEnabled = log.isDebugEnabled();

    boolean isSimpleType = ClassUtils.isSimpleType(fieldType);

    String[] prefixs = props.prefix();
    if (ObjectUtils.isEmpty(prefixs)) {
      prefixs = new String[] { Constant.BLANK };
    }

    for (String prefix : prefixs) {// maybe a default value: ""
      String key = computeKey(property, prefix);

      if (!propertyResolver.containsProperty(key)) {
        // try xxx.yyy.applicationType to xxx.yyy.application-type
        key = convert(key);
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

  /**
   * prefix like 'xxx.' or 'xxx.yyy'
   */
  private String computeKey(BeanProperty property, String prefix) {
    String key;
    if (StringUtils.matchesLast(prefix, '.')) {
      key = prefix.concat(property.getName());
    }
    else if (StringUtils.isEmpty(prefix)) {
      key = property.getName();
    }
    else {
      key = prefix + '.' + property.getName();
    }
    return key;
  }

  static String convert(String name) {
    if (StringUtils.isEmpty(name)) {
      return Constant.BLANK;
    }
    final int length = name.length();
    final StringBuilder ret = new StringBuilder();
    ret.append(name.charAt(0));

    for (int i = 1; i < length; i++) {
      final char c = name.charAt(i);
      if (c > 0x40 && c < 0x5b) {
        ret.append('-').append((char) (c | 0x20));
      }
      else {
        ret.append(c);
      }
    }
    return ret.toString();
  }

  @NonNull
  public PropertyResolver getResolver(Props props) {
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
    return conversionService.convert(value, property.getTypeDescriptor());
  }

  /**
   * Resolve target object with {@link Props} and target object's class
   *
   * @param props {@link Props}
   * @param beanClass Target class, must have default {@link Constructor}, cannot be simple type
   * @since 2.1.5
   */
  public <T> T read(Props props, Class<T> beanClass) {
    PropertyResolver propertyResolver = getResolver(props);
    return read(props, beanClass, propertyResolver);
  }

  public <T> T read(Props props, Class<T> beanClass, PropertyResolver propertyResolver) {
    if (beanInstantiator == null) {
      Assert.state(beanFactory != null, "No BeanFactory set");
      beanInstantiator = DependencyInjectorAwareInstantiator.from(beanFactory);
    }
    return read(props, beanInstantiator.instantiate(beanClass), propertyResolver);
  }

  /**
   * Resolve target object with {@link Props} and target object's instance
   *
   * @param bean Bean instance
   * @since 2.1.5
   */
  public <T> T read(Props props, T bean) {
    PropertyResolver propertyResolver = getResolver(props);
    return read(props, bean, propertyResolver);
  }

  public <T> T read(Props props, T bean, PropertyResolver propertyResolver) {
    for (BeanProperty property : BeanMetadata.from(bean)) {
      if (!property.isReadOnly()) {
        Object converted = readProperty(property, props, propertyResolver);
        if (converted != null) {
          property.setValue(bean, converted);
        }
      }
    }
    return bean;
  }

  /**
   * Load {@link Properties} from {@link Props} {@link Annotation}
   *
   * @param props {@link Props}
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

  public void setBeanInstantiator(@Nullable DependencyInjectorAwareInstantiator beanInstantiator) {
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
  public DependencyInjectorAwareInstantiator getBeanInstantiator() {
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

