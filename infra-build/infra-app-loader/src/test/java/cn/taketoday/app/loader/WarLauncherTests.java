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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.app.loader.archive.Archive;
import cn.taketoday.app.loader.archive.ExplodedArchive;
import cn.taketoday.app.loader.archive.JarFileArchive;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WarLauncher}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class WarLauncherTests extends AbstractExecutableArchiveLauncherTests {

  @Test
  void explodedWarHasOnlyWebInfClassesAndContentsOfWebInfLibOnClasspath() throws Exception {
    File explodedRoot = explode(createJarArchive("archive.war", "WEB-INF"));
    WarLauncher launcher = new WarLauncher(new ExplodedArchive(explodedRoot, true));
    List<Archive> archives = new ArrayList<>();
    launcher.getClassPathArchivesIterator().forEachRemaining(archives::add);
    assertThat(getUrls(archives)).containsExactlyInAnyOrder(getExpectedFileUrls(explodedRoot));
    for (Archive archive : archives) {
      archive.close();
    }
  }

  @Test
  void archivedWarHasOnlyWebInfClassesAndContentsOfWebInfLibOnClasspath() throws Exception {
    File jarRoot = createJarArchive("archive.war", "WEB-INF");
    try (JarFileArchive archive = new JarFileArchive(jarRoot)) {
      WarLauncher launcher = new WarLauncher(archive);
      List<Archive> classPathArchives = new ArrayList<>();
      launcher.getClassPathArchivesIterator().forEachRemaining(classPathArchives::add);
      assertThat(getUrls(classPathArchives)).containsOnly(
              new URL("jar:" + jarRoot.toURI().toURL() + "!/WEB-INF/classes!/"),
              new URL("jar:" + jarRoot.toURI().toURL() + "!/WEB-INF/lib/foo.jar!/"),
              new URL("jar:" + jarRoot.toURI().toURL() + "!/WEB-INF/lib/bar.jar!/"),
              new URL("jar:" + jarRoot.toURI().toURL() + "!/WEB-INF/lib/baz.jar!/"));
      for (Archive classPathArchive : classPathArchives) {
        classPathArchive.close();
      }
    }
  }

  @Test
  void explodedWarShouldPreserveClasspathOrderWhenIndexPresent() throws Exception {
    File explodedRoot = explode(createJarArchive("archive.war", "WEB-INF", true, Collections.emptyList()));
    WarLauncher launcher = new WarLauncher(new ExplodedArchive(explodedRoot, true));
    Iterator<Archive> archives = launcher.getClassPathArchivesIterator();
    URLClassLoader classLoader = (URLClassLoader) launcher.createClassLoader(archives);
    URL[] urls = classLoader.getURLs();
    assertThat(urls).containsExactly(getExpectedFileUrls(explodedRoot));
  }

  @Test
  void warFilesPresentInWebInfLibsAndNotInClasspathIndexShouldBeAddedAfterWebInfClasses() throws Exception {
    ArrayList<String> extraLibs = new ArrayList<>(Arrays.asList("extra-1.jar", "extra-2.jar"));
    File explodedRoot = explode(createJarArchive("archive.war", "WEB-INF", true, extraLibs));
    WarLauncher launcher = new WarLauncher(new ExplodedArchive(explodedRoot, true));
    Iterator<Archive> archives = launcher.getClassPathArchivesIterator();
    URLClassLoader classLoader = (URLClassLoader) launcher.createClassLoader(archives);
    URL[] urls = classLoader.getURLs();
    List<File> expectedFiles = getExpectedFilesWithExtraLibs(explodedRoot);
    URL[] expectedFileUrls = expectedFiles.stream().map(this::toUrl).toArray(URL[]::new);
    assertThat(urls).containsExactly(expectedFileUrls);
  }

  protected final URL[] getExpectedFileUrls(File explodedRoot) {
    return getExpectedFiles(explodedRoot).stream().map(this::toUrl).toArray(URL[]::new);
  }

  protected final List<File> getExpectedFiles(File parent) {
    List<File> expected = new ArrayList<>();
    expected.add(new File(parent, "WEB-INF/classes"));
    expected.add(new File(parent, "WEB-INF/lib/foo.jar"));
    expected.add(new File(parent, "WEB-INF/lib/bar.jar"));
    expected.add(new File(parent, "WEB-INF/lib/baz.jar"));
    return expected;
  }

  protected final List<File> getExpectedFilesWithExtraLibs(File parent) {
    List<File> expected = new ArrayList<>();
    expected.add(new File(parent, "WEB-INF/classes"));
    expected.add(new File(parent, "WEB-INF/lib/extra-1.jar"));
    expected.add(new File(parent, "WEB-INF/lib/extra-2.jar"));
    expected.add(new File(parent, "WEB-INF/lib/foo.jar"));
    expected.add(new File(parent, "WEB-INF/lib/bar.jar"));
    expected.add(new File(parent, "WEB-INF/lib/baz.jar"));
    return expected;
  }

}
