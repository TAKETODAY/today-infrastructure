/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.gradle.util;

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
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class VersionExtractor {

  private VersionExtractor() {
  }

  /**
   * Return the version information for the provided {@link Class}.
   *
   * @param cls the Class to retrieve the version for
   * @return the version, or {@code null} if a version can not be extracted
   */
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
