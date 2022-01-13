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
import java.util.Objects;

import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
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
  private ResolvableMethodParameter[] parameters;

  /** @since 3.0 */
  private ResponseStatus responseStatus;

  /** @since 3.0 @Produce */
  private String contentType;

  /** @since 4.0 */
  private Boolean responseBody;

  public HandlerMethod(Method method) {
    Assert.notNull(method, "No method");
    this.method = method;
    this.returnType = method.getReturnType();
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
    this.responseStatus = other.responseStatus;
    this.responseBody = other.responseBody; // since 4.0
    this.parameters = other.parameters != null ? other.parameters.clone() : null;
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

  public <A extends Annotation> A getMethodAnnotation(Class<A> annotation) {
    return getAnnotation(method, annotation);
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

  public ResolvableMethodParameter[] getParameters() {
    return parameters;
  }

  public void setParameters(ResolvableMethodParameter[] parameters) {
    this.parameters = parameters;
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

  // helper

  /**
   * ResponseBody present?
   */
  public boolean isResponseBody() {
    if (responseBody == null) {
      responseBody = WebUtils.isResponseBody(method);
    }
    return responseBody;
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

  // static

  public static HandlerMethod from(Method method) {
    return new HandlerMethod(method);
  }

}
