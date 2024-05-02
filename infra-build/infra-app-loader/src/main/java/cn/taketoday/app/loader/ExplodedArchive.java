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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Manifest;

/**
 * {@link Archive} implementation backed by an exploded archive directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ExplodedArchive implements Archive {

  private static final Object NO_MANIFEST = new Object();

  private static final Set<String> SKIPPED_NAMES = Set.of(".", "..");

  private static final Comparator<File> entryComparator = Comparator.comparing(File::getAbsolutePath);

  private final File rootDirectory;

  private final String rootUriPath;

  private volatile Object manifest;

  /**
   * Create a new {@link ExplodedArchive} instance.
   *
   * @param rootDirectory the root directory
   */
  ExplodedArchive(File rootDirectory) {
    if (!rootDirectory.exists() || !rootDirectory.isDirectory()) {
      throw new IllegalArgumentException("Invalid source directory " + rootDirectory);
    }
    this.rootDirectory = rootDirectory;
    this.rootUriPath = ExplodedArchive.this.rootDirectory.toURI().getPath();
  }

  @Override
  public Manifest getManifest() throws IOException {
    Object manifest = this.manifest;
    if (manifest == null) {
      manifest = loadManifest();
      this.manifest = manifest;
    }
    return (manifest != NO_MANIFEST) ? (Manifest) manifest : null;
  }

  private Object loadManifest() throws IOException {
    File file = new File(this.rootDirectory, "META-INF/MANIFEST.MF");
    if (!file.exists()) {
      return NO_MANIFEST;
    }
    try (FileInputStream inputStream = new FileInputStream(file)) {
      return new Manifest(inputStream);
    }
  }

  @Override
  public Set<URL> getClassPathUrls(Predicate<Entry> includeFilter, Predicate<Entry> directorySearchFilter) throws IOException {
    var urls = new LinkedHashSet<URL>();
    LinkedList<File> files = new LinkedList<>(listFiles(this.rootDirectory));
    while (!files.isEmpty()) {
      File file = files.poll();
      if (SKIPPED_NAMES.contains(file.getName())) {
        continue;
      }
      String entryName = file.toURI().getPath().substring(this.rootUriPath.length());
      Entry entry = new FileArchiveEntry(entryName, file);
      if (entry.isDirectory() && directorySearchFilter.test(entry)) {
        files.addAll(0, listFiles(file));
      }
      if (includeFilter.test(entry)) {
        urls.add(file.toURI().toURL());
      }
    }
    return urls;
  }

  private List<File> listFiles(File file) {
    File[] files = file.listFiles();
    if (files == null) {
      return Collections.emptyList();
    }
    Arrays.sort(files, entryComparator);
    return Arrays.asList(files);
  }

  @Override
  public File getRootDirectory() {
    return this.rootDirectory;
  }

  @Override
  public String toString() {
    return this.rootDirectory.toString();
  }

  /**
   * {@link Entry} backed by a File.
   */
  private record FileArchiveEntry(String name, File file) implements Entry {

    @Override
    public boolean isDirectory() {
      return this.file.isDirectory();
    }

  }

}
