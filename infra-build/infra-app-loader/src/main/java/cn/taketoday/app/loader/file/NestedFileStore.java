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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

import cn.taketoday.app.loader.net.protocol.nested.NestedLocation;

/**
 * {@link FileStore} implementation for {@link NestedLocation nested} jar files.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NestedFileSystemProvider
 * @since 5.0
 */
class NestedFileStore extends FileStore {

  private final NestedFileSystem fileSystem;

  NestedFileStore(NestedFileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  @Override
  public String name() {
    return this.fileSystem.toString();
  }

  @Override
  public String type() {
    return "nestedfs";
  }

  @Override
  public boolean isReadOnly() {
    return this.fileSystem.isReadOnly();
  }

  @Override
  public long getTotalSpace() throws IOException {
    return 0;
  }

  @Override
  public long getUsableSpace() throws IOException {
    return 0;
  }

  @Override
  public long getUnallocatedSpace() throws IOException {
    return 0;
  }

  @Override
  public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
    return getJarPathFileStore().supportsFileAttributeView(type);
  }

  @Override
  public boolean supportsFileAttributeView(String name) {
    return getJarPathFileStore().supportsFileAttributeView(name);
  }

  @Override
  public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
    return getJarPathFileStore().getFileStoreAttributeView(type);
  }

  @Override
  public Object getAttribute(String attribute) throws IOException {
    try {
      return getJarPathFileStore().getAttribute(attribute);
    }
    catch (UncheckedIOException ex) {
      throw ex.getCause();
    }
  }

  protected FileStore getJarPathFileStore() {
    try {
      return Files.getFileStore(this.fileSystem.getJarPath());
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

}
