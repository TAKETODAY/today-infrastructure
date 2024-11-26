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

package infra.app.loader.file;

import java.io.IOException;
import java.net.URI;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import infra.app.loader.net.protocol.nested.NestedLocation;

/**
 * {@link FileSystem} implementation for {@link NestedLocation nested} jar files.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NestedFileSystemProvider
 * @since 5.0
 */
class NestedFileSystem extends FileSystem {

  private static final Set<String> SUPPORTED_FILE_ATTRIBUTE_VIEWS = Set.of("basic");

  private static final String FILE_SYSTEMS_CLASS_NAME = FileSystems.class.getName();

  private static final Object EXISTING_FILE_SYSTEM = new Object();

  private final NestedFileSystemProvider provider;

  private final Path jarPath;

  private volatile boolean closed;

  private final Map<String, Object> zipFileSystems = new HashMap<>();

  NestedFileSystem(NestedFileSystemProvider provider, Path jarPath) {
    if (provider == null || jarPath == null) {
      throw new IllegalArgumentException("Provider and JarPath is required");
    }
    this.provider = provider;
    this.jarPath = jarPath;
  }

  void installZipFileSystemIfNecessary(String nestedEntryName) {
    try {
      boolean seen;
      synchronized(this.zipFileSystems) {
        seen = this.zipFileSystems.putIfAbsent(nestedEntryName, EXISTING_FILE_SYSTEM) != null;
      }
      if (!seen) {
        URI uri = new URI("jar:nested:" + this.jarPath.toUri().getPath() + "/!" + nestedEntryName);
        if (!hasFileSystem(uri)) {
          FileSystem zipFileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
          synchronized(this.zipFileSystems) {
            this.zipFileSystems.put(nestedEntryName, zipFileSystem);
          }
        }
      }
    }
    catch (Exception ex) {
      // Ignore
    }
  }

  private boolean hasFileSystem(URI uri) {
    try {
      FileSystems.getFileSystem(uri);
      return true;
    }
    catch (FileSystemNotFoundException ex) {
      return isCreatingNewFileSystem();
    }
  }

  private boolean isCreatingNewFileSystem() {
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    if (stack != null) {
      for (StackTraceElement element : stack) {
        if (FILE_SYSTEMS_CLASS_NAME.equals(element.getClassName())) {
          return "newFileSystem".equals(element.getMethodName());
        }
      }
    }
    return false;
  }

  @Override
  public FileSystemProvider provider() {
    return this.provider;
  }

  Path getJarPath() {
    return this.jarPath;
  }

  @Override
  public void close() throws IOException {
    if (this.closed) {
      return;
    }
    this.closed = true;
    synchronized(this.zipFileSystems) {
      this.zipFileSystems.values()
              .stream()
              .filter(FileSystem.class::isInstance)
              .map(FileSystem.class::cast)
              .forEach(this::closeZipFileSystem);
    }
    this.provider.removeFileSystem(this);
  }

  private void closeZipFileSystem(FileSystem zipFileSystem) {
    try {
      zipFileSystem.close();
    }
    catch (Exception ex) {
      // Ignore
    }
  }

  @Override
  public boolean isOpen() {
    return !this.closed;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public String getSeparator() {
    return "/!";
  }

  @Override
  public Iterable<Path> getRootDirectories() {
    assertNotClosed();
    return Collections.emptySet();
  }

  @Override
  public Iterable<FileStore> getFileStores() {
    assertNotClosed();
    return Collections.emptySet();
  }

  @Override
  public Set<String> supportedFileAttributeViews() {
    assertNotClosed();
    return SUPPORTED_FILE_ATTRIBUTE_VIEWS;
  }

  @Override
  public Path getPath(String first, String... more) {
    assertNotClosed();
    if (more.length != 0) {
      throw new IllegalArgumentException("Nested paths must contain a single element");
    }
    return new NestedPath(this, first);
  }

  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern) {
    throw new UnsupportedOperationException("Nested paths do not support path matchers");
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    throw new UnsupportedOperationException("Nested paths do not have a user principal lookup service");
  }

  @Override
  public WatchService newWatchService() throws IOException {
    throw new UnsupportedOperationException("Nested paths do not support the WatchService");
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    NestedFileSystem other = (NestedFileSystem) obj;
    return this.jarPath.equals(other.jarPath);
  }

  @Override
  public int hashCode() {
    return this.jarPath.hashCode();
  }

  @Override
  public String toString() {
    return this.jarPath.toAbsolutePath().toString();
  }

  private void assertNotClosed() {
    if (this.closed) {
      throw new ClosedFileSystemException();
    }
  }

}
