/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package infra.web.socket;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link WebSocketExtension}
 *
 * @author Brian Clozel
 */
public class WebSocketExtensionTests {

  @Test
  public void parseHeaderSingle() {
    List<WebSocketExtension> extensions =
            WebSocketExtension.parseExtensions("x-test-extension ; foo=bar ; bar=baz");

    assertThat(extensions).hasSize(1);
    WebSocketExtension extension = extensions.get(0);

    assertThat(extension.getName()).isEqualTo("x-test-extension");
    assertThat(extension.getParameters()).hasSize(2);
    assertThat(extension.getParameters().get("foo")).isEqualTo("bar");
    assertThat(extension.getParameters().get("bar")).isEqualTo("baz");
  }

  @Test
  public void parseHeaderMultiple() {
    List<WebSocketExtension> extensions =
            WebSocketExtension.parseExtensions("x-foo-extension, x-bar-extension");

    assertThat(extensions.stream().map(WebSocketExtension::getName))
            .containsExactly("x-foo-extension", "x-bar-extension");
  }

}
