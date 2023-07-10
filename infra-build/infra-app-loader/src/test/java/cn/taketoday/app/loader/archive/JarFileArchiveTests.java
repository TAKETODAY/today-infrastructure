/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.app.loader.archive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import cn.taketoday.app.loader.TestJarCreator;
import cn.taketoday.app.loader.archive.Archive.Entry;
import cn.taketoday.app.loader.jar.JarFile;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarFileArchive}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Camille Vienot
 */
class JarFileArchiveTests {

  @TempDir
  File tempDir;

  private File rootJarFile;

  private JarFileArchive archive;

  private String rootJarFileUrl;

  @BeforeEach
  void setup() throws Exception {
    setup(false);
  }

  @AfterEach
  void tearDown() throws Exception {
    this.archive.close();
  }

  private void setup(boolean unpackNested) throws Exception {
    this.rootJarFile = new File(this.tempDir, "root.jar");
    this.rootJarFileUrl = this.rootJarFile.toURI().toString();
    TestJarCreator.createTestJar(this.rootJarFile, unpackNested);
    if (this.archive != null) {
      this.archive.close();
    }
    this.archive = new JarFileArchive(this.rootJarFile);
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
    URL url = this.archive.getUrl();
    assertThat(url).hasToString(this.rootJarFileUrl);
  }

  @Test
  void getNestedArchive() throws Exception {
    Entry entry = getEntriesMap(this.archive).get("nested.jar");
    try (Archive nested = this.archive.getNestedArchive(entry)) {
      assertThat(nested.getUrl()).hasToString("jar:" + this.rootJarFileUrl + "!/nested.jar!/");
    }
  }

  @Test
  void getNestedUnpackedArchive() throws Exception {
    setup(true);
    Entry entry = getEntriesMap(this.archive).get("nested.jar");
    try (Archive nested = this.archive.getNestedArchive(entry)) {
      assertThat(nested.getUrl().toString()).startsWith("file:");
      assertThat(nested.getUrl().toString()).endsWith("/nested.jar");
    }
  }

  @Test
  void unpackedLocationsAreUniquePerArchive() throws Exception {
    setup(true);
    Entry entry = getEntriesMap(this.archive).get("nested.jar");
    URL firstNestedUrl;
    try (Archive firstNested = this.archive.getNestedArchive(entry)) {
      firstNestedUrl = firstNested.getUrl();
    }
    this.archive.close();
    setup(true);
    entry = getEntriesMap(this.archive).get("nested.jar");
    try (Archive secondNested = this.archive.getNestedArchive(entry)) {
      URL secondNestedUrl = secondNested.getUrl();
      assertThat(secondNestedUrl).isNotEqualTo(firstNestedUrl);
    }
  }

  @Test
  void unpackedLocationsFromSameArchiveShareSameParent() throws Exception {
    setup(true);
    try (Archive nestedArchive = this.archive.getNestedArchive(getEntriesMap(this.archive).get("nested.jar"));
            Archive anotherNestedArchive = this.archive
                    .getNestedArchive(getEntriesMap(this.archive).get("another-nested.jar"))) {
      File nested = new File(nestedArchive.getUrl().toURI());
      File anotherNested = new File(anotherNestedArchive.getUrl().toURI());
      assertThat(nested).hasParent(anotherNested.getParent());
    }
  }

  @Test
  void filesInZip64ArchivesAreAllListed() throws IOException {
    File file = new File(this.tempDir, "test.jar");
    FileCopyUtils.copy(writeZip64Jar(), file);
    try (JarFileArchive zip64Archive = new JarFileArchive(file)) {
      @SuppressWarnings("deprecation")
      Iterator<Entry> entries = zip64Archive.iterator();
      for (int i = 0; i < 65537; i++) {
        assertThat(entries.hasNext()).as(i + "nth file is present").isTrue();
        entries.next();
      }
    }
  }

  @Test
  void nestedZip64ArchivesAreHandledGracefully() throws Exception {
    File file = new File(this.tempDir, "test.jar");
    try (JarOutputStream output = new JarOutputStream(new FileOutputStream(file))) {
      JarEntry zip64JarEntry = new JarEntry("nested/zip64.jar");
      output.putNextEntry(zip64JarEntry);
      byte[] zip64JarData = writeZip64Jar();
      zip64JarEntry.setSize(zip64JarData.length);
      zip64JarEntry.setCompressedSize(zip64JarData.length);
      zip64JarEntry.setMethod(ZipEntry.STORED);
      CRC32 crc32 = new CRC32();
      crc32.update(zip64JarData);
      zip64JarEntry.setCrc(crc32.getValue());
      output.write(zip64JarData);
      output.closeEntry();
    }
    try (JarFile jarFile = new JarFile(file)) {
      ZipEntry nestedEntry = jarFile.getEntry("nested/zip64.jar");
      try (JarFile nestedJarFile = jarFile.getNestedJarFile(nestedEntry)) {
        Iterator<JarEntry> iterator = nestedJarFile.iterator();
        for (int i = 0; i < 65537; i++) {
          assertThat(iterator.hasNext()).as(i + "nth file is present").isTrue();
          iterator.next();
        }
      }
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

  private Map<String, Archive.Entry> getEntriesMap(Archive archive) {
    Map<String, Archive.Entry> entries = new HashMap<>();
    for (Archive.Entry entry : archive) {
      entries.put(entry.getName(), entry);
    }
    return entries;
  }

}
