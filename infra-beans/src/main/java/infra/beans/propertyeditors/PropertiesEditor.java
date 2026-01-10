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

package infra.beans.propertyeditors;

import org.jspecify.annotations.Nullable;

import java.beans.PropertyEditorSupport;
import java.util.Map;
import java.util.Properties;

import infra.core.io.PropertiesUtils;

/**
 * Custom {@link java.beans.PropertyEditor} for {@link Properties} objects.
 *
 * <p>Handles conversion from content {@link String} to {@code Properties} object.
 * Also handles {@link Map} to {@code Properties} conversion, for populating
 * a {@code Properties} object via XML "map" entries.
 *
 * <p>The required format is defined in the standard {@code Properties}
 * documentation. Each property must be on a new line.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see Properties#load
 * @since 4.0
 */
public class PropertiesEditor extends PropertyEditorSupport {

  /**
   * Convert {@link String} into {@link Properties}, considering it as
   * properties content.
   *
   * @param text the text to be so converted
   */
  @Override
  public void setAsText(@Nullable String text) throws IllegalArgumentException {
    setValue(PropertiesUtils.parse(text));
  }

  /**
   * Take {@link Properties} as-is; convert {@link Map} into {@code Properties}.
   */
  @Override
  public void setValue(Object value) {
    if (!(value instanceof Properties) && value instanceof Map) {
      Properties props = new Properties();
      props.putAll((Map<?, ?>) value);
      super.setValue(props);
    }
    else {
      super.setValue(value);
    }
  }

}
