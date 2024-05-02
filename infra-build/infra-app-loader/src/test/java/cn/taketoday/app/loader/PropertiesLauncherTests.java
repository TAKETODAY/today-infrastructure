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

import org.assertj.core.api.Condition;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import cn.taketoday.app.loader.net.protocol.jar.JarUrl;
import cn.taketoday.app.loader.testsupport.TestJar;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;
import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.util.ReflectionTestUtils;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests for {@link PropertiesLauncher}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
@AssertFileChannelDataBlocksClosed
class PropertiesLauncherTests {

  @TempDir
  File tempDir;

  private PropertiesLauncher launcher;

  private ClassLoader contextClassLoader;

  private CapturedOutput output;

  @BeforeEach
  void setup(CapturedOutput capturedOutput) {
    this.contextClassLoader = Thread.currentThread().getContextClassLoader();
    System.setProperty("loader.home", new File("src/test/resources").getAbsolutePath());
    this.output = capturedOutput;
  }

  @AfterEach
  void close() throws Exception {
    Thread.currentThread().setContextClassLoader(this.contextClassLoader);
    System.clearProperty("loader.home");
    System.clearProperty("loader.path");
    System.clearProperty("loader.main");
    System.clearProperty("loader.config.name");
    System.clearProperty("loader.config.location");
    System.clearProperty("loader.system");
    System.clearProperty("loader.classLoader");
    if (this.launcher != null) {
      this.launcher.close();
    }
  }

  @Test
  void testDefaultHome() throws Exception {
    System.clearProperty("loader.home");
    this.launcher = new PropertiesLauncher();
    assertThat(this.launcher.getHomeDirectory()).isEqualTo(new File(System.getProperty("user.dir")));
  }

  @Test
  void testAlternateHome() throws Exception {
    System.setProperty("loader.home", "src/test/resources/home");
    this.launcher = new PropertiesLauncher();
    assertThat(this.launcher.getHomeDirectory()).isEqualTo(new File(System.getProperty("loader.home")));
    assertThat(this.launcher.getMainClass()).isEqualTo("demo.HomeApplication");
  }

  @Test
  void testNonExistentHome() {
    System.setProperty("loader.home", "src/test/resources/nonexistent");
    assertThatIllegalArgumentException().isThrownBy(PropertiesLauncher::new)
            .withMessageContaining("Invalid source directory");
  }

  @Test
  void testUserSpecifiedMain() throws Exception {
    this.launcher = new PropertiesLauncher();
    assertThat(this.launcher.getMainClass()).isEqualTo("demo.Application");
    assertThat(System.getProperty("loader.main")).isNull();
  }

  @Test
  void testUserSpecifiedConfigName() throws Exception {
    System.setProperty("loader.config.name", "foo");
    this.launcher = new PropertiesLauncher();
    assertThat(this.launcher.getMainClass()).isEqualTo("my.Application");
    assertThat(paths()).hasToString("[etc/]");
  }

  @Test
  void testRootOfClasspathFirst() throws Exception {
    System.setProperty("loader.config.name", "bar");
    this.launcher = new PropertiesLauncher();
    assertThat(this.launcher.getMainClass()).isEqualTo("my.BarApplication");
  }

  @Test
  void testUserSpecifiedDotPath() throws Exception {
    System.setProperty("loader.path", ".");
    this.launcher = new PropertiesLauncher();
    assertThat(paths()).hasToString("[.]");
  }

  @Nullable
  private Object paths() {
    return ReflectionTestUtils.getField(this.launcher, "paths");
  }

  @Test
  void testUserSpecifiedSlashPath() throws Exception {
    System.setProperty("loader.path", "jars/");
    this.launcher = new PropertiesLauncher();
    assertThat(paths()).hasToString("[jars/]");
    Set<URL> urls = this.launcher.getClassPathUrls();
    assertThat(urls).areExactly(1, endingWith("app.jar"));
  }

  @Test
  void testUserSpecifiedWildcardPath() throws Exception {
    System.setProperty("loader.path", "jars/*");
    System.setProperty("loader.main", "demo.Application");
    this.launcher = new PropertiesLauncher();
    assertThat(paths()).hasToString("[jars/]");
    this.launcher.launch(new String[0]);
    waitFor("Hello World");
  }

  @Test
  void testUserSpecifiedJarPath() throws Exception {
    System.setProperty("loader.path", "jars/app.jar");
    System.setProperty("loader.main", "demo.Application");
    this.launcher = new PropertiesLauncher();
    assertThat(paths()).hasToString("[jars/app.jar]");
    this.launcher.launch(new String[0]);
    waitFor("Hello World");
  }

