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

package cn.taketoday.web.handler;

import java.lang.reflect.Parameter;

import cn.taketoday.web.resolver.ParameterResolverRegistry;

/**
 * ParameterResolvers MethodParametersBuilder
 *
 * @author TODAY 2021/5/9 23:28
 * @since 3.0.1
 */
public class ParameterResolversMethodParameterBuilder extends MethodParametersBuilder {
  private ParameterResolverRegistry resolversRegistry;

  public ParameterResolversMethodParameterBuilder() {
    this(new ParameterResolverRegistry());
  }

  public ParameterResolversMethodParameterBuilder(ParameterResolverRegistry resolversRegistry) {
    this.resolversRegistry = resolversRegistry;
  }

  @Override
  protected MethodParameter createParameter(String methodArgsName, Parameter parameter, int index) {
    return new ParameterResolverMethodParameter(index, parameter, methodArgsName, resolversRegistry);
  }

  public void setParameterResolvers(ParameterResolverRegistry resolversRegistry) {
    this.resolversRegistry = resolversRegistry;
  }

  public ParameterResolverRegistry getParameterResolvers() {
    return resolversRegistry;
  }
}
