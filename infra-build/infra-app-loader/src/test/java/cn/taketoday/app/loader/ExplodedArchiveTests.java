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

package cn.taketoday.app.loader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import cn.taketoday.app.loader.Archive.Entry;
import cn.taketoday.app.loader.testsupport.TestJar;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExplodedArchive}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@AssertFileChannelDataBlocksClosed
class ExplodedArchiveTests {

  @TempDir
  File tempDir;

  private File rootDirectory;

  private ExplodedArchive archive;

  @BeforeEach
  void setup() throws Exception {
    createArchive();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (this.archive != null) {
      this.archive.close();
    }
  }

  @Test
  void isExplodedReturnsTrue() {
    assertThat(this.archive.isExploded()).isTrue();
  }

  @Test
  void getRootDirectoryReturnsRootDirectory() {
    assertThat(this.archive.getRootDirectory()).isEqualTo(this.rootDirectory);
  }

  @Test
  void getManifestReturnsManifest() throws Exception {
    assertThat(this.archive.getManifest().getMainAttributes().getValue("Built-By")).isEqualTo("j1");
  }

  @Test
  void getClassPathUrlsWhenNoPredicatesReturnsUrls() throws Exception {
    Set<URL> urls = this.archive.getClassPathUrls(Archive.ALL_ENTRIES);
    URL[] expectedUrls = TestJar.expectedEntries().stream().map(this::toUrl).toArray(URL[]::new);
    assertThat(urls).containsExactlyInAnyOrder(expectedUrls);
  }

  @Test
  void getClassPathUrlsWhenHasIncludeFilterReturnsUrls() throws Exception {
    Set<URL> urls = this.archive.getClassPathUrls(this::entryNameIsNestedJar);
    assertThat(urls).containsOnly(toUrl("nested.jar"));
  }

  @Test
  void getClassPathUrlsWhenHasIncludeFilterAndSpaceInRootNameReturnsUrls() throws Exception {
    createArchive("spaces in the name");
    Set<URL> urls = this.archive.getClassPathUrls(this::entryNameIsNestedJar);
    assertThat(urls).containsOnly(toUrl("nested.jar"));
  }

  @Test
  void getClassPathUrlsWhenHasSearchFilterReturnsUrls() throws Exception {
    Set<URL> urls = this.archive.getClassPathUrls(Archive.ALL_ENTRIES, (entry) -> !entry.name().equals("d/"));
    assertThat(urls).contains(toUrl("nested.jar")).doesNotContain(toUrl("d/9.dat"));
  }

  private void createArchive() throws Exception {
    createArchive(null);
  }

  private void createArchive(String directoryName) throws Exception {
    File file = new File(this.tempDir, "test.jar");
    TestJar.create(file);
    this.rootDirectory = (StringUtils.hasText(directoryName) ? new File(this.tempDir, directoryName)
            : new File(this.tempDir, UUID.randomUUID().toString()));
    try (JarFile jarFile = new JarFile(file)) {
      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        File destination = new File(this.rootDirectory, entry.getName());
        destination.getParentFile().mkdirs();
        if (entry.isDirectory()) {
          destination.mkdir();
        }
        else {
          try (InputStream in = jarFile.getInputStream(entry);
                  OutputStream out = new FileOutputStream(destination)) {
            in.transferTo(out);
          }
        }
      }
      this.archive = new ExplodedArchive(this.rootDirectory);
    }
  }

  private URL toUrl(String name) {
    return toUrl(new File(this.rootDirectory, name));
  }

  private URL toUrl(File file) {
    try {
      return file.toURI().toURL();
    }
    catch (MalformedURLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private boolean entryNameIsNestedJar(Entry entry) {
    return entry.name().equals("nested.jar");
  }

}
