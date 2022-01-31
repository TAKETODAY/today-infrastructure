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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.handler.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.WebUtils;
import cn.taketoday.web.annotation.Produce;
import cn.taketoday.web.annotation.ResponseStatus;

/**
 * Annotation handler metadata
 *
 * @author TODAY 2018-06-25 20:03:11
 */
public class HandlerMethod {

  /** action **/
  private final Method method;

  /** @since 2.3.7 */
  private final Class<?> returnType;

  /** parameter list **/
  private final MethodParameter[] parameters;

  /** @since 3.0 */
  private ResponseStatus responseStatus;

  /** @since 3.0 @Produce */
  private String contentType;

  /** @since 4.0 */
  private Boolean responseBody;

  private final MethodParameter methodReturnType;

  /** @since 4.0 */
  @Nullable
  private volatile List<Annotation[][]> interfaceParameterAnnotations;

  public HandlerMethod(Method method) {
    Assert.notNull(method, "Method is required");
    this.method = method;
    this.parameters = initMethodParameters();
    this.returnType = method.getReturnType();
    this.methodReturnType = new SynthesizingMethodParameter(method, -1);
    // @since 3.0
    Produce produce = getMethodAnnotation(Produce.class);
    if (produce != null) {
      setContentType(produce.value());
    }

    setResponseStatus(WebUtils.getResponseStatus(this));
  }

  /**
   * Copy Constructor
   */
  public HandlerMethod(HandlerMethod other) {
    this.method = other.method;
    this.returnType = other.returnType;
    this.contentType = other.contentType; // @since 3.0
    this.responseBody = other.responseBody; // since 4.0
    this.responseStatus = other.responseStatus;
    this.methodReturnType = other.methodReturnType;
    this.parameters = other.parameters != null ? other.parameters.clone() : null;
  }

  private MethodParameter[] initMethodParameters() {
    int count = method.getParameterCount();
    MethodParameter[] result = new MethodParameter[count];
    for (int i = 0; i < count; i++) {
      result[i] = new HandlerMethodParameter(i);
    }
    return result;
  }

  // ---- useful methods

  public boolean returnTypeIsInterface() {
    return returnType.isInterface();
  }

  public boolean returnTypeIsArray() {
    return returnType.isArray();
  }

  /**
   * isAssignableFrom
   *
   * @since 4.0
   */
  public boolean isReturnTypeAssignableFrom(Class<?> childClass) {
    return returnType.isAssignableFrom(childClass);
  }

  public boolean isReturnTypeAssignableTo(Class<?> superClass) {
    return superClass.isAssignableFrom(returnType);
  }

  public boolean isReturn(Class<?> returnType) {
    return returnType == this.returnType;
  }

  public boolean isDeclaringClassPresent(Class<? extends Annotation> annotationClass) {
    return AnnotationUtils.isPresent(method.getDeclaringClass(), annotationClass);
  }

  public boolean isMethodPresent(Class<? extends Annotation> annotationClass) {
    return AnnotationUtils.isPresent(method, annotationClass);
  }

  public <A extends Annotation> A getDeclaringClassAnnotation(Class<A> annotation) {
    return getAnnotation(method.getDeclaringClass(), annotation);
  }

  public <A extends Annotation> A getAnnotation(AnnotatedElement element, Class<A> annotation) {
    return AnnotationUtils.getAnnotation(element, annotation);
  }

  /**
   * Set the response status according to the {@link ResponseStatus} annotation.
   */

  //Getter Setter
  @NonNull
  public Method getMethod() {
    return method;
  }

  public MethodParameter[] getParameters() {
    return parameters;
  }

  public MethodParameter getMethodReturnType() {
    return methodReturnType;
  }

  public Class<?> getReturnType() {
    return returnType;
  }

  public ResponseStatus getResponseStatus() {
    return responseStatus;
  }

  public void setResponseStatus(ResponseStatus responseStatus) {
    this.responseStatus = responseStatus;
  }

  // handleRequest
  // -----------------------------------------

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getContentType() {
    return contentType;
  }

  /**
   * ResponseBody present?
   */
  public boolean isResponseBody() {
    if (responseBody == null) {
      responseBody = WebUtils.isResponseBody(method);
    }
    return responseBody;
  }

  /**
   * Return {@code true} if the method return type is void, {@code false} otherwise.
   */
  public boolean isVoid() {
    return Void.TYPE.equals(getReturnType());
  }

