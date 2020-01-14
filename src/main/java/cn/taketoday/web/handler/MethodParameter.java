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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.NumberUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.resolver.method.ParameterResolver;
import cn.taketoday.web.resolver.method.ParameterResolvers;

/**
 * @author TODAY
 * @version 2.3.7 <br>
 */
@SuppressWarnings("serial")
public class MethodParameter implements Serializable {

    private final String name;
    private final boolean required;
    private final int parameterIndex;
    private final Class<?> parameterClass;

    /** the default value */
    private final String defaultValue;
    private final Parameter parameter; // reflect parameter instance
    private HandlerMethod handlerMethod;
    private final Type[] genericityClass;
    private final ParameterResolver resolver;

    public MethodParameter(HandlerMethod handlerMethod, MethodParameter other) {
        this(other.name,
             other.required,
             other.parameterIndex,
             other.defaultValue,
             other.parameter,
             other.genericityClass,
             other.parameterClass,
             handlerMethod);
    }

    public MethodParameter(String name, //@off
                           boolean required,
                           int parameterIndex,
                           String defaultValue,
                           Parameter parameter,
                           Type[] genericityClass,
                           Class<?> parameterClass) {//@on
        this(name,
             required,
             parameterIndex,
             defaultValue,
             parameter,
             genericityClass,
             parameterClass,
             null);
    }

    public MethodParameter(String name, //@off
                           boolean required,
                           int parameterIndex,
                           String defaultValue,
                           Parameter parameter,
                           Type[] genericityClass,
                           Class<?> parameterClass, 
                           HandlerMethod handlerMethod) {//@on
        this.name = name;
        this.required = required;
        this.defaultValue = defaultValue;
        this.handlerMethod = handlerMethod;
        this.parameterIndex = parameterIndex;
        this.genericityClass = genericityClass;
        this.parameter = Objects.requireNonNull(parameter);
        this.parameterClass = Objects.requireNonNull(parameterClass);

        this.resolver = ParameterResolvers.obtainResolver(this); // must invoke at last
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
        return annotationClass == null ? null : ClassUtils.getAnnotation(annotationClass, parameter);
    }

    // ----- resolver

    protected Object resolveParameter(final RequestContext requestContext) throws Throwable {
        return resolver.resolveParameter(requestContext, this);
    }

    public int getParameterIndex() {
        return parameterIndex;
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

    public HandlerMethod getHandlerMethod() {
        return handlerMethod;
    }

    MethodParameter setHandlerMethod(HandlerMethod handlerMethod) {
        this.handlerMethod = handlerMethod;
        return this;
    }

    // static
    // --------------------------------------

    public static MethodParameter[] ofMethod(Method method) {

        final Parameter[] parameters = method.getParameters();
        final int length = parameters.length;

        final MethodParameter[] ret = new MethodParameter[length];
        final String[] methodArgsNames = ClassUtils.getMethodArgsNames(method);

        for (int i = 0; i < length; i++) {
            ret[i] = of(i, parameters[i], methodArgsNames[i]);
        }
        return ret;
    }

    /**
     * Create a method parameter
     * 
     * @param parameter
     *            Reflect parameter
     * @param methodArgsName
     *            method parameter names
     * @return {@link MethodParameter}
     */
    public static MethodParameter of(final int index, final Parameter parameter, final String methodArgsName) {

        Type[] genericityClass = null;
        String parameterName = Constant.BLANK;
        Class<?> parameterClass = parameter.getType();
        // annotation
        boolean required = false;
        String defaultValue = null;

        final Type parameterizedType = parameter.getParameterizedType();

        if (parameterizedType instanceof ParameterizedType) {
            genericityClass = ((ParameterizedType) parameterizedType).getActualTypeArguments();
        }

        final AnnotationAttributes attributes = ClassUtils.getAnnotationAttributes(RequestParam.class, parameter);
        if (attributes != null) {
            required = attributes.getBoolean("required");
            defaultValue = attributes.getString("defaultValue");
            parameterName = attributes.getString(Constant.VALUE);
        }

        if (StringUtils.isEmpty(defaultValue) && NumberUtils.isNumber(parameterClass)) {
            defaultValue = "0"; // fix default value
        }

        if (StringUtils.isEmpty(parameterName)) {
            parameterName = methodArgsName; // use method parameter name
        }
        return new MethodParameter(parameterName, required, index, defaultValue, parameter, genericityClass, parameterClass);
    }

}
