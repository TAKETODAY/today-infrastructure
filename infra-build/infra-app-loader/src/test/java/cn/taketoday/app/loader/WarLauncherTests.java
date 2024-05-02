/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.app.loader;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.app.loader.net.protocol.jar.JarUrl;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WarLauncher}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class WarLauncherTests extends AbstractExecutableArchiveLauncherTests {

  @Test
  void explodedWarHasOnlyWebInfClassesAndContentsOfWebInfLibOnClasspath() throws Exception {
    File explodedRoot = explode(createJarArchive("archive.war", "WEB-INF"));
    WarLauncher launcher = new WarLauncher(new ExplodedArchive(explodedRoot));
    Set<URL> urls = launcher.getClassPathUrls();
    assertThat(urls).containsExactlyInAnyOrder(getExpectedFileUrls(explodedRoot));
  }

  @Test
  void archivedWarHasOnlyWebInfClassesAndContentsOfWebInfLibOnClasspath() throws Exception {
    File file = createJarArchive("archive.war", "WEB-INF");
    try (JarFileArchive archive = new JarFileArchive(file)) {
      WarLauncher launcher = new WarLauncher(archive);
      Set<URL> urls = launcher.getClassPathUrls();
      List<URL> expected = new ArrayList<>();
      expected.add(JarUrl.create(file, "WEB-INF/classes/"));
      expected.add(JarUrl.create(file, "WEB-INF/lib/foo.jar"));
      expected.add(JarUrl.create(file, "WEB-INF/lib/bar.jar"));
      expected.add(JarUrl.create(file, "WEB-INF/lib/baz.jar"));
      assertThat(urls).containsOnly(expected.toArray(URL[]::new));
    }
  }

  @Test
  void explodedWarShouldPreserveClasspathOrderWhenIndexPresent() throws Exception {
    File explodedRoot = explode(createJarArchive("archive.war", "WEB-INF", true, Collections.emptyList()));
    WarLauncher launcher = new WarLauncher(new ExplodedArchive(explodedRoot));
    URLClassLoader classLoader = createClassLoader(launcher);
    URL[] urls = classLoader.getURLs();
    assertThat(urls).containsExactly(getExpectedFileUrls(explodedRoot));
  }

  @Test
  void warFilesPresentInWebInfLibsAndNotInClasspathIndexShouldBeAddedAfterWebInfClasses() throws Exception {
    ArrayList<String> extraLibs = new ArrayList<>(Arrays.asList("extra-1.jar", "extra-2.jar"));
    File explodedRoot = explode(createJarArchive("archive.war", "WEB-INF", true, extraLibs));
    WarLauncher launcher = new WarLauncher(new ExplodedArchive(explodedRoot));
    URLClassLoader classLoader = createClassLoader(launcher);
    URL[] urls = classLoader.getURLs();
    List<File> expectedFiles = getExpectedFilesWithExtraLibs(explodedRoot);
    URL[] expectedFileUrls = expectedFiles.stream().map(this::toUrl).toArray(URL[]::new);
    assertThat(urls).containsExactly(expectedFileUrls);
  }

  private URLClassLoader createClassLoader(Launcher launcher) throws Exception {
    return (URLClassLoader) launcher.createClassLoader(launcher.getClassPathUrls());
  }

  private URL[] getExpectedFileUrls(File explodedRoot) {
    return getExpectedFiles(explodedRoot).stream().map(this::toUrl).toArray(URL[]::new);
  }

  private List<File> getExpectedFiles(File parent) {
    List<File> expected = new ArrayList<>();
    expected.add(new File(parent, "WEB-INF/classes"));
    expected.add(new File(parent, "WEB-INF/lib/foo.jar"));
    expected.add(new File(parent, "WEB-INF/lib/bar.jar"));
    expected.add(new File(parent, "WEB-INF/lib/baz.jar"));
    return expected;
  }

  private List<File> getExpectedFilesWithExtraLibs(File parent) {
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
