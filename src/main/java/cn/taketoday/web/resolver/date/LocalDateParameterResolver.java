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

package cn.taketoday.web.resolver.date;

import java.time.LocalDate;
import java.time.temporal.TemporalAccessor;

import cn.taketoday.util.DateUtils;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.resolver.ParameterResolvingStrategy;

/**
 * for {@link LocalDate}
 *
 * @author TODAY 2021/2/23 20:45
 * @since 3.0
 */
public class LocalDateParameterResolver
        extends AbstractJavaTimeParameterResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    return parameter.is(LocalDate.class);
  }

  @Override
  protected Object fromTemporalAccessor(TemporalAccessor temporalAccessor) {
    return DateUtils.ofDate(temporalAccessor);
  }

}
