/**
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.core.Assert;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.util.AnnotationUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.OrderUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebUtils;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.Produce;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.http.HttpStatus;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.view.ReturnValueHandler;
import cn.taketoday.web.view.ReturnValueHandlers;

/**
 * Annotation handler
 *
 * @author TODAY <br>
 * 2018-06-25 20:03:11
 * @see cn.taketoday.web.registry.HandlerMethodRegistry#isController(BeanDefinition)
 */
public class HandlerMethod
        extends InterceptableRequestHandler implements HandlerAdapter, ReturnValueHandler {

  private final Object bean; // controller bean
  /** action **/
  private final Method method;
  /** @since 2.3.7 */
  private final Class<?> returnType;
  private ReturnValueHandler returnValueHandler;
  private final MethodInvoker handlerInvoker;
  /** parameter list **/
  private MethodParameter[] parameters;

  /** @since 3.0 */
  private ResponseStatus responseStatus;

  /** @since 3.0 @Produce */
  private String contentType;

  /** @since 3.0 */
  private ReturnValueHandlers resultHandlers;

  public HandlerMethod() {
    this(null, null, null);
  }

  public HandlerMethod(Object bean, Method method) {
    this(bean, method, null);
  }

  public HandlerMethod(Object bean, Method method, List<HandlerInterceptor> interceptors) {
    Assert.notNull(bean);
    Assert.notNull(method);
    this.bean = bean;
    this.method = method;
    setInterceptors(interceptors);

    this.returnType = method.getReturnType();
    this.handlerInvoker = MethodInvoker.fromMethod(method);
    setOrder(OrderUtils.getOrder(method) + OrderUtils.getOrder(bean));
    // @since 3.0
    final Produce produce = getMethodAnnotation(Produce.class);
    if (produce != null) {
      setContentType(produce.value());
    }

    setResponseStatus(WebUtils.getResponseStatus(this));
  }

  /**
   * Copy Constructor
   */
  public HandlerMethod(HandlerMethod other) {
    this.bean = other.bean;
    setOrder(other.getOrder()); // fix update order
    this.method = other.method;
    this.returnType = other.returnType;
    this.contentType = other.contentType; // @since 3.0
    this.returnValueHandler = other.returnValueHandler;
    this.resultHandlers = other.resultHandlers; // @since 3.0
    this.handlerInvoker = other.handlerInvoker;
    this.responseStatus = other.responseStatus;
    setInterceptors(other.getInterceptors());
    this.parameters = other.parameters != null ? other.parameters.clone() : null;
  }

  // ---- useful methods
  public boolean isInterface() {
    return returnType.isInterface();
  }

  public boolean isArray() {
    return returnType.isArray();
  }

  public boolean isAssignableTo(final Class<?> superClass) {
    return superClass.isAssignableFrom(returnType);
  }

  public boolean isReturn(final Class<?> returnType) {
    return returnType == this.returnType;
  }

  public boolean isDeclaringClassPresent(final Class<? extends Annotation> annotationClass) {
    return AnnotationUtils.isPresent(method.getDeclaringClass(), annotationClass);
  }

  public boolean isMethodPresent(final Class<? extends Annotation> annotationClass) {
    return AnnotationUtils.isPresent(method, annotationClass);
  }

  public <A extends Annotation> A getDeclaringClassAnnotation(final Class<A> annotation) {
    return getAnnotation(method.getDeclaringClass(), annotation);
  }

  public <A extends Annotation> A getMethodAnnotation(final Class<A> annotation) {
    return getAnnotation(method, annotation);
  }

  public <A extends Annotation> A getAnnotation(final AnnotatedElement element, final Class<A> annotation) {
    return AnnotationUtils.getAnnotation(annotation, element);
  }

  /**
   * Set the response status according to the {@link ResponseStatus} annotation.
   */
  protected void applyResponseStatus(RequestContext context) {
    applyResponseStatus(context, getResponseStatus());
  }

  protected void applyResponseStatus(RequestContext context, ResponseStatus status) {
    if (status != null) {
      final String reason = status.reason();
      final HttpStatus httpStatus = status.value();
      if (StringUtils.hasText(reason)) {
        context.setStatus(httpStatus.value(), reason);
      }
      else {
        context.setStatus(httpStatus);
      }
    }
  }

  //Getter Setter

  public Method getMethod() {
    return method;
  }

  public MethodParameter[] getParameters() {
    return parameters;
  }

  public void setParameters(MethodParameter[] parameters) {
    this.parameters = parameters;
  }

  public Class<?> getReturnType() {
    return returnType;
  }

  /**
   * Get The {@link Controller} object
   */
  public Object getBean() {
    return bean;
  }

  public ResponseStatus getResponseStatus() {
    return responseStatus;
  }

  public void setResponseStatus(ResponseStatus responseStatus) {
    this.responseStatus = responseStatus;
  }

  public MethodInvoker getHandlerInvoker() {
    return handlerInvoker;
  }

  // handleRequest
  // -----------------------------------------

  public void setResultHandlers(ReturnValueHandlers resultHandlers) {
    this.resultHandlers = resultHandlers;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getContentType() {
    return contentType;
  }

  @Override
  public void handleReturnValue(
          final RequestContext context, final Object handler, final Object returnValue) throws Throwable {
    applyResponseStatus(context);
    ReturnValueHandler returnValueHandler = this.returnValueHandler;
    if (returnValueHandler == null) {
      returnValueHandler = resultHandlers.obtainHandler(this);
      this.returnValueHandler = returnValueHandler;
    }
    returnValueHandler.handleReturnValue(context, handler, returnValue);
    // @since 3.0
    final String contentType = getContentType();
    if (contentType != null) {
      context.setContentType(contentType);
    }
  }

  public Object invokeHandler(final RequestContext request) throws Throwable {
    return handleInternal(request);
  }

  @Override
  protected Object handleInternal(final RequestContext context) throws Throwable {
    final MethodParameter[] parameters = this.parameters;
    if (ObjectUtils.isEmpty(parameters)) {
      return handlerInvoker.invoke(bean, null);
    }
    final Object[] args = new Object[parameters.length];
    int i = 0;
    for (final MethodParameter parameter : parameters) {
      args[i++] = parameter.resolveParameter(context);
    }
    return handlerInvoker.invoke(bean, args);
  }

  @Override
  public boolean supportsHandler(Object handler) {
    return handler == this;
  }

  // HandlerAdapter

  @Override
  public boolean supports(final Object handler) {
    return handler == this;
  }

  @Override
  public Object handle(final RequestContext context, final Object handler) throws Throwable {
    return handleRequest(context);
  }

  // Object

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof HandlerMethod))
      return false;
    final HandlerMethod that = (HandlerMethod) o;
    return Objects.equals(bean, that.bean)
            && Objects.equals(method, that.method)
            && Objects.equals(contentType, that.contentType)
            && Objects.equals(returnValueHandler, that.returnValueHandler)
            && Objects.equals(responseStatus, that.responseStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bean, method, returnValueHandler, responseStatus, contentType);
  }

  @Override
  public String toString() {
    final Class<?> declaringClass = method.getDeclaringClass();
    final String simpleName = declaringClass.getSimpleName();

    final StringBuilder builder = new StringBuilder();
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

  public static HandlerMethod create(Object bean, Method method) {
    return new HandlerMethod(bean, method);
  }

  public static HandlerMethod create(Object bean, Method method, List<HandlerInterceptor> interceptors) {
    return new HandlerMethod(bean, method, interceptors);
  }

}
