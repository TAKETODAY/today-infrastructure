/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.format.datetime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import cn.taketoday.context.support.EmbeddedValueResolutionSupport;
import cn.taketoday.format.AnnotationFormatterFactory;
import cn.taketoday.format.Formatter;
import cn.taketoday.format.Parser;
import cn.taketoday.format.Printer;
import cn.taketoday.format.annotation.DateTimeFormat;
import cn.taketoday.util.StringUtils;

/**
 * Formats fields annotated with the {@link DateTimeFormat} annotation using a {@link DateFormatter}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
        implements AnnotationFormatterFactory<DateTimeFormat> {

  @Override
  public Set<Class<?>> getFieldTypes() {
    return Set.of(Date.class, Calendar.class, Long.class);
  }

  @Override
  public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
    return getFormatter(annotation, fieldType);
  }

  @Override
  public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
    return getFormatter(annotation, fieldType);
  }

  protected Formatter<Date> getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
    DateFormatter formatter = new DateFormatter();
    formatter.setSource(annotation);
    formatter.setIso(annotation.iso());

    String style = resolveEmbeddedValue(annotation.style());
    if (StringUtils.isNotEmpty(style)) {
      formatter.setStylePattern(style);
    }

    String pattern = resolveEmbeddedValue(annotation.pattern());
    if (StringUtils.isNotEmpty(pattern)) {
      formatter.setPattern(pattern);
    }

    ArrayList<String> resolvedFallbackPatterns = new ArrayList<>();
    for (String fallbackPattern : annotation.fallbackPatterns()) {
      String resolvedFallbackPattern = resolveEmbeddedValue(fallbackPattern);
      if (StringUtils.isNotEmpty(resolvedFallbackPattern)) {
        resolvedFallbackPatterns.add(resolvedFallbackPattern);
      }
    }
    if (!resolvedFallbackPatterns.isEmpty()) {
      formatter.setFallbackPatterns(StringUtils.toStringArray(resolvedFallbackPatterns));
    }

    return formatter;
  }

}
