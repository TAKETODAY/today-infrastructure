/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.InvalidPathException;

import infra.core.io.ClassPathResource;
import infra.core.io.FileSystemResource;
import infra.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author TODAY <br>
 * 2019-05-15 14:04
 */
class ResourceUtilsTests {

  @Test
  public void testGetResource() throws IOException {

//		final Resource resource = ResourceUtils.getResource("/META-INF/maven/infra/today-expression/pom.properties");
    Resource resource = ResourceUtils.getResource("classpath:META-INF/maven/org.slf4j/slf4j-api/pom.properties");
    Resource createRelative = resource.createRelative("pom.xml");
    assertThat(createRelative.exists()).isTrue();
    assertThat(resource.exists()).isTrue();

    final String userDir = System.getProperty("user.dir");

    resource = ResourceUtils.getResource("file://" + userDir + "/src/main/resources/META-INF/ignore/jar-prefix");
    assertThat(resource.exists()).isFalse();

    // location is empty
    final Resource classpath = ResourceUtils.getResource("");
    assert classpath.createRelative("info.properties").exists();
    // start with '/'
    assert ResourceUtils.getResource("info.properties").exists();
    assert ResourceUtils.getResource("classpath:info.properties").exists();
    try {
      ResourceUtils.getResource("today://info");
    }
    catch (InvalidPathException e) {
      System.err.println(e);
    }
    ResourceUtils.getResource("info.properties");

  }

  //
  // ----------------------------------------