  @Test
  void testUserSpecifiedRootOfJarPath() throws Exception {
    System.setProperty("loader.path", "jar:file:./src/test/resources/nested-jars/app.jar!/");
    this.launcher = new PropertiesLauncher();
    assertThat(paths())
            .hasToString("[jar:file:./src/test/resources/nested-jars/app.jar!/]");
    Set<URL> urls = this.launcher.getClassPathUrls();
    assertThat(urls).areExactly(1, endingWith("foo.jar!/"));
    assertThat(urls).areExactly(1, endingWith("app.jar!/"));
  }

  @Test
  void testUserSpecifiedRootOfJarPathWithDot() throws Exception {
    System.setProperty("loader.path", "nested-jars/app.jar!/./");
    this.launcher = new PropertiesLauncher();
    Set<URL> urls = this.launcher.getClassPathUrls();
    assertThat(urls).areExactly(1, endingWith("foo.jar!/"));
    assertThat(urls).areExactly(1, endingWith("app.jar!/"));
  }

  @Test
  void testUserSpecifiedRootOfJarPathWithDotAndJarPrefix() throws Exception {
    System.setProperty("loader.path", "jar:file:./src/test/resources/nested-jars/app.jar!/./");
    this.launcher = new PropertiesLauncher();
    Set<URL> urls = this.launcher.getClassPathUrls();
    assertThat(urls).areExactly(1, endingWith("foo.jar!/"));
  }

  @Test
  void testUserSpecifiedJarFileWithNestedArchives() throws Exception {
    System.setProperty("loader.path", "nested-jars/app.jar");
    System.setProperty("loader.main", "demo.Application");
    this.launcher = new PropertiesLauncher();
    Set<URL> urls = this.launcher.getClassPathUrls();
    assertThat(urls).areExactly(1, endingWith("foo.jar!/"));
    assertThat(urls).areExactly(1, endingWith("app.jar"));
  }

  @Test
  void testUserSpecifiedNestedJarPath() throws Exception {
    System.setProperty("loader.path", "nested-jars/nested-jar-app.jar!/BOOT-INF/classes/");
    System.setProperty("loader.main", "demo.Application");
    this.launcher = new PropertiesLauncher();
    assertThat(paths()).hasToString("[nested-jars/nested-jar-app.jar!/BOOT-INF/classes/]");
    this.launcher.launch(new String[0]);
    waitFor("Hello World");
  }

  @Test
  void testUserSpecifiedDirectoryContainingJarFileWithNestedArchives() throws Exception {
    System.setProperty("loader.path", "nested-jars");
    System.setProperty("loader.main", "demo.Application");
    this.launcher = new PropertiesLauncher();
    this.launcher.launch(new String[0]);
    waitFor("Hello World");
  }

  @Test
  void testUserSpecifiedJarPathWithDot() throws Exception {
    System.setProperty("loader.path", "./jars/app.jar");
    System.setProperty("loader.main", "demo.Application");
    this.launcher = new PropertiesLauncher();
    assertThat(paths()).hasToString("[jars/app.jar]");
    this.launcher.launch(new String[0]);
    waitFor("Hello World");
  }

  @Test
  void testUserSpecifiedClassLoader() throws Exception {
    System.setProperty("loader.path", "jars/app.jar");
    System.setProperty("loader.classLoader", URLClassLoader.class.getName());
    this.launcher = new PropertiesLauncher();
    assertThat(paths()).hasToString("[jars/app.jar]");
    this.launcher.launch(new String[0]);
    waitFor("Hello World");
  }

  @Test
  void testUserSpecifiedClassPathOrder() throws Exception {
    System.setProperty("loader.path", "more-jars/app.jar,jars/app.jar");
    System.setProperty("loader.classLoader", URLClassLoader.class.getName());
    this.launcher = new PropertiesLauncher();
    assertThat(paths())
            .hasToString("[more-jars/app.jar, jars/app.jar]");
    this.launcher.launch(new String[0]);
    waitFor("Hello Other World");
  }

  @Test
  void testCustomClassLoaderCreation() throws Exception {
    System.setProperty("loader.classLoader", TestLoader.class.getName());
    this.launcher = new PropertiesLauncher();
    ClassLoader loader = this.launcher.createClassLoader(classPathUrls());
    assertThat(loader).isNotNull();
    assertThat(loader.getClass().getName()).isEqualTo(TestLoader.class.getName());
  }

  private Set<URL> classPathUrls() throws Exception {
    Set<URL> urls = new LinkedHashSet<>();
    String classPath = System.getProperty("java.class.path");
    for (String path : classPath.split(File.pathSeparator)) {
      File file = new FileSystemResource(path).getFile();
      if (file.exists()) {
        urls.add(file.toURI().toURL());
      }
    }
    return urls;
  }

  @Test
  void testUserSpecifiedConfigPathWins() throws Exception {
    System.setProperty("loader.config.name", "foo");
    System.setProperty("loader.config.location", "classpath:bar.properties");
    this.launcher = new PropertiesLauncher();
    assertThat(this.launcher.getMainClass()).isEqualTo("my.BarApplication");
  }

