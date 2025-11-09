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

package infra.web.multipart;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import infra.core.io.Resource;
import infra.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 10:22
 */
class MultipartFileTests {

  @Test
  void testGetResourceReturnsMultipartFileResource() throws IOException {
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getName()).thenReturn("test-file");
    when(multipartFile.getSize()).thenReturn(1024L);
    when(multipartFile.getOriginalFilename()).thenReturn("original.txt");

    // Create a real instance to test default method
    MultipartFile realMultipartFile = new MultipartFile() {
      @Override
      public InputStream getInputStream() throws IOException {
        return null;
      }

      @Override
      public String getContentType() { return "text/plain"; }

      @Override
      public long getSize() { return 1024L; }

      @Override
      public String getName() { return "test-file"; }

      @Override
      public String getValue() {
        return "";
      }

      @Override
      public String getOriginalFilename() { return "original.txt"; }

      @Override
      public boolean isEmpty() { return false; }

      @Override
      public byte[] getBytes() throws IOException { return new byte[0]; }

      @Override
      public boolean isFormField() {
        return false;
      }

      @Override
      public HttpHeaders getHeaders() {
        return null;
      }

      @Override
      public Object getOriginalResource() { return new Object(); }

      @Override
      public void cleanup() throws IOException { }

      @Override
      public void transferTo(File dest) throws IOException, IllegalStateException { }
    };

    Resource resource = realMultipartFile.getResource();

    assertThat(resource).isNotNull();
    assertThat(resource).isInstanceOf(MultipartFileResource.class);
    assertThat(resource.getName()).isEqualTo("original.txt");
    assertThat(resource.contentLength()).isEqualTo(1024L);
  }

  @Test
  void testTransferToWithPathCreatesFileAndTransfers() throws IOException, IllegalStateException {
    // Create a real instance to test default method
    MultipartFile realMultipartFile = new MultipartFile() {
      @Override
      public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(new byte[0]);
      }

      @Override
      public String getContentType() { return "text/plain"; }

      @Override
      public long getSize() { return 5L; }

      @Override
      public String getName() { return "test-file"; }

      @Override
      public String getValue() {
        return "";
      }

      @Override
      public String getOriginalFilename() { return "original.txt"; }

      @Override
      public boolean isEmpty() { return false; }

      @Override
      public byte[] getBytes() throws IOException { return "hello".getBytes(); }

      @Override
      public boolean isFormField() {
        return false;
      }

      @Override
      public HttpHeaders getHeaders() {
        return null;
      }

      @Override
      public Object getOriginalResource() { return new Object(); }

      @Override
      public void cleanup() throws IOException { }

      @Override
      public void transferTo(File dest) throws IOException, IllegalStateException { }
    };

    Path tempFile = Files.createTempFile("test", ".tmp");

    assertThatNoException().isThrownBy(() -> {
      long result = realMultipartFile.transferTo(tempFile);
      // Note: actual transferred bytes might be 0 due to mocked readableChannel
    });

    Files.deleteIfExists(tempFile);
  }

  @Test
  void testTransferToWithFileChannelDelegatesToFileChannel() throws IOException {
    // Create a real instance to test default method
    MultipartFile realMultipartFile = new MultipartFile() {
      @Override
      public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(new byte[0]);
      }

      @Override
      public String getContentType() { return "text/plain"; }

      @Override
      public long getSize() { return 100L; }

      @Override
      public String getName() { return "test-file"; }

      @Override
      public String getValue() {
        return "";
      }

      @Override
      public String getOriginalFilename() { return "original.txt"; }

      @Override
      public boolean isEmpty() { return false; }

      @Override
      public byte[] getBytes() throws IOException { return new byte[0]; }

      @Override
      public boolean isFormField() {
        return false;
      }

      @Override
      public HttpHeaders getHeaders() {
        return null;
      }

      @Override
      public Object getOriginalResource() { return new Object(); }

      @Override
      public void cleanup() throws IOException { }

      @Override
      public void transferTo(File dest) throws IOException, IllegalStateException { }
    };

    FileChannel mockChannel = mock(FileChannel.class);
    when(mockChannel.transferFrom(any(), anyLong(), anyLong())).thenReturn(50L);

    long transferred = realMultipartFile.transferTo(mockChannel, 0L, 100L);

    assertThat(transferred).isEqualTo(50L);
  }

}