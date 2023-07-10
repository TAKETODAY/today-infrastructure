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

package cn.taketoday.app.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;

import cn.taketoday.app.loader.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LaunchedURLClassLoader}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@SuppressWarnings("resource")
class LaunchedURLClassLoaderTests {

  @TempDir
  File tempDir;

  @Test
  void resolveResourceFromArchive() throws Exception {
    LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
            new URL[] { new URL("jar:file:src/test/resources/jars/app.jar!/") }, getClass().getClassLoader());
    assertThat(loader.getResource("demo/Application.java")).isNotNull();
  }

  @Test
  void resolveResourcesFromArchive() throws Exception {
    LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
            new URL[] { new URL("jar:file:src/test/resources/jars/app.jar!/") }, getClass().getClassLoader());
    assertThat(loader.getResources("demo/Application.java").hasMoreElements()).isTrue();
  }

  @Test
  void resolveRootPathFromArchive() throws Exception {
    LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
            new URL[] { new URL("jar:file:src/test/resources/jars/app.jar!/") }, getClass().getClassLoader());
    assertThat(loader.getResource("")).isNotNull();
  }

  @Test
  void resolveRootResourcesFromArchive() throws Exception {
    LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
            new URL[] { new URL("jar:file:src/test/resources/jars/app.jar!/") }, getClass().getClassLoader());
    assertThat(loader.getResources("").hasMoreElements()).isTrue();
  }

  @Test
  void resolveFromNested() throws Exception {
    File file = new File(this.tempDir, "test.jar");
    TestJarCreator.createTestJar(file);
    try (JarFile jarFile = new JarFile(file)) {
      URL url = jarFile.getUrl();
      try (LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { url }, null)) {
        URL resource = loader.getResource("nested.jar!/3.dat");
        assertThat(resource).hasToString(url + "nested.jar!/3.dat");
        try (InputStream input = resource.openConnection().getInputStream()) {
          assertThat(input.read()).isEqualTo(3);
        }
      }
    }
  }

  @Test
  void resolveFromNestedWhileThreadIsInterrupted() throws Exception {
    File file = new File(this.tempDir, "test.jar");
    TestJarCreator.createTestJar(file);
    try (JarFile jarFile = new JarFile(file)) {
      URL url = jarFile.getUrl();
      try (LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { url }, null)) {
        Thread.currentThread().interrupt();
        URL resource = loader.getResource("nested.jar!/3.dat");
        assertThat(resource).hasToString(url + "nested.jar!/3.dat");
        URLConnection connection = resource.openConnection();
        try (InputStream input = connection.getInputStream()) {
          assertThat(input.read()).isEqualTo(3);
        }
        ((JarURLConnection) connection).getJarFile().close();
      }
      finally {
        Thread.interrupted();
      }
    }
  }

}
