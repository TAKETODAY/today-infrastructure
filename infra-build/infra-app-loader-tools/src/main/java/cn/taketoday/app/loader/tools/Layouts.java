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

package cn.taketoday.app.loader.tools;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Common {@link Layout}s.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class Layouts {

  /**
   * Return a layout for the given source file.
   *
   * @param file the source file
   * @return a {@link Layout}
   */
  public static Layout forFile(File file) {
    if (file == null) {
      throw new IllegalArgumentException("File must not be null");
    }
    String lowerCaseFileName = file.getName().toLowerCase(Locale.ENGLISH);
    if (lowerCaseFileName.endsWith(".jar")) {
      return new Jar();
    }
    if (lowerCaseFileName.endsWith(".war")) {
      return new War();
    }
    if (file.isDirectory() || lowerCaseFileName.endsWith(".zip")) {
      return new Expanded();
    }
    throw new IllegalStateException("Unable to deduce layout for '" + file + "'");
  }

  /**
   * Executable JAR layout.
   */
  public static class Jar implements RepackagingLayout {

    @Override
    public String getLauncherClassName() {
      return "cn.taketoday.app.loader.JarLauncher";
    }

    @Override
    public String getLibraryLocation(String libraryName, LibraryScope scope) {
      return "APP-INF/lib/";
    }

    @Override
    public String getClassesLocation() {
      return "";
    }

    @Override
    public String getRepackagedClassesLocation() {
      return "APP-INF/classes/";
    }

    @Override
    public String getClasspathIndexFileLocation() {
      return "APP-INF/classpath.idx";
    }

    @Override
    public String getLayersIndexFileLocation() {
      return "APP-INF/layers.idx";
    }

    @Override
    public boolean isExecutable() {
      return true;
    }

  }

  /**
   * Executable expanded archive layout.
   */
  public static class Expanded extends Jar {

    @Override
    public String getLauncherClassName() {
      return "cn.taketoday.app.loader.PropertiesLauncher";
    }

  }

  /**
   * No layout.
   */
  public static class None extends Jar {

    @Override
    public String getLauncherClassName() {
      return null;
    }

    @Override
    public boolean isExecutable() {
      return false;
    }

  }

  /**
   * Executable WAR layout.
   */
  public static class War implements Layout {

    private static final Map<LibraryScope, String> SCOPE_LOCATION;

    static {
      Map<LibraryScope, String> locations = new HashMap<>();
      locations.put(LibraryScope.COMPILE, "WEB-INF/lib/");
      locations.put(LibraryScope.CUSTOM, "WEB-INF/lib/");
      locations.put(LibraryScope.RUNTIME, "WEB-INF/lib/");
      locations.put(LibraryScope.PROVIDED, "WEB-INF/lib-provided/");
      SCOPE_LOCATION = Collections.unmodifiableMap(locations);
    }

    @Override
    public String getLauncherClassName() {
      return "cn.taketoday.app.loader.WarLauncher";
    }

    @Override
    public String getLibraryLocation(String libraryName, LibraryScope scope) {
      return SCOPE_LOCATION.get(scope);
    }

    @Override
    public String getClassesLocation() {
      return "WEB-INF/classes/";
    }

    @Override
    public String getClasspathIndexFileLocation() {
      return "WEB-INF/classpath.idx";
    }

    @Override
    public String getLayersIndexFileLocation() {
      return "WEB-INF/layers.idx";
    }

    @Override
    public boolean isExecutable() {
      return true;
    }

  }

}
