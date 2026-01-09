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
