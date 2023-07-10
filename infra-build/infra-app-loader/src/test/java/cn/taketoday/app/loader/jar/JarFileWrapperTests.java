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

package cn.taketoday.app.loader.jar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import cn.taketoday.app.loader.jar.JarFileWrapperTests.SpyJarFile.Call;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JarFileWrapper}.
 *
 * @author Phillip Webb
 */
class JarFileWrapperTests {

  private SpyJarFile parent;

  private JarFileWrapper wrapper;

  @BeforeEach
  void setup(@TempDir File temp) throws Exception {
    this.parent = new SpyJarFile(createTempJar(temp));
    this.wrapper = new JarFileWrapper(this.parent);
  }

  @AfterEach
  void cleanup() throws Exception {
    this.parent.close();
  }

  private File createTempJar(File temp) throws IOException {
    File file = new File(temp, "temp.jar");
    new JarOutputStream(new FileOutputStream(file)).close();
    return file;
  }

  @Test
  void getUrlDelegatesToParent() throws MalformedURLException {
    this.wrapper.getUrl();
    this.parent.verify(Call.GET_URL);
  }

  @Test
  void getTypeDelegatesToParent() {
    this.wrapper.getType();
    this.parent.verify(Call.GET_TYPE);
  }

  @Test
  void getPermissionDelegatesToParent() {
    this.wrapper.getPermission();
    this.parent.verify(Call.GET_PERMISSION);
  }

  @Test
  void getManifestDelegatesToParent() throws IOException {
    this.wrapper.getManifest();
    this.parent.verify(Call.GET_MANIFEST);
  }

  @Test
  void entriesDelegatesToParent() {
    this.wrapper.entries();
    this.parent.verify(Call.ENTRIES);
  }

  @Test
  void getJarEntryDelegatesToParent() {
    this.wrapper.getJarEntry("test");
    this.parent.verify(Call.GET_JAR_ENTRY);
  }

  @Test
  void getEntryDelegatesToParent() {
    this.wrapper.getEntry("test");
    this.parent.verify(Call.GET_ENTRY);
  }

  @Test
  void getInputStreamDelegatesToParent() throws IOException {
    this.wrapper.getInputStream();
    this.parent.verify(Call.GET_INPUT_STREAM);
  }

  @Test
  void getEntryInputStreamDelegatesToParent() throws IOException {
    ZipEntry entry = new ZipEntry("test");
    this.wrapper.getInputStream(entry);
    this.parent.verify(Call.GET_ENTRY_INPUT_STREAM);
  }

  @Test
  void getCommentDelegatesToParent() {
    this.wrapper.getComment();
    this.parent.verify(Call.GET_COMMENT);
  }

  @Test
  void sizeDelegatesToParent() {
    this.wrapper.size();
    this.parent.verify(Call.SIZE);
  }

  @Test
  void toStringDelegatesToParent() {
    assertThat(this.wrapper.toString()).endsWith("temp.jar");
  }

  @Test
    // gh-22991
  void wrapperMustNotImplementClose() {
    // If the wrapper overrides close then on Java 11 a FinalizableResource
    // instance will be used to perform cleanup. This can result in a lot
    // of additional memory being used since cleanup only occurs when the
    // finalizer thread runs. See gh-22991
    assertThatExceptionOfType(NoSuchMethodException.class)
            .isThrownBy(() -> JarFileWrapper.class.getDeclaredMethod("close"));
  }

  @Test
  void streamDelegatesToParent() {
    this.wrapper.stream();
    this.parent.verify(Call.STREAM);
  }

  /**
   * {@link JarFile} that we can spy (even on Java 11+)
   */
  static class SpyJarFile extends JarFile {

    private final Set<Call> calls = EnumSet.noneOf(Call.class);

    SpyJarFile(File file) throws IOException {
      super(file);
    }

    @Override
    Permission getPermission() {
      mark(Call.GET_PERMISSION);
      return super.getPermission();
    }

    @Override
    public Manifest getManifest() throws IOException {
      mark(Call.GET_MANIFEST);
      return super.getManifest();
    }

    @Override
    public Enumeration<java.util.jar.JarEntry> entries() {
      mark(Call.ENTRIES);
      return super.entries();
    }

    @Override
    public Stream<java.util.jar.JarEntry> stream() {
      mark(Call.STREAM);
      return super.stream();
    }

    @Override
    public JarEntry getJarEntry(String name) {
      mark(Call.GET_JAR_ENTRY);
      return super.getJarEntry(name);
    }

    @Override
    public ZipEntry getEntry(String name) {
      mark(Call.GET_ENTRY);
      return super.getEntry(name);
    }

    @Override
    InputStream getInputStream() throws IOException {
      mark(Call.GET_INPUT_STREAM);
      return super.getInputStream();
    }

    @Override
    InputStream getInputStream(String name) throws IOException {
      mark(Call.GET_ENTRY_INPUT_STREAM);
      return super.getInputStream(name);
    }

    @Override
    public String getComment() {
      mark(Call.GET_COMMENT);
      return super.getComment();
    }

    @Override
    public int size() {
      mark(Call.SIZE);
      return super.size();
    }

    @Override
    public URL getUrl() throws MalformedURLException {
      mark(Call.GET_URL);
      return super.getUrl();
    }

    @Override
    JarFileType getType() {
      mark(Call.GET_TYPE);
      return super.getType();
    }

    private void mark(Call call) {
      this.calls.add(call);
    }

    void verify(Call call) {
      assertThat(call).matches(this.calls::contains);
    }

    enum Call {

      GET_URL,

      GET_TYPE,

      GET_PERMISSION,

      GET_MANIFEST,

      ENTRIES,

      GET_JAR_ENTRY,

      GET_ENTRY,

      GET_INPUT_STREAM,

      GET_ENTRY_INPUT_STREAM,

      GET_COMMENT,

      SIZE,

      STREAM

    }

  }

}
