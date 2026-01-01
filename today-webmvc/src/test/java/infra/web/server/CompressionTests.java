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