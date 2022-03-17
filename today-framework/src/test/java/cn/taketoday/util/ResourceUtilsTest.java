/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.InvalidPathException;

import cn.taketoday.core.io.JarEntryResource;
import cn.taketoday.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY <br>
 * 2019-05-15 14:04
 */
public class ResourceUtilsTest {

  @Test
  public void testGetRelativePath() throws IOException {
    final String relativePath = ResourceUtils.getRelativePath("D:/java/", "1.txt");
    final String relativePath1 = ResourceUtils.getRelativePath("D:/java", "1.txt");
    final String relativePath2 = ResourceUtils.getRelativePath("D:/java/2.txt", "1.txt");

    System.err.println(relativePath);
    assert relativePath.equals("D:/java/1.txt");

    System.err.println(relativePath1);
    assert relativePath1.equals("D:/1.txt");

    System.err.println(relativePath2);
    assert relativePath2.equals("D:/java/1.txt");

    assert ResourceUtils.getRelativePath("index", "TODAY").equals("TODAY");

  }

  @Test
  public void testGetResource() throws IOException {

//		final Resource resource = ResourceUtils.getResource("/META-INF/maven/cn.taketoday/today-expression/pom.properties");
    Resource resource = ResourceUtils.getResource("classpath:META-INF/maven/org.slf4j/slf4j-api/pom.properties");
    Resource createRelative = resource.createRelative("pom.xml");
    assertThat(createRelative.exists()).isTrue();
    assertThat(resource.exists()).isTrue();

    final String userDir = System.getProperty("user.dir");

    resource = ResourceUtils.getResource("file://" + userDir + "/src/main/resources/META-INF/ignore/jar-prefix");
    assertThat(resource.exists()).isFalse();

//    resource = ResourceUtils.getResource("file://" + userDir + "/src/main/resources/META-INF/today.strategies");
//    assertThat(resource.exists()).isTrue();
//    assertThat(StreamUtils.copyToString(resource.getInputStream())).isNotEmpty();

    resource = ResourceUtils.getResource("jar:file://" + userDir + "/src/test/resources/test.jar!/META-INF/");
//        System.err.println(resource);

    if (resource instanceof JarEntryResource) {
      JarEntryResource jarEntryResource = (JarEntryResource) resource.createRelative("/maven/cn.taketoday/today-expression/pom.properties");
      if (jarEntryResource.exists()) {
        System.out.println(StreamUtils.copyToString(jarEntryResource.getInputStream()));
      }
      System.err.println(jarEntryResource);
    }
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

//        final Resource resource2 = ResourceUtils.getResource("info"); // ConfigurationException

    // getResource(URL)

//        final Resource taketoday = ResourceUtils.getResource(new URL("https://taketoday.cn"));
//
//        assert taketoday.exists();
//        assert StreamUtils.copyToString(taketoday.getInputStream()) != null;
//        System.err.println(StreamUtils.copyToString(taketoday.getInputStream()));

  }

  // spring
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
      assertThat(blankDirTestResource.isDirectory()).isFalse();
      assertThat(blankDirTestResource.contentLength()).isEqualTo(4);
      assertThat(blankDirTestResource.getName()).isEqualTo("test.txt");
    }
  }

}
