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

package cn.taketoday.beans.factory.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.support.AutowireCandidateQualifier;
import cn.taketoday.beans.factory.support.AutowireCandidateResolver;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.DependencyDescriptor;
import cn.taketoday.beans.factory.support.GenericTypeAwareAutowireCandidateResolver;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * {@link AutowireCandidateResolver} implementation that matches bean definition qualifiers
 * against {@link Qualifier qualifier annotations} on the field or parameter to be autowired.
 *
 * <p>Also supports JSR-330's {@link jakarta.inject.Qualifier} annotation, if available.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see AutowireCandidateQualifier
 * @see Qualifier
 * @since 4.0
 */
public class QualifierAnnotationAutowireCandidateResolver extends GenericTypeAwareAutowireCandidateResolver {

  private final LinkedHashSet<Class<? extends Annotation>> qualifierTypes = new LinkedHashSet<>(2);

  private Class<? extends Annotation> valueAnnotationType = Value.class;

  @Nullable
  private ConversionService conversionService;

  /**
   * Create a new QualifierAnnotationAutowireCandidateResolver
   * for Framework's standard {@link Qualifier} annotation.
   * <p>Also supports JSR-330's {@link jakarta.inject.Qualifier} annotation, if available.
   */
  public QualifierAnnotationAutowireCandidateResolver() {
    this.qualifierTypes.add(Qualifier.class);
    try {
      this.qualifierTypes.add(ClassUtils.forName("jakarta.inject.Qualifier",
              QualifierAnnotationAutowireCandidateResolver.class.getClassLoader()));
    }
    catch (ClassNotFoundException ex) {
      // JSR-330 API not available - simply skip.
    }
  }

  /**
   * Create a new QualifierAnnotationAutowireCandidateResolver
   * for the given qualifier annotation type.
   *
   * @param qualifierType the qualifier annotation to look for
   */
  public QualifierAnnotationAutowireCandidateResolver(Class<? extends Annotation> qualifierType) {
    Assert.notNull(qualifierType, "'qualifierType' must not be null");
    this.qualifierTypes.add(qualifierType);
  }

  /**
   * Create a new QualifierAnnotationAutowireCandidateResolver
   * for the given qualifier annotation types.
   *
   * @param qualifierTypes the qualifier annotations to look for
   */
  public QualifierAnnotationAutowireCandidateResolver(Set<Class<? extends Annotation>> qualifierTypes) {
    Assert.notNull(qualifierTypes, "'qualifierTypes' must not be null");
    this.qualifierTypes.addAll(qualifierTypes);
  }

  /**
   * Set the 'value' annotation type, to be used on fields, method parameters
   * and constructor parameters.
   * <p>The default value annotation type is the Framework-provided
   * {@link Value} annotation.
   * <p>This setter property exists so that developers can provide their own
   * (non-Framework-specific) annotation type to indicate a default value
   * expression for a specific argument.
   */
  public void setValueAnnotationType(Class<? extends Annotation> valueAnnotationType) {
    this.valueAnnotationType = valueAnnotationType;
  }

  /**
   * Register the given type to be used as a qualifier when autowiring.
   * <p>This identifies qualifier annotations for direct use (on fields,
   * method parameters and constructor parameters) as well as meta
   * annotations that in turn identify actual qualifier annotations.
   * <p>This implementation only supports annotations as qualifier types.
   * The default is Framework's {@link Qualifier} annotation which serves
   * as a qualifier for direct use and also as a meta annotation.
   *
   * @param qualifierType the annotation type to register
   */
  public void addQualifierType(Class<? extends Annotation> qualifierType) {
    this.qualifierTypes.add(qualifierType);
  }