  /**
   * Return a single annotation on the underlying method traversing its super methods
   * if no annotation can be found on the given method itself.
   * <p>Also supports <em>merged</em> composed annotations with attribute
   * overrides
   *
   * @param annotationType the type of annotation to introspect the method for
   * @return the annotation, or {@code null} if none found
   * @see AnnotatedElementUtils#findMergedAnnotation
   */
  @Nullable
  public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
    return AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
  }

  /**
   * Return whether the parameter is declared with the given annotation type.
   *
   * @param annotationType the annotation type to look for
   * @see AnnotatedElementUtils#hasAnnotation
   * @since 4.0
   */
  public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
    return AnnotatedElementUtils.hasAnnotation(this.method, annotationType);
  }

  private List<Annotation[][]> getInterfaceParameterAnnotations() {
    List<Annotation[][]> parameterAnnotations = this.interfaceParameterAnnotations;
    if (parameterAnnotations == null) {
      parameterAnnotations = new ArrayList<>();
      for (Class<?> ifc : ClassUtils.getAllInterfacesForClassAsSet(this.method.getDeclaringClass())) {
        for (Method candidate : ifc.getMethods()) {
          if (isOverrideFor(candidate)) {
            parameterAnnotations.add(candidate.getParameterAnnotations());
          }
        }
      }
      this.interfaceParameterAnnotations = parameterAnnotations;
    }
    return parameterAnnotations;
  }

  private boolean isOverrideFor(Method candidate) {
    if (!candidate.getName().equals(this.method.getName()) ||
            candidate.getParameterCount() != this.method.getParameterCount()) {
      return false;
    }
    Class<?>[] paramTypes = this.method.getParameterTypes();
    if (Arrays.equals(candidate.getParameterTypes(), paramTypes)) {
      return true;
    }
    for (int i = 0; i < paramTypes.length; i++) {
      if (paramTypes[i] !=
              ResolvableType.forParameter(candidate, i, this.method.getDeclaringClass()).resolve()) {
        return false;
      }
    }
    return true;
  }

  // Object

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof HandlerMethod that))
      return false;
    return Objects.equals(method, that.method)
            && Objects.equals(contentType, that.contentType)
            && Objects.equals(responseStatus, that.responseStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, responseStatus, contentType);
  }

  @Override
  public String toString() {
    Class<?> declaringClass = method.getDeclaringClass();
    String simpleName = declaringClass.getSimpleName();

    StringBuilder builder = new StringBuilder();
    builder.append(simpleName)
            .append('#')
            .append(method.getName())
            .append('(');

    if (ObjectUtils.isNotEmpty(parameters)) {
      builder.append(StringUtils.arrayToString(parameters, ", "));
    }

    builder.append(')');
    return builder.toString();
  }

  /**
   * A MethodParameter with HandlerMethod-specific behavior.
   */
  protected class HandlerMethodParameter extends SynthesizingMethodParameter {

    @Nullable
    private volatile Annotation[] combinedAnnotations;

    public HandlerMethodParameter(int index) {
      super(method, index);
    }

    protected HandlerMethodParameter(HandlerMethodParameter original) {
      super(original);
    }

    @Override
    @NonNull
    public Method getMethod() {
      return method;
    }

    @Override
    public <T extends Annotation> T getMethodAnnotation(Class<T> annotationType) {
      return HandlerMethod.this.getMethodAnnotation(annotationType);
    }

    @Override
    public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
      return HandlerMethod.this.hasMethodAnnotation(annotationType);
    }

    @Override
    public Annotation[] getParameterAnnotations() {
      Annotation[] anns = this.combinedAnnotations;
      if (anns == null) {
        anns = super.getParameterAnnotations();
        int index = getParameterIndex();
        if (index >= 0) {
          for (Annotation[][] ifcAnns : getInterfaceParameterAnnotations()) {
            if (index < ifcAnns.length) {
              Annotation[] paramAnns = ifcAnns[index];
              if (paramAnns.length > 0) {
                ArrayList<Annotation> merged = new ArrayList<>(anns.length + paramAnns.length);
                CollectionUtils.addAll(merged, anns);
                for (Annotation paramAnn : paramAnns) {
                  boolean existingType = false;
                  for (Annotation ann : anns) {
                    if (ann.annotationType() == paramAnn.annotationType()) {
                      existingType = true;
                      break;
                    }
                  }
                  if (!existingType) {
                    merged.add(adaptAnnotation(paramAnn));
                  }
                }
                anns = merged.toArray(Constant.EMPTY_ANNOTATION_ARRAY);
              }
            }
          }
        }
        this.combinedAnnotations = anns;
      }
      return anns;
    }

    @Override
    public HandlerMethodParameter clone() {
      return new HandlerMethodParameter(this);
    }
  }

  // static

  public static HandlerMethod from(Method method) {
    return new HandlerMethod(method);
  }

}
