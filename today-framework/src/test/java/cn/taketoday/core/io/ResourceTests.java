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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

import cn.taketoday.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author TODAY 2021/3/9 20:14
 */
class ResourceTests {

  @Test
  void byteArrayResource() throws IOException {
    Resource resource = new ByteArrayResource("testString".getBytes());
    assertThat(resource.exists()).isTrue();
    assertThat(resource.isOpen()).isFalse();

    String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    assertThat(content).isEqualTo("testString");
    assertThat(new ByteArrayResource("testString".getBytes())).isEqualTo(resource);
  }

  @Test
  void byteArrayResourceWithDescription() throws IOException {
    Resource resource = new ByteArrayResource("testString".getBytes(), "my description");
    assertThat(resource.exists()).isTrue();
    assertThat(resource.isOpen()).isFalse();
    String content = StreamUtils.copyToString(resource.getInputStream());
    assertThat(content).isEqualTo("testString");
    assertThat(resource.toString().contains("my description")).isTrue();
    assertThat(new ByteArrayResource("testString".getBytes())).isEqualTo(resource);
  }

  @Test
  void inputStreamResource() throws IOException {
    InputStream is = new ByteArrayInputStream("testString".getBytes());
    Resource resource = new InputStreamResource(is);
    assertThat(resource.exists()).isTrue();
    assertThat(resource.isOpen()).isTrue();
    String content = StreamUtils.copyToString(resource.getInputStream());
    assertThat(content).isEqualTo("testString");
    assertThat(new InputStreamResource(is)).isEqualTo(resource);
  }

  @Test
  void inputStreamResourceWithDescription() throws IOException {
    InputStream is = new ByteArrayInputStream("testString".getBytes());
    Resource resource = new InputStreamResource(is, "my description");
    assertThat(resource.exists()).isTrue();
    assertThat(resource.isOpen()).isTrue();

    String content = StreamUtils.copyToString(resource.getInputStream());

    assertThat(content).isEqualTo("testString");
    assertThat(resource.toString().contains("my description")).isTrue();
    assertThat(new InputStreamResource(is)).isEqualTo(resource);
  }

  @Test
  void classPathResource() throws IOException {
    Resource resource = new ClassPathResource("cn/taketoday/core/io/Resource.class");
    doTestResource(resource);
    Resource resource2 = new ClassPathResource("cn/taketoday/context/../core/io/./Resource.class");
    assertThat(resource2).isEqualTo(resource);
    Resource resource3 = new ClassPathResource("cn/taketoday/context/")
            .createRelative("../core/io/./Resource.class");

    assertThat(resource3).isEqualTo(resource);

    // Check whether equal/hashCode works in a HashSet.
    HashSet<Resource> resources = new HashSet<>();
    resources.add(resource);
    resources.add(resource2);
    assertThat(resources.size()).isEqualTo(1);
  }

  @Test
  void classPathResourceWithClassLoader() throws IOException {
    Resource resource = new ClassPathResource("cn/taketoday/core/io/Resource.class", getClass().getClassLoader());
    doTestResource(resource);
    assertThat(new ClassPathResource("cn/taketoday/context/../core/io/./Resource.class", getClass().getClassLoader()))
            .isEqualTo(resource);
  }

  @Test
  void classPathResourceWithClass() throws IOException {
    ClassPathResource resource = new ClassPathResource("Resource.class", getClass());
    final Resource originalResource = resource.getOriginalResource();
    doTestResource(resource);
    assertThat(new ClassPathResource("Resource.class", getClass())).isEqualTo(resource);
  }

  @Test
  void fileBasedResource() throws IOException {
    String file = getClass().getResource("Resource.class").getFile();
    Resource resource = new FileSystemResource(file);
    doTestResource(resource);
    assertThat(resource).isEqualTo(new FileSystemResource(file));
  }

  @Test
  void fileBasedResourceWithFile() throws IOException {
    File file = new File(getClass().getResource("Resource.class").getFile());
    Resource resource = new FileSystemResource(file);
    doTestResource(resource);
    assertThat(resource).isEqualTo(new FileSystemResource(file));
  }

  @Test
  void fileBasedResourceWithFilePath() throws Exception {
    Path filePath = Paths.get(getClass().getResource("Resource.class").toURI());
    Resource resource = new FileSystemResource(filePath);
    doTestResource(resource);
    assertThat(resource).isEqualTo(new FileSystemResource(filePath));
  }

  @Test
  void FileBasedResourceWithPlainPath() {
    Resource resource = new FileSystemResource("core/io/Resource.class");
    assertThat(new FileSystemResource("context/../core/io/./Resource.class")).isEqualTo(resource);
  }

  @Test
  void urlResource() throws IOException {
    Resource resource = new UrlBasedResource(getClass().getResource("Resource.class"));
    doTestResource(resource);
    assertThat(resource).isEqualTo(new UrlBasedResource(getClass().getResource("Resource.class")));

    Resource resource2 = new UrlBasedResource("file:core/io/Resource.class");
    assertThat(new UrlBasedResource("file:context/../core/io/./Resource.class")).isEqualTo(resource2);

    assertThat(new UrlBasedResource("file:/dir/test.txt?argh").getName()).isEqualTo("test.txt");
    assertThat(new UrlBasedResource("file:\\dir\\test.txt?argh").getName()).isEqualTo("test.txt");
    assertThat(new UrlBasedResource("file:\\dir/test.txt?argh").getName()).isEqualTo("test.txt");
  }

