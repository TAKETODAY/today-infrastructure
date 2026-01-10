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

package infra.web.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 13:06
 */
class Http2Tests {

  @Test
  void defaultConstructorShouldSetEnabledToFalse() {
    Http2 http2 = new Http2();

    assertThat(http2.isEnabled()).isFalse();
  }

  @Test
  void setEnabledShouldUpdateEnabledFlag() {
    Http2 http2 = new Http2();

    http2.setEnabled(true);
    assertThat(http2.isEnabled()).isTrue();

    http2.setEnabled(false);
    assertThat(http2.isEnabled()).isFalse();
  }

  @Test
  void staticIsEnabledShouldReturnFalseWhenHttp2IsNull() {
    assertThat(Http2.isEnabled(null)).isFalse();
  }

  @Test
  void staticIsEnabledShouldReturnFalseWhenHttp2IsDisabled() {
    Http2 http2 = new Http2();
    http2.setEnabled(false);

    assertThat(Http2.isEnabled(http2)).isFalse();
  }

  @Test
  void staticIsEnabledShouldReturnTrueWhenHttp2IsEnabled() {
    Http2 http2 = new Http2();
    http2.setEnabled(true);

    assertThat(Http2.isEnabled(http2)).isTrue();
  }

}