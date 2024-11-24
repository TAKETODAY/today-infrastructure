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

package infra.app.loader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import infra.app.loader.net.protocol.jar.JarUrl;
import infra.app.loader.testsupport.TestJar;
import infra.app.loader.zip.AssertFileChannelDataBlocksClosed;
import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarFileArchive}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Camille Vienot
 */
@AssertFileChannelDataBlocksClosed
class JarFileArchiveTests {

  @TempDir
  File tempDir;

  private File file;

  private JarFileArchive archive;

  @BeforeEach
  void setup() throws Exception {
    createTestJarArchive(false);
  }

  @AfterEach
  void tearDown() throws Exception {
    this.archive.close();
  }

  @Test
  void isExplodedReturnsFalse() {
    assertThat(this.archive.isExploded()).isFalse();
  }

  @Test
  void getRootDirectoryReturnsNull() {
    assertThat(this.archive.getRootDirectory()).isNull();
  }

  @Test
  void getManifestReturnsManifest() throws Exception {
    assertThat(this.archive.getManifest().getMainAttributes().getValue("Built-By")).isEqualTo("j1");
  }

  @Test
  void getClassPathUrlsWhenNoPredicatesReturnsUrls() throws Exception {
    Set<URL> urls = this.archive.getClassPathUrls(Archive.ALL_ENTRIES);
    URL[] expected = TestJar.expectedEntries()
            .stream()
            .map((name) -> JarUrl.create(this.file, name))
            .toArray(URL[]::new);
    assertThat(urls).containsExactly(expected);
  }

  @Test
  void getClassPathUrlsWhenHasIncludeFilterReturnsUrls() throws Exception {
    Set<URL> urls = this.archive.getClassPathUrls(this::entryNameIsNestedJar);
    assertThat(urls).containsOnly(JarUrl.create(this.file, "nested.jar"));
  }

  @Test
  void getClassPathUrlsWhenHasSearchFilterAllUrlsSinceSearchFilterIsNotUsed() throws Exception {
    Set<URL> urls = this.archive.getClassPathUrls(Archive.ALL_ENTRIES, (entry) -> false);
    URL[] expected = TestJar.expectedEntries()
            .stream()
            .map((name) -> JarUrl.create(this.file, name))
            .toArray(URL[]::new);
    assertThat(urls).containsExactly(expected);
  }

  @Test
  void getClassPathUrlsWhenHasUnpackCommentUnpacksAndReturnsUrls() throws Exception {
    createTestJarArchive(true);
    Set<URL> urls = this.archive.getClassPathUrls(this::entryNameIsNestedJar);
    assertThat(urls).hasSize(1);
    URL url = urls.iterator().next();
    assertThat(url).isNotEqualTo(JarUrl.create(this.file, "nested.jar"));
    // The unpack URL must be a raw file URL (see gh-38833)
    assertThat(url.toString()).startsWith("file:").endsWith("/nested.jar");
  }

  @Test
  void getClassPathUrlsWhenHasUnpackCommentUnpacksToUniqueLocationsPerArchive() throws Exception {
    createTestJarArchive(true);
    URL firstNestedUrl = this.archive.getClassPathUrls(this::entryNameIsNestedJar).iterator().next();
    createTestJarArchive(true);
    URL secondNestedUrl = this.archive.getClassPathUrls(this::entryNameIsNestedJar).iterator().next();
    assertThat(secondNestedUrl).isNotEqualTo(firstNestedUrl);
  }

  @Test
  void getClassPathUrlsWhenHasUnpackCommentUnpacksAndShareSameParent() throws Exception {
    createTestJarArchive(true);
    URL nestedUrl = this.archive.getClassPathUrls(this::entryNameIsNestedJar).iterator().next();
    URL anotherNestedUrl = this.archive.getClassPathUrls((entry) -> entry.name().equals("another-nested.jar"))
            .iterator()
            .next();
    assertThat(nestedUrl.toString())
            .isEqualTo(anotherNestedUrl.toString().replace("another-nested.jar", "nested.jar"));
  }

  @Test
  void getClassPathUrlsWhenZip64ListsAllEntries() throws Exception {
    File file = new File(this.tempDir, "test.jar");
    FileCopyUtils.copy(writeZip64Jar(), file);
    try (Archive jarArchive = new JarFileArchive(file)) {
      Set<URL> urls = jarArchive.getClassPathUrls(Archive.ALL_ENTRIES);
      assertThat(urls).hasSize(65537);
    }
  }

  private byte[] writeZip64Jar() throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (JarOutputStream jarOutput = new JarOutputStream(bytes)) {
      for (int i = 0; i < 65537; i++) {
        jarOutput.putNextEntry(new JarEntry(i + ".dat"));
        jarOutput.closeEntry();
      }
    }
    return bytes.toByteArray();
  }

  private void createTestJarArchive(boolean unpackNested) throws Exception {
    if (this.archive != null) {
      this.archive.close();
    }
    this.file = new File(this.tempDir, "root.jar");
    TestJar.create(this.file, unpackNested);
    this.archive = new JarFileArchive(this.file);
  }

  private boolean entryNameIsNestedJar(Archive.Entry entry) {
    return entry.name().equals("nested.jar");
  }

}
