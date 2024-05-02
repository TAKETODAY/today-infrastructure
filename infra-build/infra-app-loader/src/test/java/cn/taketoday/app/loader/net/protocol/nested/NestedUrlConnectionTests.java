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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner.Cleanable;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.time.Instant;
import java.time.temporal.ChronoField;

import cn.taketoday.app.loader.net.protocol.Handlers;
import cn.taketoday.app.loader.ref.Cleaner;
import cn.taketoday.app.loader.testsupport.TestJar;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;
import cn.taketoday.app.loader.zip.ZipContent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NestedUrlConnection}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class NestedUrlConnectionTests {

  @TempDir
  File temp;

  private File jarFile;

  private URL url;

  @BeforeAll
  static void registerHandlers() {
    Handlers.register();
  }

  @BeforeEach
  void setup() throws Exception {
    this.jarFile = new File(this.temp, "test.jar");
    TestJar.create(this.jarFile);
    this.url = new URL("nested:" + this.jarFile.getAbsolutePath() + "/!nested.jar");
  }

  @Test
  void getContentLengthWhenContentLengthMoreThanMaxIntReturnsMinusOne() {
    NestedUrlConnection connection = mock(NestedUrlConnection.class);
    given(connection.getContentLength()).willCallRealMethod();
    given(connection.getContentLengthLong()).willReturn((long) Integer.MAX_VALUE + 1);
    assertThat(connection.getContentLength()).isEqualTo(-1);
  }

  @Test
  void getContentLengthGetsContentLength() throws Exception {
    NestedUrlConnection connection = new NestedUrlConnection(this.url);
    try (ZipContent zipContent = ZipContent.open(this.jarFile.toPath())) {
      int expectedSize = zipContent.getEntry("nested.jar").getUncompressedSize();
      assertThat(connection.getContentLength()).isEqualTo(expectedSize);
    }
  }

  @Test
  void getContentLengthLongReturnsContentLength() throws Exception {
    NestedUrlConnection connection = new NestedUrlConnection(this.url);
    try (ZipContent zipContent = ZipContent.open(this.jarFile.toPath())) {
      int expectedSize = zipContent.getEntry("nested.jar").getUncompressedSize();
      assertThat(connection.getContentLengthLong()).isEqualTo(expectedSize);
    }
  }

  @Test
  void getContentTypeReturnsJavaJar() throws Exception {
    NestedUrlConnection connection = new NestedUrlConnection(this.url);
    assertThat(connection.getContentType()).isEqualTo("x-java/jar");
  }

  @Test
  void getLastModifiedReturnsFileLastModified() throws Exception {
    NestedUrlConnection connection = new NestedUrlConnection(this.url);
    assertThat(connection.getLastModified()).isEqualTo(this.jarFile.lastModified());
  }

  @Test
  void getPermissionReturnsFilePermission() throws Exception {
    NestedUrlConnection connection = new NestedUrlConnection(this.url);
    Permission permission = connection.getPermission();
    assertThat(permission).isInstanceOf(FilePermission.class);
    assertThat(permission.getName()).isEqualTo(this.jarFile.getCanonicalPath());
  }

  @Test
  void getInputStreamReturnsContentOfNestedJar() throws Exception {
    NestedUrlConnection connection = new NestedUrlConnection(this.url);
    assertHasSameContentAsNestedJar(connection);
  }

  @Test
  void inputStreamCloseCleansResource() throws Exception {
    Cleaner cleaner = mock(Cleaner.class);
    Cleanable cleanable = mock(Cleanable.class);
    given(cleaner.register(any(), any())).willReturn(cleanable);
    NestedUrlConnection connection = new NestedUrlConnection(this.url, cleaner);
    connection.getInputStream().close();
    then(cleanable).should().clean();
    ArgumentCaptor<Runnable> actionCaptor = ArgumentCaptor.forClass(Runnable.class);
    then(cleaner).should().register(any(), actionCaptor.capture());
    actionCaptor.getValue().run();
  }

  @Test
    // gh-38204
  void getLastModifiedReturnsFileModifiedTime() throws Exception {
    NestedUrlConnection connection = new NestedUrlConnection(this.url);
    assertThat(connection.getLastModified()).isEqualTo(this.jarFile.lastModified());
  }

  @Test
    // gh-38204
  void getLastModifiedHeaderReturnsFileModifiedTime() throws IOException {
    NestedUrlConnection connection = new NestedUrlConnection(this.url);
    URLConnection fileConnection = this.jarFile.toURI().toURL().openConnection();
    try {
      assertThat(connection.getHeaderFieldDate("last-modified", 0))
              .isEqualTo(withoutNanos(this.jarFile.lastModified()))
              .isEqualTo(fileConnection.getHeaderFieldDate("last-modified", 0));
    }
    finally {
      fileConnection.getInputStream().close();
    }
  }

  @Test
  void createDecodesUrlPath() throws Exception {
    File withSpace = new File(this.temp, "te st");
    withSpace.mkdirs();
    this.jarFile = new File(withSpace, "test.jar");
    TestJar.create(this.jarFile);
    this.url = new URL("nested:" + this.jarFile.toURI().getRawPath() + "/!nested.jar");
    assertThat(this.url.toString()).contains("%20");
    NestedUrlConnection connection = new NestedUrlConnection(this.url);
    assertHasSameContentAsNestedJar(connection);
    assertThat(connection.getLastModified()).isEqualTo(this.jarFile.lastModified());
  }

  private void assertHasSameContentAsNestedJar(NestedUrlConnection connection) throws IOException {
    try (InputStream actual = connection.getInputStream()) {
      try (ZipContent zipContent = ZipContent.open(this.jarFile.toPath())) {
        try (InputStream expected = zipContent.getEntry("nested.jar").openContent().asInputStream()) {
          assertThat(actual).hasSameContentAs(expected);
        }
      }
    }
  }

  private long withoutNanos(long epochMilli) {
    return Instant.ofEpochMilli(epochMilli).with(ChronoField.NANO_OF_SECOND, 0).toEpochMilli();
  }

}
