/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.beans.factory.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.SimpleTypeConverter;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.beans.factory.support.AutowireCandidateQualifier;
import cn.taketoday.beans.factory.support.AutowireCandidateResolver;
import cn.taketoday.beans.factory.support.GenericTypeAwareAutowireCandidateResolver;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * {@link AutowireCandidateResolver} implementation that matches bean holder qualifiers
 * against {@link Qualifier qualifier annotations} on the field or parameter to be autowired.
 *
 * <p>Also supports JSR-330's {@link jakarta.inject.Qualifier} annotation, if available.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AutowireCandidateQualifier
 * @see Qualifier
 * @since 4.0
 */
public class QualifierAnnotationAutowireCandidateResolver extends GenericTypeAwareAutowireCandidateResolver {

  private final LinkedHashSet<Class<? extends Annotation>> qualifierTypes = new LinkedHashSet<>(2);

  private Class<? extends Annotation> valueAnnotationType = Value.class;

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
    Assert.notNull(qualifierType, "'qualifierType' is required");
    this.qualifierTypes.add(qualifierType);
  }

  /**
   * Create a new QualifierAnnotationAutowireCandidateResolver
   * for the given qualifier annotation types.
   *
   * @param qualifierTypes the qualifier annotations to look for
   */
  public QualifierAnnotationAutowireCandidateResolver(Set<Class<? extends Annotation>> qualifierTypes) {
    Assert.notNull(qualifierTypes, "'qualifierTypes' is required");
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

  /**
   * Determine whether the provided bean holder is an autowire candidate.
   * <p>To be considered a candidate the bean's <em>autowire-candidate</em>
   * attribute must not have been set to 'false'. Also, if an annotation on
   * the field or parameter to be autowired is recognized by this bean factory
   * as a <em>qualifier</em>, the bean must 'match' against the annotation as
   * well as any attributes it may contain. The bean holder must contain
   * the same qualifier or match by meta attributes. A "value" attribute will
   * fallback to match against the bean name or an alias if a qualifier or
   * attribute does not match.
   *
   * @see Qualifier
   */
  @Override
  public boolean isAutowireCandidate(BeanDefinitionHolder holder, DependencyDescriptor descriptor) {
    boolean match = super.isAutowireCandidate(holder, descriptor);
    if (match) {
      match = checkQualifiers(holder, descriptor.getAnnotations());
      if (match) {
        MethodParameter methodParam = descriptor.getMethodParameter();
        if (methodParam != null) {
          Method method = methodParam.getMethod();
          if (method == null || void.class == method.getReturnType()) {
            match = checkQualifiers(holder, methodParam.getMethodAnnotations());
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
   * Match the given qualifier annotations against the candidate bean holder.
   */
  protected boolean checkQualifiers(BeanDefinitionHolder bdHolder, Annotation[] annotationsToSearch) {
    if (ObjectUtils.isEmpty(annotationsToSearch)) {
      return true;
    }
    SimpleTypeConverter typeConverter = new SimpleTypeConverter();
    for (Annotation annotation : annotationsToSearch) {
      Class<? extends Annotation> type = annotation.annotationType();
      boolean checkMeta = true;
      boolean fallbackToMeta = false;
      if (isQualifier(type)) {
        if (!checkQualifier(bdHolder, annotation, typeConverter)) {
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
            if ((fallbackToMeta && ObjectUtils.isEmpty(AnnotationUtils.getValue(metaAnn))) ||
                    !checkQualifier(bdHolder, metaAnn, typeConverter)) {
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
   * Match the given qualifier annotation against the candidate bean holder.
   */
  protected boolean checkQualifier(BeanDefinitionHolder bdHolder, Annotation annotation, SimpleTypeConverter typeConverter) {
    Class<? extends Annotation> type = annotation.annotationType();
    RootBeanDefinition bd = (RootBeanDefinition) bdHolder.getBeanDefinition();

    AutowireCandidateQualifier qualifier = bd.getQualifier(type.getName());
    if (qualifier == null) {
      qualifier = bd.getQualifier(ClassUtils.getShortName(type));
    }
    if (qualifier == null) {
      // First, check annotation on qualified element, if any
      Annotation targetAnnotation = getQualifiedElementAnnotation(bd, type);
      // Then, check annotation on factory method, if applicable
      if (targetAnnotation == null) {
        targetAnnotation = getFactoryMethodAnnotation(bd, type);
      }
      if (targetAnnotation == null) {
        RootBeanDefinition dbd = getResolvedDecoratedDefinition(bd);
        if (dbd != null) {
          targetAnnotation = getFactoryMethodAnnotation(dbd, type);
        }
      }
      if (targetAnnotation == null) {
        // Look for matching annotation on the target class
        if (getBeanFactory() != null) {
          try {
            Class<?> beanType = getBeanFactory().getType(bdHolder.getBeanName());
            if (beanType != null) {
              targetAnnotation = AnnotationUtils.getAnnotation(ClassUtils.getUserClass(beanType), type);
            }
          }
          catch (NoSuchBeanDefinitionException ex) {
            // Not the usual case - simply forget about the type check...
          }
        }
        if (targetAnnotation == null && bd.hasBeanClass()) {
          targetAnnotation = AnnotationUtils.getAnnotation(ClassUtils.getUserClass(bd.getBeanClass()), type);
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
        actualValue = bd.getAttribute(attributeName);
      }
      if (actualValue == null && attributeName.equals(AutowireCandidateQualifier.VALUE_KEY) &&
              expectedValue instanceof String && bdHolder.matchesName((String) expectedValue)) {
        // Fall back on bean name (or alias) match
        continue;
      }
      if (actualValue == null && qualifier != null) {
        // Fall back on default, but only if the qualifier is present
        actualValue = AnnotationUtils.getDefaultValue(annotation, attributeName);
      }
      if (actualValue != null) {
        actualValue = typeConverter.convertIfNecessary(actualValue, expectedValue.getClass());
      }
      if (!ObjectUtils.nullSafeEquals(expectedValue, actualValue)) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  protected Annotation getQualifiedElementAnnotation(RootBeanDefinition bd, Class<? extends Annotation> type) {
    AnnotatedElement qualifiedElement = bd.getQualifiedElement();
    return qualifiedElement != null ? AnnotationUtils.getAnnotation(qualifiedElement, type) : null;
  }

  @Nullable
  protected Annotation getFactoryMethodAnnotation(RootBeanDefinition bd, Class<? extends Annotation> type) {
    Method resolvedFactoryMethod = bd.getResolvedFactoryMethod();
    return resolvedFactoryMethod != null ? AnnotationUtils.getAnnotation(resolvedFactoryMethod, type) : null;
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

  @Override
  @Nullable
  public String getSuggestedName(DependencyDescriptor descriptor) {
    for (Annotation annotation : descriptor.getAnnotations()) {
      if (isQualifier(annotation.annotationType())) {
        Object value = AnnotationUtils.getValue(annotation);
        if (value instanceof String str) {
          return str;
        }
      }
    }
    return null;
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
