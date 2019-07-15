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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.resolver.result.ResultResolver;

/**
 * @author TODAY <br>
 *         2018-06-25 20:03:11
 */
public class HandlerMethod {

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

        for (final ResultResolver resolver : RESULT_RESOLVERS) {
            if (resolver.supports(this)) {
                return resolver;
            }
        }

        throw ExceptionUtils.newConfigurationException(null,
                "There isn't have a result resolver to resolve : [" + toString() + "]");
    }

    public HandlerMethod(Method method, List<MethodParameter> parameters, Class<?> reutrnType) {
        this(method, reutrnType, parameters.toArray(new MethodParameter[0]));
    }

    public HandlerMethod(Method method, Class<?> reutrnType, MethodParameter... parameters) {
        this.method = method;
        this.parameters = parameters;
        this.reutrnType = reutrnType;

        final Type parameterizedType = reutrnType.getGenericSuperclass();
        if (parameterizedType instanceof ParameterizedType) {
            genericityClass = ((ParameterizedType) parameterizedType).getActualTypeArguments();
        }
        else {
            genericityClass = null;
        }
        this.resultResolver = obtainResolver();
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

    // ----
    public boolean isInterface() {
        return reutrnType.isInterface();
    }

    public boolean isArray() {
        return reutrnType.isArray();
    }

    public boolean isAssignableFrom(Class<?> superClass) {
        return superClass.isAssignableFrom(reutrnType);
    }

    public boolean is(Class<?> reutrnType) {
        return reutrnType == this.reutrnType;
    }

    public Type getGenericityClass(int index) {

        if (genericityClass != null) {
            if (genericityClass.length > index) {
                return genericityClass[index];
            }
        }
        return null;
    }

    public boolean isGenericPresent(final Type requiredType, int index) {

        if (genericityClass != null) {
            if (genericityClass.length > index) {
                return genericityClass[index].equals(requiredType);
            }
        }
        return false;
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

    public boolean isDeclaringClassPresent(Class<? extends Annotation> annotationClass) {
        return getDeclaringClassAnnotation(annotationClass) != null;
    }

    public boolean isMethodPresent(Class<? extends Annotation> annotationClass) {
        return getMethodAnnotation(annotationClass) != null;
    }

    public <A extends Annotation> A getDeclaringClassAnnotation(Class<A> annotation) {
        return getAnnotation(method.getDeclaringClass(), annotation);
    }

    public <A extends Annotation> A getMethodAnnotation(Class<A> annotation) {
        return getAnnotation(method, annotation);
    }

    public <A extends Annotation> A getAnnotation(AnnotatedElement element, Class<A> annotation) {

        final Collection<A> a = ClassUtils.getAnnotation(element, annotation);
        if (a.isEmpty()) {
            return null;
        }

        return a.iterator().next();
    }

    // -------------

    public void resolveResult(final RequestContext requestContext, final Object result) throws Throwable {
        resultResolver.resolveResult(requestContext, result);
    }

    public Object[] resolveParameters(final RequestContext requestContext) throws Throwable {
        // log.debug("set parameter start");
        final Object[] args = new Object[parameters.length];
        int i = 0;
        for (final MethodParameter parameter : parameters) {
            args[i++] = parameter.resolveParameter(requestContext);
        }
        return args;
    }

    public static void addResolver(ResultResolver... parameterResolver) {
        RESULT_RESOLVERS.addAll(Arrays.asList(parameterResolver));
    }

    public static void addResolver(List<ResultResolver> parameterResolver) {
        RESULT_RESOLVERS.addAll(parameterResolver);
    }

    @Override
    public String toString() {
        return "{method=" + getMethod() + ", parameter=[" + Arrays.toString(getParameters()) + "]}";
    }
}
