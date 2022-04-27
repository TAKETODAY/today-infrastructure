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

package cn.taketoday.web.bind.resolver;

import java.util.Optional;

import cn.taketoday.lang.Experimental;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * Optional
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Optional
 * @since 4.0 2022/4/27 13:57
 */
@Experimental
public class OptionalTypeParameterResolvingStrategy implements ParameterResolvingStrategy {
  private final ParameterResolvingRegistry registry;

  public OptionalTypeParameterResolvingStrategy(ParameterResolvingRegistry registry) {
    this.registry = registry;
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.is(Optional.class)
            && registry.findStrategy(resolvable.nested()) != null;
  }

  @Nullable
  @Override
  public Object resolveParameter(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    ResolvableMethodParameter nested = resolvable.nested();
    Object parameter = nested.resolveParameter(context);
    return Optional.ofNullable(parameter);
  }

}
