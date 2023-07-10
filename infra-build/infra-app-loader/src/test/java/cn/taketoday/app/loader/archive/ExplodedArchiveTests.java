/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.archive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import cn.taketoday.app.loader.TestJarCreator;
import cn.taketoday.app.loader.archive.Archive.Entry;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExplodedArchive}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
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

  private void createArchive() throws Exception {
    createArchive(null);
  }

  private void createArchive(String directoryName) throws Exception {
    File file = new File(this.tempDir, "test.jar");
    TestJarCreator.createTestJar(file);
    this.rootDirectory = (StringUtils.hasText(directoryName) ? new File(this.tempDir, directoryName)
                                                             : new File(this.tempDir, UUID.randomUUID().toString()));
    JarFile jarFile = new JarFile(file);
    Enumeration<JarEntry> entries = jarFile.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      File destination = new File(this.rootDirectory.getAbsolutePath() + File.separator + entry.getName());
      destination.getParentFile().mkdirs();
      if (entry.isDirectory()) {
        destination.mkdir();
      }
      else {
        FileCopyUtils.copy(jarFile.getInputStream(entry), new FileOutputStream(destination));
      }
    }
    this.archive = new ExplodedArchive(this.rootDirectory);
    jarFile.close();
  }

  @Test
  void getManifest() throws Exception {
    assertThat(this.archive.getManifest().getMainAttributes().getValue("Built-By")).isEqualTo("j1");
  }

  @Test
  void getEntries() {
    Map<String, Archive.Entry> entries = getEntriesMap(this.archive);
    assertThat(entries).hasSize(12);
  }

  @Test
  void getUrl() throws Exception {
    assertThat(this.archive.getUrl()).isEqualTo(this.rootDirectory.toURI().toURL());
  }

  @Test
  void getUrlWithSpaceInPath() throws Exception {
    createArchive("spaces in the name");
    assertThat(this.archive.getUrl()).isEqualTo(this.rootDirectory.toURI().toURL());
  }

  @Test
  void getNestedArchive() throws Exception {
    Entry entry = getEntriesMap(this.archive).get("nested.jar");
    Archive nested = this.archive.getNestedArchive(entry);
    assertThat(nested.getUrl()).hasToString(this.rootDirectory.toURI() + "nested.jar");
    nested.close();
  }

  @Test
  void nestedDirArchive() throws Exception {
    Entry entry = getEntriesMap(this.archive).get("d/");
    Archive nested = this.archive.getNestedArchive(entry);
    Map<String, Entry> nestedEntries = getEntriesMap(nested);
    assertThat(nestedEntries).hasSize(1);
    assertThat(nested.getUrl()).hasToString("file:" + this.rootDirectory.toURI().getPath() + "d/");
  }

  @Test
  void getNonRecursiveEntriesForRoot() throws Exception {
    try (ExplodedArchive explodedArchive = new ExplodedArchive(new File("/"), false)) {
      Map<String, Archive.Entry> entries = getEntriesMap(explodedArchive);
      assertThat(entries).hasSizeGreaterThan(1);
    }
  }

  @Test
  void getNonRecursiveManifest() throws Exception {
    try (ExplodedArchive explodedArchive = new ExplodedArchive(new File("src/test/resources/root"))) {
      assertThat(explodedArchive.getManifest()).isNotNull();
      Map<String, Archive.Entry> entries = getEntriesMap(explodedArchive);
      assertThat(entries).hasSize(4);
    }
  }

  @Test
  void getNonRecursiveManifestEvenIfNonRecursive() throws Exception {
    try (ExplodedArchive explodedArchive = new ExplodedArchive(new File("src/test/resources/root"), false)) {
      assertThat(explodedArchive.getManifest()).isNotNull();
      Map<String, Archive.Entry> entries = getEntriesMap(explodedArchive);
      assertThat(entries).hasSize(3);
    }
  }

  @Test
  void getResourceAsStream() throws Exception {
    try (ExplodedArchive explodedArchive = new ExplodedArchive(new File("src/test/resources/root"))) {
      assertThat(explodedArchive.getManifest()).isNotNull();
      URLClassLoader loader = new URLClassLoader(new URL[] { explodedArchive.getUrl() });
      assertThat(loader.getResourceAsStream("META-INF/infra/application.xml")).isNotNull();
      loader.close();
    }
  }

  @Test
  void getResourceAsStreamNonRecursive() throws Exception {
    try (ExplodedArchive explodedArchive = new ExplodedArchive(new File("src/test/resources/root"), false)) {
      assertThat(explodedArchive.getManifest()).isNotNull();
      URLClassLoader loader = new URLClassLoader(new URL[] { explodedArchive.getUrl() });
      assertThat(loader.getResourceAsStream("META-INF/infra/application.xml")).isNotNull();
      loader.close();
    }
  }

  private Map<String, Archive.Entry> getEntriesMap(Archive archive) {
    Map<String, Archive.Entry> entries = new HashMap<>();
    for (Archive.Entry entry : archive) {
      entries.put(entry.getName(), entry);
    }
    return entries;
  }

}
