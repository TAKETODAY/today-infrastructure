/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.validation.ParameterNameProvider;

import cn.taketoday.core.DefaultParameterNameDiscoverer;

/**
 * @author TODAY 2019-07-21 20:26
 * @since 3.0
 */
public class ContextParameterNameProvider implements ParameterNameProvider {
  DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  @Override
  public List<String> getParameterNames(Constructor<?> constructor) {
    List<String> parameterNames = new ArrayList<>(constructor.getParameterCount());

    for (final Parameter parameter : constructor.getParameters()) {
      parameterNames.add(parameter.getName());
    }
    return Collections.unmodifiableList(parameterNames);
  }

  @Override
  public List<String> getParameterNames(Method method) {
    return Arrays.asList(parameterNameDiscoverer.getParameterNames(method));
  }

  public void setParameterNameDiscoverer(DefaultParameterNameDiscoverer parameterNameDiscoverer) {
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  public DefaultParameterNameDiscoverer getParameterNameDiscoverer() {
    return parameterNameDiscoverer;
  }

}
