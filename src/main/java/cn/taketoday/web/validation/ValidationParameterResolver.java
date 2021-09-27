/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.validation;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import cn.taketoday.beans.Autowired;
import cn.taketoday.core.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.resolver.ParameterResolvingStrategy;

/**
 * @author TODAY 2019-07-20 17:00
 * @see javax.validation.Valid
 */
public class ValidationParameterResolver implements ParameterResolvingStrategy {

  /** list of validators @since 3.0 */
  private final WebValidator validator;
  private final HashMap<MethodParameter, ParameterResolvingStrategy> resolverMap = new HashMap<>();
  private static final Class<? extends Annotation> VALID_CLASS = ClassUtils.loadClass("javax.validation.Valid");

  private ParameterResolvingRegistry resolvingRegistry;

  public ValidationParameterResolver(WebValidator validator) {
    this(validator, null);
  }

  @Autowired
  public ValidationParameterResolver(
          WebValidator validator, ParameterResolvingRegistry registry) {
    Assert.notNull(validator, "WebValidator must not be null");
    this.validator = validator;
    this.resolvingRegistry = registry;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    if (parameter.isAnnotationPresent(Validated.class)
            || parameter.isAnnotationPresent(VALID_CLASS)) {
      for (final ParameterResolvingStrategy resolver : obtainResolvers().getDefaultStrategies()) {
        if (resolver != this && resolver.supportsParameter(parameter)) {
          resolverMap.put(parameter, resolver);
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Object resolveParameter(final RequestContext context, final MethodParameter parameter) throws Throwable {
    final Object value = resolveValue(context, parameter);

    final DefaultErrors errors = new DefaultErrors();
    context.setAttribute(Validator.KEY_VALIDATION_ERRORS, errors);

    doValidate(getValidator(), value, errors);

    if (errors.hasErrors()) {
      HandlerMethod method = parameter.getHandlerMethod();
      Assert.state(method != null, "No HandlerMethod");
      final MethodParameter[] parameters = method.getParameters();
      final int length = parameters.length;
      if (length == 1) {
        // use  @ExceptionHandler(ValidationException.class) to handle validation exception
        throw buildException(errors);
      }
      // > 1
      int index = parameter.getParameterIndex();
      if (++index == length || !parameters[index].isAssignableTo(Errors.class)) {
        // use  @ExceptionHandler(ValidationException.class) to handle validation exception
        throw buildException(errors);
      }
    }
    return value;
  }

  /**
   * Use {@link ParameterResolvingStrategy#resolveParameter(RequestContext, MethodParameter)}
   *
   * @return Has not been validate parameter value
   */
  protected Object resolveValue(RequestContext context, MethodParameter parameter) throws Throwable {
    return obtainResolver(parameter).resolveParameter(context, parameter);
  }

  /**
   * {@link WebValidator#validate(Object, Errors)}
   */
  protected void doValidate(WebValidator validator, Object value, DefaultErrors errors) {
    validator.validate(value, errors);
  }

  protected Throwable buildException(final Errors errors) {
    if (errors instanceof Throwable) {
      return (Throwable) errors;
    }
    return new ValidationException(errors);
  }

  public WebValidator getValidator() {
    return validator;
  }

  protected ParameterResolvingStrategy getResolver(final MethodParameter parameter) {
    return resolverMap.get(parameter);
  }

  public void setResolvingRegistry(ParameterResolvingRegistry resolvingRegistry) {
    this.resolvingRegistry = resolvingRegistry;
  }

  public ParameterResolvingRegistry getResolvingRegistry() {
    return resolvingRegistry;
  }

  private ParameterResolvingRegistry obtainResolvers() {
    final ParameterResolvingRegistry ret = getResolvingRegistry();
    Assert.state(ret != null, "No ParameterResolvingRegistry.");
    return ret;
  }

  private ParameterResolvingStrategy obtainResolver(final MethodParameter parameter) {
    final ParameterResolvingStrategy resolver = getResolver(parameter);
    if (resolver == null) {
      throw new IllegalStateException(
              "There is not a parameter resolver in [" + resolvingRegistry + "] to resolve " + parameter);
    }
    return resolver;
  }

}
