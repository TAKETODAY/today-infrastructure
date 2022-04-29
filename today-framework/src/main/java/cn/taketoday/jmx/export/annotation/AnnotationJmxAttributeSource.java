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

package cn.taketoday.jmx.export.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.EmbeddedValueResolver;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotationPredicates;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.annotation.RepeatableContainers;
import cn.taketoday.jmx.export.metadata.InvalidMetadataException;
import cn.taketoday.jmx.export.metadata.JmxAttributeSource;
import cn.taketoday.lang.Nullable;

/**
 * Implementation of the {@code JmxAttributeSource} interface that
 * reads annotations and exposes the corresponding attributes.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Jennifer Hickey
 * @author Stephane Nicoll
 * @see ManagedResource
 * @see ManagedAttribute
 * @see ManagedOperation
 * @since 4.0
 */
public class AnnotationJmxAttributeSource implements JmxAttributeSource, BeanFactoryAware {

  @Nullable
  private StringValueResolver embeddedValueResolver;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (beanFactory instanceof ConfigurableBeanFactory) {
      this.embeddedValueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
    }
  }

  @Override
  @Nullable
  public cn.taketoday.jmx.export.metadata.ManagedResource getManagedResource(Class<?> beanClass) throws InvalidMetadataException {
    MergedAnnotation<ManagedResource> ann = MergedAnnotations.from(beanClass, SearchStrategy.TYPE_HIERARCHY)
            .get(ManagedResource.class).withNonMergedAttributes();
    if (!ann.isPresent()) {
      return null;
    }
    Class<?> declaringClass = (Class<?>) ann.getSource();
    Class<?> target = (declaringClass != null && !declaringClass.isInterface() ? declaringClass : beanClass);
    if (!Modifier.isPublic(target.getModifiers())) {
      throw new InvalidMetadataException("@ManagedResource class '" + target.getName() + "' must be public");
    }

    cn.taketoday.jmx.export.metadata.ManagedResource bean = new cn.taketoday.jmx.export.metadata.ManagedResource();
    Map<String, Object> map = ann.asMap();
    List<PropertyValue> list = new ArrayList<>(map.size());
    map.forEach((attrName, attrValue) -> {
      if (!"value".equals(attrName)) {
        Object value = attrValue;
        if (this.embeddedValueResolver != null && value instanceof String) {
          value = this.embeddedValueResolver.resolveStringValue((String) value);
        }
        list.add(new PropertyValue(attrName, value));
      }
    });
    BeanWrapper.forBeanPropertyAccess(bean).setPropertyValues(new PropertyValues(list));
    return bean;
  }

  @Override
  @Nullable
  public cn.taketoday.jmx.export.metadata.ManagedAttribute getManagedAttribute(Method method) throws InvalidMetadataException {
    MergedAnnotation<ManagedAttribute> ann = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY)
            .get(ManagedAttribute.class).withNonMergedAttributes();
    if (!ann.isPresent()) {
      return null;
    }

    cn.taketoday.jmx.export.metadata.ManagedAttribute bean = new cn.taketoday.jmx.export.metadata.ManagedAttribute();
    Map<String, Object> map = ann.asMap();
    PropertyValues pvs = new PropertyValues(map);
    pvs.remove("defaultValue");
    BeanWrapper.forBeanPropertyAccess(bean).setPropertyValues(pvs);
    String defaultValue = (String) map.get("defaultValue");
    if (defaultValue.length() > 0) {
      bean.setDefaultValue(defaultValue);
    }
    return bean;
  }

  @Override
  @Nullable
  public cn.taketoday.jmx.export.metadata.ManagedMetric getManagedMetric(Method method) throws InvalidMetadataException {
    MergedAnnotation<ManagedMetric> ann = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY)
            .get(ManagedMetric.class).withNonMergedAttributes();

    return copyPropertiesToBean(ann, cn.taketoday.jmx.export.metadata.ManagedMetric.class);
  }

  @Override
  @Nullable
  public cn.taketoday.jmx.export.metadata.ManagedOperation getManagedOperation(Method method) throws InvalidMetadataException {
    MergedAnnotation<ManagedOperation> ann = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY)
            .get(ManagedOperation.class).withNonMergedAttributes();

    return copyPropertiesToBean(ann, cn.taketoday.jmx.export.metadata.ManagedOperation.class);
  }

  @Override
  public cn.taketoday.jmx.export.metadata.ManagedOperationParameter[] getManagedOperationParameters(Method method)
          throws InvalidMetadataException {

    List<MergedAnnotation<? extends Annotation>> anns = getRepeatableAnnotations(
            method, ManagedOperationParameter.class, ManagedOperationParameters.class);

    return copyPropertiesToBeanArray(anns, cn.taketoday.jmx.export.metadata.ManagedOperationParameter.class);
  }

  @Override
  public cn.taketoday.jmx.export.metadata.ManagedNotification[] getManagedNotifications(Class<?> clazz)
          throws InvalidMetadataException {

    List<MergedAnnotation<? extends Annotation>> anns = getRepeatableAnnotations(
            clazz, ManagedNotification.class, ManagedNotifications.class);

    return copyPropertiesToBeanArray(anns, cn.taketoday.jmx.export.metadata.ManagedNotification.class);
  }

  private static List<MergedAnnotation<? extends Annotation>> getRepeatableAnnotations(
          AnnotatedElement annotatedElement, Class<? extends Annotation> annotationType,
          Class<? extends Annotation> containerAnnotationType) {

    return MergedAnnotations.from(annotatedElement, SearchStrategy.TYPE_HIERARCHY,
                    RepeatableContainers.valueOf(annotationType, containerAnnotationType))
            .stream(annotationType)
            .filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
            .map(MergedAnnotation::withNonMergedAttributes)
            .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  private static <T> T[] copyPropertiesToBeanArray(
          List<MergedAnnotation<? extends Annotation>> anns, Class<T> beanClass) {

    T[] beans = (T[]) Array.newInstance(beanClass, anns.size());
    int i = 0;
    for (MergedAnnotation<? extends Annotation> ann : anns) {
      beans[i++] = copyPropertiesToBean(ann, beanClass);
    }
    return beans;
  }

  @Nullable
  private static <T> T copyPropertiesToBean(MergedAnnotation<? extends Annotation> ann, Class<T> beanClass) {
    if (!ann.isPresent()) {
      return null;
    }
    T bean = BeanUtils.newInstance(beanClass);
    BeanWrapper bw = BeanWrapper.forBeanPropertyAccess(bean);
    bw.setPropertyValues(new PropertyValues(ann.asMap()));
    return bean;
  }

}
