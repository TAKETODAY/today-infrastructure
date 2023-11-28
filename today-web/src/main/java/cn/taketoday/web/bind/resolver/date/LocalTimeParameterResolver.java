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

package cn.taketoday.web.bind.resolver.date;

import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;

import cn.taketoday.util.DateUtils;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;

/**
 * for {@link LocalTime}
 *
 * @author TODAY 2021/2/23 21:16
 * @since 3.0
 */
public class LocalTimeParameterResolver
        extends AbstractJavaTimeParameterResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    return parameter.is(LocalTime.class);
  }

  @Override
  protected Object fromTemporalAccessor(TemporalAccessor temporalAccessor) {
    return DateUtils.ofTime(temporalAccessor);
  }

}

