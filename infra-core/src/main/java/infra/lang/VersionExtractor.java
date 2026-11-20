/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.lang;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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
   * @throws NullPointerException if cls is null
   */
  public static @Nullable String forClass(Class<?> cls) {
    Objects.requireNonNull(cls, "Class is required");

    Package classPackage = cls.getPackage();
    if (classPackage != null) {
      String implementationVersion = classPackage.getImplementationVersion();
      if (implementationVersion != null) {
        return implementationVersion;
      }
    }

    URL codeSourceLocation = getCodeSourceLocation(cls);
    if (codeSourceLocation == null) {
      return null;
    }

    try {
      if ("jar".equalsIgnoreCase(codeSourceLocation.getProtocol())) {
        // The underlying JarFile is cached and closed by the JDK's JarFileFactory
        URLConnection connection = codeSourceLocation.openConnection();
        if (connection instanceof JarURLConnection jarURLConnection) {
          return getImplementationVersion(jarURLConnection.getJarFile());
        }
      }
      else if ("file".equalsIgnoreCase(codeSourceLocation.getProtocol())) {
        // Directories (exploded classes) are not jar files and simply fail to open
        try (JarFile jarFile = new JarFile(new File(codeSourceLocation.toURI()), false)) {
          return getImplementationVersion(jarFile);
        }
      }
      // Other protocols are ignored to avoid unnecessary network access
    }
    catch (Exception ignored) {
    }
    return null;
  }

  private static @Nullable URL getCodeSourceLocation(Class<?> cls) {
    ProtectionDomain protectionDomain = cls.getProtectionDomain();
    if (protectionDomain != null) {
      CodeSource codeSource = protectionDomain.getCodeSource();
      if (codeSource != null) {
        return codeSource.getLocation();
      }
    }
    return null;
  }

  private static @Nullable String getImplementationVersion(JarFile jarFile) throws IOException {
    Manifest manifest = jarFile.getManifest();
    if (manifest == null) {
      return null;
    }
    return manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
  }

}