  private void doTestResource(Resource resource) throws IOException {
    assertThat(resource.getName()).isEqualTo("Resource.class");
    assertThat(resource.getURL().getFile().endsWith("Resource.class")).isTrue();
    assertThat(resource.exists()).isTrue();
    assertThat(resource.isReadable()).isTrue();
    assertThat(resource.contentLength() > 0).isTrue();
    assertThat(resource.lastModified() > 0).isTrue();

    Resource relative1 = resource.createRelative("ClassPathResource.class");
    assertThat(relative1.getName()).isEqualTo("ClassPathResource.class");
    assertThat(relative1.getURL().getFile().endsWith("ClassPathResource.class")).isTrue();
    assertThat(relative1.exists()).isTrue();
    assertThat(relative1.isReadable()).isTrue();
    assertThat(relative1.contentLength() > 0).isTrue();
    assertThat(relative1.lastModified() > 0).isTrue();

    Resource relative2 = resource.createRelative("PathMatchingPatternResourceLoader.class");
    assertThat(relative2.getName()).isEqualTo("PathMatchingPatternResourceLoader.class");
    assertThat(relative2.getURL().getFile().endsWith("PatternResourceLoader.class")).isTrue();
    assertThat(relative2.exists()).isTrue();
    assertThat(relative2.isReadable()).isTrue();
    assertThat(relative2.contentLength() > 0).isTrue();
    assertThat(relative2.lastModified() > 0).isTrue();

    Resource relative3 = resource.createRelative("../../lang/Constant.class");
    assertThat(relative3.getName()).isEqualTo("Constant.class");
    assertThat(relative3.getURL().getFile().endsWith("Constant.class")).isTrue();
    assertThat(relative3.exists()).isTrue();
    assertThat(relative3.isReadable()).isTrue();
    assertThat(relative3.contentLength() > 0).isTrue();
    assertThat(relative3.lastModified() > 0).isTrue();

    Resource relative4 = resource.createRelative("X.class");
    assertThat(relative4.exists()).isFalse();
    assertThat(relative4.isReadable()).isFalse();
    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(relative4::contentLength);

    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(relative4::lastModified);
  }

  @Test
  void urlResourceFactoryMethods() throws IOException {
    Resource resource1 = new UrlBasedResource("file:core/io/Resource.class");
    Resource resource2 = UrlBasedResource.from("file:core/io/Resource.class");
    Resource resource3 = UrlBasedResource.from(resource1.getURI());

    assertThat(resource2.getURL()).isEqualTo(resource1.getURL());
    assertThat(resource3.getURL()).isEqualTo(resource1.getURL());

    assertThat(UrlBasedResource.from("file:core/../core/io/./Resource.class")).isEqualTo(resource1);
    assertThat(UrlBasedResource.from("file:/dir/test.txt?argh").getName()).isEqualTo("test.txt");
    assertThat(UrlBasedResource.from("file:\\dir\\test.txt?argh").getName()).isEqualTo("test.txt");
    assertThat(UrlBasedResource.from("file:\\dir/test.txt?argh").getName()).isEqualTo("test.txt");
  }

  @Test
  void classPathResourceWithRelativePath() throws IOException {
    Resource resource = new ClassPathResource("dir/");
    Resource relative = resource.createRelative("subdir");
    assertThat(relative).isEqualTo(new ClassPathResource("dir/subdir"));
  }

  @Test
  void FileBasedResourceWithRelativePath() throws IOException {
    Resource resource = new FileSystemResource("dir/");
    Resource relative = resource.createRelative("subdir");
    assertThat(relative).isEqualTo(new FileSystemResource("dir/subdir"));
  }

  @Test
  void urlResourceWithRelativePath() throws IOException {
    Resource resource = new UrlBasedResource("file:dir/");
    Resource relative = resource.createRelative("subdir");
    assertThat(relative).isEqualTo(new UrlBasedResource("file:dir/subdir"));
  }

  @Test
  void nonFileResourceExists() throws Exception {
    URL url = new URL("https://spring.io/");

    // Abort if spring.io is not reachable.
    assumeTrue(urlIsReachable(url));

    Resource resource = new UrlBasedResource(url);
    assertThat(resource.exists()).isTrue();
  }

  private boolean urlIsReachable(URL url) {
    try {
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("HEAD");
      connection.setReadTimeout(5_000);
      return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
    }
    catch (Exception ex) {
      return false;
    }
  }

  @Test
  void abstractResourceExceptions() throws Exception {
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

    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(resource::getURL)
            .withMessageContaining(name);

    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(resource::getFile)
            .withMessageContaining(name);

    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(() -> resource.createRelative("/testing"))
            .withMessageContaining(name);

    assertThat(resource.getName()).isNull();
  }

  @Test
  void contentLength() throws IOException {
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

  @Test
  void readableChannel() throws IOException {
    Resource resource = new FileSystemResource(getClass().getResource("Resource.class").getFile());
    try (ReadableByteChannel channel = resource.readableChannel()) {
      ByteBuffer buffer = ByteBuffer.allocate((int) resource.contentLength());
      channel.read(buffer);
      buffer.rewind();
      assertThat(buffer.limit() > 0).isTrue();
    }
  }

  @Test
  void inputStreamNotFoundOnFileBasedResource() throws IOException {
    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(() -> new FileSystemResource(getClass().getResource("Resource.class").getFile())
                    .createRelative("X").getInputStream());
  }

  @Test
  void readableChannelNotFoundOnFileBasedResource() throws IOException {
    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(() -> new FileSystemResource(
                    getClass().getResource("Resource.class").getFile())
                    .createRelative("X").readableChannel());
  }

  @Test
  void inputStreamNotFoundOnClassPathResource() throws IOException {
    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(() -> new ClassPathResource("Resource.class", getClass())
                    .createRelative("X").getInputStream());
  }

  @Test
  void readableChannelNotFoundOnClassPathResource() throws IOException {
    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(() -> new ClassPathResource("Resource.class", getClass())
                    .createRelative("X").readableChannel());
  }

}
