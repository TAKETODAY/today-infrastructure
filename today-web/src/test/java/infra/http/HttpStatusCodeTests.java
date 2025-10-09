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

package infra.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2025/10/6 11:22
 */
class HttpStatusCodeTests {

  @Test
  void valueOfWithValidCodeReturnsHttpStatus() {
    HttpStatusCode status = HttpStatusCode.valueOf(200);
    assertThat(status).isEqualTo(HttpStatus.OK);
    assertThat(status.value()).isEqualTo(200);
  }

  @Test
  void valueOfWithUnknownCodeReturnsSimpleHttpStatusCode() {
    HttpStatusCode status = HttpStatusCode.valueOf(299);
    assertThat(status).isInstanceOf(SimpleHttpStatusCode.class);
    assertThat(status.value()).isEqualTo(299);
  }

  @Test
  void valueOfWithInvalidCodeThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpStatusCode.valueOf(99))
            .withMessage("Code '99' should be a three-digit positive integer");

    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpStatusCode.valueOf(1000))
            .withMessage("Code '1000' should be a three-digit positive integer");

    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpStatusCode.valueOf(0))
            .withMessage("Code '0' should be a three-digit positive integer");
  }

  @Test
  void isSameCodeAs() {
    HttpStatusCode status200 = HttpStatusCode.valueOf(200);
    HttpStatusCode another200 = HttpStatusCode.valueOf(200);
    HttpStatusCode status201 = HttpStatusCode.valueOf(201);

    assertThat(status200.isSameCodeAs(another200)).isTrue();
    assertThat(status200.isSameCodeAs(status201)).isFalse();
  }

  @Test
  void is1xxInformational() {
    HttpStatusCode status100 = HttpStatusCode.valueOf(100);
    HttpStatusCode status101 = HttpStatusCode.valueOf(101);
    HttpStatusCode status200 = HttpStatusCode.valueOf(200);

    assertThat(status100.is1xxInformational()).isTrue();
    assertThat(status101.is1xxInformational()).isTrue();
    assertThat(status200.is1xxInformational()).isFalse();
  }

  @Test
  void is2xxSuccessful() {
    HttpStatusCode status200 = HttpStatusCode.valueOf(200);
    HttpStatusCode status201 = HttpStatusCode.valueOf(201);
    HttpStatusCode status300 = HttpStatusCode.valueOf(300);

    assertThat(status200.is2xxSuccessful()).isTrue();
    assertThat(status201.is2xxSuccessful()).isTrue();
    assertThat(status300.is2xxSuccessful()).isFalse();
  }

  @Test
  void is3xxRedirection() {
    HttpStatusCode status300 = HttpStatusCode.valueOf(300);
    HttpStatusCode status301 = HttpStatusCode.valueOf(301);
    HttpStatusCode status400 = HttpStatusCode.valueOf(400);

    assertThat(status300.is3xxRedirection()).isTrue();
    assertThat(status301.is3xxRedirection()).isTrue();
    assertThat(status400.is3xxRedirection()).isFalse();
  }

  @Test
  void is4xxClientError() {
    HttpStatusCode status400 = HttpStatusCode.valueOf(400);
    HttpStatusCode status404 = HttpStatusCode.valueOf(404);
    HttpStatusCode status500 = HttpStatusCode.valueOf(500);

    assertThat(status400.is4xxClientError()).isTrue();
    assertThat(status404.is4xxClientError()).isTrue();
    assertThat(status500.is4xxClientError()).isFalse();
  }

  @Test
  void is5xxServerError() {
    HttpStatusCode status500 = HttpStatusCode.valueOf(500);
    HttpStatusCode status503 = HttpStatusCode.valueOf(503);
    HttpStatusCode status400 = HttpStatusCode.valueOf(400);

    assertThat(status500.is5xxServerError()).isTrue();
    assertThat(status503.is5xxServerError()).isTrue();
    assertThat(status400.is5xxServerError()).isFalse();
  }

  @Test
  void isError() {
    HttpStatusCode status400 = HttpStatusCode.valueOf(400);
    HttpStatusCode status404 = HttpStatusCode.valueOf(404);
    HttpStatusCode status500 = HttpStatusCode.valueOf(500);
    HttpStatusCode status503 = HttpStatusCode.valueOf(503);
    HttpStatusCode status200 = HttpStatusCode.valueOf(200);

    assertThat(status400.isError()).isTrue();
    assertThat(status404.isError()).isTrue();
    assertThat(status500.isError()).isTrue();
    assertThat(status503.isError()).isTrue();
    assertThat(status200.isError()).isFalse();
  }
}
