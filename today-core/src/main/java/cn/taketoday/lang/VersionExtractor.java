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

package cn.taketoday.lang;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Extracts version information for a Class.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Package#getImplementationVersion()
 * @see Attributes.Name#IMPLEMENTATION_VERSION
 * @since 4.0 2023/7/21 17:52
 */
public abstract class VersionExtractor {

  /**
   * Return the version information for the provided {@link Class}.
   *
   * @param cls the Class to retrieve the version for
   * @return the version, or {@code null} if a version can not be extracted
   */
  @Nullable
  public static String forClass(Class<?> cls) {
    String implementationVersion = cls.getPackage().getImplementationVersion();
    if (implementationVersion != null) {
      return implementationVersion;
    }
    URL codeSourceLocation = cls.getProtectionDomain().getCodeSource().getLocation();
    try {
      URLConnection connection = codeSourceLocation.openConnection();
      if (connection instanceof JarURLConnection jarURLConnection) {
        return getImplementationVersion(jarURLConnection.getJarFile());
      }
      try (JarFile jarFile = new JarFile(new File(codeSourceLocation.toURI()))) {
        return getImplementationVersion(jarFile);
      }
    }
    catch (Exception ex) {
      return null;
    }
  }

  private static String getImplementationVersion(JarFile jarFile) throws IOException {
    return jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
  }

}
