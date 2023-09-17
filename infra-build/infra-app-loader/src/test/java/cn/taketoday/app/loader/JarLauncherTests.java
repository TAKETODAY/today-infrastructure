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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import cn.taketoday.app.loader.archive.Archive;
import cn.taketoday.app.loader.archive.ExplodedArchive;
import cn.taketoday.app.loader.archive.JarFileArchive;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.test.tools.SourceFile;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarLauncher}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class JarLauncherTests extends AbstractExecutableArchiveLauncherTests {

  @Test
  void explodedJarHasOnlyBootInfClassesAndContentsOfBootInfLibOnClasspath() throws Exception {
    File explodedRoot = explode(createJarArchive("archive.jar", "APP-INF"));
    JarLauncher launcher = new JarLauncher(new ExplodedArchive(explodedRoot, true));
    List<Archive> archives = new ArrayList<>();
    launcher.getClassPathArchivesIterator().forEachRemaining(archives::add);
    assertThat(getUrls(archives)).containsExactlyInAnyOrder(getExpectedFileUrls(explodedRoot));
    for (Archive archive : archives) {
      archive.close();
    }
  }

  @Test
  void archivedJarHasOnlyBootInfClassesAndContentsOfBootInfLibOnClasspath() throws Exception {
    File jarRoot = createJarArchive("archive.jar", "APP-INF");
    try (JarFileArchive archive = new JarFileArchive(jarRoot)) {
      JarLauncher launcher = new JarLauncher(archive);
      List<Archive> classPathArchives = new ArrayList<>();
      launcher.getClassPathArchivesIterator().forEachRemaining(classPathArchives::add);
      assertThat(classPathArchives).hasSize(4);
      assertThat(getUrls(classPathArchives)).containsOnly(
              new URL("jar:" + jarRoot.toURI().toURL() + "!/APP-INF/classes!/"),
              new URL("jar:" + jarRoot.toURI().toURL() + "!/APP-INF/lib/foo.jar!/"),
              new URL("jar:" + jarRoot.toURI().toURL() + "!/APP-INF/lib/bar.jar!/"),
              new URL("jar:" + jarRoot.toURI().toURL() + "!/APP-INF/lib/baz.jar!/"));
      for (Archive classPathArchive : classPathArchives) {
        classPathArchive.close();
      }
    }
  }

  @Test
  void explodedJarShouldPreserveClasspathOrderWhenIndexPresent() throws Exception {
    File explodedRoot = explode(createJarArchive("archive.jar", "APP-INF", true, Collections.emptyList()));
    JarLauncher launcher = new JarLauncher(new ExplodedArchive(explodedRoot, true));
    Iterator<Archive> archives = launcher.getClassPathArchivesIterator();
    URLClassLoader classLoader = (URLClassLoader) launcher.createClassLoader(archives);
    URL[] urls = classLoader.getURLs();
    assertThat(urls).containsExactly(getExpectedFileUrls(explodedRoot));
  }

  @Test
  void jarFilesPresentInBootInfLibsAndNotInClasspathIndexShouldBeAddedAfterBootInfClasses() throws Exception {
    ArrayList<String> extraLibs = new ArrayList<>(Arrays.asList("extra-1.jar", "extra-2.jar"));
    File explodedRoot = explode(createJarArchive("archive.jar", "APP-INF", true, extraLibs));
    JarLauncher launcher = new JarLauncher(new ExplodedArchive(explodedRoot, true));
    Iterator<Archive> archives = launcher.getClassPathArchivesIterator();
    URLClassLoader classLoader = (URLClassLoader) launcher.createClassLoader(archives);
    URL[] urls = classLoader.getURLs();
    List<File> expectedFiles = getExpectedFilesWithExtraLibs(explodedRoot);
    URL[] expectedFileUrls = expectedFiles.stream().map(this::toUrl).toArray(URL[]::new);
    assertThat(urls).containsExactly(expectedFileUrls);
  }

  @Test
  void explodedJarDefinedPackagesIncludeManifestAttributes() {
    Manifest manifest = new Manifest();
    Attributes attributes = manifest.getMainAttributes();
    attributes.put(Name.MANIFEST_VERSION, "1.0");
    attributes.put(Name.IMPLEMENTATION_TITLE, "test");
    SourceFile sourceFile = SourceFile.of("explodedsample/ExampleClass.java",
            new ClassPathResource("explodedsample/ExampleClass.txt"));
    TestCompiler.forSystem().compile(sourceFile, ThrowingConsumer.of((compiled) -> {
      File explodedRoot = explode(
              createJarArchive("archive.jar", manifest, "APP-INF", true, Collections.emptyList()));
      File target = new File(explodedRoot, "APP-INF/classes/explodedsample/ExampleClass.class");
      target.getParentFile().mkdirs();
      FileCopyUtils.copy(compiled.getClassLoader().getResourceAsStream("explodedsample/ExampleClass.class"),
              new FileOutputStream(target));
      JarLauncher launcher = new JarLauncher(new ExplodedArchive(explodedRoot, true));
      Iterator<Archive> archives = launcher.getClassPathArchivesIterator();
      URLClassLoader classLoader = (URLClassLoader) launcher.createClassLoader(archives);
      Class<?> loaded = classLoader.loadClass("explodedsample.ExampleClass");
      assertThat(loaded.getPackage().getImplementationTitle()).isEqualTo("test");
    }));
  }

  protected final URL[] getExpectedFileUrls(File explodedRoot) {
    return getExpectedFiles(explodedRoot).stream().map(this::toUrl).toArray(URL[]::new);
  }

  protected final List<File> getExpectedFiles(File parent) {
    List<File> expected = new ArrayList<>();
    expected.add(new File(parent, "APP-INF/classes"));
    expected.add(new File(parent, "APP-INF/lib/foo.jar"));
    expected.add(new File(parent, "APP-INF/lib/bar.jar"));
    expected.add(new File(parent, "APP-INF/lib/baz.jar"));
    return expected;
  }

  protected final List<File> getExpectedFilesWithExtraLibs(File parent) {
    List<File> expected = new ArrayList<>();
    expected.add(new File(parent, "APP-INF/classes"));
    expected.add(new File(parent, "APP-INF/lib/extra-1.jar"));
    expected.add(new File(parent, "APP-INF/lib/extra-2.jar"));
    expected.add(new File(parent, "APP-INF/lib/foo.jar"));
    expected.add(new File(parent, "APP-INF/lib/bar.jar"));
    expected.add(new File(parent, "APP-INF/lib/baz.jar"));
    return expected;
  }

}
