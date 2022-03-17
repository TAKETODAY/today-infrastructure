/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * Provides access to the application home directory. Attempts to pick a sensible home for
 * both Jar Files, Exploded Archives and directly running applications.
 *
 * @author Phillip Webb
 * @author Raja Kolli
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 23:14
 */
public class ApplicationHome {

  private final File source;

  private final File dir;

  /**
   * Create a new {@link ApplicationHome} instance.
   */
  public ApplicationHome() {
    this(null);
  }

  /**
   * Create a new {@link ApplicationHome} instance for the specified source class.
   *
   * @param sourceClass the source class or {@code null}
   */
  public ApplicationHome(@Nullable Class<?> sourceClass) {
    this.source = findSource((sourceClass != null) ? sourceClass : getStartClass());
    this.dir = findHomeDir(this.source);
  }

  @Nullable
  private Class<?> getStartClass() {
    try {
      ClassLoader classLoader = getClass().getClassLoader();
      return getStartClass(classLoader.getResources("META-INF/MANIFEST.MF"));
    }
    catch (Exception ex) {
      return null;
    }
  }

  @Nullable
  private Class<?> getStartClass(Enumeration<URL> manifestResources) {
    while (manifestResources.hasMoreElements()) {
      try (InputStream inputStream = manifestResources.nextElement().openStream()) {
        Manifest manifest = new Manifest(inputStream);
        String startClass = manifest.getMainAttributes().getValue("Main-Class");
        if (startClass != null) {
          return ClassUtils.forName(startClass, getClass().getClassLoader());
        }
      }
      catch (Exception ignored) { }
    }
    return null;
  }

  @Nullable
  private File findSource(@Nullable Class<?> sourceClass) {
    try {
      ProtectionDomain domain = (sourceClass != null) ? sourceClass.getProtectionDomain() : null;
      CodeSource codeSource = (domain != null) ? domain.getCodeSource() : null;
      URL location = (codeSource != null) ? codeSource.getLocation() : null;
      File source = (location != null) ? findSource(location) : null;
      if (source != null && source.exists() && !isUnitTest()) {
        return source.getAbsoluteFile();
      }
    }
    catch (Exception ignored) { }
    return null;
  }

  private boolean isUnitTest() {
    try {
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      for (int i = stackTrace.length - 1; i >= 0; i--) {
        if (stackTrace[i].getClassName().startsWith("org.junit.")) {
          return true;
        }
      }
    }
    catch (Exception ignored) { }
    return false;
  }

  private File findSource(URL location) throws IOException, URISyntaxException {
    URLConnection connection = location.openConnection();
    if (connection instanceof JarURLConnection) {
      return getRootJarFile(((JarURLConnection) connection).getJarFile());
    }
    return new File(location.toURI());
  }

  private File getRootJarFile(JarFile jarFile) {
    String name = jarFile.getName();
    int separator = name.indexOf("!/");
    if (separator > 0) {
      name = name.substring(0, separator);
    }
    return new File(name);
  }

  private File findHomeDir(File source) {
    File homeDir = source;
    homeDir = (homeDir != null) ? homeDir : findDefaultHomeDir();
    if (homeDir.isFile()) {
      homeDir = homeDir.getParentFile();
    }
    homeDir = homeDir.exists() ? homeDir : new File(".");
    return homeDir.getAbsoluteFile();
  }

  private File findDefaultHomeDir() {
    String userDir = System.getProperty("user.dir");
    return new File(StringUtils.isNotEmpty(userDir) ? userDir : ".");
  }

  /**
   * Returns the underlying source used to find the home directory. This is usually the
   * jar file or a directory. Can return {@code null} if the source cannot be
   * determined.
   *
   * @return the underlying source or {@code null}
   */
  @Nullable
  public File getSource() {
    return this.source;
  }

  /**
   * Returns the application home directory.
   *
   * @return the home directory (never {@code null})
   */
  public File getDir() {
    return this.dir;
  }

  @Override
  public String toString() {
    return getDir().toString();
  }

}
