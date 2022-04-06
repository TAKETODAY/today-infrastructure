/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.servlet.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StaticResourceJars}.
 *
 * @author Rupert Madden-Abbott
 * @author Andy Wilkinson
 */
class StaticResourceJarsTests {

  @TempDir
  File tempDir;

  @Test
  void includeJarWithStaticResources() throws Exception {
    File jarFile = createResourcesJar("test-resources.jar");
    List<URL> staticResourceJarUrls = new StaticResourceJars().getUrlsFrom(jarFile.toURI().toURL());
    assertThat(staticResourceJarUrls).hasSize(1);
  }

  @Test
  void includeJarWithStaticResourcesWithUrlEncodedSpaces() throws Exception {
    File jarFile = createResourcesJar("test resources.jar");
    List<URL> staticResourceJarUrls = new StaticResourceJars().getUrlsFrom(jarFile.toURI().toURL());
    assertThat(staticResourceJarUrls).hasSize(1);
  }

  @Test
  void includeJarWithStaticResourcesWithPlusInItsPath() throws Exception {
    File jarFile = createResourcesJar("test + resources.jar");
    List<URL> staticResourceJarUrls = new StaticResourceJars().getUrlsFrom(jarFile.toURI().toURL());
    assertThat(staticResourceJarUrls).hasSize(1);
  }

  @Test
  void excludeJarWithoutStaticResources() throws Exception {
    File jarFile = createJar("dependency.jar");
    List<URL> staticResourceJarUrls = new StaticResourceJars().getUrlsFrom(jarFile.toURI().toURL());
    assertThat(staticResourceJarUrls).hasSize(0);
  }

  @Test
  void uncPathsAreTolerated() throws Exception {
    File jarFile = createResourcesJar("test-resources.jar");
    List<URL> staticResourceJarUrls = new StaticResourceJars().getUrlsFrom(jarFile.toURI().toURL(),
            new URL("file://unc.example.com/test.jar"));
    assertThat(staticResourceJarUrls).hasSize(1);
  }

  @Test
  void ignoreWildcardUrls() throws Exception {
    File jarFile = createResourcesJar("test-resources.jar");
    URL folderUrl = jarFile.getParentFile().toURI().toURL();
    URL wildcardUrl = new URL(folderUrl.toString() + "*.jar");
    List<URL> staticResourceJarUrls = new StaticResourceJars().getUrlsFrom(wildcardUrl);
    assertThat(staticResourceJarUrls).isEmpty();
  }

  private File createResourcesJar(String name) throws IOException {
    return createJar(name, (output) -> {
      JarEntry jarEntry = new JarEntry("META-INF/resources");
      try {
        output.putNextEntry(jarEntry);
        output.closeEntry();
      }
      catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    });
  }

  private File createJar(String name) throws IOException {
    return createJar(name, null);
  }

  private File createJar(String name, Consumer<JarOutputStream> customizer) throws IOException {
    File jarFile = new File(this.tempDir, name);
    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile));
    if (customizer != null) {
      customizer.accept(jarOutputStream);
    }
    jarOutputStream.close();
    return jarFile;
  }

}
