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

package infra.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 11:22
 */
class SimpleHttpStatusCodeTests {

  @Test
  void value() {
    SimpleHttpStatusCode status = new SimpleHttpStatusCode(200);
    assertThat(status.value()).isEqualTo(200);
  }

  @Test
  void is1xxInformational() {
    SimpleHttpStatusCode status = new SimpleHttpStatusCode(101);
    assertThat(status.is1xxInformational()).isTrue();
    assertThat(status.is2xxSuccessful()).isFalse();
    assertThat(status.is3xxRedirection()).isFalse();
    assertThat(status.is4xxClientError()).isFalse();
    assertThat(status.is5xxServerError()).isFalse();
    assertThat(status.isError()).isFalse();
  }

  @Test
  void is2xxSuccessful() {
    SimpleHttpStatusCode status = new SimpleHttpStatusCode(201);
    assertThat(status.is1xxInformational()).isFalse();
    assertThat(status.is2xxSuccessful()).isTrue();
    assertThat(status.is3xxRedirection()).isFalse();
    assertThat(status.is4xxClientError()).isFalse();
    assertThat(status.is5xxServerError()).isFalse();
    assertThat(status.isError()).isFalse();
  }

  @Test
  void is3xxRedirection() {
    SimpleHttpStatusCode status = new SimpleHttpStatusCode(301);
    assertThat(status.is1xxInformational()).isFalse();
    assertThat(status.is2xxSuccessful()).isFalse();
    assertThat(status.is3xxRedirection()).isTrue();
    assertThat(status.is4xxClientError()).isFalse();
    assertThat(status.is5xxServerError()).isFalse();
    assertThat(status.isError()).isFalse();
  }

  @Test
  void is4xxClientError() {
    SimpleHttpStatusCode status = new SimpleHttpStatusCode(404);
    assertThat(status.is1xxInformational()).isFalse();
    assertThat(status.is2xxSuccessful()).isFalse();
    assertThat(status.is3xxRedirection()).isFalse();
    assertThat(status.is4xxClientError()).isTrue();
    assertThat(status.is5xxServerError()).isFalse();
    assertThat(status.isError()).isTrue();
  }

  @Test
  void is5xxServerError() {
    SimpleHttpStatusCode status = new SimpleHttpStatusCode(500);
    assertThat(status.is1xxInformational()).isFalse();
    assertThat(status.is2xxSuccessful()).isFalse();
    assertThat(status.is3xxRedirection()).isFalse();
    assertThat(status.is4xxClientError()).isFalse();
    assertThat(status.is5xxServerError()).isTrue();
    assertThat(status.isError()).isTrue();
  }

  @Test
  void compareTo() {
    SimpleHttpStatusCode status200 = new SimpleHttpStatusCode(200);
    SimpleHttpStatusCode status404 = new SimpleHttpStatusCode(404);
    assertThat(status200.compareTo(status404)).isNegative();
    assertThat(status404.compareTo(status200)).isPositive();
    assertThat(status200.compareTo(new SimpleHttpStatusCode(200))).isZero();
  }

  @Test
  void equals() {
    SimpleHttpStatusCode status200 = new SimpleHttpStatusCode(200);
    SimpleHttpStatusCode another200 = new SimpleHttpStatusCode(200);
    SimpleHttpStatusCode status404 = new SimpleHttpStatusCode(404);
    assertThat(status200).isEqualTo(another200);
    assertThat(status200).isNotEqualTo(status404);
    assertThat(status200).isNotEqualTo(null);
    assertThat(status200).isNotEqualTo("200");
  }

  @Test
  void hashCodeTest() {
    SimpleHttpStatusCode status200 = new SimpleHttpStatusCode(200);
    SimpleHttpStatusCode another200 = new SimpleHttpStatusCode(200);
    assertThat(status200.hashCode()).isEqualTo(another200.hashCode());
  }

  @Test
  void toStringTest() {
    SimpleHttpStatusCode status = new SimpleHttpStatusCode(418);
    assertThat(status.toString()).isEqualTo("418");
  }
}
