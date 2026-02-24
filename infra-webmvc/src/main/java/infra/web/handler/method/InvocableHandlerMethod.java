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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import infra.beans.factory.BeanFactory;
import infra.context.MessageSource;
import infra.http.HttpStatusCode;
import infra.util.StringUtils;
import infra.web.HttpRequestHandler;
import infra.web.RequestContext;
import infra.web.annotation.ResponseStatus;
import infra.web.bind.RequestContextDataBinder;
import infra.web.bind.resolver.ParameterResolvingStrategies;
import infra.web.view.View;

/**
 * Extension of {@link HandlerMethod} that invokes the underlying method with
 * argument values resolved from the current HTTP request through a list of
 * {@link infra.web.bind.resolver.ParameterResolvingStrategy}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:12
 */
public class InvocableHandlerMethod extends HandlerMethod {

  private static final Object[] EMPTY_ARGS = new Object[0];

  protected final ResolvableMethodParameter[] resolvableParameters;

  /**
   * Create an instance from a {@code HandlerMethod}.
   */
  public InvocableHandlerMethod(HandlerMethod handlerMethod, ResolvableParameterFactory factory) {
    super(handlerMethod);
    this.resolvableParameters = factory.getParameters(this);
  }

  /**
   * Create an instance from a bean instance and a method.
   */
  public InvocableHandlerMethod(Object bean, Method method, ResolvableParameterFactory factory) {
    this(bean, method, null, factory);
  }

  public InvocableHandlerMethod(Object bean, Method method, @Nullable MessageSource messageSource, ResolvableParameterFactory factory) {
    super(bean, method, messageSource);
    this.resolvableParameters = factory.getParameters(this);
  }

  public InvocableHandlerMethod(String beanName, BeanFactory beanFactory,
          @Nullable MessageSource messageSource, Method method, ResolvableParameterFactory factory) {
    super(beanName, beanFactory, messageSource, method);
    this.resolvableParameters = factory.getParameters(this);
  }

  private InvocableHandlerMethod(HandlerMethod handlerMethod, Object handler, ResolvableMethodParameter[] resolvableParameters) {
    super(handlerMethod, handler);
    this.resolvableParameters = resolvableParameters;
  }

  @Override
  public HandlerMethod withBean(Object handler) {
    return new InvocableHandlerMethod(this, handler, resolvableParameters);
  }

  /**
   * Invoke the method and handle the status
   *
   * @param request the current request
   */
  @Nullable
  public Object invokeAndHandle(RequestContext request) throws Throwable {
    Object returnValue = invokeForRequest(request, (Object[]) null);
    applyResponseStatus(request);

    if (returnValue == null) {
      if (request.isNotModified() || getResponseStatus() != null) {
        return HttpRequestHandler.NONE_RETURN_VALUE;
      }
    }
    else if (StringUtils.hasText(getResponseStatusReason())) {
      return HttpRequestHandler.NONE_RETURN_VALUE;
    }

    return returnValue;
  }

  @Nullable
  public Object invokeAndHandle(RequestContext request, Object @Nullable ... providedArgs) throws Throwable {
    Object returnValue = invokeForRequest(request, providedArgs);
    applyResponseStatus(request);

    if (returnValue == null) {
      if (request.isNotModified() || getResponseStatus() != null) {
        return HttpRequestHandler.NONE_RETURN_VALUE;
      }
    }
    else if (StringUtils.hasText(getResponseStatusReason())) {
      return HttpRequestHandler.NONE_RETURN_VALUE;
    }

    return returnValue;
  }

