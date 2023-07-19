/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.app.loader;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import cn.taketoday.app.loader.archive.Archive;
import cn.taketoday.app.loader.archive.ExplodedArchive;

/**
 * Base class for executable archive {@link Launcher}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ExecutableArchiveLauncher extends Launcher {

  private static final String START_CLASS_ATTRIBUTE = "Start-Class";

  protected static final String BOOT_CLASSPATH_INDEX_ATTRIBUTE = "Infra-App-Classpath-Index";

  protected static final String DEFAULT_CLASSPATH_INDEX_FILE_NAME = "classpath.idx";

  private final Archive archive;

  private final ClassPathIndexFile classPathIndex;

  public ExecutableArchiveLauncher() {
    try {
      this.archive = createArchive();
      this.classPathIndex = getClassPathIndex(this.archive);
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  protected ExecutableArchiveLauncher(Archive archive) {
    try {
      this.archive = archive;
      this.classPathIndex = getClassPathIndex(this.archive);
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  protected ClassPathIndexFile getClassPathIndex(Archive archive) throws IOException {
    // Only needed for exploded archives, regular ones already have a defined order
    if (archive instanceof ExplodedArchive) {
      String location = getClassPathIndexFileLocation(archive);
      return ClassPathIndexFile.loadIfPossible(archive.getUrl(), location);
    }
    return null;
  }

  private String getClassPathIndexFileLocation(Archive archive) throws IOException {
    Manifest manifest = archive.getManifest();
    Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
    String location = (attributes != null) ? attributes.getValue(BOOT_CLASSPATH_INDEX_ATTRIBUTE) : null;
    return (location != null) ? location : getArchiveEntryPathPrefix() + DEFAULT_CLASSPATH_INDEX_FILE_NAME;
  }

  @Override
  protected String getMainClass() throws Exception {
    Manifest manifest = this.archive.getManifest();
    String mainClass = null;
    if (manifest != null) {
      mainClass = manifest.getMainAttributes().getValue(START_CLASS_ATTRIBUTE);
    }
    if (mainClass == null) {
      throw new IllegalStateException("No 'Start-Class' manifest entry specified in " + this);
    }
    return mainClass;
  }

  @Override
  protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
    ArrayList<URL> urls = new ArrayList<>(guessClassPathSize());
    while (archives.hasNext()) {
      urls.add(archives.next().getUrl());
    }
    if (this.classPathIndex != null) {
      urls.addAll(this.classPathIndex.getUrls());
    }
    return createClassLoader(urls.toArray(new URL[0]));
  }

  private int guessClassPathSize() {
    if (this.classPathIndex != null) {
      return this.classPathIndex.size() + 10;
    }
    return 50;
  }

  @Override
  protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
    Archive.EntryFilter searchFilter = this::isSearchCandidate;
    Iterator<Archive> archives = this.archive.getNestedArchives(searchFilter,
            (entry) -> isNestedArchive(entry) && !isEntryIndexed(entry));
    if (isPostProcessingClassPathArchives()) {
      archives = applyClassPathArchivePostProcessing(archives);
    }
    return archives;
  }

  private boolean isEntryIndexed(Archive.Entry entry) {
    if (this.classPathIndex != null) {
      return this.classPathIndex.containsEntry(entry.getName());
    }
    return false;
  }

  private Iterator<Archive> applyClassPathArchivePostProcessing(Iterator<Archive> archives) throws Exception {
    List<Archive> list = new ArrayList<>();
    while (archives.hasNext()) {
      list.add(archives.next());
    }
    postProcessClassPathArchives(list);
    return list.iterator();
  }

  /**
   * Determine if the specified entry is a candidate for further searching.
   *
   * @param entry the entry to check
   * @return {@code true} if the entry is a candidate for further searching
   */
  protected boolean isSearchCandidate(Archive.Entry entry) {
    if (getArchiveEntryPathPrefix() == null) {
      return true;
    }
    return entry.getName().startsWith(getArchiveEntryPathPrefix());
  }

  /**
   * Determine if the specified entry is a nested item that should be added to the
   * classpath.
   *
   * @param entry the entry to check
   * @return {@code true} if the entry is a nested item (jar or directory)
   */
  protected abstract boolean isNestedArchive(Archive.Entry entry);

  /**
   * Return if post-processing needs to be applied to the archives. For back
   * compatibility this method returns {@code true}, but subclasses that don't override
   * {@link #postProcessClassPathArchives(List)} should provide an implementation that
   * returns {@code false}.
   *
   * @return if the {@link #postProcessClassPathArchives(List)} method is implemented
   */
  protected boolean isPostProcessingClassPathArchives() {
    return true;
  }

  /**
   * Called to post-process archive entries before they are used. Implementations can
   * add and remove entries.
   *
   * @param archives the archives
   * @throws Exception if the post-processing fails
   * @see #isPostProcessingClassPathArchives()
   */
  protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {
  }

  /**
   * Return the path prefix for entries in the archive.
   *
   * @return the path prefix
   */
  protected String getArchiveEntryPathPrefix() {
    return null;
  }

  @Override
  protected boolean isExploded() {
    return this.archive.isExploded();
  }

  @Override
  protected final Archive getArchive() {
    return this.archive;
  }

}
