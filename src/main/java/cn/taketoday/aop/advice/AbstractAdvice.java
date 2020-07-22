/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
 * <p>
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.aop.advice;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Joinpoint;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import cn.taketoday.aop.annotation.Annotated;
import cn.taketoday.aop.annotation.Argument;
import cn.taketoday.aop.annotation.Arguments;
import cn.taketoday.aop.annotation.JoinPoint;
import cn.taketoday.aop.annotation.Returning;
import cn.taketoday.aop.annotation.Throwing;
import cn.taketoday.context.Constant;
import cn.taketoday.context.invoker.MethodInvoker;
import cn.taketoday.context.utils.ExceptionUtils;

/**
 * @author TODAY <br>
 * 2018-11-10 11:26
 */
public abstract class AbstractAdvice implements Advice, MethodInterceptor {

    private final Object aspect;
    //private final Method adviceMethod;
    private final MethodInvoker invoker;
    private final byte[] adviceParameters;
    private final int adviceParameterLength;
    private final Class<?>[] adviceParameterTypes;

    public AbstractAdvice(Method adviceMethod, Object aspect) {

        this.aspect = aspect;
        this.invoker = MethodInvoker.create(adviceMethod);
        this.adviceParameterLength = adviceMethod.getParameterCount();
        this.adviceParameters = new byte[adviceParameterLength];
        this.adviceParameterTypes = adviceMethod.getParameterTypes();

        Parameter[] parameters = adviceMethod.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            adviceParameters[i] = Constant.TYPE_NULL;
            if (parameter.isAnnotationPresent(JoinPoint.class)) {
                adviceParameters[i] = Constant.TYPE_JOIN_POINT;
            }
            if (parameter.isAnnotationPresent(Argument.class)) {
                adviceParameters[i] = Constant.TYPE_ARGUMENT;
            }
            if (parameter.isAnnotationPresent(Arguments.class)) {
                adviceParameters[i] = Constant.TYPE_ARGUMENTS;
            }
            if (parameter.isAnnotationPresent(Returning.class)) {
                adviceParameters[i] = Constant.TYPE_RETURNING;
            }
            if (parameter.isAnnotationPresent(Throwing.class)) {
                adviceParameters[i] = Constant.TYPE_THROWING;
            }
            if (parameter.isAnnotationPresent(Annotated.class)) {
                adviceParameters[i] = Constant.TYPE_ANNOTATED;
            }
        }
    }

    @Override
    public abstract Object invoke(MethodInvocation invocation) throws Throwable;

    /**
     * Invoke advice method
     *
     * @param i
     *     Target method invocation
     * @param returnValue
     *     Target method return value
     * @param throwable
     *     Target method throws {@link Exception}
     *
     * @throws Throwable
     *     If any {@link Exception} occurred
     */
    protected Object invokeAdviceMethod(final MethodInvocation i,
                                        final Object returnValue,
                                        final Throwable throwable) throws Throwable //
    {
        return invoker.invoke(aspect,
                              adviceParameterLength == 0 ? null : resolveParameter(i, returnValue, throwable));
    }

    /**
     * Resolve method parameter list
     *
     * @param invocation
     *     The join point
     * @param returnedValue
     *     The method returned value
     * @param ex
     *     The exception
     *
     * @return Method parameter list
     */
    @SuppressWarnings("unchecked")
    protected Object[] resolveParameter(final MethodInvocation invocation,
                                        final Object returnedValue, final Throwable ex) {

        final byte[] adviceParameters = this.adviceParameters;
        final Object[] args = new Object[adviceParameterLength];
        final int adviceParameterLength = this.adviceParameterLength;
        final Class<?>[] adviceParameterTypes = this.adviceParameterTypes;

        for (int i = 0; i < adviceParameterLength; i++) {
            switch (adviceParameters[i]) {
                case Constant.TYPE_THROWING: {
                    if (ex != null) {
                        final Class<?> parameterType = adviceParameterTypes[i];
                        final Throwable throwable = ExceptionUtils.unwrapThrowable(ex);
                        if (parameterType == Throwable.class //
                            || parameterType.isAssignableFrom(throwable.getClass())) //
                        {
                            args[i] = throwable;
                        }
                    }
                    break;
                }
                case Constant.TYPE_ARGUMENT: {
                    // fix: NullPointerException
                    Object[] arguments = invocation.getArguments();
                    if (arguments.length == 1) {
                        args[i] = arguments[0];
                        break;
                    }
                    // for every argument matching
                    for (Object argument : arguments) {
                        if (argument != null && argument.getClass() == adviceParameterTypes[i]) {
                            args[i] = argument;
                            break;
                        }
                    }
                    break;
                }
                case Constant.TYPE_ARGUMENTS:
                    args[i] = invocation.getArguments();
                    break;
                case Constant.TYPE_RETURNING:
                    args[i] = returnedValue;
                    break;
                case Constant.TYPE_ANNOTATED:
                    args[i] = resolveAnnotation(invocation, (Class<? extends Annotation>) adviceParameterTypes[i]);
                    break;
                case Constant.TYPE_JOIN_POINT:
                    args[i] = invocation;
                    break;
                default: {
                    Class<?> parameterType = adviceParameterTypes[i];
                    if (Joinpoint.class.isAssignableFrom(parameterType)) {
                        args[i] = invocation;
                    }
                    if (Annotation.class.isAssignableFrom(parameterType)) {
                        args[i] = resolveAnnotation(invocation, (Class<? extends Annotation>) parameterType);
                    }
                    if (ex != null) {
                        final Throwable throwable = ExceptionUtils.unwrapThrowable(ex);
                        if (parameterType == Throwable.class //
                            || parameterType.isAssignableFrom(throwable.getClass())) //
                        {
                            args[i] = throwable;
                        }
                    }
                    if (returnedValue != null && parameterType.isAssignableFrom(returnedValue.getClass())) {
                        args[i] = returnedValue;
                    }
                    break;
                }
            }
        }
        return args;
    }

    /**
     * Resolve an annotation
     *
     * @param methodInvocation
     *     The join point
     * @param annotationClass
     *     Given annotation class
     *
     * @return Annotation
     */
    private Object resolveAnnotation(MethodInvocation methodInvocation,
                                     Class<? extends Annotation> annotationClass) {
        final Method method = methodInvocation.getMethod();
        Annotation annotation = method.getAnnotation(annotationClass);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(annotationClass);
        }
        return annotation;
    }

}
