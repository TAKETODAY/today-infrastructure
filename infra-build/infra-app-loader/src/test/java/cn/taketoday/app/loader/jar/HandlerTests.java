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

package cn.taketoday.app.loader.jar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import cn.taketoday.app.loader.TestJarCreator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Handler}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(JarUrlProtocolHandler.class)
class HandlerTests {

  private final Handler handler = new Handler();

  @Test
  void parseUrlWithJarRootContextAndAbsoluteSpecThatUsesContext() throws MalformedURLException {
    String spec = "/entry.txt";
    URL context = createUrl("file:example.jar!/");
    this.handler.parseURL(context, spec, 0, spec.length());
    assertThat(context.toExternalForm()).isEqualTo("jar:file:example.jar!/entry.txt");
  }

  @Test
  void parseUrlWithDirectoryEntryContextAndAbsoluteSpecThatUsesContext() throws MalformedURLException {
    String spec = "/entry.txt";
    URL context = createUrl("file:example.jar!/dir/");
    this.handler.parseURL(context, spec, 0, spec.length());
    assertThat(context.toExternalForm()).isEqualTo("jar:file:example.jar!/entry.txt");
  }

  @Test
  void parseUrlWithJarRootContextAndRelativeSpecThatUsesContext() throws MalformedURLException {
    String spec = "entry.txt";
    URL context = createUrl("file:example.jar!/");
    this.handler.parseURL(context, spec, 0, spec.length());
    assertThat(context.toExternalForm()).isEqualTo("jar:file:example.jar!/entry.txt");
  }

  @Test
  void parseUrlWithDirectoryEntryContextAndRelativeSpecThatUsesContext() throws MalformedURLException {
    String spec = "entry.txt";
    URL context = createUrl("file:example.jar!/dir/");
    this.handler.parseURL(context, spec, 0, spec.length());
    assertThat(context.toExternalForm()).isEqualTo("jar:file:example.jar!/dir/entry.txt");
  }

  @Test
  void parseUrlWithFileEntryContextAndRelativeSpecThatUsesContext() throws MalformedURLException {
    String spec = "entry.txt";
    URL context = createUrl("file:example.jar!/dir/file");
    this.handler.parseURL(context, spec, 0, spec.length());
    assertThat(context.toExternalForm()).isEqualTo("jar:file:example.jar!/dir/entry.txt");
  }

  @Test
  void parseUrlWithSpecThatIgnoresContext() throws MalformedURLException {
    JarFile.registerUrlProtocolHandler();
    String spec = "jar:file:/other.jar!/nested!/entry.txt";
    URL context = createUrl("file:example.jar!/dir/file");
    this.handler.parseURL(context, spec, 0, spec.length());
    assertThat(context.toExternalForm()).isEqualTo("jar:jar:file:/other.jar!/nested!/entry.txt");
  }

  @Test
  void sameFileReturnsFalseForUrlsWithDifferentProtocols() throws MalformedURLException {
    assertThat(this.handler.sameFile(new URL("jar:file:foo.jar!/content.txt"), new URL("file:/foo.jar"))).isFalse();
  }

  @Test
  void sameFileReturnsFalseForDifferentFileInSameJar() throws MalformedURLException {
    assertThat(this.handler.sameFile(new URL("jar:file:foo.jar!/the/path/to/the/first/content.txt"),
            new URL("jar:file:/foo.jar!/content.txt")))
            .isFalse();
  }

  @Test
  void sameFileReturnsFalseForSameFileInDifferentJars() throws MalformedURLException {
    assertThat(this.handler.sameFile(new URL("jar:file:/the/path/to/the/first.jar!/content.txt"),
            new URL("jar:file:/second.jar!/content.txt")))
            .isFalse();
  }

  @Test
  void sameFileReturnsTrueForSameFileInSameJar() throws MalformedURLException {
    assertThat(this.handler.sameFile(new URL("jar:file:/the/path/to/the/first.jar!/content.txt"),
            new URL("jar:file:/the/path/to/the/first.jar!/content.txt")))
            .isTrue();
  }

  @Test
  void sameFileReturnsTrueForUrlsThatReferenceSameFileViaNestedArchiveAndFromRootOfJar()
          throws MalformedURLException {
    assertThat(this.handler.sameFile(new URL("jar:file:/test.jar!/APP-INF/classes!/foo.txt"),
            new URL("jar:file:/test.jar!/APP-INF/classes/foo.txt")))
            .isTrue();
  }

