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

package infra.http;

import java.io.Serial;
import java.io.Serializable;

/**
 * Simple implementation of {@link HttpStatusCode}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
final class SimpleHttpStatusCode implements HttpStatusCode, Comparable<HttpStatusCode>, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final int value;

  public SimpleHttpStatusCode(int value) {
    this.value = value;
  }

  @Override
  public int value() {
    return this.value;
  }

  @Override
  public boolean is1xxInformational() {
    return hundreds() == 1;
  }

  @Override
  public boolean is2xxSuccessful() {
    return hundreds() == 2;
  }

  @Override
  public boolean is3xxRedirection() {
    return hundreds() == 3;
  }

  @Override
  public boolean is4xxClientError() {
    return hundreds() == 4;
  }

  @Override
  public boolean is5xxServerError() {
    return hundreds() == 5;
  }

  @Override
  public boolean isError() {
    int hundreds = hundreds();
    return hundreds == 4 || hundreds == 5;
  }

  private int hundreds() {
    return this.value / 100;
  }

  @Override
  public int compareTo(HttpStatusCode o) {
    return Integer.compare(this.value, o.value());
  }

  @Override
  public int hashCode() {
    return this.value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof HttpStatusCode other) {
      return this.value == other.value();
    }
    else {
      return false;
    }
  }

  @Override
  public String toString() {
    return Integer.toString(this.value);
  }
}