  @Test
  public void isJarURL() throws Exception {
    assertThat(ResourceUtils.isJarURL(new URL("jar:file:myjar.jar!/mypath"))).isTrue();
    assertThat(ResourceUtils.isJarURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isTrue();
    assertThat(ResourceUtils.isJarURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isTrue();
    assertThat(ResourceUtils.isJarURL(new URL(null, "jar:war:file:mywar.war*/myjar.jar!/mypath", new DummyURLStreamHandler())))
            .isTrue();
    assertThat(ResourceUtils.isJarURL(new URL("file:myjar.jar"))).isFalse();
    assertThat(ResourceUtils.isJarURL(new URL("http:myserver/myjar.jar"))).isFalse();
  }

  @Test
  public void extractJarFileURL() throws Exception {
    assertThat(ResourceUtils.extractJarFileURL(new URL("jar:file:myjar.jar!/mypath"))).isEqualTo(new URL("file:myjar.jar"));
    assertThat(ResourceUtils.extractJarFileURL(new URL(null, "jar:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL(
            "file:/myjar.jar"));
    assertThat(ResourceUtils.extractJarFileURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(
            new URL("file:myjar.jar"));
    assertThat(ResourceUtils.extractJarFileURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(
            new URL("file:myjar.jar"));

    assertThat(ResourceUtils.extractJarFileURL(new URL("file:myjar.jar"))).isEqualTo(new URL("file:myjar.jar"));
    assertThat(ResourceUtils.extractJarFileURL(new URL("jar:file:myjar.jar!/"))).isEqualTo(new URL("file:myjar.jar"));
    assertThat(ResourceUtils.extractJarFileURL(new URL(null, "zip:file:myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL(
            "file:myjar.jar"));
    assertThat(ResourceUtils.extractJarFileURL(new URL(null, "wsjar:file:myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL(
            "file:myjar.jar"));
  }

  @Test
  public void extractArchiveURL() throws Exception {
    assertThat(ResourceUtils.extractArchiveURL(new URL("jar:file:myjar.jar!/mypath"))).isEqualTo(new URL("file:myjar.jar"));
    assertThat(ResourceUtils.extractArchiveURL(new URL(null, "jar:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL(
            "file:/myjar.jar"));
    assertThat(ResourceUtils.extractArchiveURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(
            new URL("file:myjar.jar"));
    assertThat(ResourceUtils.extractArchiveURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(
            new URL("file:myjar.jar"));
    assertThat(ResourceUtils.extractArchiveURL(new URL(null, "jar:war:file:mywar.war*/myjar.jar!/mypath", new DummyURLStreamHandler())))
            .isEqualTo(new URL("file:mywar.war"));

    assertThat(ResourceUtils.extractArchiveURL(new URL("file:myjar.jar"))).isEqualTo(new URL("file:myjar.jar"));
    assertThat(ResourceUtils.extractArchiveURL(new URL("jar:file:myjar.jar!/"))).isEqualTo(new URL("file:myjar.jar"));
    assertThat(ResourceUtils.extractArchiveURL(new URL(null, "zip:file:myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL(
            "file:myjar.jar"));
    assertThat(ResourceUtils.extractArchiveURL(new URL(null, "wsjar:file:myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL(
            "file:myjar.jar"));
    assertThat(ResourceUtils.extractArchiveURL(new URL(null, "jar:war:file:mywar.war*/myjar.jar!/", new DummyURLStreamHandler())))
            .isEqualTo(new URL("file:mywar.war"));
  }

  @Test
  void isUrlReturnsTrueForClasspathPrefix() {
    assertThat(ResourceUtils.isUrl("classpath:test.txt")).isTrue();
  }

  @Test
  void isUrlReturnsTrueForValidUrl() {
    assertThat(ResourceUtils.isUrl("file:///test.txt")).isTrue();
    assertThat(ResourceUtils.isUrl("http://localhost/test.txt")).isTrue();
  }

  @Test
  void isUrlReturnsFalseForInvalidUrl() {
    assertThat(ResourceUtils.isUrl(null)).isFalse();
    assertThat(ResourceUtils.isUrl("")).isFalse();
    assertThat(ResourceUtils.isUrl("not-a-url")).isFalse();
  }

  @Test
  void getResourceReturnsClasspathResourceForEmptyLocation() {
    Resource resource = ResourceUtils.getResource("");
    assertThat(resource).isInstanceOf(ClassPathResource.class);
  }

  @Test
  void getResourceReturnsClasspathResourceForClasspathUrl() {
    Resource resource = ResourceUtils.getResource("classpath:test.txt");
    assertThat(resource).isInstanceOf(ClassPathResource.class);
    assertThat(resource.getName()).isEqualTo("test.txt");
  }

  @Test
  void getResourceReturnsFileSystemResourceForFileUrl() {
    Resource resource = ResourceUtils.getResource("file:///test.txt");
    assertThat(resource).isInstanceOf(FileSystemResource.class);
  }

  @Test
  void getResourceWithSpacesInPath() throws IOException {
    Resource resource = ResourceUtils.getResource("classpath:blank dir/test.txt");
    assertThat(resource.exists()).isTrue();
    assertThat(resource.getName()).isEqualTo("test.txt");
  }

  @Test
  void useCachesIfNecessarySetsFalseForNonJarUrls() throws Exception {
    URL url = new URL("file:///test.txt");
    URLConnection conn = url.openConnection();
    ResourceUtils.useCachesIfNecessary(conn);
    assertThat(conn.getUseCaches()).isFalse();
  }

  @Test
  void getFileThrowsFileNotFoundForNonExistentClasspathResource() {
    assertThatThrownBy(() -> ResourceUtils.getFile("classpath:non-existent.txt"))
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageContaining("cannot be resolved to absolute file path");
  }

  /**
   * Dummy URLStreamHandler that's just specified to suppress the standard
   * {@code java.net.URL} URLStreamHandler lookup, to be able to use the standard
   * URL class for parsing "rmi:..." URLs.
   */
  private static class DummyURLStreamHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      throw new UnsupportedOperationException();
    }
  }

  // ------------------------------

  @Test
  public void testBlankDir() throws IOException {
    final Resource blankDirTestResource = ResourceUtils.getResource("classpath:blank+dir%2Ftest.txt");
    try (InputStream inputStream = blankDirTestResource.getInputStream()) {
      final String text = StreamUtils.copyToString(inputStream);

      assertThat(text).isEqualTo("test");
      assertThat(blankDirTestResource.exists()).isTrue();
      assertThat(blankDirTestResource.contentLength()).isEqualTo(4);
      assertThat(blankDirTestResource.getName()).isEqualTo("test.txt");
    }
  }

}