  @Test
  void hashCodesAreEqualForUrlsThatReferenceSameFileViaNestedArchiveAndFromRootOfJar() throws MalformedURLException {
    assertThat(this.handler.hashCode(new URL("jar:file:/test.jar!/APP-INF/classes!/foo.txt")))
            .isEqualTo(this.handler.hashCode(new URL("jar:file:/test.jar!/APP-INF/classes/foo.txt")));
  }

  @Test
  void urlWithSpecReferencingParentDirectory() throws MalformedURLException {
    assertStandardAndCustomHandlerUrlsAreEqual("file:/test.jar!/APP-INF/classes!/xsd/directoryA/a.xsd",
            "../directoryB/c/d/e.xsd");
  }

  @Test
  void urlWithSpecReferencingAncestorDirectoryOutsideJarStopsAtJarRoot() throws MalformedURLException {
    assertStandardAndCustomHandlerUrlsAreEqual("file:/test.jar!/APP-INF/classes!/xsd/directoryA/a.xsd",
            "../../../../../../directoryB/b.xsd");
  }

  @Test
  void urlWithSpecReferencingCurrentDirectory() throws MalformedURLException {
    assertStandardAndCustomHandlerUrlsAreEqual("file:/test.jar!/APP-INF/classes!/xsd/directoryA/a.xsd",
            "./directoryB/c/d/e.xsd");
  }

  @Test
  void urlWithRef() throws MalformedURLException {
    assertStandardAndCustomHandlerUrlsAreEqual("file:/test.jar!/APP-INF/classes", "!/foo.txt#alpha");
  }

  @Test
  void urlWithQuery() throws MalformedURLException {
    assertStandardAndCustomHandlerUrlsAreEqual("file:/test.jar!/APP-INF/classes", "!/foo.txt?alpha");
  }

  @Test
  void fallbackToJdksJarUrlStreamHandler(@TempDir File tempDir) throws Exception {
    File testJar = new File(tempDir, "test.jar");
    TestJarCreator.createTestJar(testJar);
    URLConnection connection = new URL(null, "jar:" + testJar.toURI().toURL() + "!/nested.jar!/", this.handler)
            .openConnection();
    assertThat(connection).isInstanceOf(JarURLConnection.class);
    ((JarURLConnection) connection).getJarFile().close();
    URLConnection jdkConnection = new URL(null, "jar:file:" + testJar.toURI().toURL() + "!/nested.jar!/",
            this.handler)
            .openConnection();
    assertThat(jdkConnection).isNotInstanceOf(JarURLConnection.class);
    assertThat(jdkConnection.getClass().getName()).endsWith(".JarURLConnection");
  }

  @Test
  void whenJarHasAPlusInItsPathConnectionJarFileMatchesOriginalJarFile(@TempDir File tempDir) throws Exception {
    File testJar = new File(tempDir, "t+e+s+t.jar");
    TestJarCreator.createTestJar(testJar);
    URL url = new URL(null, "jar:" + testJar.toURI().toURL() + "!/nested.jar!/3.dat", this.handler);
    JarURLConnection connection = (JarURLConnection) url.openConnection();
    try (JarFile jarFile = JarFileWrapper.unwrap(connection.getJarFile())) {
      assertThat(jarFile.getRootJarFile().getFile()).isEqualTo(testJar);
    }
  }

  @Test
  void whenJarHasASpaceInItsPathConnectionJarFileMatchesOriginalJarFile(@TempDir File tempDir) throws Exception {
    File testJar = new File(tempDir, "t e s t.jar");
    TestJarCreator.createTestJar(testJar);
    URL url = new URL(null, "jar:" + testJar.toURI().toURL() + "!/nested.jar!/3.dat", this.handler);
    JarURLConnection connection = (JarURLConnection) url.openConnection();
    try (JarFile jarFile = JarFileWrapper.unwrap(connection.getJarFile())) {
      assertThat(jarFile.getRootJarFile().getFile()).isEqualTo(testJar);
    }
  }

  private void assertStandardAndCustomHandlerUrlsAreEqual(String context, String spec) throws MalformedURLException {
    URL standardUrl = new URL(new URL("jar:" + context), spec);
    URL customHandlerUrl = new URL(new URL("jar", null, -1, context, this.handler), spec);
    assertThat(customHandlerUrl).hasToString(standardUrl.toString());
    assertThat(customHandlerUrl.getFile()).isEqualTo(standardUrl.getFile());
    assertThat(customHandlerUrl.getPath()).isEqualTo(standardUrl.getPath());
    assertThat(customHandlerUrl.getQuery()).isEqualTo(standardUrl.getQuery());
    assertThat(customHandlerUrl.getRef()).isEqualTo(standardUrl.getRef());
  }

  private URL createUrl(String file) throws MalformedURLException {
    return new URL("jar", null, -1, file, this.handler);
  }

}