  /**
   * Set the response status according to the {@link ResponseStatus} annotation.
   */
  private void applyResponseStatus(RequestContext response) throws IOException {
    HttpStatusCode status = getResponseStatus();
    if (status == null) {
      return;
    }

    String reason = getResponseStatusReason();
    if (StringUtils.hasText(reason)) {
      response.sendError(status.value(), reason);
    }
    else {
      response.setStatus(status.value());
    }

    // To be picked up by RedirectView
    response.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, status);
  }

  /**
   * Invoke the method after resolving its argument values in the context of the given request.
   * <p>Argument values are commonly resolved through
   * {@link ParameterResolvingStrategies ParameterResolvingStrategies}.
   * The {@code providedArgs} parameter however may supply argument values to be used directly,
   * i.e. without argument resolution. Examples of provided argument values include a
   * {@link RequestContextDataBinder} or a thrown exception instance.
   * Provided argument values are checked before argument resolvers.
   * <p>Delegates to {@link #getMethodArgumentValues} and calls {@link Method#invoke(Object, Object...)} with the
   * resolved arguments.
   *
   * @param request the current request
   * @param providedArgs "given" arguments matched by type, not resolved
   * @return the raw value returned by the invoked method
   * @throws Exception raised if no suitable argument resolver can be found,
   * or if the method raised an exception
   * @see #getMethodArgumentValues
   */
  @Nullable
  public Object invokeForRequest(RequestContext request, Object @Nullable ... providedArgs) throws Throwable {
    @Nullable Object[] args = getMethodArgumentValues(request, providedArgs);
    if (log.isTraceEnabled()) {
      log.trace("Arguments: {}", Arrays.toString(args));
    }

    try {
      return bridgedMethod.invoke(getBean(), args);
    }
    catch (IllegalArgumentException ex) {
      assertTargetBean(bridgedMethod, getBean(), args);
      String text = (ex.getMessage() == null || ex.getCause() instanceof NullPointerException)
              ? "Illegal argument" : ex.getMessage();
      throw new IllegalStateException(formatInvokeError(text, args), ex);
    }
    catch (InvocationTargetException ex) {
      // Unwrap for HandlerExceptionResolvers ...
      Throwable targetException = ex.getTargetException();
      if (targetException instanceof RuntimeException) {
        throw targetException;
      }
      else if (targetException instanceof Error) {
        throw targetException;
      }
      else if (targetException instanceof Exception) {
        throw targetException;
      }
      else {
        throw new IllegalStateException(formatInvokeError("Invocation failure", args), targetException);
      }
    }
  }

  /**
   * Get the method argument values for the current request, checking the provided
   * argument values and falling back to the configured argument resolvers.
   * <p>The resulting array will be passed into {@link Method#invoke(Object, Object...)}.
   */
  private @Nullable Object[] getMethodArgumentValues(RequestContext request, Object @Nullable [] providedArgs) throws Throwable {
    ResolvableMethodParameter[] parameters = this.resolvableParameters;
    int length = parameters.length;
    if (length == 0) {
      return EMPTY_ARGS;
    }
    @Nullable Object[] args = new Object[length];
    for (int i = 0; i < length; i++) {
      Object arg = null;
      if (providedArgs != null) {
        // type match
        Class<?> parameterType = parameters[i].getParameterType();
        for (Object providedArg : providedArgs) {
          if (parameterType.isInstance(providedArg)) {
            arg = providedArg;
            break;
          }
        }
      }

      if (arg == null) {
        try {
          arg = parameters[i].resolveParameter(request);
        }
        catch (Throwable ex) {
          // Leave stack trace for later, exception may actually be resolved and handled...
          if (log.isDebugEnabled()) {
            String exMsg = ex.getMessage();
            if (exMsg != null && !exMsg.contains(parameters[i].getMethod().toGenericString())) {
              log.debug(formatArgumentError(parameters[i].getParameter(), exMsg));
            }
          }
          throw ex;
        }
      }
      args[i] = arg;
    }
    return args;
  }

  /**
   * Assert that the target bean class is an instance of the class where the given
   * method is declared. In some cases the actual controller instance at request-
   * processing time may be a JDK dynamic proxy (lazy initialization, prototype
   * beans, and others). {@code @Controller}'s that require proxying should prefer
   * class-based proxy mechanisms.
   */
  private void assertTargetBean(Method method, Object targetBean, @Nullable Object[] args) {
    Class<?> methodDeclaringClass = method.getDeclaringClass();
    Class<?> targetBeanClass = targetBean.getClass();
    if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
      String text = """
              The mapped handler method class '%s' is not an instance of the actual controller bean class '%s'. \
              If the controller requires proxying (e.g. due to @Transactional), please use class-based proxying."""
              .formatted(methodDeclaringClass.getName(), targetBeanClass.getName());
      throw new IllegalStateException(formatInvokeError(text, args));
    }
  }

  private String formatInvokeError(String text, @Nullable Object[] args) {
    String formattedArgs = IntStream.range(0, args.length)
            .mapToObj(i -> {
              Object arg = args[i];
              return (arg == null ? "[%d] [null]".formatted(i)
                      : "[%d] [type=%s] [value=%s]".formatted(i, arg.getClass().getName(), arg));
            })
            .collect(Collectors.joining(",\n", " ", " "));
    return "%s\nController [%s]\nMethod [%s] with argument values:\n%s"
            .formatted(text, getBeanType().getName(), bridgedMethod.toGenericString(), formattedArgs);
  }

}
