/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.jdbc.converters;

import java.util.Objects;
import java.util.UUID;

/**
 * @author aldenquimby@gmail.com
 * @since 4.0/14
 */
public class UUIDWrapper {
  private UUID text;

  public UUIDWrapper() { }

  public UUIDWrapper(UUID text) {
    this.text = text;
  }

  public UUID getText() {
    return text;
  }

  public void setText(UUID text) {
    this.text = text;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UUIDWrapper that = (UUIDWrapper) o;
    return Objects.equals(text, that.text);
  }

  @Override
  public int hashCode() {
    return text != null ? text.hashCode() : 0;
  }
}
