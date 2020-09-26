/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import cn.taketoday.context.reflect.MethodInvoker;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.ResultHandler;
import cn.taketoday.web.view.ResultHandlers;

/**
 * @author TODAY <br>
 *         2018-06-25 20:03:11
 */
public class HandlerMethod
        extends InterceptableRequestHandler implements HandlerAdapter, ResultHandler {

  private Object bean; // controller bean
  /** action **/
  private Method method;
  /** @since 2.3.7 */
  private Class<?> returnType;
  private ResultHandler resultHandler;
  private ResponseStatus status;
  /** @since 2.3.7 */
  private Type[] genericityClass;
  private MethodInvoker handlerInvoker;
  /** parameter list **/
  private MethodParameter[] parameters;

  public HandlerMethod(Object bean, Method method) {
    this(bean, method, null);
  }

  public HandlerMethod(Object bean, Method method, List<HandlerInterceptor> interceptors) {
    this.bean = bean;
    this.method = method;
    setInterceptors(interceptors);

    if (method != null) {
      this.returnType = method.getReturnType();
      this.handlerInvoker = MethodInvoker.create(method);
      this.genericityClass = ClassUtils.getGenericityClass(returnType);
      this.parameters = ParameterResolverMethodParameter.ofMethod(method);
      setOrder(OrderUtils.getOrder(method) + OrderUtils.getOrder(bean));
      if (ObjectUtils.isNotEmpty(parameters)) {
        for (MethodParameter parameter : parameters) {
          parameter.setHandlerMethod(this);
        }
      }
    }

    this.status = WebUtils.getStatus(this);
    this.resultHandler = ResultHandlers.obtainHandler(this);
  }

  // -----------------------------------------

  public static HandlerMethod create(Object bean, Method method) {
    return new HandlerMethod(bean, method);
  }

  public static HandlerMethod create(Object bean, Method method, List<HandlerInterceptor> interceptors) {
    return new HandlerMethod(bean, method, interceptors);
  }

  // ---- useful methods
  public boolean isInterface() {
    return returnType.isInterface();
  }

  public boolean isArray() {
    return returnType.isArray();
  }

  public boolean isAssignableFrom(final Class<?> superClass) {
    return superClass.isAssignableFrom(returnType);
  }

  public boolean is(final Class<?> reutrnType) {
    return reutrnType == this.returnType;
  }

  public Type getGenericityClass(final int index) {

    final Type[] genericityClass = this.genericityClass;
    if (genericityClass != null && genericityClass.length > index) {
      return genericityClass[index];
    }
    return null;
  }

  public boolean isGenericPresent(final Type requiredType, final int index) {
    return requiredType.equals(getGenericityClass(index));
  }

  public boolean isGenericPresent(final Type requiredType) {

    if (genericityClass != null) {
      for (final Type type : genericityClass) {
        if (type.equals(requiredType)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isDeclaringClassPresent(final Class<? extends Annotation> annotationClass) {
    return ClassUtils.isAnnotationPresent(method.getDeclaringClass(), annotationClass);
  }

  public boolean isMethodPresent(final Class<? extends Annotation> annotationClass) {
    return ClassUtils.isAnnotationPresent(method, annotationClass);
  }

  public <A extends Annotation> A getDeclaringClassAnnotation(final Class<A> annotation) {
    return getAnnotation(method.getDeclaringClass(), annotation);
  }

  public <A extends Annotation> A getMethodAnnotation(final Class<A> annotation) {
    return getAnnotation(method, annotation);
  }

  public <A extends Annotation> A getAnnotation(final AnnotatedElement element, final Class<A> annotation) {
    return ClassUtils.getAnnotation(annotation, element);
  }

  @Override
  public String toString() {
    return method == null ? super.toString() : method.toString();
  }

  @Override
  public int hashCode() {
    return method.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof HandlerMethod) {
      return Objects.equals(method, ((HandlerMethod) obj).method);
    }
    return false;
  }

  //Getter Setter

  public Method getMethod() {
    return method;
  }

  public void setMethod(Method method) {
    this.method = method;
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

  public void setReturnType(Class<?> returnType) {
    this.returnType = returnType;
  }

  /**
   * Get The {@link Controller} object
   */
  public Object getBean() {
    return bean;
  }

  public void setBean(Object bean) {
    this.bean = bean;
  }

  public ResponseStatus getStatus() {
    return status;
  }

  public void setStatus(ResponseStatus status) {
    this.status = status;
  }

  public MethodInvoker getHandlerInvoker() {
    return handlerInvoker;
  }

  public void setHandlerInvoker(MethodInvoker handlerInvoker) {
    this.handlerInvoker = handlerInvoker;
  }

  public Type[] getGenericityClass() {
    return genericityClass;
  }

  public void setGenericityClass(Type[] genericityClass) {
    this.genericityClass = genericityClass;
  }

  public ResultHandler getResultHandler() {
    return resultHandler;
  }

  public void setResultHandler(ResultHandler resultHandler) {
    this.resultHandler = resultHandler;
  }

  // handleRequest
  // -----------------------------------------

  @Override
  public void handleResult(final RequestContext context, final Object result) throws Throwable {
    resultHandler.handleResult(context, result);
  }

  public Object invokeHandler(final RequestContext request) throws Throwable {
    return handleInternal(request);
  }

  @Override
  protected Object handleInternal(final RequestContext context) throws Throwable {
    final MethodParameter[] parameters = getParameters();
    if (ObjectUtils.isEmpty(parameters)) {
      return handlerInvoker.invoke(getBean(), null);
    }
    final Object[] args = new Object[parameters.length];
    int i = 0;
    for (final MethodParameter parameter : parameters) {
      args[i++] = parameter.resolveParameter(context);
    }
    return handlerInvoker.invoke(getBean(), args);
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

  @Override
  public long getLastModified(RequestContext context, Object handler) {
    return -1;
  }

}
