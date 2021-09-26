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

import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.resolver.ParameterResolvingStrategy;

/**
 * @author TODAY <br>
 * 2019-07-20 17:00
 */
public class ErrorsParameterResolver
        extends OrderedSupport implements ParameterResolvingStrategy {

  public ErrorsParameterResolver() {
    this(HIGHEST_PRECEDENCE);
  }

  public ErrorsParameterResolver(final int order) {
    super(order);
  }

  static final Errors EMPTY = new Errors() {

    @Override
    public boolean hasErrors() {
      return false;
    }

    @Override
    public int getErrorCount() {
      return 0;
    }

    @Override
    public Set<ObjectError> getAllErrors() {
      return Collections.emptySet();
    }

    @Override
    public void addError(ObjectError error) {
      // empty implementation
    }
  };

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.is(Errors.class); // fix
  }

  @Override
  public Object resolveParameter(final RequestContext context, final MethodParameter parameter) throws Throwable {
    final Object error = context.getAttribute(Validator.KEY_VALIDATION_ERRORS);
    if (error == null) {
      return EMPTY;
    }
    return error;
  }

}
