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
package cn.taketoday.web.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.ObjectFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.resolver.result.ResultResolver;

/**
 * @author TODAY <br>
 *         2018-06-25 20:03:11
 */
public class HandlerMethod implements ObjectFactory<Object> {

    /** action **/
    private final Method method;
    /** parameter list **/
    private final MethodParameter[] parameters;
    /** @since 2.3.7 */
    private final Class<?> reutrnType;
    /** @since 2.3.7 */
    private final Type[] genericityClass;

    private final ResultResolver resultResolver;

    private static final List<ResultResolver> RESULT_RESOLVERS = new ArrayList<>();

    /**
     * Get correspond result resolver, If there isn't a suitable resolver will be
     * throw {@link ConfigurationException}
     * 
     * @return A suitable {@link ResultResolver}
     */
    protected ResultResolver obtainResolver() throws ConfigurationException {

        if (method != null) {
            for (final ResultResolver resolver : getResultResolvers()) {
                if (resolver.supports(this)) {
                    return resolver;
                }
            }
            throw ExceptionUtils.newConfigurationException(null,
                                                           "There isn't have a result resolver to resolve : [" + toString() + "]");
        }
        return null;
    }

    public HandlerMethod(Method method, List<MethodParameter> parameters) {
        this(method, parameters == null ? null : parameters.toArray(MethodParameter.EMPTY_ARRAY));
    }

    public HandlerMethod(Method method, MethodParameter... parameters) {

        this.method = method;
        this.reutrnType = method != null ? method.getReturnType() : null;
        this.genericityClass = ClassUtils.getGenericityClass(reutrnType);

        if (ObjectUtils.isEmpty(parameters)) {
            this.parameters = MethodParameter.EMPTY_ARRAY;
        }
        else {
            for (final MethodParameter parameter : parameters) {
                parameter.setHandlerMethod(this);
            }
            this.parameters = parameters;
        }
        this.resultResolver = obtainResolver();
    }

    public static HandlerMethod create(final Method method, final List<MethodParameter> methodParameters) {
        return new HandlerMethod(method, methodParameters);
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
        return getDeclaringClassAnnotation(annotationClass) != null;
    }

    public boolean isMethodPresent(final Class<? extends Annotation> annotationClass) {
        return getMethodAnnotation(annotationClass) != null;
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

    public void resolveResult(final RequestContext requestContext, final Object result) throws Throwable {
        resultResolver.resolveResult(requestContext, result);
    }

    public Object[] resolveParameters(final RequestContext requestContext) throws Throwable {
        // log.debug("set parameter start");
        final MethodParameter[] parameters = getParameters();
        if (parameters == MethodParameter.EMPTY_ARRAY) {
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
        return method.invoke(getObject(), resolveParameters(request));
    }

    // Useful methods
    // ------------------------------------

    public static void addResolver(ResultResolver... resolvers) {
        Collections.addAll(RESULT_RESOLVERS, resolvers);
        OrderUtils.reversedSort(RESULT_RESOLVERS);
    }

    public static void addResolver(List<ResultResolver> parameterResolver) {
        RESULT_RESOLVERS.addAll(parameterResolver);
        OrderUtils.reversedSort(RESULT_RESOLVERS);
    }

    public static List<ResultResolver> getResultResolvers() {
        return RESULT_RESOLVERS;
    }

    @Override
    public String toString() {
        return "{method=" + getMethod() + ", parameter=[" + Arrays.toString(getParameters()) + "]}";
    }

    /**
     * Get The {@link Controller} object
     */
    @Override
    public Object getObject() {
        return null;
    }

}
