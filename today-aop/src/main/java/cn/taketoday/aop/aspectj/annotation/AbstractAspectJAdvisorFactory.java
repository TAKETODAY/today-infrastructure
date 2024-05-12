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

package cn.taketoday.aop.aspectj.annotation;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;
import org.aspectj.lang.reflect.PerClauseKind;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.StringTokenizer;

import cn.taketoday.aop.framework.AopConfigException;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Abstract base class for factories that can create Framework AOP Advisors
 * given AspectJ classes from classes honoring the AspectJ 5 annotation syntax.
 *
 * <p>This class handles annotation parsing and validation functionality.
 * It does not actually generate Framework AOP Advisors, which is deferred to subclasses.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractAspectJAdvisorFactory implements AspectJAdvisorFactory {

  private static final Class<?>[] ASPECTJ_ANNOTATION_CLASSES = new Class<?>[] {
          Pointcut.class, Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class
  };

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final ParameterNameDiscoverer parameterNameDiscoverer = new AspectJAnnotationParameterNameDiscoverer();

  @Override
  public boolean isAspect(Class<?> clazz) {
    return (AnnotationUtils.findAnnotation(clazz, Aspect.class) != null);
  }

  @Override
  public void validate(Class<?> aspectClass) throws AopConfigException {
    AjType<?> ajType = AjTypeSystem.getAjType(aspectClass);
    if (!ajType.isAspect()) {
      throw new NotAnAtAspectException(aspectClass);
    }
    if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOW) {
      throw new AopConfigException(aspectClass.getName() + " uses percflow instantiation model: " +
              "This is not supported in Infra AOP.");
    }
    if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOWBELOW) {
      throw new AopConfigException(aspectClass.getName() + " uses percflowbelow instantiation model: " +
              "This is not supported in Infra AOP.");
    }
  }

  /**
   * Find and return the first AspectJ annotation on the given method
   * (there <i>should</i> only be one anyway...).
   */
  @SuppressWarnings("unchecked")
  @Nullable
  protected static AspectJAnnotation findAspectJAnnotationOnMethod(Method method) {
    for (Class<?> annotationType : ASPECTJ_ANNOTATION_CLASSES) {
      AspectJAnnotation annotation = findAnnotation(method, (Class<Annotation>) annotationType);
      if (annotation != null) {
        return annotation;
      }
    }
    return null;
  }

  @Nullable
  private static AspectJAnnotation findAnnotation(Method method, Class<? extends Annotation> annotationType) {
    Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
    if (annotation != null) {
      return new AspectJAnnotation(annotation);
    }
    else {
      return null;
    }
  }

  /**
   * Enum for AspectJ annotation types.
   *
   * @see AspectJAnnotation#getAnnotationType()
   */
  protected enum AspectJAnnotationType {

    AtPointcut, AtAround, AtBefore, AtAfter, AtAfterReturning, AtAfterThrowing
  }

  /**
   * Class modeling an AspectJ annotation, exposing its type enumeration and
   * pointcut String.
   */
  protected static class AspectJAnnotation {

    private static final String[] EXPRESSION_ATTRIBUTES = { "pointcut", "value" };

    private static final Map<Class<?>, AspectJAnnotationType> annotationTypeMap = Map.of(
            Pointcut.class, AspectJAnnotationType.AtPointcut, //
            Around.class, AspectJAnnotationType.AtAround, //
            Before.class, AspectJAnnotationType.AtBefore, //
            After.class, AspectJAnnotationType.AtAfter, //
            AfterReturning.class, AspectJAnnotationType.AtAfterReturning, //
            AfterThrowing.class, AspectJAnnotationType.AtAfterThrowing //
    );

    private final Annotation annotation;

    private final AspectJAnnotationType annotationType;

    private final String pointcutExpression;

    private final String argumentNames;

    public AspectJAnnotation(Annotation annotation) {
      this.annotation = annotation;
      this.annotationType = determineAnnotationType(annotation);
      try {
        this.pointcutExpression = resolvePointcutExpression(annotation);
        Object argNames = AnnotationUtils.getValue(annotation, "argNames");
        this.argumentNames = (argNames instanceof String names ? names : "");
      }
      catch (Exception ex) {
        throw new IllegalArgumentException(annotation + " is not a valid AspectJ annotation", ex);
      }
    }

    private AspectJAnnotationType determineAnnotationType(Annotation annotation) {
      AspectJAnnotationType type = annotationTypeMap.get(annotation.annotationType());
      if (type != null) {
        return type;
      }
      throw new IllegalStateException("Unknown annotation type: " + annotation);
    }

    private String resolvePointcutExpression(Annotation annotation) {
      for (String attributeName : EXPRESSION_ATTRIBUTES) {
        Object val = AnnotationUtils.getValue(annotation, attributeName);
        if (val instanceof String str && !str.isEmpty()) {
          return str;
        }
      }
      throw new IllegalStateException("Failed to resolve pointcut expression in: " + annotation);
    }

    public AspectJAnnotationType getAnnotationType() {
      return this.annotationType;
    }

    public Annotation getAnnotation() {
      return this.annotation;
    }

    public String getPointcutExpression() {
      return this.pointcutExpression;
    }

    public String getArgumentNames() {
      return this.argumentNames;
    }

    @Override
    public String toString() {
      return this.annotation.toString();
    }
  }

  /**
   * ParameterNameDiscoverer implementation that analyzes the arg names
   * specified at the AspectJ annotation level.
   */
  private static class AspectJAnnotationParameterNameDiscoverer extends ParameterNameDiscoverer {

    @Override
    @Nullable
    public String[] getParameterNames(Executable executable) {
      if (executable instanceof Method method) {
        if (method.getParameterCount() == 0) {
          return Constant.EMPTY_STRING_ARRAY;
        }
        AspectJAnnotation annotation = findAspectJAnnotationOnMethod(method);
        if (annotation == null) {
          return null;
        }
        StringTokenizer nameTokens = new StringTokenizer(annotation.getArgumentNames(), ",");
        int numTokens = nameTokens.countTokens();
        if (numTokens > 0) {
          String[] names = new String[numTokens];
          for (int i = 0; i < names.length; i++) {
            names[i] = nameTokens.nextToken();
          }
          return names;
        }
        else {
          return null;
        }
      }
      throw new UnsupportedOperationException("Infra AOP cannot handle constructor advice");
    }

  }

}
