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

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import infra.beans.ConversionNotSupportedException;
import infra.beans.TypeMismatchException;
import infra.beans.factory.config.BeanExpressionContext;
import infra.beans.factory.config.BeanExpressionResolver;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.core.MethodParameter;
import infra.web.BindingContext;
import infra.web.RequestContext;
import infra.web.bind.MissingRequestValueException;
import infra.web.bind.RequestContextDataBinder;
import infra.web.context.support.RequestScope;
import infra.web.handler.method.MethodArgumentConversionNotSupportedException;
import infra.web.handler.method.MethodArgumentTypeMismatchException;
import infra.web.handler.method.NamedValueInfo;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * Abstract base class for resolving method arguments from a named value.
 * Request parameters, request headers, and path variables are examples of named
 * values. Each may have a name, a required flag, and a default value.
 *
 * <p>Subclasses define how to do the following:
 * <ul>
 * <li>Obtain named value information for a method parameter
 * <li>Resolve names into argument values
 * <li>Handle missing argument values when argument values are required
 * <li>Optionally handle a resolved value
 * </ul>
 *
 * <p>A default value string can contain ${...} placeholders and Expression
 * Language #{...} expressions. For this to work a
 * {@link ConfigurableBeanFactory} must be supplied to the class constructor.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/19 21:26
 */
public abstract class AbstractNamedValueResolvingStrategy implements ParameterResolvingStrategy {

  @Nullable
  private final ConfigurableBeanFactory configurableBeanFactory;

  @Nullable
  private final BeanExpressionContext expressionContext;

  public AbstractNamedValueResolvingStrategy() {
    this.configurableBeanFactory = null;
    this.expressionContext = null;
  }

  /**
   * Create a new {@link AbstractNamedValueResolvingStrategy} instance.
   *
   * @param beanFactory a bean factory to use for resolving ${...} placeholder
   * and #{...} EL expressions in default values, or {@code null} if default
   * values are not expected to contain expressions
   */
  public AbstractNamedValueResolvingStrategy(@Nullable ConfigurableBeanFactory beanFactory) {
    this.configurableBeanFactory = beanFactory;
    this.expressionContext =
            beanFactory != null ? new BeanExpressionContext(beanFactory, RequestScope.instance) : null;
  }

  @Nullable
  @Override
  public final Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    MethodParameter methodParameter = resolvable.getParameter();
    NamedValueInfo namedValueInfo = getNamedValueInfo(resolvable);

    Object arg;
    if (namedValueInfo.nameEmbedded) {
      Object resolvedName = resolveEmbeddedValuesAndExpressions(namedValueInfo.name);
      if (resolvedName == null) {
        throw new IllegalArgumentException(
                "Specified name must not resolve to null: [%s]".formatted(namedValueInfo.name));
      }
      arg = resolveName(resolvedName.toString(), resolvable, context);
    }
    else {
      arg = resolveName(namedValueInfo.name, resolvable, context);
    }

    if (arg == null) {
      if (namedValueInfo.defaultValue != null) {
        arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
      }
      else if (namedValueInfo.required && !methodParameter.isNullable()) {
        handleMissingValue(namedValueInfo.name, methodParameter, context);
      }
      arg = handleNullValue(namedValueInfo.name, arg, methodParameter.getParameterType());
    }
    else if ("".equals(arg) && namedValueInfo.defaultValue != null) {
      arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
    }

    BindingContext bindingContext = context.getBinding();
    if (bindingContext != null) {
      arg = convertIfNecessary(context, bindingContext, namedValueInfo, methodParameter, arg);
      // Check for null value after conversion of incoming argument value
      if (arg == null) {
        if (namedValueInfo.defaultValue != null) {
          arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
          arg = convertIfNecessary(context, bindingContext, namedValueInfo, methodParameter, arg);
        }
        else if (namedValueInfo.required && !methodParameter.isNullable()) {
          handleMissingValueAfterConversion(namedValueInfo.name, methodParameter, context);
        }
      }
    }

