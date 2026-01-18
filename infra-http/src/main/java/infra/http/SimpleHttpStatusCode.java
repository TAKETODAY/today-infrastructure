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
