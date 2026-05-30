/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.messaging.handler.annotation;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.security.Principal;

import infra.aot.hint.BindingReflectionHintsRegistrar;
import infra.aot.hint.ExecutableMode;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.annotation.ReflectiveProcessor;
import infra.core.MethodParameter;
import infra.messaging.Message;
import infra.messaging.MessageHeaders;
import infra.messaging.simp.annotation.SubscribeMapping;
import infra.messaging.support.MessageHeaderAccessor;

/**
 * {@link ReflectiveProcessor} implementation for types annotated
 * with {@link MessageMapping @MessageMapping},
 * {@link SubscribeMapping @SubscribeMapping}
 * and {@link MessageExceptionHandler @MessageExceptionHandler}.
 * In addition to registering reflection hints for invoking
 * the annotated method, this implementation handles:
 *
 * <ul>
 *     <li>Return types</li>
 *     <li>Parameters identified as potential payloads</li>
 *     <li>{@link Message} parameters</li>
 *     <li>Exception classes specified via {@link MessageExceptionHandler @MessageExceptionHandler}</li>
 * </ul>
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class MessageMappingReflectiveProcessor implements ReflectiveProcessor {

  private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

  @Override
  public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
    if (element instanceof Class<?> type) {
      registerTypeHints(hints, type);
    }
    else if (element instanceof Method method) {
      registerMethodHints(hints, method);
      if (element.isAnnotationPresent(MessageExceptionHandler.class)) {
        registerMessageExceptionHandlerHints(hints, element.getAnnotation(MessageExceptionHandler.class));
      }
    }
  }

  protected void registerTypeHints(ReflectionHints hints, Class<?> type) {
    hints.registerType(type);
  }

  protected void registerMethodHints(ReflectionHints hints, Method method) {
    hints.registerMethod(method, ExecutableMode.INVOKE);
    registerParameterHints(hints, method);
    registerReturnValueHints(hints, method);
  }

  protected void registerParameterHints(ReflectionHints hints, Method method) {
    hints.registerMethod(method, ExecutableMode.INVOKE);
    for (Parameter parameter : method.getParameters()) {
      MethodParameter methodParameter = MethodParameter.forParameter(parameter);
      if (Message.class.isAssignableFrom(methodParameter.getParameterType())) {
        Type messageType = getMessageType(methodParameter);
        if (messageType != null) {
          this.bindingRegistrar.registerReflectionHints(hints, messageType);
        }
      }
      else if (couldBePayload(methodParameter)) {
        this.bindingRegistrar.registerReflectionHints(hints, methodParameter.getGenericParameterType());
      }
    }
  }

  protected void registerMessageExceptionHandlerHints(ReflectionHints hints, MessageExceptionHandler annotation) {
    for (Class<?> exceptionClass : annotation.value()) {
      hints.registerType(exceptionClass);
    }
  }

  protected boolean couldBePayload(MethodParameter methodParameter) {
    return !methodParameter.hasParameterAnnotation(DestinationVariable.class) &&
            !methodParameter.hasParameterAnnotation(Header.class) &&
            !methodParameter.hasParameterAnnotation(Headers.class) &&
            !MessageHeaders.class.isAssignableFrom(methodParameter.getParameterType()) &&
            !MessageHeaderAccessor.class.isAssignableFrom(methodParameter.getParameterType()) &&
            !Principal.class.isAssignableFrom(methodParameter.nestedIfOptional().getNestedParameterType());
  }

  protected void registerReturnValueHints(ReflectionHints hints, Method method) {
    MethodParameter returnType = MethodParameter.forExecutable(method, -1);
    this.bindingRegistrar.registerReflectionHints(hints, returnType.getGenericParameterType());
  }

  protected @Nullable Type getMessageType(MethodParameter parameter) {
    MethodParameter nestedParameter = parameter.nested();
    return (nestedParameter.getNestedParameterType() == nestedParameter.getParameterType() ?
            null : nestedParameter.getNestedParameterType());
  }

}
