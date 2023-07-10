/*
 * Copyright 2012 - 2023 the original author or authors.
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

/**
 * {@link Launcher} for WAR based archives. This launcher for standard WAR archives.
 * Supports dependencies in {@code WEB-INF/lib} as well as {@code WEB-INF/lib-provided},
 * classes are loaded from {@code WEB-INF/classes}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WarLauncher extends ExecutableArchiveLauncher {

  public WarLauncher() { }

  protected WarLauncher(Archive archive) {
    super(archive);
  }

  @Override
  protected boolean isPostProcessingClassPathArchives() {
    return false;
  }

  @Override
  public boolean isNestedArchive(Archive.Entry entry) {
    if (entry.isDirectory()) {
      return entry.getName().equals("WEB-INF/classes/");
    }
    return entry.getName().startsWith("WEB-INF/lib/") || entry.getName().startsWith("WEB-INF/lib-provided/");
  }

  @Override
  protected String getArchiveEntryPathPrefix() {
    return "WEB-INF/";
  }

  public static void main(String[] args) throws Exception {
    new WarLauncher().launch(args);
  }

}
