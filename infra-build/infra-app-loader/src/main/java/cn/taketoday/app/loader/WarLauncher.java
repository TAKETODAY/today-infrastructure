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

/**
 * {@link Launcher} for WAR based archives. This launcher for standard WAR archives.
 * Supports dependencies in {@code WEB-INF/lib} as well as {@code WEB-INF/lib-provided},
 * classes are loaded from {@code WEB-INF/classes}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class WarLauncher extends ExecutableArchiveLauncher {

  public WarLauncher() throws Exception {
  }

  protected WarLauncher(Archive archive) throws Exception {
    super(archive);
  }

  @Override
  public boolean isIncludedOnClassPath(Archive.Entry entry) {
    return isLibraryFileOrClassesDirectory(entry);
  }

  @Override
  protected String getEntryPathPrefix() {
    return "WEB-INF/";
  }

  static boolean isLibraryFileOrClassesDirectory(Archive.Entry entry) {
    String name = entry.name();
    if (entry.isDirectory()) {
      return name.equals("WEB-INF/classes/");
    }
    return name.startsWith("WEB-INF/lib/") || name.startsWith("WEB-INF/lib-provided/");
  }

  public static void main(String[] args) throws Exception {
    new WarLauncher().launch(args);
  }

}