  public void setConversionService(@Nullable ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Nullable
  public ConversionService getConversionService() {
    return conversionService;
  }

  /**
   * Determine whether the provided bean definition is an autowire candidate.
   * <p>To be considered a candidate the bean's <em>autowire-candidate</em>
   * attribute must not have been set to 'false'. Also, if an annotation on
   * the field or parameter to be autowired is recognized by this bean factory
   * as a <em>qualifier</em>, the bean must 'match' against the annotation as
   * well as any attributes it may contain. The bean definition must contain
   * the same qualifier or match by meta attributes. A "value" attribute will
   * fallback to match against the bean name or an alias if a qualifier or
   * attribute does not match.
   *
   * @see Qualifier
   */
  @Override
  public boolean isAutowireCandidate(BeanDefinition definition, DependencyDescriptor descriptor) {
    boolean match = super.isAutowireCandidate(definition, descriptor);
    if (match) {
      match = checkQualifiers(definition, descriptor.getAnnotations());
      if (match) {
        MethodParameter methodParam = descriptor.getMethodParameter();
        if (methodParam != null) {
          Method method = methodParam.getMethod();
          if (method == null || void.class == method.getReturnType()) {
            match = checkQualifiers(definition, methodParam.getMethodAnnotations());
          }
        }
      }
    }
    return match;
  }

  /**
   * Determine whether the given dependency declares an autowired annotation,
   * checking its required flag.
   *
   * @see Autowired#required()
   */
  @Override
  public boolean isRequired(DependencyDescriptor descriptor) {
    if (!super.isRequired(descriptor)) {
      return false;
    }
    Autowired autowired = descriptor.getAnnotation(Autowired.class);
    return (autowired == null || autowired.required());
  }

  /**
   * Match the given qualifier annotations against the candidate bean definition.
   */
  protected boolean checkQualifiers(BeanDefinition definition, Annotation[] annotationsToSearch) {
    if (ObjectUtils.isEmpty(annotationsToSearch)) {
      return true;
    }
    for (Annotation annotation : annotationsToSearch) {
      Class<? extends Annotation> type = annotation.annotationType();
      boolean checkMeta = true;
      boolean fallbackToMeta = false;
      if (isQualifier(type)) {
        if (!checkQualifier(definition, annotation)) {
          fallbackToMeta = true;
        }
        else {
          checkMeta = false;
        }
      }
      if (checkMeta) {
        boolean foundMeta = false;
        for (Annotation metaAnn : type.getAnnotations()) {
          Class<? extends Annotation> metaType = metaAnn.annotationType();
          if (isQualifier(metaType)) {
            foundMeta = true;
            // Only accept fallback match if @Qualifier annotation has a value...
            // Otherwise it is just a marker for a custom qualifier annotation.
            if ((fallbackToMeta && ObjectUtils.isEmpty(AnnotationUtils.getValue(metaAnn)))
                    || !checkQualifier(definition, metaAnn)) {
              return false;
            }
          }
        }
        if (fallbackToMeta && !foundMeta) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Checks whether the given annotation type is a recognized qualifier type.
   */
  protected boolean isQualifier(Class<? extends Annotation> annotationType) {
    for (Class<? extends Annotation> qualifierType : this.qualifierTypes) {
      if (annotationType.equals(qualifierType) || annotationType.isAnnotationPresent(qualifierType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Match the given qualifier annotation against the candidate bean definition.
   */
  protected boolean checkQualifier(BeanDefinition definition, Annotation annotation) {
    Class<? extends Annotation> type = annotation.annotationType();
    AutowireCandidateQualifier qualifier = definition.getQualifier(type.getName());
    if (qualifier == null) {
      qualifier = definition.getQualifier(ClassUtils.getShortName(type));
    }
    if (qualifier == null) {
      // First, check annotation on qualified element, if any
      Annotation targetAnnotation = getQualifiedElementAnnotation(definition, type);
      // Then, check annotation on factory method, if applicable
      if (targetAnnotation == null) {
        targetAnnotation = getFactoryMethodAnnotation(definition, type);
      }
      if (targetAnnotation == null) {
        // Look for matching annotation on the target class
        if (getBeanFactory() != null) {
          try {
            Class<?> beanType = getBeanFactory().getType(definition.getBeanName());
            if (beanType != null) {
              targetAnnotation = AnnotationUtils.getAnnotation(ClassUtils.getUserClass(beanType), type);
            }
          }
          catch (NoSuchBeanDefinitionException ex) {
            // Not the usual case - simply forget about the type check...
          }
        }
        if (targetAnnotation == null && definition.hasBeanClass()) {
          targetAnnotation = AnnotationUtils.getAnnotation(ClassUtils.getUserClass(definition.getBeanClass()), type);
        }
      }
      if (targetAnnotation != null && targetAnnotation.equals(annotation)) {
        return true;
      }
    }

    Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);
    if (attributes.isEmpty() && qualifier == null) {
      // If no attributes, the qualifier must be present
      return false;
    }
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      String attributeName = entry.getKey();
      Object expectedValue = entry.getValue();
      Object actualValue = null;
      // Check qualifier first
      if (qualifier != null) {
        actualValue = qualifier.getAttribute(attributeName);
      }
      if (actualValue == null) {
        // Fall back on bean definition attribute
        actualValue = definition.getAttribute(attributeName);
      }
      if (actualValue == null && attributeName.equals(AutowireCandidateQualifier.VALUE_KEY)
              && expectedValue instanceof String && definition.matchesName((String) expectedValue)) {
        // Fall back on bean name (or alias) match
        continue;
      }
      if (actualValue == null && qualifier != null) {
        // Fall back on default, but only if the qualifier is present
        actualValue = AnnotationUtils.getDefaultValue(annotation.annotationType(), attributeName);
      }
      if (actualValue != null) {
        actualValue = convertIfNecessary(actualValue, expectedValue.getClass());
      }
      if (!expectedValue.equals(actualValue)) {
        return false;
      }
    }
    return true;
  }

  private Object convertIfNecessary(Object actualValue, Class<?> targetClass) {
    ConversionService conversionService = getConversionService();
    if (conversionService == null) {
      conversionService = DefaultConversionService.getSharedInstance();
    }
    return conversionService.convert(actualValue, targetClass);
  }

  @Nullable
  protected Annotation getQualifiedElementAnnotation(BeanDefinition bd, Class<? extends Annotation> type) {
    AnnotatedElement qualifiedElement = bd.getQualifiedElement();
    return qualifiedElement != null ? AnnotationUtils.getAnnotation(qualifiedElement, type) : null;
  }

  @Nullable
  protected Annotation getFactoryMethodAnnotation(BeanDefinition bd, Class<? extends Annotation> type) {
    Method resolvedFactoryMethod = bd.getResolvedFactoryMethod();
    return (resolvedFactoryMethod != null ? AnnotationUtils.getAnnotation(resolvedFactoryMethod, type) : null);
  }

  /**
   * Determine whether the given dependency declares a qualifier annotation.
   *
   * @see #isQualifier(Class)
   * @see Qualifier
   */
  @Override
  public boolean hasQualifier(DependencyDescriptor descriptor) {
    for (Annotation ann : descriptor.getAnnotations()) {
      if (isQualifier(ann.annotationType())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine whether the given dependency declares a value annotation.
   *
   * @see Value
   */
  @Override
  @Nullable
  public Object getSuggestedValue(DependencyDescriptor descriptor) {
    Object value = findValue(descriptor.getAnnotations());
    if (value == null) {
      MethodParameter methodParam = descriptor.getMethodParameter();
      if (methodParam != null) {
        value = findValue(methodParam.getMethodAnnotations());
      }
    }
    return value;
  }

  /**
   * Determine a suggested value from any of the given candidate annotations.
   */
  @Nullable
  protected Object findValue(Annotation[] annotationsToSearch) {
    if (annotationsToSearch.length > 0) {   // qualifier annotations have to be local
      AnnotationAttributes attr = AnnotatedElementUtils.getMergedAnnotationAttributes(
              AnnotatedElementUtils.forAnnotations(annotationsToSearch), this.valueAnnotationType);
      if (attr != null) {
        return extractValue(attr);
      }
    }
    return null;
  }

  /**
   * Extract the value attribute from the given annotation.
   */
  protected Object extractValue(AnnotationAttributes attr) {
    Object value = attr.get(AnnotationUtils.VALUE);
    if (value == null) {
      throw new IllegalStateException("Value annotation must have a value attribute");
    }
    return value;
  }

}
