/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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

import cn.taketoday.context.invoker.MethodInvoker;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.ResultHandler;

/**
 * @author TODAY <br>
 *         2018-06-25 20:03:11
 */
public class HandlerMethod extends InterceptableRequestHandler {

    private final Object bean; // controller bean 

    /** action **/
    private final Method method;
    /** parameter list **/
    private final MethodParameter[] parameters;
    /** @since 2.3.7 */
    private final Class<?> reutrnType;
    private ResultHandler resultHandler;
    /** @since 2.3.7 */
    private final Type[] genericityClass;
    private final MethodInvoker handlerInvoker;

    private String pathPattern;

    public HandlerMethod(Object bean, Method method) {
        this(bean, method, null);
    }

    public HandlerMethod(Object bean, Method method, List<HandlerInterceptor> interceptors) {

        this.bean = bean;
        this.method = method;
        this.reutrnType = method != null ? method.getReturnType() : null;
        this.parameters = WebUtils.createMethodParametersArray(method);
        this.genericityClass = ClassUtils.getGenericityClass(reutrnType);

        this.handlerInvoker = method != null ? MethodInvoker.create(method) : null;

        setInterceptors(ObjectUtils.isNotEmpty(interceptors)
                ? interceptors.toArray(new HandlerInterceptor[interceptors.size()])
                : null);
        if (method != null) {
            setOrder(OrderUtils.getOrder(method) + OrderUtils.getOrder(bean));
        }
        this.resultHandler = ResultHandler.obtainHandler(this);
    }

    public final Method getMethod() {
        return method;
    }

    public final MethodParameter[] getParameters() {
        return parameters;
    }

    public final Class<?> getReutrnType() {
        return reutrnType;
    }

    // ---- useful methods
    public boolean isInterface() {
        return reutrnType.isInterface();
    }

    public boolean isArray() {
        return reutrnType.isArray();
    }

    public boolean isAssignableFrom(final Class<?> superClass) {
        return superClass.isAssignableFrom(reutrnType);
    }

    public boolean is(final Class<?> reutrnType) {
        return reutrnType == this.reutrnType;
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

    // ------------- resolver

    public void handleResult(final RequestContext requestContext, final Object result) throws Throwable {
        resultHandler.handleResult(requestContext, result);
    }

    public Object[] resolveParameters(final RequestContext requestContext) throws Throwable {
        // log.debug("set parameter start");
        final MethodParameter[] parameters = getParameters();
        if (parameters == null) {
            return null;
        }
        final Object[] args = new Object[parameters.length];
        int i = 0;
        for (final MethodParameter parameter : parameters) {
            args[i++] = parameter.resolveParameter(requestContext);
        }
        return args;
    }

    public Object invokeHandler(final RequestContext request) throws Throwable {
        return handlerInvoker.invoke(getObject(), resolveParameters(request));
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

    /**
     * Get The {@link Controller} object
     */
    public Object getObject() {
        return bean;
    }

    // handleRequest
    // -----------------------------------------

    @Override
    public Object handleRequest(final RequestContext context) throws Throwable {
        final Object result = super.handleRequest(context);
        if (result != null) {
            handleResult(context, result);
        }
        return HandlerAdapter.NONE_RETURN_VALUE;
    }

    @Override
    protected Object handleInternal(final RequestContext context) throws Throwable {
        return invokeHandler(context);
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    // -----------------------------------------

    public static HandlerMethod create(Object bean, Method method) {
        return new HandlerMethod(bean, method);
    }

    public static HandlerMethod create(Object bean, Method method, List<HandlerInterceptor> interceptors) {
        return new HandlerMethod(bean, method, interceptors);
    }

}
