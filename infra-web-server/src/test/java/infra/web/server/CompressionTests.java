/*
 * Copyright 2012-present the original author or authors.
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

package infra.web.server;

import org.junit.jupiter.api.Test;

import infra.util.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 13:09
 */
class CompressionTests {

  @Test
  void defaultConstructorShouldSetDefaultValues() {
    Compression compression = new Compression();

    assertThat(compression.isEnabled()).isFalse();
    assertThat(compression.getMimeTypes()).containsExactly(
            "text/html", "text/xml", "text/plain", "text/css", "text/javascript",
            "application/javascript", "application/json", "application/xml");
    assertThat(compression.getExcludedUserAgents()).isNull();
    assertThat(compression.getMinResponseSize()).isEqualTo(DataSize.ofKilobytes(2));
  }

  @Test
  void setEnabledShouldUpdateEnabledFlag() {
    Compression compression = new Compression();

    compression.setEnabled(true);
    assertThat(compression.isEnabled()).isTrue();

    compression.setEnabled(false);
    assertThat(compression.isEnabled()).isFalse();
  }

  @Test
  void setMimeTypesShouldUpdateMimeTypes() {
    Compression compression = new Compression();
    String[] mimeTypes = { "text/html", "application/json" };

    compression.setMimeTypes(mimeTypes);

    assertThat(compression.getMimeTypes()).isEqualTo(mimeTypes);
  }

  @Test
  void setExcludedUserAgentsShouldUpdateExcludedUserAgents() {
    Compression compression = new Compression();
    String[] userAgents = { "Mozilla/5.0", "Chrome/90.0" };

    compression.setExcludedUserAgents(userAgents);

    assertThat(compression.getExcludedUserAgents()).isEqualTo(userAgents);
  }

  @Test
  void setExcludedUserAgentsWithNullShouldSetToNull() {
    Compression compression = new Compression();
    compression.setExcludedUserAgents(new String[] { "Mozilla/5.0" });

    compression.setExcludedUserAgents(null);

    assertThat(compression.getExcludedUserAgents()).isNull();
  }

  @Test
  void setMinResponseSizeShouldUpdateMinSize() {
    Compression compression = new Compression();
    DataSize minSize = DataSize.ofKilobytes(5);

    compression.setMinResponseSize(minSize);

    assertThat(compression.getMinResponseSize()).isEqualTo(minSize);
  }

  @Test
  void staticIsEnabledShouldReturnFalseWhenCompressionIsNull() {
    assertThat(Compression.isEnabled(null)).isFalse();
  }

  @Test
  void staticIsEnabledShouldReturnFalseWhenCompressionIsDisabled() {
    Compression compression = new Compression();
    compression.setEnabled(false);

    assertThat(Compression.isEnabled(compression)).isFalse();
  }

  @Test
  void staticIsEnabledShouldReturnTrueWhenCompressionIsEnabled() {
    Compression compression = new Compression();
    compression.setEnabled(true);

    assertThat(Compression.isEnabled(compression)).isTrue();
  }

}