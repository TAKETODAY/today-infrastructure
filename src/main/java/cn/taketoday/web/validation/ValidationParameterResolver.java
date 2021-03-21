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
import java.util.Map;

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
  private final CompositeValidator validator;
  private final Map<MethodParameter, ParameterResolver> resolverMap = new HashMap<>();
  private static final Class<? extends Annotation> VALID_CLASS = ClassUtils.loadClass("javax.validation.Valid");

  private ParameterResolvers resolvers;

  public ValidationParameterResolver(CompositeValidator validator) {
    this(HIGHEST_PRECEDENCE + 100, validator);
  }

  @Autowired
  public ValidationParameterResolver(CompositeValidator validator, ParameterResolvers resolvers) {
    this(HIGHEST_PRECEDENCE + 100, validator, resolvers);
  }

  public ValidationParameterResolver(final int order, final CompositeValidator validator) {
    super(order);
    this.validator = validator;
  }

  public ValidationParameterResolver(final int order, final CompositeValidator validator, ParameterResolvers resolvers) {
    super(order);
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
    validate(value, errors);
    if (errors.hasErrors()) {
      final MethodParameter[] parameters = parameter.getHandlerMethod().getParameters();
      final int length = parameters.length;
      context.attribute(Constant.VALIDATION_ERRORS, errors);
      if (length == 1) {
        throw buildException(errors);
      }
      // > 1
      int index = parameter.getParameterIndex();
      if (++index == length || !parameters[index].isAssignableFrom(Errors.class)) {
        throw buildException(errors);
      }
    }
    return value;
  }

  protected Object resolveValue(RequestContext context, MethodParameter parameter) throws Throwable {
    return obtainResolver(parameter).resolveParameter(context, parameter);
  }

  protected void validate(Object value, DefaultErrors errors) {
    getValidator().validate(value, errors);
  }

  protected Throwable buildException(final Errors errors) {
    if (errors instanceof Throwable) {
      return (Throwable) errors;
    }
    return new ValidationException(errors);
  }

  public CompositeValidator getValidator() {
    return validator;
  }

  protected ParameterResolver getResolver(final MethodParameter parameter) {
    return resolverMap.get(parameter);
  }

  protected ParameterResolver obtainResolver(final MethodParameter parameter) {
    final ParameterResolver resolver = getResolver(parameter);
    Assert.state(resolver != null, "target parameter resolver must not be null");
    return resolver;
  }

  public void setResolvers(ParameterResolvers resolvers) {
    this.resolvers = resolvers;
  }

  public ParameterResolvers getResolvers() {
    return resolvers;
  }

  protected ParameterResolvers obtainResolvers() {
    final ParameterResolvers ret = getResolvers();
    Assert.state(ret != null, "No ParameterResolvers.");
    return ret;
  }
}
