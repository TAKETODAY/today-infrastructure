/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.StringTokenizer;

import cn.taketoday.aop.framework.AopConfigException;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.annotation.AnnotationUtils;
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

  private static final String AJC_MAGIC = "ajc$";

  private static final Class<?>[] ASPECTJ_ANNOTATION_CLASSES = new Class<?>[] {
          Pointcut.class, Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class
  };

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final ParameterNameDiscoverer parameterNameDiscoverer = new AspectJAnnotationParameterNameDiscoverer();

  /**
   * We consider something to be an AspectJ aspect suitable for use by the Framework AOP system
   * if it has the @Aspect annotation, and was not compiled by ajc. The reason for this latter test
   * is that aspects written in the code-style (AspectJ language) also have the annotation present
   * when compiled by ajc with the -1.5 flag, yet they cannot be consumed by Framework AOP.
   */
  @Override
  public boolean isAspect(Class<?> clazz) {
    return hasAspectAnnotation(clazz) && !compiledByAjc(clazz);
  }

  private boolean hasAspectAnnotation(Class<?> clazz) {
    return AnnotationUtils.findAnnotation(clazz, Aspect.class) != null;
  }

  /**
   * We need to detect this as "code-style" AspectJ aspects should not be
   * interpreted by Framework AOP.
   */
  static boolean compiledByAjc(Class<?> clazz) {
    // The AJTypeSystem goes to great lengths to provide a uniform appearance between code-style and
    // annotation-style aspects. Therefore there is no 'clean' way to tell them apart. Here we rely on
    // an implementation detail of the AspectJ compiler.
    for (Field field : clazz.getDeclaredFields()) {
      if (field.getName().startsWith(AJC_MAGIC)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void validate(Class<?> aspectClass) throws AopConfigException {
    AjType<?> ajType = AjTypeSystem.getAjType(aspectClass);
    if (!ajType.isAspect()) {
      throw new NotAnAtAspectException(aspectClass);
    }
    if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOW) {
      throw new AopConfigException(aspectClass.getName() + " uses percflow instantiation model: " +
              "This is not supported in AOP.");
    }
    if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOWBELOW) {
      throw new AopConfigException(aspectClass.getName() + " uses percflowbelow instantiation model: " +
              "This is not supported in AOP.");
    }
  }

  /**
   * Find and return the first AspectJ annotation on the given method
   * (there <i>should</i> only be one anyway...).
   */
  @SuppressWarnings("unchecked")
  @Nullable
  protected static AspectJAnnotation<?> findAspectJAnnotationOnMethod(Method method) {
    for (Class<?> clazz : ASPECTJ_ANNOTATION_CLASSES) {
      AspectJAnnotation<?> foundAnnotation = findAnnotation(method, (Class<Annotation>) clazz);
      if (foundAnnotation != null) {
        return foundAnnotation;
      }
    }
    return null;
  }

  @Nullable
  private static <A extends Annotation> AspectJAnnotation<A> findAnnotation(Method method, Class<A> toLookFor) {
    A result = AnnotationUtils.findAnnotation(method, toLookFor);
    if (result != null) {
      return new AspectJAnnotation<>(result);
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
   * Class modelling an AspectJ annotation, exposing its type enumeration and
   * pointcut String.
   *
   * @param <A> the annotation type
   */
  protected static class AspectJAnnotation<A extends Annotation> {

    private static final String[] EXPRESSION_ATTRIBUTES = new String[] { "pointcut", "value" };

    private static final HashMap<Class<?>, AspectJAnnotationType> annotationTypeMap = new HashMap<>(8);

    static {
      annotationTypeMap.put(Pointcut.class, AspectJAnnotationType.AtPointcut);
      annotationTypeMap.put(Around.class, AspectJAnnotationType.AtAround);
      annotationTypeMap.put(Before.class, AspectJAnnotationType.AtBefore);
      annotationTypeMap.put(After.class, AspectJAnnotationType.AtAfter);
      annotationTypeMap.put(AfterReturning.class, AspectJAnnotationType.AtAfterReturning);
      annotationTypeMap.put(AfterThrowing.class, AspectJAnnotationType.AtAfterThrowing);
    }

    private final A annotation;

    private final AspectJAnnotationType annotationType;

    private final String pointcutExpression;

    private final String argumentNames;

    public AspectJAnnotation(A annotation) {
      this.annotation = annotation;
      this.annotationType = determineAnnotationType(annotation);
      try {
        this.pointcutExpression = resolveExpression(annotation);
        Object argNames = AnnotationUtils.getValue(annotation, "argNames");
        this.argumentNames = (argNames instanceof String ? (String) argNames : "");
      }
      catch (Exception ex) {
        throw new IllegalArgumentException(annotation + " is not a valid AspectJ annotation", ex);
      }
    }

    private AspectJAnnotationType determineAnnotationType(A annotation) {
      AspectJAnnotationType type = annotationTypeMap.get(annotation.annotationType());
      if (type != null) {
        return type;
      }
      throw new IllegalStateException("Unknown annotation type: " + annotation);
    }

    private String resolveExpression(A annotation) {
      for (String attributeName : EXPRESSION_ATTRIBUTES) {
        Object val = AnnotationUtils.getValue(annotation, attributeName);
        if (val instanceof String str) {
          if (!str.isEmpty()) {
            return str;
          }
        }
      }
      throw new IllegalStateException("Failed to resolve expression: " + annotation);
    }

    public AspectJAnnotationType getAnnotationType() {
      return this.annotationType;
    }

    public A getAnnotation() {
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

    @Nullable
    @Override
    public String[] getParameterNames(Executable executable) {
      if (executable instanceof Method method) {
        if (method.getParameterCount() == 0) {
          return new String[0];
        }
        AspectJAnnotation<?> annotation = findAspectJAnnotationOnMethod(method);
        if (annotation == null) {
          return null;
        }
        StringTokenizer nameTokens = new StringTokenizer(annotation.getArgumentNames(), ",");
        if (nameTokens.countTokens() > 0) {
          String[] names = new String[nameTokens.countTokens()];
          for (int i = 0; i < names.length; i++) {
            names[i] = nameTokens.nextToken();
          }
          return names;
        }
        else {
          return null;
        }
      }
      throw new UnsupportedOperationException("Framework AOP cannot handle constructor advice");
    }
  }

}
