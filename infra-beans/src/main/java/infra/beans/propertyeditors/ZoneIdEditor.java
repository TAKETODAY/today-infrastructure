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

import java.beans.PropertyEditorSupport;
import java.time.DateTimeException;
import java.time.ZoneId;

import infra.util.StringUtils;

/**
 * Editor for {@code java.time.ZoneId}, translating zone ID Strings into {@code ZoneId}
 * objects. Exposes the {@code TimeZone} ID as a text representation.
 *
 * @author Nicholas Williams
 * @see ZoneId
 * @see TimeZoneEditor
 * @since 4.0
 */
public class ZoneIdEditor extends PropertyEditorSupport {

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (StringUtils.hasText(text)) {
      text = text.trim();
    }
    try {
      setValue(ZoneId.of(text));
    }
    catch (DateTimeException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }

  @Override
  public String getAsText() {
    ZoneId value = (ZoneId) getValue();
    return (value != null ? value.getId() : "");
  }

}
