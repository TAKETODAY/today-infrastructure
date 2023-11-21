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

import org.junit.jupiter.api.Test;

import cn.taketoday.buildpack.platform.io.TarArchive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ContainerContent}.
 *
 * @author Phillip Webb
 */
class ContainerContentTests {

  @Test
  void ofWhenArchiveIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> ContainerContent.of(null))
            .withMessage("Archive is required");
  }

  @Test
  void ofWhenDestinationPathIsNullThrowsException() {
    TarArchive archive = mock(TarArchive.class);
    assertThatIllegalArgumentException().isThrownBy(() -> ContainerContent.of(archive, null))
            .withMessage("DestinationPath must not be empty");
  }

  @Test
  void ofWhenDestinationPathIsEmptyThrowsException() {
    TarArchive archive = mock(TarArchive.class);
    assertThatIllegalArgumentException().isThrownBy(() -> ContainerContent.of(archive, ""))
            .withMessage("DestinationPath must not be empty");
  }

  @Test
  void ofCreatesContainerContent() {
    TarArchive archive = mock(TarArchive.class);
    ContainerContent content = ContainerContent.of(archive);
    assertThat(content.getArchive()).isSameAs(archive);
    assertThat(content.getDestinationPath()).isEqualTo("/");
  }

  @Test
  void ofWithDestinationPathCreatesContainerContent() {
    TarArchive archive = mock(TarArchive.class);
    ContainerContent content = ContainerContent.of(archive, "/test");
    assertThat(content.getArchive()).isSameAs(archive);
    assertThat(content.getDestinationPath()).isEqualTo("/test");
  }

}
