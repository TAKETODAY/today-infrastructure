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

package infra.web.multipart.support;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.beans.propertyeditors.ByteArrayPropertyEditor;
import infra.web.multipart.Part;

/**
 * Custom {@link java.beans.PropertyEditor} for converting
 * {@link Part Parts} to byte arrays.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 16:35
 */
public class ByteArrayMultipartEditor extends ByteArrayPropertyEditor {

  @Override
  public void setValue(@Nullable Object value) {
    if (value instanceof Part part) {
      try {
        super.setValue(part.getContentAsByteArray());
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

}
