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

package infra.web.multipart.support;

import org.jspecify.annotations.Nullable;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.nio.charset.Charset;

import infra.web.multipart.Part;

/**
 * Custom {@link java.beans.PropertyEditor} for converting
 * {@link Part Parts} to Strings.
 *
 * <p>Allows one to specify the charset to use.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 16:37
 */
public class StringPartEditor extends PropertyEditorSupport {

  private final @Nullable Charset charset;

  /**
   * Create a new {@link StringPartEditor}, using the default charset.
   */
  public StringPartEditor() {
    this.charset = null;
  }

  /**
   * Create a new {@link StringPartEditor}, using the given charset.
   *
   * @param charset charset
   * @see java.lang.String#String(byte[], Charset)
   */
  public StringPartEditor(Charset charset) {
    this.charset = charset;
  }

  @Override
  public void setAsText(String text) {
    setValue(text);
  }

  @Override
  public void setValue(Object value) {
    if (value instanceof Part part) {
      try {
        super.setValue(this.charset != null ? part.getContentAsString(this.charset) : part.getContentAsString());
      }
      catch (IOException ex) {
        throw new IllegalArgumentException("Cannot read contents of multipart file", ex);
      }
    }
    else {
      super.setValue(value);
    }
  }

}

