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

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import infra.format.Printer;

/**
 * {@link Printer} implementation for a JSR-310 {@link TemporalAccessor},
 * using a {@link DateTimeFormatter}) (the contextual one, if available).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DateTimeContextHolder#getFormatter
 * @see DateTimeFormatter#format(TemporalAccessor)
 * @since 4.0
 */
public final class TemporalAccessorPrinter implements Printer<TemporalAccessor> {

  private final DateTimeFormatter formatter;

  /**
   * Create a new TemporalAccessorPrinter.
   *
   * @param formatter the base DateTimeFormatter instance
   */
  public TemporalAccessorPrinter(DateTimeFormatter formatter) {
    this.formatter = formatter;
  }

  @Override
  public String print(TemporalAccessor partial, Locale locale) {
    return DateTimeContextHolder.getFormatter(this.formatter, locale).format(partial);
  }

}
