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

package cn.taketoday.jarmode.layertools;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;

import cn.taketoday.lang.Assert;

/**
 * Context for use by commands.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class Context {

  private final File archiveFile;

  private final File workingDir;

  private final String relativeDir;

  /**
   * Create a new {@link Context} instance.
   */
  Context() {
    this(getSourceArchiveFile(), Paths.get(".").toAbsolutePath().normalize().toFile());
  }

  /**
   * Create a new {@link Context} instance with the specified value.
   *
   * @param archiveFile the source archive file
   * @param workingDir the working directory
   */
  Context(File archiveFile, File workingDir) {
    Assert.state(isExistingFile(archiveFile), "Unable to find source archive");
    Assert.state(isJarOrWar(archiveFile), "Source archive " + archiveFile + " must end with .jar or .war");
    this.archiveFile = archiveFile;
    this.workingDir = workingDir;
    this.relativeDir = deduceRelativeDir(archiveFile.getParentFile(), this.workingDir);
  }

  private boolean isExistingFile(File archiveFile) {
    return archiveFile != null && archiveFile.isFile() && archiveFile.exists();
  }

  private boolean isJarOrWar(File jarFile) {
    String name = jarFile.getName().toLowerCase();
    return name.endsWith(".jar") || name.endsWith(".war");
  }

  private static File getSourceArchiveFile() {
    try {
      ProtectionDomain domain = Context.class.getProtectionDomain();
      CodeSource codeSource = (domain != null) ? domain.getCodeSource() : null;
      URL location = (codeSource != null) ? codeSource.getLocation() : null;
      File source = (location != null) ? findSource(location) : null;
      if (source != null && source.exists()) {
        return source.getAbsoluteFile();
      }
      return null;
    }
    catch (Exception ex) {
      return null;
    }
  }

  private static File findSource(URL location) throws IOException, URISyntaxException {
    URLConnection connection = location.openConnection();
    if (connection instanceof JarURLConnection jarURLConnection) {
      return getRootJarFile(jarURLConnection.getJarFile());
    }
    return new File(location.toURI());
  }

  private static File getRootJarFile(JarFile jarFile) {
    String name = jarFile.getName();
    int separator = name.indexOf("!/");
    if (separator > 0) {
      name = name.substring(0, separator);
    }
    return new File(name);
  }

  private String deduceRelativeDir(File sourceDirectory, File workingDir) {
    String sourcePath = sourceDirectory.getAbsolutePath();
    String workingPath = workingDir.getAbsolutePath();
    if (sourcePath.equals(workingPath) || !sourcePath.startsWith(workingPath)) {
      return null;
    }
    String relativePath = sourcePath.substring(workingPath.length() + 1);
    return !relativePath.isEmpty() ? relativePath : null;
  }

  /**
   * Return the source archive file that is running in tools mode.
   *
   * @return the archive file
   */
  File getArchiveFile() {
    return this.archiveFile;
  }

  /**
   * Return the current working directory.
   *
   * @return the working dir
   */
  File getWorkingDir() {
    return this.workingDir;
  }

  /**
   * Return the directory relative to {@link #getWorkingDir()} that contains the archive
   * or {@code null} if none relative directory can be deduced.
   *
   * @return the relative dir ending in {@code /} or {@code null}
   */
  String getRelativeArchiveDir() {
    return this.relativeDir;
  }

}
