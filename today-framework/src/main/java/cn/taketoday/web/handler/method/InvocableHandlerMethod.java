/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.method;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import cn.taketoday.context.MessageSource;
import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategies;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.bind.support.SessionStatus;
import cn.taketoday.web.bind.support.WebDataBinderFactory;

/**
 * Extension of {@link HandlerMethod} that invokes the underlying method with
 * argument values resolved from the current HTTP request through a list of
 * {@link cn.taketoday.web.bind.resolver.ParameterResolvingStrategy}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:12
 */
public class InvocableHandlerMethod extends HandlerMethod {
  /** Logger that is available to subclasses. */
  protected static final Logger log = LoggerFactory.getLogger(InvocableHandlerMethod.class);

  private static final Object[] EMPTY_ARGS = new Object[0];

  private ParameterResolvingRegistry resolvingRegistry;

  private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  @Nullable
  private WebDataBinderFactory dataBinderFactory;

  private ResolvableParameterFactory parameterFactory;

  /**
   * Create an instance from a {@code HandlerMethod}.
   */
  public InvocableHandlerMethod(HandlerMethod handlerMethod) {
    super(handlerMethod);
  }

  /**
   * Create an instance from a bean instance and a method.
   */
  public InvocableHandlerMethod(Object bean, Method method) {
    super(bean, method);
  }

  /**
   * Variant of {@link #InvocableHandlerMethod(Object, Method)} that
   * also accepts a {@link MessageSource}, for use in sub-classes.
   */
  protected InvocableHandlerMethod(Object bean, Method method, @Nullable MessageSource messageSource) {
    super(bean, method, messageSource);
  }

  /**
   * Construct a new handler method with the given bean instance, method name and parameters.
   *
   * @param bean the object bean
   * @param methodName the method name
   * @param parameterTypes the method parameter types
   * @throws NoSuchMethodException when the method cannot be found
   */
  public InvocableHandlerMethod(Object bean, String methodName, Class<?>... parameterTypes)
          throws NoSuchMethodException {

    super(bean, methodName, parameterTypes);
  }

  /**
   * Set {@link ParameterResolvingStrategies ParameterResolvingStrategies}
   * to use for resolving method argument values.
   */
  public void setResolvingRegistry(ParameterResolvingRegistry resolvingRegistry) {
    this.resolvingRegistry = resolvingRegistry;
  }

  /**
   * Set the ParameterNameDiscoverer for resolving parameter names when needed
   * (e.g. default request attribute name).
   * <p>Default is a {@link DefaultParameterNameDiscoverer}.
   */
  public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  /**
   * Set the {@link WebDataBinderFactory} to be passed to argument resolvers allowing them
   * to create a {@link WebDataBinder} for data binding and type conversion purposes.
   */
  public void setDataBinderFactory(WebDataBinderFactory dataBinderFactory) {
    this.dataBinderFactory = dataBinderFactory;
  }

  /**
   * Invoke the method after resolving its argument values in the context of the given request.
   * <p>Argument values are commonly resolved through
   * {@link ParameterResolvingStrategies ParameterResolvingStrategies}.
   * The {@code providedArgs} parameter however may supply argument values to be used directly,
   * i.e. without argument resolution. Examples of provided argument values include a
   * {@link WebDataBinder}, a {@link SessionStatus}, or a thrown exception instance.
   * Provided argument values are checked before argument resolvers.
   * <p>Delegates to {@link #getMethodArgumentValues} and calls {@link #doInvoke} with the
   * resolved arguments.
   *
   * @param request the current request
   * @param providedArgs "given" arguments matched by type, not resolved
   * @return the raw value returned by the invoked method
   * @throws Exception raised if no suitable argument resolver can be found,
   * or if the method raised an exception
   * @see #getMethodArgumentValues
   * @see #doInvoke
   */
  @Nullable
  public Object invokeForRequest(
          RequestContext request, Object... providedArgs) throws Throwable {

    Object[] args = getMethodArgumentValues(request, providedArgs);
    if (log.isTraceEnabled()) {
      log.trace("Arguments: {}", Arrays.toString(args));
    }
    return doInvoke(args);
  }

  /**
   * Get the method argument values for the current request, checking the provided
   * argument values and falling back to the configured argument resolvers.
   * <p>The resulting array will be passed into {@link #doInvoke}.
   */
  protected Object[] getMethodArgumentValues(
          RequestContext request, Object... providedArgs) throws Throwable {

    MethodParameter[] parameters = getMethodParameters();
    if (ObjectUtils.isEmpty(parameters)) {
      return EMPTY_ARGS;
    }

    Object[] args = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      MethodParameter parameter = parameters[i];
      parameter.initParameterNameDiscovery(parameterNameDiscoverer);
      args[i] = findProvidedArgument(parameter, providedArgs);
      if (args[i] != null) {
        continue;
      }
      var resolvable = new ParameterResolverMethodParameter(parameter, resolvingRegistry);
      ParameterResolvingStrategy strategy = resolvingRegistry.findStrategy(resolvable);
      if (strategy == null) {
        throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));
      }
      try {
        args[i] = strategy.resolveParameter(request, resolvable);
      }
      catch (Throwable ex) {
        // Leave stack trace for later, exception may actually be resolved and handled...
        if (log.isDebugEnabled()) {
          String exMsg = ex.getMessage();
          if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {
            log.debug(formatArgumentError(parameter, exMsg));
          }
        }
        throw ex;
      }
    }
    return args;
  }

  /**
   * Invoke the handler method with the given argument values.
   */
  @Nullable
  protected Object doInvoke(Object... args) throws Exception {
    Method method = getBridgedMethod();
    try {
      return method.invoke(getBean(), args);
    }
    catch (IllegalArgumentException ex) {
      assertTargetBean(method, getBean(), args);
      String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");
      throw new IllegalStateException(formatInvokeError(text, args), ex);
    }
    catch (InvocationTargetException ex) {
      // Unwrap for HandlerExceptionResolvers ...
      Throwable targetException = ex.getTargetException();
      if (targetException instanceof RuntimeException) {
        throw (RuntimeException) targetException;
      }
      else if (targetException instanceof Error) {
        throw (Error) targetException;
      }
      else if (targetException instanceof Exception) {
        throw (Exception) targetException;
      }
      else {
        throw new IllegalStateException(formatInvokeError("Invocation failure", args), targetException);
      }
    }
  }

}
