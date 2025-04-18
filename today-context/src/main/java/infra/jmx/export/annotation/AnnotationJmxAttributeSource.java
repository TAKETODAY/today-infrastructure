/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jmx.export.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import infra.beans.BeanUtils;
import infra.beans.BeanWrapper;
import infra.beans.PropertyValue;
import infra.beans.PropertyValues;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.EmbeddedValueResolver;
import infra.core.StringValueResolver;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotationPredicates;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.jmx.export.metadata.InvalidMetadataException;
import infra.jmx.export.metadata.JmxAttributeSource;
import infra.lang.Nullable;
import infra.util.StringUtils;

/**
 * Implementation of the {@code JmxAttributeSource} interface that
 * reads annotations and exposes the corresponding attributes.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Jennifer Hickey
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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
    if (beanFactory instanceof ConfigurableBeanFactory cbf) {
      this.embeddedValueResolver = new EmbeddedValueResolver(cbf);
    }
  }

  @Override
  @Nullable
  public infra.jmx.export.metadata.ManagedResource getManagedResource(Class<?> beanClass) throws InvalidMetadataException {
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

    var bean = new infra.jmx.export.metadata.ManagedResource();
    Map<String, Object> map = ann.asMap();
    List<PropertyValue> list = new ArrayList<>(map.size());
    map.forEach((attrName, attrValue) -> {
      if (!"value".equals(attrName)) {
        Object value = attrValue;
        if (embeddedValueResolver != null && value instanceof String text) {
          value = embeddedValueResolver.resolveStringValue(text);
        }
        list.add(new PropertyValue(attrName, value));
      }
    });
    BeanWrapper.forBeanPropertyAccess(bean).setPropertyValues(new PropertyValues(list));
    return bean;
  }

  @Override
  @Nullable
  public infra.jmx.export.metadata.ManagedAttribute getManagedAttribute(Method method) throws InvalidMetadataException {
    MergedAnnotation<ManagedAttribute> ann = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY)
            .get(ManagedAttribute.class).withNonMergedAttributes();
    if (!ann.isPresent()) {
      return null;
    }

    var bean = new infra.jmx.export.metadata.ManagedAttribute();
    Map<String, Object> map = ann.asMap();
    PropertyValues pvs = new PropertyValues(map);
    pvs.remove("defaultValue");
    BeanWrapper.forBeanPropertyAccess(bean).setPropertyValues(pvs);
    String defaultValue = (String) map.get("defaultValue");
    if (StringUtils.isNotEmpty(defaultValue)) {
      bean.setDefaultValue(defaultValue);
    }
    return bean;
  }

  @Override
  @Nullable
  public infra.jmx.export.metadata.ManagedMetric getManagedMetric(Method method) throws InvalidMetadataException {
    MergedAnnotation<ManagedMetric> ann = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY)
            .get(ManagedMetric.class).withNonMergedAttributes();

    return copyPropertiesToBean(ann, infra.jmx.export.metadata.ManagedMetric.class);
  }

  @Override
  @Nullable
  public infra.jmx.export.metadata.ManagedOperation getManagedOperation(Method method) throws InvalidMetadataException {
    MergedAnnotation<ManagedOperation> ann = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY)
            .get(ManagedOperation.class).withNonMergedAttributes();

    return copyPropertiesToBean(ann, infra.jmx.export.metadata.ManagedOperation.class);
  }

  @Override
  public infra.jmx.export.metadata.ManagedOperationParameter[] getManagedOperationParameters(Method method)
          throws InvalidMetadataException {

    List<MergedAnnotation<? extends Annotation>> anns = getRepeatableAnnotations(method, ManagedOperationParameter.class);
    return copyPropertiesToBeanArray(anns, infra.jmx.export.metadata.ManagedOperationParameter.class);
  }

  @Override
  public infra.jmx.export.metadata.ManagedNotification[] getManagedNotifications(Class<?> clazz)
          throws InvalidMetadataException {

    List<MergedAnnotation<? extends Annotation>> anns = getRepeatableAnnotations(clazz, ManagedNotification.class);
    return copyPropertiesToBeanArray(anns, infra.jmx.export.metadata.ManagedNotification.class);
  }

  private static List<MergedAnnotation<? extends Annotation>> getRepeatableAnnotations(
          AnnotatedElement annotatedElement, Class<? extends Annotation> annotationType) {

    return MergedAnnotations.from(annotatedElement, SearchStrategy.TYPE_HIERARCHY)
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
