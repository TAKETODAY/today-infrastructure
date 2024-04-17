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

package cn.taketoday.framework.io;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/17 10:29
 */
class Base64ProtocolResolverTests {

  @Test
  void base64LocationResolves() throws IOException {
    String location = Base64.getEncoder().encodeToString("test value".getBytes());
    Resource resource = new Base64ProtocolResolver().resolve("base64:" + location, new DefaultResourceLoader());
    assertThat(resource).isNotNull();
    assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("test value");
  }

  @Test
  void base64LocationWithInvalidBase64ThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new Base64ProtocolResolver().resolve("base64:not valid base64", new DefaultResourceLoader()))
            .withMessageContaining("Illegal base64");
  }

  @Test
  void locationWithoutPrefixDoesNotResolve() {
    Resource resource = new Base64ProtocolResolver().resolve("file:notbase64.txt", new DefaultResourceLoader());
    assertThat(resource).isNull();
  }

}