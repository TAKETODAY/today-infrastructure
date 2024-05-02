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
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import cn.taketoday.app.loader.net.protocol.jar.JarUrl;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;
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
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class JarLauncherTests extends AbstractExecutableArchiveLauncherTests {

  @Test
  void explodedJarHasOnlyBootInfClassesAndContentsOfBootInfLibOnClasspath() throws Exception {
    File explodedRoot = explode(createJarArchive("archive.jar", "APP-INF"));
    JarLauncher launcher = new JarLauncher(new ExplodedArchive(explodedRoot));
    Set<URL> urls = launcher.getClassPathUrls();
    assertThat(urls).containsExactlyInAnyOrder(getExpectedFileUrls(explodedRoot));
  }

  @Test
  void archivedJarHasOnlyBootInfClassesAndContentsOfBootInfLibOnClasspath() throws Exception {
    File jarRoot = createJarArchive("archive.jar", "APP-INF");
    try (JarFileArchive archive = new JarFileArchive(jarRoot)) {
      JarLauncher launcher = new JarLauncher(archive);
      Set<URL> urls = launcher.getClassPathUrls();
      List<URL> expectedUrls = new ArrayList<>();
      expectedUrls.add(JarUrl.create(jarRoot, "APP-INF/classes/"));
      expectedUrls.add(JarUrl.create(jarRoot, "APP-INF/lib/foo.jar"));
      expectedUrls.add(JarUrl.create(jarRoot, "APP-INF/lib/bar.jar"));
      expectedUrls.add(JarUrl.create(jarRoot, "APP-INF/lib/baz.jar"));
      assertThat(urls).containsOnlyOnceElementsOf(expectedUrls);
    }
  }

  @Test
  void explodedJarShouldPreserveClasspathOrderWhenIndexPresent() throws Exception {
    File explodedRoot = explode(createJarArchive("archive.jar", "APP-INF", true, Collections.emptyList()));
    JarLauncher launcher = new JarLauncher(new ExplodedArchive(explodedRoot));
    URLClassLoader classLoader = createClassLoader(launcher);
    assertThat(classLoader.getURLs()).containsExactly(getExpectedFileUrls(explodedRoot));
  }

  @Test
  void jarFilesPresentInBootInfLibsAndNotInClasspathIndexShouldBeAddedAfterBootInfClasses() throws Exception {
    ArrayList<String> extraLibs = new ArrayList<>(Arrays.asList("extra-1.jar", "extra-2.jar"));
    File explodedRoot = explode(createJarArchive("archive.jar", "APP-INF", true, extraLibs));
    JarLauncher launcher = new JarLauncher(new ExplodedArchive(explodedRoot));
    URLClassLoader classLoader = createClassLoader(launcher);
    List<File> expectedFiles = getExpectedFilesWithExtraLibs(explodedRoot);
    URL[] expectedFileUrls = expectedFiles.stream().map(this::toUrl).toArray(URL[]::new);
    assertThat(classLoader.getURLs()).containsExactly(expectedFileUrls);
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
      JarLauncher launcher = new JarLauncher(new ExplodedArchive(explodedRoot));
      URLClassLoader classLoader = createClassLoader(launcher);
      Class<?> loaded = classLoader.loadClass("explodedsample.ExampleClass");
      assertThat(loaded.getPackage().getImplementationTitle()).isEqualTo("test");
    }));
  }

  private URLClassLoader createClassLoader(JarLauncher launcher) throws Exception {
    return (URLClassLoader) launcher.createClassLoader(launcher.getClassPathUrls());
  }

  private URL[] getExpectedFileUrls(File explodedRoot) {
    return getExpectedFiles(explodedRoot).stream().map(this::toUrl).toArray(URL[]::new);
  }

  private List<File> getExpectedFiles(File parent) {
    List<File> expected = new ArrayList<>();
    expected.add(new File(parent, "APP-INF/classes"));
    expected.add(new File(parent, "APP-INF/lib/foo.jar"));
    expected.add(new File(parent, "APP-INF/lib/bar.jar"));
    expected.add(new File(parent, "APP-INF/lib/baz.jar"));
    return expected;
  }

  private List<File> getExpectedFilesWithExtraLibs(File parent) {
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
