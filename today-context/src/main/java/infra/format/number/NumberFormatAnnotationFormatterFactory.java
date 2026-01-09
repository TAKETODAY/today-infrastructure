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

package infra.format.number;

import java.util.Set;

import infra.context.support.EmbeddedValueResolutionSupport;
import infra.format.AnnotationFormatterFactory;
import infra.format.Formatter;
import infra.format.Parser;
import infra.format.Printer;
import infra.format.annotation.NumberFormat;
import infra.format.annotation.NumberFormat.Style;
import infra.util.NumberUtils;
import infra.util.StringUtils;

/**
 * Formats fields annotated with the {@link NumberFormat} annotation.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NumberFormat
 * @since 4.0
 */
public class NumberFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
        implements AnnotationFormatterFactory<NumberFormat> {

  @Override
  public Set<Class<?>> getFieldTypes() {
    return NumberUtils.STANDARD_NUMBER_TYPES;
  }

  @Override
  public Printer<Number> getPrinter(NumberFormat annotation, Class<?> fieldType) {
    return configureFormatterFrom(annotation);
  }

  @Override
  public Parser<Number> getParser(NumberFormat annotation, Class<?> fieldType) {
    return configureFormatterFrom(annotation);
  }

  private Formatter<Number> configureFormatterFrom(NumberFormat annotation) {
    String pattern = resolveEmbeddedValue(annotation.pattern());
    if (StringUtils.isNotEmpty(pattern)) {
      return new NumberStyleFormatter(pattern);
    }
    else {
      Style style = annotation.style();
      if (style == Style.CURRENCY) {
        return new CurrencyStyleFormatter();
      }
      else if (style == Style.PERCENT) {
        return new PercentStyleFormatter();
      }
      else {
        return new NumberStyleFormatter();
      }
    }
  }

}
