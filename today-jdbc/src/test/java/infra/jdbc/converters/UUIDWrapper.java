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
