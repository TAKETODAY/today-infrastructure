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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * Base class for a {@link Launcher} backed by an executable archive.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JarLauncher
 * @see WarLauncher
 * @since 4.0
 */
public abstract class ExecutableArchiveLauncher extends Launcher {

  private static final String START_CLASS_ATTRIBUTE = "Start-Class";

  private final Archive archive;

  public ExecutableArchiveLauncher() throws Exception {
    this(Archive.create(Launcher.class));
  }

  protected ExecutableArchiveLauncher(Archive archive) throws Exception {
    this.archive = archive;
    this.classPathIndex = getClassPathIndex(this.archive);
  }

  @Override
  protected ClassLoader createClassLoader(Collection<URL> urls) throws Exception {
    if (this.classPathIndex != null) {
      urls = new ArrayList<>(urls);
      urls.addAll(this.classPathIndex.getUrls());
    }
    return super.createClassLoader(urls);
  }

  @Override
  protected final Archive getArchive() {
    return this.archive;
  }

  @Override
  protected String getMainClass() throws Exception {
    Manifest manifest = this.archive.getManifest();
    String mainClass = (manifest != null) ? manifest.getMainAttributes().getValue(START_CLASS_ATTRIBUTE) : null;
    if (mainClass == null) {
      throw new IllegalStateException("No 'Start-Class' manifest entry specified in " + this);
    }
    return mainClass;
  }

  @Override
  protected Set<URL> getClassPathUrls() throws Exception {
    return this.archive.getClassPathUrls(this::isIncludedOnClassPathAndNotIndexed, this::isSearchedDirectory);
  }

  /**
   * Determine if the specified directory entry is a candidate for further searching.
   *
   * @param entry the entry to check
   * @return {@code true} if the entry is a candidate for further searching
   */
  protected boolean isSearchedDirectory(Archive.Entry entry) {
    return ((getEntryPathPrefix() == null) || entry.name().startsWith(getEntryPathPrefix()))
            && !isIncludedOnClassPath(entry);
  }

}
