/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.core.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 16:26
 */
class WritableResourceTests {

  @Test
  void writableResourceDefaultIsWritableReturnsTrue() {
    WritableResource resource = new TestWritableResource();
    assertThat(resource.isWritable()).isTrue();
  }

  @Test
  void writableResourceCanGetOutputStream() throws IOException {
    WritableResource resource = new TestWritableResource();
    try (OutputStream out = resource.getOutputStream()) {
      assertThat(out).isNotNull();
    }
  }

  @Test
  void writableResourceCanWriteContent() throws IOException {
    WritableResource resource = new TestWritableResource();
    try (OutputStream out = resource.getOutputStream()) {
      out.write("test data".getBytes(StandardCharsets.UTF_8));
    }
    assertThat(resource.getInputStream())
            .hasContent("test data");
  }

  @Test
  void writableResourceRespectsReadableInterface() throws IOException {
    WritableResource resource = new TestWritableResource();
    assertThat(resource.exists()).isTrue();
    assertThat(resource.isReadable()).isTrue();
    assertThat(resource.contentLength()).isZero();
  }

  @Test
  void writableResourceThrowsWhenOutputStreamUnavailable() {
    WritableResource resource = new NonWritableResource();
    assertThatExceptionOfType(IOException.class)
            .isThrownBy(resource::getOutputStream);
  }

  private static class TestWritableResource implements WritableResource {
    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(content.toByteArray());
    }

    @Override
    public OutputStream getOutputStream() {
      return content;
    }

    @Override
    public String getName() {
      return "test.txt";
    }

    @Override
    public long contentLength() throws IOException {
      return 0;
    }

    @Override
    public long lastModified() throws IOException {
      return 0;
    }

    @Override
    public URL getURL() throws IOException {
      return null;
    }

    @Override
    public URI getURI() throws IOException {
      return null;
    }

    @Override
    public File getFile() throws IOException {
      return null;
    }

    @Override
    public boolean exists() {
      return true;
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
      return null;
    }
  }

  private static class NonWritableResource implements WritableResource {

    @Override
    public boolean isWritable() {
      return false;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      throw new IOException("Writing not supported");
    }

    @Override
    public InputStream getInputStream() {
      return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public String getName() {
      return "readonly.txt";
    }

    @Override
    public long contentLength() throws IOException {
      return 0;
    }

    @Override
    public long lastModified() throws IOException {
      return 0;
    }

    @Override
    public URL getURL() throws IOException {
      return null;
    }

    @Override
    public URI getURI() throws IOException {
      return null;
    }

    @Override
    public File getFile() throws IOException {
      return null;
    }

    @Override
    public boolean exists() {
      return true;
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
      return null;
    }
  }

}