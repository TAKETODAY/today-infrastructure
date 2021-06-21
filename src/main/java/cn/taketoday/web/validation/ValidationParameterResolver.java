/**
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

import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.resolver.ParameterResolvers;

/**
 * @author TODAY 2019-07-20 17:00
 */
public class ValidationParameterResolver
        extends OrderedSupport implements ParameterResolver {

  /** list of validators @since 3.0 */
  private final WebValidator validator;
  private final HashMap<MethodParameter, ParameterResolver> resolverMap = new HashMap<>();
  private static final Class<? extends Annotation> VALID_CLASS = ClassUtils.loadClass("javax.validation.Valid");

  private ParameterResolvers resolvers;

  public ValidationParameterResolver(WebValidator validator) {
    this(HIGHEST_PRECEDENCE + 100, validator);
  }

  @Autowired
  public ValidationParameterResolver(WebValidator validator, ParameterResolvers resolvers) {
    this(HIGHEST_PRECEDENCE + 100, validator, resolvers);
  }

  public ValidationParameterResolver(final int order, final WebValidator validator) {
    this(order, validator, null);
  }

  public ValidationParameterResolver(
          final int order, final WebValidator validator, ParameterResolvers resolvers) {
    super(order);
    Assert.notNull(validator, "WebValidator must not be null");
    this.validator = validator;
    this.resolvers = resolvers;
  }

  @Override
  public boolean supports(MethodParameter parameter) {

    if (parameter.isAnnotationPresent(VALID_CLASS)) {
      for (final ParameterResolver resolver : obtainResolvers().getResolvers()) {
        if (resolver != this && resolver.supports(parameter)) {
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
    context.setAttribute(Constant.VALIDATION_ERRORS, errors);

    doValidate(getValidator(), value, errors);

    if (errors.hasErrors()) {
      final MethodParameter[] parameters = parameter.getHandlerMethod().getParameters();
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
   * Use {@link ParameterResolver#resolveParameter(RequestContext, MethodParameter)}
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

  protected ParameterResolver getResolver(final MethodParameter parameter) {
    return resolverMap.get(parameter);
  }

  public void setResolvers(ParameterResolvers resolvers) {
    this.resolvers = resolvers;
  }

  public ParameterResolvers getResolvers() {
    return resolvers;
  }

  private ParameterResolvers obtainResolvers() {
    final ParameterResolvers ret = getResolvers();
    Assert.state(ret != null, "No ParameterResolvers.");
    return ret;
  }

  private ParameterResolver obtainResolver(final MethodParameter parameter) {
    final ParameterResolver resolver = getResolver(parameter);
    Assert.state(resolver != null, "target parameter resolver must not be null");
    return resolver;
  }

}
