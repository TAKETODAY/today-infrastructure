/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.bind.resolver;

import cn.taketoday.beans.ConversionNotSupportedException;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.beans.factory.config.BeanExpressionContext;
import cn.taketoday.beans.factory.config.BeanExpressionResolver;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.context.support.RequestScope;
import cn.taketoday.web.handler.method.MethodArgumentConversionNotSupportedException;
import cn.taketoday.web.handler.method.MethodArgumentTypeMismatchException;
import cn.taketoday.web.handler.method.NamedValueInfo;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

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
    MethodParameter nestedParameter = methodParameter.nestedIfOptional();

    Object arg;
    if (namedValueInfo.nameEmbedded) {
      Object resolvedName = resolveEmbeddedValuesAndExpressions(namedValueInfo.name);
      if (resolvedName == null) {
        throw new IllegalArgumentException(
                "Specified name must not resolve to null: [" + namedValueInfo.name + "]");
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
      else if (namedValueInfo.required && !nestedParameter.isOptional()) {
        handleMissingValue(namedValueInfo.name, nestedParameter, context);
      }
      arg = handleNullValue(namedValueInfo.name, arg, nestedParameter.getNestedParameterType());
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
        else if (namedValueInfo.required && !nestedParameter.isOptional()) {
          handleMissingValueAfterConversion(namedValueInfo.name, nestedParameter, context);
        }
      }
    }

    handleResolvedValue(arg, namedValueInfo.name, resolvable, context);
    return arg;
  }

  @Nullable
  private static Object convertIfNecessary(RequestContext context, BindingContext bindingContext,
          NamedValueInfo namedValueInfo, MethodParameter methodParameter, Object arg) throws Throwable {
    WebDataBinder binder = bindingContext.createBinder(context, namedValueInfo.name);
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
    throw new RequestBindingException("Missing argument '" + name +
            "' for method parameter of type " + parameter.getNestedParameterType().getSimpleName());
  }

  /**
   * Invoked when a named value is present but becomes {@code null} after conversion.
   *
   * @param name the name for the value
   * @param parameter the method parameter
   * @param request the current request
   */
  protected void handleMissingValueAfterConversion(
          String name, MethodParameter parameter, RequestContext request) throws Exception {

    handleMissingValue(name, parameter, request);
  }

  /**
   * A {@code null} results in a {@code false} value for
   * {@code boolean}s or an exception for other primitives.
   */
  @Nullable
  private Object handleNullValue(String name, @Nullable Object value, Class<?> paramType) {
    if (value == null) {
      if (Boolean.TYPE.equals(paramType)) {
        return Boolean.FALSE;
      }
      else if (paramType.isPrimitive()) {
        throw new IllegalStateException("Optional " + paramType.getSimpleName() + " parameter '" + name +
                "' is present but cannot be translated into a null value due to being declared as a " +
                "primitive type. Consider declaring it as object wrapper for the corresponding primitive type.");
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
  protected void handleResolvedValue(
          @Nullable Object arg, String name, ResolvableMethodParameter resolvable, RequestContext context) {

  }

}
