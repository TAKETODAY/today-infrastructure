/*
 * Copyright 2002-2019 the original author or authors.
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

package cn.taketoday.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.UUID;

import cn.taketoday.util.StringUtils;

/**
 * Editor for {@code java.util.UUID}, translating UUID
 * String representations into UUID objects and back.
 *
 * @author Juergen Hoeller
 * @see UUID
 * @since 3.0.1
 */
public class UUIDEditor extends PropertyEditorSupport {

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (StringUtils.hasText(text)) {
      setValue(UUID.fromString(text.trim()));
    }
    else {
      setValue(null);
    }
  }

  @Override
  public String getAsText() {
    UUID value = (UUID) getValue();
    return (value != null ? value.toString() : "");
  }

}
