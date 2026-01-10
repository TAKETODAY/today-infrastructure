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

import java.io.File;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 13:59
 */
class FileUrlResourceTests {

  @Test
  void createFromValidFileUrl() throws Exception {
    File tempFile = File.createTempFile("test", ".txt");
    FileUrlResource resource = new FileUrlResource(tempFile.toURI().toURL());
    assertThat(resource.getFile()).isEqualTo(tempFile);
    tempFile.deleteOnExit();
  }

  @Test
  void createFromValidFileLocation() throws Exception {
    File tempFile = File.createTempFile("test", ".txt");
    FileUrlResource resource = new FileUrlResource(tempFile.getAbsolutePath());
    assertThat(resource.getFile()).isEqualTo(tempFile);
    tempFile.deleteOnExit();
  }

  @Test
  void isWritableReturnsTrueForWritableFile() throws Exception {
    File tempFile = File.createTempFile("test", ".txt");
    FileUrlResource resource = new FileUrlResource(tempFile.toURI().toURL());
    assertThat(resource.isWritable()).isTrue();
    tempFile.deleteOnExit();
  }

  @Test
  void isWritableReturnsFalseForDirectory() throws Exception {
    File tempDir = Files.createTempDirectory("test").toFile();
    FileUrlResource resource = new FileUrlResource(tempDir.toURI().toURL());
    assertThat(resource.isWritable()).isFalse();
    tempDir.delete();
  }

  @Test
  void getOutputStreamWritesContent() throws Exception {
    File tempFile = File.createTempFile("test", ".txt");
    FileUrlResource resource = new FileUrlResource(tempFile.toURI().toURL());

    try (OutputStream out = resource.getOutputStream()) {
      out.write("test content".getBytes());
    }

    assertThat(Files.readString(tempFile.toPath())).isEqualTo("test content");
    tempFile.deleteOnExit();
  }

  @Test
  void writableChannelWritesContent() throws Exception {
    File tempFile = File.createTempFile("test", ".txt");
    FileUrlResource resource = new FileUrlResource(tempFile.toURI().toURL());

    try (WritableByteChannel channel = resource.writableChannel()) {
      channel.write(ByteBuffer.wrap("test content".getBytes()));
    }

    assertThat(Files.readString(tempFile.toPath())).isEqualTo("test content");
    tempFile.deleteOnExit();
  }

  @Test
  void createRelativeGeneratesValidResource() throws Exception {
    File tempFile = File.createTempFile("test", ".txt");
    FileUrlResource resource = new FileUrlResource(tempFile.getParentFile().toURI().toURL());
    FileUrlResource relative = resource.createRelative(tempFile.getName());
    assertThat(relative.getFile()).isEqualTo(tempFile);
    tempFile.deleteOnExit();
  }

  @Test
  void fileCachingWorks() throws Exception {
    File tempFile = File.createTempFile("test", ".txt");
    FileUrlResource resource = new FileUrlResource(tempFile.toURI().toURL());
    File first = resource.getFile();
    File second = resource.getFile();
    assertThat(first).isSameAs(second);
    tempFile.deleteOnExit();
  }

}