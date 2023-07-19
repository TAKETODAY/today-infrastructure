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

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;

import cn.taketoday.app.loader.archive.Archive;
import cn.taketoday.app.loader.archive.ExplodedArchive;
import cn.taketoday.app.loader.archive.JarFileArchive;
import cn.taketoday.app.loader.jar.JarFile;

/**
 * Base class for launchers that can start an application with a fully configured
 * classpath backed by one or more {@link Archive}s.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class Launcher {

  private static final String JAR_MODE_LAUNCHER = "cn.taketoday.app.loader.jarmode.JarModeLauncher";

  /**
   * Launch the application. This method is the initial entry point that should be
   * called by a subclass {@code public static void main(String[] args)} method.
   *
   * @param args the incoming arguments
   * @throws Exception if the application fails to launch
   */
  protected void launch(String[] args) throws Exception {
    if (!isExploded()) {
      JarFile.registerUrlProtocolHandler();
    }
    ClassLoader classLoader = createClassLoader(getClassPathArchivesIterator());
    String jarMode = System.getProperty("jarmode");
    String launchClass = (jarMode != null && !jarMode.isEmpty()) ? JAR_MODE_LAUNCHER : getMainClass();
    launch(args, launchClass, classLoader);
  }

  /**
   * Create a classloader for the specified archives.
   *
   * @param archives the archives
   * @return the classloader
   * @throws Exception if the classloader cannot be created
   */
  protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
    ArrayList<URL> urls = new ArrayList<>(50);
    while (archives.hasNext()) {
      urls.add(archives.next().getUrl());
    }
    return createClassLoader(urls.toArray(new URL[0]));
  }

  /**
   * Create a classloader for the specified URLs.
   *
   * @param urls the URLs
   * @return the classloader
   * @throws Exception if the classloader cannot be created
   */
  protected ClassLoader createClassLoader(URL[] urls) throws Exception {
    return new LaunchedURLClassLoader(isExploded(), getArchive(), urls, getClass().getClassLoader());
  }

  /**
   * Launch the application given the archive file and a fully configured classloader.
   *
   * @param args the incoming arguments
   * @param launchClass the launch class to run
   * @param classLoader the classloader
   * @throws Exception if the launch fails
   */
  protected void launch(String[] args, String launchClass, ClassLoader classLoader) throws Exception {
    Thread.currentThread().setContextClassLoader(classLoader);
    createMainMethodRunner(launchClass, args, classLoader).run();
  }

  /**
   * Create the {@code MainMethodRunner} used to launch the application.
   *
   * @param mainClass the main class
   * @param args the incoming arguments
   * @param classLoader the classloader
   * @return the main method runner
   */
  protected MainMethodRunner createMainMethodRunner(String mainClass, String[] args, ClassLoader classLoader) {
    return new MainMethodRunner(mainClass, args);
  }

  /**
   * Returns the main class that should be launched.
   *
   * @return the name of the main class
   * @throws Exception if the main class cannot be obtained
   */
  protected abstract String getMainClass() throws Exception;

  /**
   * Returns the archives that will be used to construct the class path.
   *
   * @return the class path archives
   * @throws Exception if the class path archives cannot be obtained
   */
  protected abstract Iterator<Archive> getClassPathArchivesIterator() throws Exception;

  protected final Archive createArchive() throws Exception {
    ProtectionDomain protectionDomain = getClass().getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
    String path = (location != null) ? location.getSchemeSpecificPart() : null;
    if (path == null) {
      throw new IllegalStateException("Unable to determine code source archive");
    }
    File root = new File(path);
    if (!root.exists()) {
      throw new IllegalStateException("Unable to determine code source archive from " + root);
    }
    return (root.isDirectory() ? new ExplodedArchive(root) : new JarFileArchive(root));
  }

  /**
   * Returns if the launcher is running in an exploded mode. If this method returns
   * {@code true} then only regular JARs are supported and the additional URL and
   * ClassLoader support infrastructure can be optimized.
   *
   * @return if the jar is exploded.
   */
  protected boolean isExploded() {
    return false;
  }

  /**
   * Return the root archive.
   *
   * @return the root archive
   */
  protected Archive getArchive() {
    return null;
  }

}
