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

import java.io.IOException;

import infra.beans.propertyeditors.ByteArrayPropertyEditor;
import infra.web.multipart.MultipartFile;

/**
 * Custom {@link java.beans.PropertyEditor} for converting
 * {@link MultipartFile MultipartFiles} to byte arrays.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 16:35
 */
public class ByteArrayMultipartFileEditor extends ByteArrayPropertyEditor {

  @Override
  public void setValue(@Nullable Object value) {
    if (value instanceof MultipartFile multipartFile) {
      try {
        super.setValue(multipartFile.getBytes());
      }
      catch (IOException ex) {
        throw new IllegalArgumentException("Cannot read contents of multipart file", ex);
      }
    }
    else if (value instanceof byte[]) {
      super.setValue(value);
    }
    else {
      super.setValue(value != null ? value.toString().getBytes() : null);
    }
  }

  @Override
  public String getAsText() {
    byte[] value = (byte[]) getValue();
    return (value != null ? new String(value) : "");
  }

}
