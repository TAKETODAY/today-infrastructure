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

import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;

import infra.app.loader.net.protocol.nested.NestedLocation;
import infra.app.loader.zip.ZipContent;

/**
 * {@link Path} implementation for {@link NestedLocation nested} jar files.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NestedFileSystemProvider
 * @since 5.0
 */
final class NestedPath implements Path {

  private final NestedFileSystem fileSystem;

  private final String nestedEntryName;

  private volatile Boolean entryExists;

  NestedPath(NestedFileSystem fileSystem, String nestedEntryName) {
    if (fileSystem == null) {
      throw new IllegalArgumentException("'filesSystem' is required");
    }
    this.fileSystem = fileSystem;
    this.nestedEntryName = (nestedEntryName != null && !nestedEntryName.isBlank()) ? nestedEntryName : null;
  }

  Path getJarPath() {
    return this.fileSystem.getJarPath();
  }

  String getNestedEntryName() {
    return this.nestedEntryName;
  }

  @Override
  public NestedFileSystem getFileSystem() {
    return this.fileSystem;
  }

  @Override
  public boolean isAbsolute() {
    return true;
  }

  @Override
  public Path getRoot() {
    return null;
  }

  @Override
  public Path getFileName() {
    return this;
  }

  @Override
  public Path getParent() {
    return null;
  }

  @Override
  public int getNameCount() {
    return 1;
  }

  @Override
  public Path getName(int index) {
    if (index != 0) {
      throw new IllegalArgumentException("Nested paths only have a single element");
    }
    return this;
  }

  @Override
  public Path subpath(int beginIndex, int endIndex) {
    if (beginIndex != 0 || endIndex != 1) {
      throw new IllegalArgumentException("Nested paths only have a single element");
    }
    return this;
  }

  @Override
  public boolean startsWith(Path other) {
    return equals(other);
  }

  @Override
  public boolean endsWith(Path other) {
    return equals(other);
  }

  @Override
  public Path normalize() {
    return this;
  }

  @Override
  public Path resolve(Path other) {
    throw new UnsupportedOperationException("Unable to resolve nested path");
  }

  @Override
  public Path relativize(Path other) {
    throw new UnsupportedOperationException("Unable to relativize nested path");
  }

  @Override
  public URI toUri() {
    try {
      String uri = "nested:" + this.fileSystem.getJarPath().toUri().getRawPath();
      if (this.nestedEntryName != null) {
        uri += "/!" + UriPathEncoder.encode(this.nestedEntryName);
      }
      return new URI(uri);
    }
    catch (URISyntaxException ex) {
      throw new IOError(ex);
    }
  }

  @Override
  public Path toAbsolutePath() {
    return this;
  }

  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    return this;
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
    throw new UnsupportedOperationException("Nested paths cannot be watched");
  }

  @Override
  public int compareTo(Path other) {
    NestedPath otherNestedPath = cast(other);
    return this.nestedEntryName.compareTo(otherNestedPath.nestedEntryName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    NestedPath other = (NestedPath) obj;
    return Objects.equals(this.fileSystem, other.fileSystem)
            && Objects.equals(this.nestedEntryName, other.nestedEntryName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.fileSystem, this.nestedEntryName);
  }

  @Override
  public String toString() {
    String string = this.fileSystem.getJarPath().toString();
    if (this.nestedEntryName != null) {
      string += this.fileSystem.getSeparator() + this.nestedEntryName;
    }
    return string;
  }

  void assertExists() throws NoSuchFileException {
    if (!Files.isRegularFile(getJarPath())) {
      throw new NoSuchFileException(toString());
    }
    Boolean entryExists = this.entryExists;
    if (entryExists == null) {
      try {
        try (ZipContent content = ZipContent.open(getJarPath(), this.nestedEntryName)) {
          entryExists = true;
        }
      }
      catch (IOException ex) {
        entryExists = false;
      }
      this.entryExists = entryExists;
    }
    if (!entryExists) {
      throw new NoSuchFileException(toString());
    }
  }

  static NestedPath cast(Path path) {
    if (path instanceof NestedPath nestedPath) {
      return nestedPath;
    }
    throw new ProviderMismatchException();
  }

}
