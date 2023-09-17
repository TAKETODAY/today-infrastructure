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

import cn.taketoday.app.loader.archive.Archive;
import cn.taketoday.app.loader.archive.Archive.EntryFilter;

/**
 * {@link Launcher} for JAR based archives. This launcher assumes that dependency jars are
 * included inside a {@code /APP-INF/lib} directory and that application classes are
 * included inside a {@code /APP-INF/classes} directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JarLauncher extends ExecutableArchiveLauncher {

  static final EntryFilter NESTED_ARCHIVE_ENTRY_FILTER = (entry) -> {
    if (entry.isDirectory()) {
      return entry.getName().equals("APP-INF/classes/");
    }
    return entry.getName().startsWith("APP-INF/lib/");
  };

  public JarLauncher() {
  }

  protected JarLauncher(Archive archive) {
    super(archive);
  }

  @Override
  protected boolean isPostProcessingClassPathArchives() {
    return false;
  }

  @Override
  protected boolean isNestedArchive(Archive.Entry entry) {
    return NESTED_ARCHIVE_ENTRY_FILTER.matches(entry);
  }

  @Override
  protected String getArchiveEntryPathPrefix() {
    return "APP-INF/";
  }

  public static void main(String[] args) throws Exception {
    new JarLauncher().launch(args);
  }

}
