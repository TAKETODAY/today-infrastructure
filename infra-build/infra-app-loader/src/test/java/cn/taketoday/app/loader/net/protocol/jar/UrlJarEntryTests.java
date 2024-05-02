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

package cn.taketoday.app.loader.net.protocol.jar;

import org.junit.jupiter.api.Test;

import java.util.jar.Attributes;
import java.util.jar.JarEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UrlJarEntry}.
 *
 * @author Phillip Webb
 */
class UrlJarEntryTests {

  @Test
  void ofWhenEntryIsNullReturnsNull() {
    assertThat(UrlJarEntry.of(null, null)).isNull();
  }

  @Test
  void ofReturnsUrlJarEntry() {
    JarEntry entry = new JarEntry("test");
    assertThat(UrlJarEntry.of(entry, null)).isNotNull();

  }

  @Test
  void getAttributesDelegatesToUrlJarManifest() throws Exception {
    JarEntry entry = new JarEntry("test");
    UrlJarManifest manifest = mock(UrlJarManifest.class);
    Attributes attributes = mock(Attributes.class);
    given(manifest.getEntryAttributes(any())).willReturn(attributes);
    assertThat(UrlJarEntry.of(entry, manifest).getAttributes()).isSameAs(attributes);
  }

}
