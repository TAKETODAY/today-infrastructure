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

package cn.taketoday.format.number;

import java.util.Set;

import cn.taketoday.context.support.EmbeddedValueResolutionSupport;
import cn.taketoday.format.AnnotationFormatterFactory;
import cn.taketoday.format.Formatter;
import cn.taketoday.format.Parser;
import cn.taketoday.format.Printer;
import cn.taketoday.format.annotation.NumberFormat;
import cn.taketoday.format.annotation.NumberFormat.Style;
import cn.taketoday.util.NumberUtils;
import cn.taketoday.util.StringUtils;

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
