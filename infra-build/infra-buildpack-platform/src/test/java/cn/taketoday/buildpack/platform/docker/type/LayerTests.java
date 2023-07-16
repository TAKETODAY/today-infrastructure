/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.docker.type;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import cn.taketoday.buildpack.platform.io.Content;
import cn.taketoday.buildpack.platform.io.IOConsumer;
import cn.taketoday.buildpack.platform.io.Layout;
import cn.taketoday.buildpack.platform.io.Owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Layer}.
 *
 * @author Phillip Webb
 */
class LayerTests {

  @Test
  void ofWhenLayoutIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Layer.of((IOConsumer<Layout>) null))
            .withMessage("Layout must not be null");
  }

  @Test
  void fromTarArchiveWhenTarArchiveIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Layer.fromTarArchive(null))
            .withMessage("TarArchive must not be null");
  }

  @Test
  void ofCreatesLayer() throws Exception {
    Layer layer = Layer.of((layout) -> {
      layout.directory("/directory", Owner.ROOT);
      layout.file("/directory/file", Owner.ROOT, Content.of("test"));
    });
    assertThat(layer.getId())
            .hasToString("sha256:d03a34f73804698c875eb56ff694fc2fceccc69b645e4adceb004ed13588613b");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    layer.writeTo(outputStream);
    try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
            new ByteArrayInputStream(outputStream.toByteArray()))) {
      assertThat(tarStream.getNextTarEntry().getName()).isEqualTo("/directory/");
      assertThat(tarStream.getNextTarEntry().getName()).isEqualTo("/directory/file");
      assertThat(tarStream.getNextTarEntry()).isNull();
    }
  }

}
