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

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

import infra.lang.Constant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 13:08
 */
class EncodingPropertiesTests {

  @Test
  void defaultConstructorShouldSetDefaultCharset() {
    EncodingProperties properties = new EncodingProperties();

    assertThat(properties.getCharset()).isEqualTo(Constant.DEFAULT_CHARSET);
  }

  @Test
  void setCharsetShouldUpdateCharset() {
    EncodingProperties properties = new EncodingProperties();
    Charset charset = Charset.forName("UTF-8");

    properties.setCharset(charset);

    assertThat(properties.getCharset()).isEqualTo(charset);
  }

  @Test
  void getMappingShouldReturnNullByDefault() {
    EncodingProperties properties = new EncodingProperties();

    assertThat(properties.getMapping()).isNull();
  }

  @Test
  void setMappingShouldUpdateMapping() {
    EncodingProperties properties = new EncodingProperties();
    Map<Locale, Charset> mapping = Map.of(Locale.US, Charset.forName("UTF-8"));

    properties.setMapping(mapping);

    assertThat(properties.getMapping()).isEqualTo(mapping);
  }

  @Test
  void setMappingWithNullShouldSetMappingToNull() {
    EncodingProperties properties = new EncodingProperties();
    properties.setMapping(Map.of(Locale.US, Charset.forName("UTF-8")));

    properties.setMapping(null);

    assertThat(properties.getMapping()).isNull();
  }

}