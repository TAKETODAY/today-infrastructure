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

package cn.taketoday.app.loader.net.protocol.nested;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;

import cn.taketoday.app.loader.net.protocol.Handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link Handler}.
 *
 * @author Phillip Webb
 */
class HandlerTests {

  @TempDir
  File temp;

  @BeforeAll
  static void registerHandlers() {
    Handlers.register();
  }

  @Test
  void openConnectionReturnsNestedUrlConnection() throws Exception {
    URL url = new URL("nested:" + this.temp.getAbsolutePath() + "/!nested.jar");
    assertThat(url.openConnection()).isInstanceOf(NestedUrlConnection.class);
  }

  @Test
  void assertUrlIsNotMalformedWhenUrlIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Handler.assertUrlIsNotMalformed(null))
            .withMessageContaining("'url' must not be null");
  }

  @Test
  void assertUrlIsNotMalformedWhenUrlIsNotNestedThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Handler.assertUrlIsNotMalformed("file:"))
            .withMessageContaining("must use 'nested'");
  }

  @Test
  void assertUrlIsNotMalformedWhenUrlIsValidDoesNotThrowException() {
    String url = "nested:" + this.temp.getAbsolutePath() + "/!nested.jar";
    assertThatNoException().isThrownBy(() -> Handler.assertUrlIsNotMalformed(url));
  }

}
