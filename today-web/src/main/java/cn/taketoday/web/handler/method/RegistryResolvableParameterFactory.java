/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;

/**
 * ParameterResolvingRegistry ResolvableParameterFactory
 *
 * @author TODAY 2021/5/9 23:28
 * @since 3.0.1
 */
public class RegistryResolvableParameterFactory extends ResolvableParameterFactory {
  private final ParameterResolvingRegistry resolvingRegistry;

  public RegistryResolvableParameterFactory() {
    this(new ParameterResolvingRegistry(), ParameterNameDiscoverer.getSharedInstance());
  }

  public RegistryResolvableParameterFactory(ParameterResolvingRegistry resolvingRegistry) {
    Assert.notNull(resolvingRegistry, "ParameterResolvingRegistry is required");
    this.resolvingRegistry = resolvingRegistry;
  }

  public RegistryResolvableParameterFactory(
          ParameterResolvingRegistry resolvingRegistry, ParameterNameDiscoverer parameterNameDiscoverer) {
    super(parameterNameDiscoverer);
    Assert.notNull(resolvingRegistry, "ParameterResolvingRegistry is required");
    this.resolvingRegistry = resolvingRegistry;
  }

  @Override
  public ResolvableMethodParameter createParameter(MethodParameter parameter) {
    return new ParameterResolverMethodParameter(parameter, resolvingRegistry);
  }

}
