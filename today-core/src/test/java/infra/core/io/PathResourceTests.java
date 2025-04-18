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

package infra.core.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/25 11:46
 */
class PathResourceTests {

  private static final String TEST_DIR =
          platformPath("src/test/resources/infra/core/io");

  private static final String TEST_FILE =
          platformPath("src/test/resources/infra/core/io/example.properties");

  private static final String NON_EXISTING_FILE =
          platformPath("src/test/resources/infra/core/io/doesnotexist.properties");

  private static String platformPath(String string) {
    return string.replace('/', File.separatorChar);
  }

  @Test
  void nullPath() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    new PathResource((Path) null))
            .withMessageContaining("Path is required");
  }

  @Test
  void nullPathString() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    new PathResource((String) null))
            .withMessageContaining("Path is required");
  }

  @Test
  void nullUri() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    new PathResource((URI) null))
            .withMessageContaining("URI is required");
  }

  @Test
  void createFromPath() {
    Path path = Paths.get(TEST_FILE);
    PathResource resource = new PathResource(path);
    assertThat(resource.getPath()).isEqualTo(TEST_FILE);
  }

  @Test
  void createFromString() {
    PathResource resource = new PathResource(TEST_FILE);
    assertThat(resource.getPath()).isEqualTo(TEST_FILE);
  }

  @Test
  void createFromUri() {
    File file = new File(TEST_FILE);
    PathResource resource = new PathResource(file.toURI());
    assertThat(resource.getPath()).isEqualTo(file.getAbsoluteFile().toString());
  }

  @Test
  void getPathForFile() {
    PathResource resource = new PathResource(TEST_FILE);
    assertThat(resource.getPath()).isEqualTo(TEST_FILE);
  }

  @Test
  void getPathForDir() {
    PathResource resource = new PathResource(TEST_DIR);
    assertThat(resource.getPath()).isEqualTo(TEST_DIR);
  }

  @Test
  void fileExists() {
    PathResource resource = new PathResource(TEST_FILE);
    assertThat(resource.exists()).isTrue();
  }

  @Test
  void dirExists() {
    PathResource resource = new PathResource(TEST_DIR);
    assertThat(resource.exists()).isTrue();
  }

  @Test
  void fileDoesNotExist() {
    PathResource resource = new PathResource(NON_EXISTING_FILE);
    assertThat(resource.exists()).isFalse();
  }

  @Test
  void fileIsReadable() {
    PathResource resource = new PathResource(TEST_FILE);
    assertThat(resource.isReadable()).isTrue();
  }

  @Test
  void doesNotExistIsNotReadable() {
    PathResource resource = new PathResource(NON_EXISTING_FILE);
    assertThat(resource.isReadable()).isFalse();
  }

  @Test
  void directoryIsNotReadable() {
    PathResource resource = new PathResource(TEST_DIR);
    assertThat(resource.isReadable()).isFalse();
  }

  @Test
  void getInputStream() throws IOException {
    PathResource resource = new PathResource(TEST_FILE);
    byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
    assertThat(bytes.length).isGreaterThan(0);
  }

  @Test
  void getInputStreamForDir() throws IOException {
    PathResource resource = new PathResource(TEST_DIR);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
            resource::getInputStream);
  }

  @Test
  void getInputStreamDoesNotExist() throws IOException {
    PathResource resource = new PathResource(NON_EXISTING_FILE);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
            resource::getInputStream);
  }

  @Test
  void getUrl() throws IOException {
    PathResource resource = new PathResource(TEST_FILE);
    assertThat(resource.getURL().toString()).endsWith("core/io/example.properties");
  }

  @Test
  void getUri() throws IOException {
    PathResource resource = new PathResource(TEST_FILE);
    assertThat(resource.getURI().toString()).endsWith("core/io/example.properties");
  }

  @Test
  void getFile() throws IOException {
    PathResource resource = new PathResource(TEST_FILE);
    File file = new File(TEST_FILE);
    assertThat(resource.getFile().getAbsoluteFile()).isEqualTo(file.getAbsoluteFile());
  }

  @Test
  void getFileUnsupported() throws IOException {
    Path path = mock(Path.class);
    given(path.normalize()).willReturn(path);
    given(path.toFile()).willThrow(new UnsupportedOperationException());
    PathResource resource = new PathResource(path);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
            resource::getFile);
  }

  @Test
  void contentLength() throws IOException {
    PathResource resource = new PathResource(TEST_FILE);
    File file = new File(TEST_FILE);
    assertThat(resource.contentLength()).isEqualTo(file.length());
  }

  @Test
  void contentLengthForDirectory() throws IOException {
    PathResource resource = new PathResource(TEST_DIR);
    File file = new File(TEST_DIR);
    assertThat(resource.contentLength()).isEqualTo(file.length());
  }

  @Test
  void lastModified() throws IOException {
    PathResource resource = new PathResource(TEST_FILE);
    File file = new File(TEST_FILE);
    assertThat(resource.lastModified() / 1000).isEqualTo(file.lastModified() / 1000);
  }

  @Test
  void createRelativeFromDir() throws IOException {
    Resource resource = new PathResource(TEST_DIR).createRelative("example.properties");
    assertThat(resource).isEqualTo(new PathResource(TEST_FILE));
  }

  @Test
  void createRelativeFromFile() throws IOException {
    Resource resource = new PathResource(TEST_FILE).createRelative("../example.properties");
    assertThat(resource).isEqualTo(new PathResource(TEST_FILE));
  }

  @Test
  void filename() {
    Resource resource = new PathResource(TEST_FILE);
    assertThat(resource.getName()).isEqualTo("example.properties");
  }

  @Test
  void description() {
    Resource resource = new PathResource(TEST_FILE);
    assertThat(resource.toString()).contains("path [");
    assertThat(resource.toString()).contains(TEST_FILE);
  }

  @Test
  void fileIsWritable() {
    PathResource resource = new PathResource(TEST_FILE);
    assertThat(resource.isWritable()).isTrue();
  }

  @Test
  void directoryIsNotWritable() {
    PathResource resource = new PathResource(TEST_DIR);
    assertThat(resource.isWritable()).isFalse();
  }

  @Test
  void equalsAndHashCode() {
    Resource resource1 = new PathResource(TEST_FILE);
    Resource resource2 = new PathResource(TEST_FILE);
    Resource resource3 = new PathResource(TEST_DIR);
    assertThat(resource1).isEqualTo(resource1);
    assertThat(resource1).isEqualTo(resource2);
    assertThat(resource2).isEqualTo(resource1);
    assertThat(resource1).isNotEqualTo(resource3);
    assertThat(resource1).hasSameHashCodeAs(resource2);
    assertThat(resource1).doesNotHaveSameHashCodeAs(resource3);
  }

  @Test
  void outputStream(@TempDir Path temporaryFolder) throws IOException {
    PathResource resource = new PathResource(temporaryFolder.resolve("test"));
    FileCopyUtils.copy("test".getBytes(StandardCharsets.UTF_8), resource.getOutputStream());
    assertThat(resource.contentLength()).isEqualTo(4L);
  }

  @Test
  void doesNotExistOutputStream(@TempDir Path temporaryFolder) throws IOException {
    File file = temporaryFolder.resolve("test").toFile();
    file.delete();
    PathResource resource = new PathResource(file.toPath());
    FileCopyUtils.copy("test".getBytes(), resource.getOutputStream());
    assertThat(resource.contentLength()).isEqualTo(4L);
  }

  @Test
  void directoryOutputStream() throws IOException {
    PathResource resource = new PathResource(TEST_DIR);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
            resource::getOutputStream);
  }

  @Test
  void getReadableByteChannel() throws IOException {
    PathResource resource = new PathResource(TEST_FILE);
    try (ReadableByteChannel channel = resource.readableChannel()) {
      ByteBuffer buffer = ByteBuffer.allocate((int) resource.contentLength());
      channel.read(buffer);
      buffer.rewind();
      assertThat(buffer.limit()).isGreaterThan(0);
    }
  }

  @Test
  void getReadableByteChannelForDir() throws IOException {
    PathResource resource = new PathResource(TEST_DIR);
    try {
      resource.readableChannel();
    }
    catch (AccessDeniedException ex) {
      // on Windows
    }
  }

  @Test
  void getReadableByteChannelDoesNotExist() throws IOException {
    PathResource resource = new PathResource(NON_EXISTING_FILE);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
            resource::readableChannel);
  }

  @Test
  void getWritableChannel(@TempDir Path temporaryFolder) throws IOException {
    Path testPath = temporaryFolder.resolve("test");
    Files.createFile(testPath);
    PathResource resource = new PathResource(testPath);
    ByteBuffer buffer = ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8));
    try (WritableByteChannel channel = resource.writableChannel()) {
      channel.write(buffer);
    }
    assertThat(resource.contentLength()).isEqualTo(4L);
  }

  @Test
  void getContentAsByteArrayReturnsFileContents() throws IOException {
    PathResource resource = new PathResource(TEST_FILE);
    byte[] content = resource.getContentAsByteArray();
    assertThat(content).isNotEmpty();
  }

  @Test
  void getContentAsByteArrayForNonExistingFileThrowsException() {
    PathResource resource = new PathResource(NON_EXISTING_FILE);
    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(resource::getContentAsByteArray);
  }

  @Test
  void getContentAsStringReturnsFileContents() throws IOException {
    PathResource resource = new PathResource(TEST_FILE);
    String content = resource.getContentAsString(StandardCharsets.UTF_8);
    assertThat(content).isNotEmpty();
  }

  @Test
  void getContentAsStringForNonExistingFileThrowsException() {
    PathResource resource = new PathResource(NON_EXISTING_FILE);
    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(() -> resource.getContentAsString(StandardCharsets.UTF_8));
  }

  @Test
  void pathNormalizationRemovesRedundantElements() {
    PathResource resource = new PathResource("src/test/../test/resources/infra/core/io/example.properties");
    assertThat(resource.getPath()).isEqualTo(TEST_FILE);
  }

  @Test
  void pathWithinContextProvidesDotPrefixedRelativePath(@TempDir Path temporaryFolder) throws IOException {
    Path parentDir = temporaryFolder.resolve("parent");
    Path childFile = parentDir.resolve("child.txt");
    Files.createDirectories(parentDir);
    Files.createFile(childFile);

    PathResource resource = new PathResource(childFile);
    assertThat(resource.isFile()).isTrue();
  }

  @Test
  void writableChannelCreatesNewFileIfNotExists(@TempDir Path temporaryFolder) throws IOException {
    Path newFile = temporaryFolder.resolve("new.txt");
    PathResource resource = new PathResource(newFile);

    try (WritableByteChannel channel = resource.writableChannel()) {
      ByteBuffer buffer = ByteBuffer.wrap("test".getBytes());
      channel.write(buffer);
    }

    assertThat(Files.exists(newFile)).isTrue();
  }

  @Test
  void writableChannelToDirectoryThrowsException() {
    PathResource resource = new PathResource(TEST_DIR);
    assertThatExceptionOfType(FileSystemException.class)
            .isThrownBy(resource::writableChannel);
  }

  @Test
  void readableChannelReturnsEmptyStreamForEmptyFile(@TempDir Path temporaryFolder) throws IOException {
    Path emptyFile = temporaryFolder.resolve("empty.txt");
    Files.createFile(emptyFile);

    PathResource resource = new PathResource(emptyFile);
    try (ReadableByteChannel channel = resource.readableChannel()) {
      ByteBuffer buffer = ByteBuffer.allocate(1);
      int bytesRead = channel.read(buffer);
      assertThat(bytesRead).isEqualTo(-1);
    }
  }
}