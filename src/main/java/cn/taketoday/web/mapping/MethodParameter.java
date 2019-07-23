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
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.resolver.method.ParameterResolver;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY
 * @version 2.3.7 <br>
 */
@Setter
@Getter
public class MethodParameter {

    private final String name;
    private final boolean required;
    private final Class<?> parameterClass;

    private int pathIndex = 0;
    /** the default value */
    private final String defaultValue;
    /** @since 2.3.1 */
    private String[] splitMethodUrl = null;

    private final Type[] genericityClass;

    private final ParameterResolver resolver;

    private final Parameter parameter; // reflect parameter instance

    private HandlerMethod handlerMethod;

    private static final List<ParameterResolver> PARAMETER_RESOLVERS = new ArrayList<>();

    public MethodParameter(//
            String name, //
            boolean required, //
            String defaultValue,
            Parameter parameter,
            Type[] genericityClass, //
            Class<?> parameterClass)//
    {
        this.name = name;
        this.required = required;
        this.parameter = parameter;
        this.defaultValue = defaultValue;
        this.parameterClass = parameterClass;
        this.genericityClass = genericityClass;
        this.resolver = obtainResolver();
    }

    public boolean isInterface() {
        return parameterClass.isInterface();
    }

    public boolean isArray() {
        return parameterClass.isArray();
    }

    public boolean is(Class<?> type) {
        return type == this.parameterClass;
    }

    public boolean isAssignableFrom(Class<?> superClass) {
        return superClass.isAssignableFrom(parameterClass);
    }

    public Type getGenericityClass(int index) {

        if (genericityClass != null && genericityClass.length > index) {
            return genericityClass[index];
        }
        return null;
    }

    public boolean isGenericPresent(final Type requiredType, int index) {
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

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {

        if (annotationClass == null) {
            return null;
        }

        final Collection<A> annotation = ClassUtils.getAnnotation(parameter, annotationClass);
        if (annotation.isEmpty()) {
            return null;
        }
        return annotation.iterator().next();
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

        throw ExceptionUtils.newConfigurationException(null,
                "There isn't have a parameter resolver to resolve parameter: [" //
                        + getParameterClass() + "] called: [" + getName() + "] ");
    }

    public static void addResolver(ParameterResolver... parameterResolver) {
        getParameterResolvers().addAll(Arrays.asList(parameterResolver));
    }

    public static void addResolver(List<ParameterResolver> parameterResolver) {
        getParameterResolvers().addAll(parameterResolver);
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
}
