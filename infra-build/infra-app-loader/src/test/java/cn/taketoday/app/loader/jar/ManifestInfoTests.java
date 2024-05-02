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

package cn.taketoday.app.loader.jar;

import org.junit.jupiter.api.Test;

import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManifestInfo}.
 *
 * @author Phillip Webb
 */
class ManifestInfoTests {

  @Test
  void noneReturnsNoDetails() {
    assertThat(ManifestInfo.NONE.getManifest()).isNull();
    assertThat(ManifestInfo.NONE.isMultiRelease()).isFalse();
  }

  @Test
  void getManifestReturnsManifest() {
    Manifest manifest = new Manifest();
    ManifestInfo info = new ManifestInfo(manifest);
    assertThat(info.getManifest()).isSameAs(manifest);
  }

  @Test
  void isMultiReleaseWhenHasMultiReleaseAttributeReturnsTrue() {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(new Name("Multi-Release"), "true");
    ManifestInfo info = new ManifestInfo(manifest);
    assertThat(info.isMultiRelease()).isTrue();
  }

  @Test
  void isMultiReleaseWhenHasNoMultiReleaseAttributeReturnsFalse() {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(new Name("Random-Release"), "true");
    ManifestInfo info = new ManifestInfo(manifest);
    assertThat(info.isMultiRelease()).isFalse();
  }

}
