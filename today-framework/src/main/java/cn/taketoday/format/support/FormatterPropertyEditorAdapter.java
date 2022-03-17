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

package cn.taketoday.format.support;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;

import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.format.Formatter;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Adapter that bridges between {@link Formatter} and {@link PropertyEditor}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 * @since 4.0
 */
public class FormatterPropertyEditorAdapter extends PropertyEditorSupport {

  private final Formatter<Object> formatter;

  /**
   * Create a new {@code FormatterPropertyEditorAdapter} for the given {@link Formatter}.
   *
   * @param formatter the {@link Formatter} to wrap
   */
  @SuppressWarnings("unchecked")
  public FormatterPropertyEditorAdapter(Formatter<?> formatter) {
    Assert.notNull(formatter, "Formatter must not be null");
    this.formatter = (Formatter<Object>) formatter;
  }

  /**
   * Determine the {@link Formatter}-declared field type.
   *
   * @return the field type declared in the wrapped {@link Formatter} implementation
   * (never {@code null})
   * @throws IllegalArgumentException if the {@link Formatter}-declared field type
   * cannot be inferred
   */
  public Class<?> getFieldType() {
    return FormattingConversionService.getFieldType(this.formatter);
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (StringUtils.hasText(text)) {
      try {
        setValue(this.formatter.parse(text, LocaleContextHolder.getLocale()));
      }
      catch (IllegalArgumentException ex) {
        throw ex;
      }
      catch (Throwable ex) {
        throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
      }
    }
    else {
      setValue(null);
    }
  }

  @Override
  public String getAsText() {
    Object value = getValue();
    return (value != null ? this.formatter.print(value, LocaleContextHolder.getLocale()) : "");
  }

}
