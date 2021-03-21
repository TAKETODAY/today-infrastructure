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
package cn.taketoday.web.resolver;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY 2019-07-07 23:24
 */
@MissingBean
public class ParameterResolvers {

  private final LinkedList<ParameterResolver> resolvers = new LinkedList<>();

  public void addResolver(ParameterResolver... resolver) {
    Collections.addAll(resolvers, resolver);
    OrderUtils.reversedSort(resolvers);
  }

  public void addResolver(List<ParameterResolver> resolvers) {
    this.resolvers.addAll(resolvers);
    OrderUtils.reversedSort(this.resolvers);
  }

  public void setResolver(List<ParameterResolver> resolver) {
    resolvers.clear();
    resolvers.addAll(resolver);
    OrderUtils.reversedSort(resolvers);
  }

  public List<ParameterResolver> getResolvers() {
    return resolvers;
  }

  public ParameterResolver getResolver(final MethodParameter parameter) {
    for (final ParameterResolver resolver : getResolvers()) {
      if (resolver.supports(parameter)) {
        return resolver;
      }
    }
    return null;
  }

  /**
   * Get correspond parameter resolver, If there isn't a suitable resolver will be
   * throw {@link ConfigurationException}
   *
   * @return A suitable {@link ParameterResolver}
   */
  public ParameterResolver obtainResolver(final MethodParameter parameter) {
    final ParameterResolver resolver = getResolver(parameter);
    Assert.state(resolver != null,
                 () -> "There isn't have a parameter resolver to resolve parameter: ["
                         + parameter.getParameterClass() + "] called: [" + parameter.getName() + "]");
    return resolver;
  }

}
