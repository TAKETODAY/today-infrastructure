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

package cn.taketoday.core.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.stream.Stream;

import cn.taketoday.util.FileCopyUtils;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author TODAY 2021/3/9 20:14
 */
class ResourceTests {

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("resource")
  void resourceIsValid(Resource resource) throws Exception {
    assertThat(resource.getName()).isEqualTo("Resource.class");
    assertThat(resource.getURL().getFile().endsWith("Resource.class")).isTrue();
    assertThat(resource.exists()).isTrue();
    assertThat(resource.isReadable()).isTrue();
    assertThat(resource.contentLength() > 0).isTrue();
    assertThat(resource.lastModified() > 0).isTrue();
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("resource")
  void resourceCreateRelative(Resource resource) throws Exception {
    Resource relative1 = resource.createRelative("ClassPathResource.class");
    assertThat(relative1.getName()).isEqualTo("ClassPathResource.class");
    assertThat(relative1.getURL().getFile().endsWith("ClassPathResource.class")).isTrue();
    assertThat(relative1.exists()).isTrue();
    assertThat(relative1.isReadable()).isTrue();
    assertThat(relative1.contentLength() > 0).isTrue();
    assertThat(relative1.lastModified() > 0).isTrue();
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("resource")
  void resourceCreateRelativeWithFolder(Resource resource) throws Exception {
    Resource relative2 = resource.createRelative("PatternResourceLoader.class");
    assertThat(relative2.getName()).isEqualTo("PatternResourceLoader.class");
    assertThat(relative2.getURL().getFile().endsWith("PatternResourceLoader.class")).isTrue();
    assertThat(relative2.exists()).isTrue();
    assertThat(relative2.isReadable()).isTrue();
    assertThat(relative2.contentLength() > 0).isTrue();
    assertThat(relative2.lastModified() > 0).isTrue();
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("resource")
  void resourceCreateRelativeWithDotPath(Resource resource) throws Exception {
    Resource relative3 = resource.createRelative("../../lang/Version.class");
    assertThat(relative3.getName()).isEqualTo("Version.class");
    assertThat(relative3.getURL().getFile().endsWith("Version.class")).isTrue();
    assertThat(relative3.exists()).isTrue();
    assertThat(relative3.isReadable()).isTrue();
    assertThat(relative3.contentLength() > 0).isTrue();
    assertThat(relative3.lastModified() > 0).isTrue();
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("resource")
  void resourceCreateRelativeUnknown(Resource resource) throws Exception {
    Resource relative4 = resource.createRelative("X.class");
    assertThat(relative4.exists()).isFalse();
    assertThat(relative4.isReadable()).isFalse();
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
            relative4::contentLength);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
            relative4::lastModified);
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("resource")
  void loadingMissingResourceFails(Resource resource) {
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() ->
            resource.createRelative("X").getInputStream());
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("resource")
  void readingMissingResourceFails(Resource resource) {
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() ->
            resource.createRelative("X").readableChannel());
  }

  private static Stream<Arguments> resource() throws URISyntaxException {
    URL resourceClass = ResourceTests.class.getResource("Resource.class");
    Path resourceClassFilePath = Paths.get(resourceClass.toURI());
    return Stream.of(
            Arguments.of(Named.of("ClassPathResource", new ClassPathResource("cn/taketoday/core/io/Resource.class"))),
            Arguments.of(Named.of("ClassPathResource with ClassLoader", new ClassPathResource("cn/taketoday/core/io/Resource.class", ResourceTests.class.getClassLoader()))),
            Arguments.of(Named.of("ClassPathResource with Class", new ClassPathResource("Resource.class", ResourceTests.class))),
            Arguments.of(Named.of("FileSystemResource", new FileSystemResource(resourceClass.getFile()))),
            Arguments.of(Named.of("FileSystemResource with File", new FileSystemResource(new File(resourceClass.getFile())))),
            Arguments.of(Named.of("FileSystemResource with File path", new FileSystemResource(resourceClassFilePath))),
            Arguments.of(Named.of("UrlResource", new UrlResource(resourceClass)))
    );
  }

  @Nested
  class ByteArrayResourceTests {

    @Test
    void hasContent() throws Exception {
      Resource resource = new ByteArrayResource("testString".getBytes());
      assertThat(resource.exists()).isTrue();
      assertThat(resource.isOpen()).isFalse();
      String content = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
      assertThat(content).isEqualTo("testString");
      assertThat(new ByteArrayResource("testString".getBytes())).isEqualTo(resource);
    }

    @Test
    void isNotOpen() {
      Resource resource = new ByteArrayResource("testString".getBytes());
      assertThat(resource.exists()).isTrue();
      assertThat(resource.isOpen()).isFalse();
    }

    @Test
    void hasDescription() {
      Resource resource = new ByteArrayResource("testString".getBytes(), "my description");
      assertThat(resource.toString().contains("my description")).isTrue();
    }

  }

  @Nested
  class InputStreamResourceTests {

    @Test
    void hasContent() throws Exception {
      InputStream is = new ByteArrayInputStream("testString".getBytes());
      Resource resource = new InputStreamResource(is);
      String content = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
      assertThat(content).isEqualTo("testString");
      assertThat(new InputStreamResource(is)).isEqualTo(resource);
    }

    @Test
    void isOpen() {
      InputStream is = new ByteArrayInputStream("testString".getBytes());
      Resource resource = new InputStreamResource(is);
      assertThat(resource.exists()).isTrue();
      assertThat(resource.isOpen()).isTrue();
    }

    @Test
    void hasDescription() {
      InputStream is = new ByteArrayInputStream("testString".getBytes());
      Resource resource = new InputStreamResource(is, "my description");
      assertThat(resource.toString().contains("my description")).isTrue();
    }
  }

  @Nested
  class ClassPathResourceTests {

    @Test
    void equalsAndHashCode() {
      Resource resource = new ClassPathResource("cn/taketoday/core/io/Resource.class");
      Resource resource2 = new ClassPathResource("cn/taketoday/core/../core/io/./Resource.class");
      Resource resource3 = new ClassPathResource("cn/taketoday/core/").createRelative("../core/io/./Resource.class");
      assertThat(resource2).isEqualTo(resource);
      assertThat(resource3).isEqualTo(resource);
      // Check whether equal/hashCode works in a HashSet.
      HashSet<Resource> resources = new HashSet<>();
      resources.add(resource);
      resources.add(resource2);
      assertThat(resources.size()).isEqualTo(1);
    }

    @Test
    void resourcesWithDifferentPathsAreEqual() {
      Resource resource = new ClassPathResource("cn/taketoday/core/io/Resource.class", getClass().getClassLoader());
      ClassPathResource sameResource = new ClassPathResource("cn/taketoday/core/../core/io/./Resource.class", getClass().getClassLoader());
      assertThat(sameResource).isEqualTo(resource);
    }

    @Test
    void relativeResourcesAreEqual() throws Exception {
      Resource resource = new ClassPathResource("dir/");
      Resource relative = resource.createRelative("subdir");
      assertThat(relative).isEqualTo(new ClassPathResource("dir/subdir"));
    }

  }

  @Nested
  class FileSystemResourceTests {

    @Test
    void sameResourceIsEqual() {
      String file = getClass().getResource("Resource.class").getFile();
      Resource resource = new FileSystemResource(file);
      assertThat(resource).isEqualTo(new FileSystemResource(file));
    }

    @Test
    void sameResourceFromFileIsEqual() {
      File file = new File(getClass().getResource("Resource.class").getFile());
      Resource resource = new FileSystemResource(file);
      assertThat(resource).isEqualTo(new FileSystemResource(file));
    }

    @Test
    void sameResourceFromFilePathIsEqual() throws Exception {
      Path filePath = Paths.get(getClass().getResource("Resource.class").toURI());
      Resource resource = new FileSystemResource(filePath);
      assertThat(resource).isEqualTo(new FileSystemResource(filePath));
    }

    @Test
    void sameResourceFromDotPathIsEqual() {
      Resource resource = new FileSystemResource("core/io/Resource.class");
      assertThat(new FileSystemResource("core/../core/io/./Resource.class")).isEqualTo(resource);
    }

    @Test
    void relativeResourcesAreEqual() throws Exception {
      Resource resource = new FileSystemResource("dir/");
      Resource relative = resource.createRelative("subdir");
      assertThat(relative).isEqualTo(new FileSystemResource("dir/subdir"));
    }

    @Test
    void readableChannelProvidesContent() throws Exception {
      Resource resource = new FileSystemResource(getClass().getResource("Resource.class").getFile());
      try (ReadableByteChannel channel = resource.readableChannel()) {
        ByteBuffer buffer = ByteBuffer.allocate((int) resource.contentLength());
        channel.read(buffer);
        buffer.rewind();
        assertThat(buffer.limit() > 0).isTrue();
      }
    }

  }

  @Nested
  class UrlResourceTests {

    private MockWebServer server = new MockWebServer();

    @Test
    void sameResourceWithRelativePathIsEqual() throws Exception {
      Resource resource = new UrlResource("file:core/io/Resource.class");
      assertThat(new UrlResource("file:core/../core/io/./Resource.class")).isEqualTo(resource);
    }

    @Test
    void filenameIsExtractedFromFilePath() throws Exception {
      assertThat(new UrlResource("file:/dir/test.txt?argh").getName()).isEqualTo("test.txt");
      assertThat(new UrlResource("file:\\dir\\test.txt?argh").getName()).isEqualTo("test.txt");
      assertThat(new UrlResource("file:\\dir/test.txt?argh").getName()).isEqualTo("test.txt");
    }

    @Test
    void factoryMethodsProduceEqualResources() throws Exception {
      Resource resource1 = new UrlResource("file:core/io/Resource.class");
      Resource resource2 = UrlResource.from("file:core/io/Resource.class");
      Resource resource3 = UrlResource.from(resource1.getURI());

      assertThat(resource2.getURL()).isEqualTo(resource1.getURL());
      assertThat(resource3.getURL()).isEqualTo(resource1.getURL());

      assertThat(UrlResource.from("file:core/../core/io/./Resource.class")).isEqualTo(resource1);
      assertThat(UrlResource.from("file:/dir/test.txt?argh").getName()).isEqualTo("test.txt");
      assertThat(UrlResource.from("file:\\dir\\test.txt?argh").getName()).isEqualTo("test.txt");
      assertThat(UrlResource.from("file:\\dir/test.txt?argh").getName()).isEqualTo("test.txt");
    }

    @Test
    void relativeResourcesAreEqual() throws Exception {
      Resource resource = new UrlResource("file:dir/");
      Resource relative = resource.createRelative("subdir");
      assertThat(relative).isEqualTo(new UrlResource("file:dir/subdir"));
    }

    @Test
    void missingRemoteResourceDoesNotExist() throws Exception {
      String baseUrl = startServer();
      UrlResource resource = new UrlResource(baseUrl + "/missing");
      assertThat(resource.exists()).isFalse();
    }

    @Test
    void remoteResourceExists() throws Exception {
      String baseUrl = startServer();
      UrlResource resource = new UrlResource(baseUrl + "/resource");
      assertThat(resource.exists()).isTrue();
      assertThat(resource.contentLength()).isEqualTo(6);
    }

    @Test
    void canCustomizeHttpUrlConnectionForExists() throws Exception {
      String baseUrl = startServer();
      CustomResource resource = new CustomResource(baseUrl + "/resource");
      assertThat(resource.exists()).isTrue();
      RecordedRequest request = this.server.takeRequest();
      assertThat(request.getMethod()).isEqualTo("HEAD");
      assertThat(request.getHeader("Framework-Name")).isEqualTo("Spring");
    }

    @Test
    void canCustomizeHttpUrlConnectionForRead() throws Exception {
      String baseUrl = startServer();
      CustomResource resource = new CustomResource(baseUrl + "/resource");
      assertThat(resource.getInputStream()).hasContent("Spring");
      RecordedRequest request = this.server.takeRequest();
      assertThat(request.getMethod()).isEqualTo("GET");
      assertThat(request.getHeader("Framework-Name")).isEqualTo("Spring");
    }

    @AfterEach
    void shutdown() throws Exception {
      this.server.shutdown();
    }

    private String startServer() throws Exception {
      this.server.setDispatcher(new ResourceDispatcher());
      this.server.start();
      return "http://localhost:" + this.server.getPort();
    }

    class CustomResource extends UrlResource {

      public CustomResource(String path) throws MalformedURLException {
        super(path);
      }

      @Override
      protected void customizeConnection(HttpURLConnection con) throws IOException {
        con.setRequestProperty("Framework-Name", "Spring");
      }
    }

    class ResourceDispatcher extends Dispatcher {

      @Override
      public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        if (request.getPath().equals("/resource")) {
          switch (request.getMethod()) {
            case "HEAD":
              return new MockResponse()
                      .addHeader("Content-Length", "6");
            case "GET":
              return new MockResponse()
                      .addHeader("Content-Length", "6")
                      .addHeader("Content-Type", "text/plain")
                      .setBody("Spring");
          }
        }
        return new MockResponse().setResponseCode(404);
      }
    }
  }

  @Nested
  class AbstractResourceTests {

    @Test
    void missingResourceIsNotReadable() {
      final String name = "test-resource";

      Resource resource = new AbstractResource() {
        @Override
        public String toString() {
          return name;
        }

        @Override
        public InputStream getInputStream() throws IOException {
          throw new FileNotFoundException();
        }
      };

      assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(resource::getURL)
              .withMessageContaining(name);
      assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(resource::getFile)
              .withMessageContaining(name);
      assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() ->
              resource.createRelative("/testing")).withMessageContaining(name);
      assertThat(resource.getName()).isNull();
    }

    @Test
    void hasContentLength() throws Exception {
      AbstractResource resource = new AbstractResource() {
        @Override
        public InputStream getInputStream() {
          return new ByteArrayInputStream(new byte[] { 'a', 'b', 'c' });
        }

        @Override
        public String toString() {
          return "";
        }
      };
      assertThat(resource.contentLength()).isEqualTo(3L);
    }

  }

}
