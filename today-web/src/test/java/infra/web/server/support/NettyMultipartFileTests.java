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

package infra.web.server.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import infra.core.io.FileSystemResource;
import infra.core.io.Resource;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.multipart.FileUpload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 21:51
 */
class NettyMultipartFileTests {

  @Test
  void shouldReturnCorrectContentType() {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    when(fileUpload.getContentType()).thenReturn("text/plain");

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when & then
    assertThat(multipartFile.getContentType()).isEqualTo("text/plain");
  }

  @Test
  void shouldReturnCorrectSize() {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    when(fileUpload.length()).thenReturn(1024L);

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when & then
    assertThat(multipartFile.getContentLength()).isEqualTo(1024L);
  }

  @Test
  void shouldReturnCorrectName() {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    when(fileUpload.getName()).thenReturn("file");

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when & then
    assertThat(multipartFile.getName()).isEqualTo("file");
  }

  @Test
  void shouldReturnCorrectOriginalFilename() {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    when(fileUpload.getFilename()).thenReturn("original.txt");

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when & then
    assertThat(multipartFile.getOriginalFilename()).isEqualTo("original.txt");
  }

  @Test
  void shouldBeEmptyWhenSizeIsZero() {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    when(fileUpload.length()).thenReturn(0L);

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when & then
    assertThat(multipartFile.isEmpty()).isTrue();
  }

  @Test
  void shouldNotBeEmptyWhenSizeGreaterThanZero() {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    when(fileUpload.length()).thenReturn(1L);

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when & then
    assertThat(multipartFile.isEmpty()).isFalse();
  }

  @Test
  void shouldReturnOriginalResource() {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when & then
    assertThat(multipartFile.getOriginalResource()).isSameAs(fileUpload);
  }

  @Test
  void shouldReturnInputStreamForInMemoryFile() throws IOException {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    ByteBuf byteBuf = mock(ByteBuf.class);
    when(fileUpload.isInMemory()).thenReturn(true);
    when(fileUpload.getByteBuf()).thenReturn(byteBuf);
    when(byteBuf.resetReaderIndex()).thenReturn(byteBuf);

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when
    InputStream inputStream = multipartFile.getInputStream();

    // then
    assertThat(inputStream).isNotNull().isInstanceOf(ByteBufInputStream.class);
  }

  @Test
  void shouldReturnInputStreamForFileBasedUpload(@TempDir File file) throws IOException {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    when(fileUpload.isInMemory()).thenReturn(false);
    File temp = new File(file, "temp");
    temp.createNewFile();
    when(fileUpload.getFile()).thenReturn(temp);

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when
    InputStream inputStream = multipartFile.getInputStream();

    // then
    assertThat(inputStream).isNotNull().isInstanceOf(FileInputStream.class);
    temp.delete();
  }

  @Test
  void shouldTransferInMemoryDataToOutputStream() throws IOException {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    ByteBuf byteBuf = mock(ByteBuf.class);
    OutputStream out = mock(OutputStream.class);

    when(fileUpload.isInMemory()).thenReturn(true);
    when(fileUpload.getByteBuf()).thenReturn(byteBuf);
    when(byteBuf.readableBytes()).thenReturn(100);
    when(byteBuf.readBytes(out, 100)).thenReturn(byteBuf);

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when
    long transferred = multipartFile.transferTo(out);

    // then
    assertThat(transferred).isEqualTo(100);
  }

  @Test
  void shouldTransferFileDataToOutputStream(@TempDir File file) throws IOException {
    File temp = new File(file, "temp");
    temp.createNewFile();

    // given
    FileUpload fileUpload = mock(FileUpload.class);
    OutputStream out = mock(OutputStream.class);

    when(fileUpload.isInMemory()).thenReturn(false);
    when(fileUpload.getFile()).thenReturn(temp);

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when
    long transferred = multipartFile.transferTo(out);

    // then
    assertThat(transferred).isGreaterThanOrEqualTo(0);
    temp.delete();
  }

  @Test
  void shouldTransferInMemoryDataToFileChannel() throws IOException {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    ByteBuf byteBuf = mock(ByteBuf.class);
    FileChannel fileChannel = mock(FileChannel.class);

    when(fileUpload.isInMemory()).thenReturn(true);
    when(fileUpload.getByteBuf()).thenReturn(byteBuf);
    when(fileUpload.length()).thenReturn(100L);
    when(byteBuf.readBytes(fileChannel, 0, 100)).thenReturn(100);

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when
    long transferred = multipartFile.transferTo(fileChannel, 0, 100);

    // then
    assertThat(transferred).isEqualTo(100);
  }

  @Test
  void shouldTransferFileDataToFileChannel(@TempDir File file) throws IOException {
    File temp = new File(file, "temp");
    temp.createNewFile();

    // given
    FileUpload fileUpload = mock(FileUpload.class);
    FileChannel fileChannel = mock(FileChannel.class);
    FileChannel srcChannel = mock(FileChannel.class);

    when(fileUpload.isInMemory()).thenReturn(false);
    when(fileUpload.getFile()).thenReturn(temp);
    when(fileChannel.transferFrom(srcChannel, 0, 100)).thenReturn(100L);

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when
    long transferred = multipartFile.transferTo(fileChannel, 0, 100);

    // then
    assertThat(transferred).isGreaterThanOrEqualTo(0);
    temp.delete();
  }

  @Test
  void shouldReturnFileSystemResourceForFileBasedUpload() throws IOException {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    File file = mock(File.class);

    when(fileUpload.isInMemory()).thenReturn(false);
    when(fileUpload.getFile()).thenReturn(file);

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when
    Resource resource = multipartFile.getResource();

    // then
    assertThat(resource).isNotNull().isInstanceOf(FileSystemResource.class);
  }

  @Test
  void shouldReturnBytesFromFileUpload() throws IOException {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    byte[] expectedBytes = new byte[] { 1, 2, 3, 4 };

    when(fileUpload.get()).thenReturn(expectedBytes);

    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when
    byte[] bytes = multipartFile.getContentAsByteArray();

    // then
    assertThat(bytes).isEqualTo(expectedBytes);
  }

  @Test
  void shouldCallDeleteOnFileUpload() throws IOException {
    // given
    FileUpload fileUpload = mock(FileUpload.class);
    NettyMultipartFile multipartFile = new NettyMultipartFile(fileUpload);

    // when
    multipartFile.cleanup();

    verify(fileUpload).delete();
  }

}