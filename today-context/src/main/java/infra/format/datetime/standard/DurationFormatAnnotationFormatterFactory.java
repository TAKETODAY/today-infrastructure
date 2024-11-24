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

package infra.format.datetime.standard;

import java.time.Duration;
import java.util.Set;

import infra.context.support.EmbeddedValueResolutionSupport;
import infra.format.AnnotationFormatterFactory;
import infra.format.Parser;
import infra.format.Printer;
import infra.format.annotation.DurationFormat;

/**
 * Formats fields annotated with the {@link DurationFormat} annotation using the
 * selected style for parsing and printing JSR-310 {@code Duration}.
 *
 * @author Simon Baslé
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see DurationFormat
 * @see DurationFormatter
 * @since 5.0
 */
public class DurationFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
        implements AnnotationFormatterFactory<DurationFormat> {

  // Create the set of field types that may be annotated with @DurationFormat.
  private static final Set<Class<?>> FIELD_TYPES = Set.of(Duration.class);

  @Override
  public final Set<Class<?>> getFieldTypes() {
    return FIELD_TYPES;
  }

  @Override
  public Printer<?> getPrinter(DurationFormat annotation, Class<?> fieldType) {
    return new DurationFormatter(annotation.style(), annotation.defaultUnit());
  }

  @Override
  public Parser<?> getParser(DurationFormat annotation, Class<?> fieldType) {
    return new DurationFormatter(annotation.style(), annotation.defaultUnit());
  }
}
