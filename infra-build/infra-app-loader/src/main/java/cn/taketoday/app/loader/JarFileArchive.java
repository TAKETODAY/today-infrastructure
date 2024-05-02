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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import cn.taketoday.app.loader.net.protocol.jar.JarUrl;

/**
 * {@link Archive} implementation backed by a {@link JarFile}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JarFileArchive implements Archive {

  private static final String UNPACK_MARKER = "UNPACK:";

  private static final FileAttribute<?>[] NO_FILE_ATTRIBUTES = {};

  private static final FileAttribute<?>[] DIRECTORY_PERMISSION_ATTRIBUTES = asFileAttributes(
          PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);

  private static final FileAttribute<?>[] FILE_PERMISSION_ATTRIBUTES = asFileAttributes(
          PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

  private static final Path TEMP = Paths.get(System.getProperty("java.io.tmpdir"));

  private final File file;

  private final JarFile jarFile;

  private volatile Path tempUnpackDirectory;

  JarFileArchive(File file) throws IOException {
    this(file, new JarFile(file));
  }

  private JarFileArchive(File file, JarFile jarFile) {
    this.file = file;
    this.jarFile = jarFile;
  }

  @Override
  public Manifest getManifest() throws IOException {
    return this.jarFile.getManifest();
  }

  @Override
  public Set<URL> getClassPathUrls(Predicate<Entry> includeFilter, Predicate<Entry> directorySearchFilter)
          throws IOException {
    return this.jarFile.stream()
            .map(JarArchiveEntry::new)
            .filter(includeFilter)
            .map(this::getNestedJarUrl)
            .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private URL getNestedJarUrl(JarArchiveEntry archiveEntry) {
    try {
      JarEntry jarEntry = archiveEntry.jarEntry();
      String comment = jarEntry.getComment();
      if (comment != null && comment.startsWith(UNPACK_MARKER)) {
        return getUnpackedNestedJarUrl(jarEntry);
      }
      return JarUrl.create(this.file, jarEntry);
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private URL getUnpackedNestedJarUrl(JarEntry jarEntry) throws IOException {
    String name = jarEntry.getName();
    if (name.lastIndexOf('/') != -1) {
      name = name.substring(name.lastIndexOf('/') + 1);
    }
    Path path = getTempUnpackDirectory().resolve(name);
    if (!Files.exists(path) || Files.size(path) != jarEntry.getSize()) {
      unpack(jarEntry, path);
    }
    return path.toUri().toURL();
  }

  private Path getTempUnpackDirectory() {
    Path tempUnpackDirectory = this.tempUnpackDirectory;
    if (tempUnpackDirectory != null) {
      return tempUnpackDirectory;
    }
    synchronized(TEMP) {
      tempUnpackDirectory = this.tempUnpackDirectory;
      if (tempUnpackDirectory == null) {
        tempUnpackDirectory = createUnpackDirectory(TEMP);
        this.tempUnpackDirectory = tempUnpackDirectory;
      }
    }
    return tempUnpackDirectory;
  }

  private Path createUnpackDirectory(Path parent) {
    int attempts = 0;
    String fileName = Paths.get(this.jarFile.getName()).getFileName().toString();
    while (attempts++ < 100) {
      Path unpackDirectory = parent.resolve(fileName + "-infra-app-libs-" + UUID.randomUUID());
      try {
        createDirectory(unpackDirectory);
        return unpackDirectory;
      }
      catch (IOException ex) {
        // Ignore
      }
    }
    throw new IllegalStateException("Failed to create unpack directory in directory '" + parent + "'");
  }

  private void createDirectory(Path path) throws IOException {
    Files.createDirectory(path, getFileAttributes(path, DIRECTORY_PERMISSION_ATTRIBUTES));
  }

  private void unpack(JarEntry entry, Path path) throws IOException {
    createFile(path);
    path.toFile().deleteOnExit();
    try (InputStream in = this.jarFile.getInputStream(entry)) {
      Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private void createFile(Path path) throws IOException {
    Files.createFile(path, getFileAttributes(path, FILE_PERMISSION_ATTRIBUTES));
  }

  private FileAttribute<?>[] getFileAttributes(Path path, FileAttribute<?>[] permissionAttributes) {
    return (!supportsPosix(path.getFileSystem())) ? NO_FILE_ATTRIBUTES : permissionAttributes;
  }

  private boolean supportsPosix(FileSystem fileSystem) {
    return fileSystem.supportedFileAttributeViews().contains("posix");
  }

  @Override
  public void close() throws IOException {
    this.jarFile.close();
  }

  @Override
  public String toString() {
    return this.file.toString();
  }

  private static FileAttribute<?>[] asFileAttributes(PosixFilePermission... permissions) {
    return new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(Set.of(permissions)) };
  }

  /**
   * {@link Entry} implementation backed by a {@link JarEntry}.
   */
  private record JarArchiveEntry(JarEntry jarEntry) implements Entry {

    @Override
    public String name() {
      return this.jarEntry.getName();
    }

    @Override
    public boolean isDirectory() {
      return this.jarEntry.isDirectory();
    }

  }

}
