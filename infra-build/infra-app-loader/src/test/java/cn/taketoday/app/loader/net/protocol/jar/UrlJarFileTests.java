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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import cn.taketoday.app.loader.testsupport.TestJar;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link UrlJarFile}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class UrlJarFileTests {

  @TempDir
  File temp;

  private UrlJarFile jarFile;

  @Mock
  private Consumer<JarFile> closeAction;

  @BeforeEach
  void setup() throws Exception {
    MockitoAnnotations.openMocks(this);
    File file = new File(this.temp, "test.jar");
    TestJar.create(file);
    this.jarFile = new UrlJarFile(file, Runtime.version(), this.closeAction);
  }

  @AfterEach
  void cleanup() throws Exception {
    this.jarFile.close();
  }

  @Test
  void getEntryWhenNotfoundReturnsNull() {
    assertThat(this.jarFile.getEntry("missing")).isNull();
  }

  @Test
  void getEntryWhenFoundReturnsUrlJarEntry() {
    assertThat(this.jarFile.getEntry("1.dat")).isInstanceOf(UrlJarEntry.class);
  }

  @Test
  void getManifestReturnsNewCopy() throws Exception {
    Manifest manifest1 = this.jarFile.getManifest();
    Manifest manifest2 = this.jarFile.getManifest();
    assertThat(manifest1).isNotSameAs(manifest2);
  }

  @Test
  void closeCallsCloseAction() throws Exception {
    this.jarFile.close();
    then(this.closeAction).should().accept(this.jarFile);
  }

}
