/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.bind.resolver.date;

import org.jspecify.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import infra.format.annotation.DateTimeFormat;
import infra.lang.Assert;
import infra.util.StringUtils;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * @author TODAY 2021/2/23 21:25
 * @since 3.0
 */
public abstract class AbstractJavaTimeParameterResolver extends AbstractDateParameterResolver {

  private DateTimeFormatter defaultFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Nullable
  @Override
  protected Object resolveInternal(String parameterValue, ResolvableMethodParameter parameter) {
    DateTimeFormatter formatter = getFormatter(parameter);
    try {
      return fromTemporalAccessor(formatter.parse(parameterValue));
    }
    catch (DateTimeParseException e) {
      throw new DateParameterParsingException(parameter.getParameter(), parameterValue, e);
    }
  }

  @Nullable
  protected Object fromTemporalAccessor(TemporalAccessor temporalAccessor) {
    return null;
  }

  /**
   * Get {@link DateTimeFormatter}
   */
  protected DateTimeFormatter getFormatter(ResolvableMethodParameter parameter) {
    final DateTimeFormat dateTimeFormat = getAnnotation(parameter);
    if (dateTimeFormat != null) {
      final String pattern = dateTimeFormat.pattern();
      if (StringUtils.isNotEmpty(pattern)) {
        return DateTimeFormatter.ofPattern(pattern);
      }
    }
    return defaultFormatter;
  }

  public void setDefaultFormatter(DateTimeFormatter defaultFormatter) {
    Assert.notNull(defaultFormatter, "defaultFormatter is required");
    this.defaultFormatter = defaultFormatter;
  }

  public DateTimeFormatter getDefaultFormatter() {
    return defaultFormatter;
  }

}
