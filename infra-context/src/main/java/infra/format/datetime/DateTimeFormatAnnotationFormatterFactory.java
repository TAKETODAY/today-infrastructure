/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.format.datetime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import infra.context.support.EmbeddedValueResolutionSupport;
import infra.format.AnnotationFormatterFactory;
import infra.format.Formatter;
import infra.format.Parser;
import infra.format.Printer;
import infra.format.annotation.DateTimeFormat;
import infra.util.StringUtils;

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