    handleResolvedValue(arg, namedValueInfo.name, resolvable, context);
    return arg;
  }

  @Nullable
  private static Object convertIfNecessary(RequestContext context, BindingContext bindingContext,
          NamedValueInfo namedValueInfo, MethodParameter methodParameter, @Nullable Object arg) throws Throwable {

    RequestContextDataBinder binder = bindingContext.createBinder(context, namedValueInfo.name);
    try {
      arg = binder.convertIfNecessary(arg, methodParameter.getParameterType(), methodParameter);
    }
    catch (ConversionNotSupportedException ex) {
      throw new MethodArgumentConversionNotSupportedException(arg, ex.getRequiredType(),
              namedValueInfo.name, methodParameter, ex.getCause());
    }
    catch (TypeMismatchException ex) {
      throw new MethodArgumentTypeMismatchException(arg, ex.getRequiredType(),
              namedValueInfo.name, methodParameter, ex.getCause());
    }
    return arg;
  }

  protected NamedValueInfo getNamedValueInfo(ResolvableMethodParameter resolvable) {
    return resolvable.getNamedValueInfo();
  }

  /**
   * Resolve the given annotation-specified value,
   * potentially containing placeholders and expressions.
   */
  @Nullable
  private Object resolveEmbeddedValuesAndExpressions(String value) {
    if (this.configurableBeanFactory == null || this.expressionContext == null) {
      return value;
    }
    String placeholdersResolved = this.configurableBeanFactory.resolveEmbeddedValue(value);
    BeanExpressionResolver exprResolver = this.configurableBeanFactory.getBeanExpressionResolver();
    if (exprResolver == null) {
      return value;
    }
    return exprResolver.evaluate(placeholdersResolved, this.expressionContext);
  }

  /**
   * Resolve the given parameter type and value name into an argument value.
   *
   * @param name the name of the value being resolved
   * @param resolvable the method parameter to resolve to an argument value
   * (pre-nested in case of a {@link java.util.Optional} declaration)
   * @param context the current request context
   * @return the resolved argument (may be {@code null})
   * @throws Exception in case of errors
   */
  @Nullable
  protected abstract Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context)
          throws Exception;

  /**
   * Invoked when a named value is required, but
   * {@link #resolveName(String, ResolvableMethodParameter, RequestContext)}
   * returned {@code null} and there is no default value.
   * Subclasses typically throw an exception in this case.
   *
   * @param name the name for the value
   * @param parameter the method parameter
   * @param request the current request
   */
  protected void handleMissingValue(String name, MethodParameter parameter, RequestContext request)
          throws Exception {

    handleMissingValue(name, parameter);
  }

  /**
   * Invoked when a named value is required, but
   * {@link #resolveName(String, ResolvableMethodParameter, RequestContext)}
   * returned {@code null} and there is no default value.
   * Subclasses typically throw an exception in this case.
   *
   * @param name the name for the value
   * @param parameter the method parameter
   */
  protected void handleMissingValue(String name, MethodParameter parameter) {
    throw new MissingRequestValueException("Missing argument '%s' for method parameter of type %s"
            .formatted(name, parameter.getNestedParameterType().getSimpleName()));
  }

  /**
   * Invoked when a named value is present but becomes {@code null} after conversion.
   *
   * @param name the name for the value
   * @param parameter the method parameter
   * @param request the current request
   */
  protected void handleMissingValueAfterConversion(String name, MethodParameter parameter, RequestContext request) throws Exception {
    throw new MissingRequestValueException("Missing argument '%s' for method parameter of type %s"
            .formatted(name, parameter.getParameterType().getSimpleName()), true);
  }

  /**
   * A {@code null} results in a {@code false} value for
   * {@code boolean}s or an exception for other primitives.
   */
  @Nullable
  private Object handleNullValue(String name, @Nullable Object value, Class<?> paramType) {
    if (value == null) {
      if (boolean.class == paramType) {
        return Boolean.FALSE;
      }
      else if (paramType.isPrimitive()) {
        throw new IllegalStateException("""
                Optional %s parameter '%s' is present but cannot be translated into a null value \
                due to being declared as a primitive type. Consider declaring it as object wrapper \
                for the corresponding primitive type.""".formatted(paramType.getSimpleName(), name));
      }
    }
    return value;
  }

  /**
   * Invoked after a value is resolved.
   *
   * @param arg the resolved argument value
   * @param name the argument name
   * @param resolvable the argument parameter type
   * @param context the current request
   */
  protected void handleResolvedValue(@Nullable Object arg,
          String name, ResolvableMethodParameter resolvable, RequestContext context) {

  }

}
