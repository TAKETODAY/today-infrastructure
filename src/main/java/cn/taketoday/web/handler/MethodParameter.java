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
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.resolver.method.ParameterResolver;

/**
 * @author TODAY
 * @version 2.3.7 <br>
 */
public class MethodParameter {

    private final String name;
    private final boolean required;
    private final Class<?> parameterClass;

    private int pathIndex = 0;
    /** the default value */
    private final String defaultValue;
//    private String[] splitMethodUrl = null;

    private final Type[] genericityClass;

    private final ParameterResolver resolver;

    private final Parameter parameter; // reflect parameter instance

    private HandlerMethod handlerMethod;

    private static final List<ParameterResolver> PARAMETER_RESOLVERS = new ArrayList<>();

    public MethodParameter(String name, //@off
                           boolean required,
                           String defaultValue,
                           Parameter parameter,
                           Type[] genericityClass,
                           Class<?> parameterClass) {//@on
        this.name = name;
        this.required = required;
        this.parameter = parameter;
        this.defaultValue = defaultValue;
        this.genericityClass = genericityClass;
        this.parameterClass = Objects.requireNonNull(parameterClass);

        this.resolver = obtainResolver(); // must invoke at last
    }

    public boolean isInterface() {
        return parameterClass.isInterface();
    }

    public boolean isArray() {
        return parameterClass.isArray();
    }

    public boolean is(final Class<?> type) {
        return type == this.parameterClass;
    }

    public boolean isAssignableFrom(final Class<?> superClass) {
        return superClass.isAssignableFrom(parameterClass);
    }

    public boolean isInstance(final Object obj) {
        return parameterClass.isInstance(obj);
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

    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        return ClassUtils.isAnnotationPresent(parameter, annotationClass);
    }

    public <A extends Annotation> A getAnnotation(final Class<A> annotationClass) {
        if (annotationClass == null) {
            return null;
        }
        return ClassUtils.getAnnotation(annotationClass, parameter);
    }

    // ----- resolver

    final Object resolveParameter(final RequestContext requestContext) throws Throwable {
        return resolver.resolveParameter(requestContext, this);
    }

    /**
     * Get correspond parameter resolver, If there isn't a suitable resolver will be
     * throw {@link ConfigurationException}
     * 
     * @return A suitable {@link ParameterResolver}
     */
    protected ParameterResolver obtainResolver() throws ConfigurationException {

        for (final ParameterResolver resolver : getParameterResolvers()) {
            if (resolver.supports(this)) {
                return resolver;
            }
        }

        throw new ConfigurationException("There isn't have a parameter resolver to resolve parameter: [" //
                + getParameterClass() + "] called: [" + getName() + "] ");
    }

    public static void addResolver(ParameterResolver... resolver) {

        Collections.addAll(PARAMETER_RESOLVERS, resolver);
        OrderUtils.reversedSort(PARAMETER_RESOLVERS);
    }

    public static void addResolver(List<ParameterResolver> resolvers) {

        PARAMETER_RESOLVERS.addAll(resolvers);
        OrderUtils.reversedSort(PARAMETER_RESOLVERS);
    }

    public static List<ParameterResolver> getParameterResolvers() {
        return PARAMETER_RESOLVERS;
    }

    public int getIndex() {

        final Parameter[] parameters = parameter.getDeclaringExecutable().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].equals(this.parameter)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public int hashCode() {
        return parameter.hashCode();
    }

    @Override
    public String toString() {
        return parameter.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof MethodParameter && parameter.equals(((MethodParameter) obj).parameter));
    }

    // Getter
    // ----------------------------

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public Class<?> getParameterClass() {
        return parameterClass;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Type[] getGenericityClass() {
        return genericityClass;
    }

    public ParameterResolver getResolver() {
        return resolver;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public int getPathIndex() {
        return pathIndex;
    }

    public MethodParameter setPathIndex(int pathIndex) {
        this.pathIndex = pathIndex;
        return this;
    }

    public HandlerMethod getHandlerMethod() {
        return handlerMethod;
    }

    MethodParameter setHandlerMethod(HandlerMethod handlerMethod) {
        this.handlerMethod = handlerMethod;
        return this;
    }

    public String getPathPattern() {
        return handlerMethod.getPathPattern();
    }
}
