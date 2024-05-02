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
 * {@link Launcher} for JAR based archives. This launcher assumes that dependency jars are
 * included inside a {@code /APP-INF/lib} directory and that application classes are
 * included inside a {@code /APP-INF/classes} directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class JarLauncher extends ExecutableArchiveLauncher {

  public JarLauncher() throws Exception {
  }

  protected JarLauncher(Archive archive) throws Exception {
    super(archive);
  }

  @Override
  protected boolean isIncludedOnClassPath(Archive.Entry entry) {
    return isLibraryFileOrClassesDirectory(entry);
  }

  @Override
  protected String getEntryPathPrefix() {
    return "APP-INF/";
  }

  static boolean isLibraryFileOrClassesDirectory(Archive.Entry entry) {
    String name = entry.name();
    if (entry.isDirectory()) {
      return name.equals("APP-INF/classes/");
    }
    return name.startsWith("APP-INF/lib/");
  }

  public static void main(String[] args) throws Exception {
    new JarLauncher().launch(args);
  }

}