  @Test
  void testSystemPropertySpecifiedMain() throws Exception {
    System.setProperty("loader.main", "foo.Bar");
    this.launcher = new PropertiesLauncher();
    assertThat(this.launcher.getMainClass()).isEqualTo("foo.Bar");
  }

  @Test
  void testSystemPropertiesSet() throws Exception {
    System.setProperty("loader.system", "true");
    new PropertiesLauncher();
    assertThat(System.getProperty("loader.main")).isEqualTo("demo.Application");
  }

  @Test
  void testArgsEnhanced() throws Exception {
    System.setProperty("loader.args", "foo");
    this.launcher = new PropertiesLauncher();
    assertThat(Arrays.asList(this.launcher.getArgs("bar"))).hasToString("[foo, bar]");
  }

  @Test
  @SuppressWarnings("unchecked")
  void testLoadPathCustomizedUsingManifest() throws Exception {
    System.setProperty("loader.home", this.tempDir.getAbsolutePath());
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().putValue("Loader-Path", "/foo.jar, /bar");
    File manifestFile = new File(this.tempDir, "META-INF/MANIFEST.MF");
    manifestFile.getParentFile().mkdirs();
    try (FileOutputStream manifestStream = new FileOutputStream(manifestFile)) {
      manifest.write(manifestStream);
    }
    this.launcher = new PropertiesLauncher();
    assertThat((List<String>) ReflectionTestUtils.getField(this.launcher, "paths")).containsExactly("/foo.jar",
            "/bar/");
  }

  @Test
  void testManifestWithPlaceholders() throws Exception {
    System.setProperty("loader.home", "src/test/resources/placeholders");
    this.launcher = new PropertiesLauncher();
    assertThat(this.launcher.getMainClass()).isEqualTo("demo.FooApplication");
  }

  @Test
  void encodedFileUrlLoaderPathIsHandledCorrectly() throws Exception {
    File loaderPath = new File(this.tempDir, "loader path");
    loaderPath.mkdir();
    System.setProperty("loader.path", loaderPath.toURI().toURL().toString());
    this.launcher = new PropertiesLauncher();
    Set<URL> urls = this.launcher.getClassPathUrls();
    assertThat(urls).hasSize(1);
    assertThat(urls.iterator().next()).isEqualTo(loaderPath.toURI().toURL());
  }

  @Test
    // gh-21575
  void loadResourceFromJarFile() throws Exception {
    File file = new File(this.tempDir, "app.jar");
    TestJar.create(file);
    System.setProperty("loader.home", this.tempDir.getAbsolutePath());
    System.setProperty("loader.path", "app.jar");
    this.launcher = new PropertiesLauncher();
    try {
      this.launcher.launch(new String[0]);
    }
    catch (Exception ex) {
      // Expected ClassNotFoundException
      LaunchedClassLoader classLoader = (LaunchedClassLoader) Thread.currentThread().getContextClassLoader();
      classLoader.close();
    }
    URL resource = JarUrl.create(file, "nested.jar", "3.dat");
    byte[] bytes = FileCopyUtils.copyToByteArray(resource.openStream());
    assertThat(bytes).isNotEmpty();
  }

  @Test
    // gh-37992
  void classPathWithoutLoaderPathDefaultsToJarLauncherIncludes() throws Exception {
    File file = new File(this.tempDir, "test.jar");
    try (JarOutputStream out = new JarOutputStream(new FileOutputStream(file))) {
      try (JarFile in = new JarFile(new File("src/test/resources/jars/app.jar"))) {
        out.putNextEntry(new ZipEntry("APP-INF/"));
        out.putNextEntry(new ZipEntry("APP-INF/classes/"));
        out.putNextEntry(new ZipEntry("APP-INF/classes/demo/"));
        out.putNextEntry(new ZipEntry("APP-INF/classes/demo/Application.class"));
        try (InputStream classIn = in.getInputStream(in.getEntry("demo/Application.class"))) {
          classIn.transferTo(out);
        }
        out.closeEntry();
      }
    }
    Archive archive = new JarFileArchive(file);
    System.setProperty("loader.main", "demo.Application");
    this.launcher = new PropertiesLauncher(archive);
    this.launcher.launch(new String[0]);
    waitFor("Hello World");

  }

  private void waitFor(String value) {
    Awaitility.waitAtMost(Duration.ofSeconds(5)).until(this.output::toString, containsString(value));
  }

  private Condition<URL> endingWith(String value) {
    return new Condition<>() {

      @Override
      public boolean matches(URL archive) {
        return archive.toString().endsWith(value);
      }

    };
  }

  static class TestLoader extends URLClassLoader {

    TestLoader(ClassLoader parent) {
      super(new URL[0], parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      return super.findClass(name);
    }

  }

}
