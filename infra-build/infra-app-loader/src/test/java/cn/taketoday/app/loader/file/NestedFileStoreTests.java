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

package cn.taketoday.app.loader.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NestedFileStore}.
 *
 * @author Phillip Webb
 */
class NestedFileStoreTests {

  @TempDir
  File temp;

  private NestedFileSystemProvider provider;

  private Path jarPath;

  private NestedFileSystem fileSystem;

  private TestNestedFileStore fileStore;

  @BeforeEach
  void setup() {
    this.provider = new NestedFileSystemProvider();
    this.jarPath = new File(this.temp, "test.jar").toPath();
    this.fileSystem = new NestedFileSystem(this.provider, this.jarPath);
    this.fileStore = new TestNestedFileStore(this.fileSystem);
  }

  @Test
  void nameReturnsName() {
    assertThat(this.fileStore.name()).isEqualTo(this.jarPath.toAbsolutePath().toString());
  }

  @Test
  void typeReturnsNestedFs() {
    assertThat(this.fileStore.type()).isEqualTo("nestedfs");
  }

  @Test
  void isReadOnlyReturnsTrue() {
    assertThat(this.fileStore.isReadOnly()).isTrue();
  }

  @Test
  void getTotalSpaceReturnsZero() throws Exception {
    assertThat(this.fileStore.getTotalSpace()).isZero();
  }

  @Test
  void getUsableSpaceReturnsZero() throws Exception {
    assertThat(this.fileStore.getUsableSpace()).isZero();
  }

  @Test
  void getUnallocatedSpaceReturnsZero() throws Exception {
    assertThat(this.fileStore.getUnallocatedSpace()).isZero();
  }

  @Test
  void supportsFileAttributeViewWithClassDelegatesToJarPathFileStore() {
    FileStore jarFileStore = mock(FileStore.class);
    given(jarFileStore.supportsFileAttributeView(BasicFileAttributeView.class)).willReturn(true);
    this.fileStore.setJarPathFileStore(jarFileStore);
    assertThat(this.fileStore.supportsFileAttributeView(BasicFileAttributeView.class)).isTrue();
    then(jarFileStore).should().supportsFileAttributeView(BasicFileAttributeView.class);
  }

  @Test
  void supportsFileAttributeViewWithStringDelegatesToJarPathFileStore() {
    FileStore jarFileStore = mock(FileStore.class);
    given(jarFileStore.supportsFileAttributeView("basic")).willReturn(true);
    this.fileStore.setJarPathFileStore(jarFileStore);
    assertThat(this.fileStore.supportsFileAttributeView("basic")).isTrue();
    then(jarFileStore).should().supportsFileAttributeView("basic");
  }

  @Test
  void getFileStoreAttributeViewDelegatesToJarPathFileStore() {
    FileStore jarFileStore = mock(FileStore.class);
    TestFileStoreAttributeView attributeView = mock(TestFileStoreAttributeView.class);
    given(jarFileStore.getFileStoreAttributeView(TestFileStoreAttributeView.class)).willReturn(attributeView);
    this.fileStore.setJarPathFileStore(jarFileStore);
    assertThat(this.fileStore.getFileStoreAttributeView(TestFileStoreAttributeView.class)).isEqualTo(attributeView);
    then(jarFileStore).should().getFileStoreAttributeView(TestFileStoreAttributeView.class);
  }

  @Test
  void getAttributeDelegatesToJarPathFileStore() throws Exception {
    FileStore jarFileStore = mock(FileStore.class);
    given(jarFileStore.getAttribute("test")).willReturn("spring");
    this.fileStore.setJarPathFileStore(jarFileStore);
    assertThat(this.fileStore.getAttribute("test")).isEqualTo("spring");
    then(jarFileStore).should().getAttribute("test");
  }

  static class TestNestedFileStore extends NestedFileStore {

    TestNestedFileStore(NestedFileSystem fileSystem) {
      super(fileSystem);
    }

    private FileStore jarPathFileStore;

    void setJarPathFileStore(FileStore jarPathFileStore) {
      this.jarPathFileStore = jarPathFileStore;
    }

    @Override
    protected FileStore getJarPathFileStore() {
      return (this.jarPathFileStore != null) ? this.jarPathFileStore : super.getJarPathFileStore();
    }

  }

  abstract static class TestFileStoreAttributeView implements FileStoreAttributeView {

  }

}
