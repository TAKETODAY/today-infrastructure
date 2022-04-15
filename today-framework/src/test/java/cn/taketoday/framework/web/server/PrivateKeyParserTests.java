/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.server;

import org.junit.jupiter.api.Test;

import java.security.PrivateKey;

import cn.taketoday.framework.web.server.PrivateKeyParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PrivateKeyParser}.
 *
 * @author Scott Frederick
 */
class PrivateKeyParserTests {

  @Test
  void parsePkcs8KeyFile() {
    PrivateKey privateKey = PrivateKeyParser.parse("classpath:test-key.pem");
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
  }

  @Test
  void parseWithNonKeyFileWillThrowException() {
    String path = "classpath:test-banner.txt";
    assertThatIllegalStateException().isThrownBy(() -> PrivateKeyParser.parse("file://" + path))
            .withMessageContaining(path);
  }

  @Test
  void parseWithInvalidPathWillThrowException() {
    String path = "file:///bad/path/key.pem";
    assertThatIllegalStateException().isThrownBy(() -> PrivateKeyParser.parse(path)).withMessageContaining(path);
  }

}
