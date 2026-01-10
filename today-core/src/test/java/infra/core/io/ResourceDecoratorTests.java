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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

import infra.util.ExceptionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 14:03
 */
class ResourceDecoratorTests {

  @Test
  void delegatesGetInputStream() throws IOException {
    Resource delegate = mock(Resource.class);
    InputStream expected = new ByteArrayInputStream("test".getBytes());
    when(delegate.getInputStream()).thenReturn(expected);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.getInputStream()).isSameAs(expected);
  }

  @Test
  void delegatesExists() throws IOException {
    Resource delegate = mock(Resource.class);
    when(delegate.exists()).thenReturn(true);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.exists()).isTrue();
  }

  @Test
  void delegatesIsReadable() {
    Resource delegate = mock(Resource.class);
    when(delegate.isReadable()).thenReturn(true);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.isReadable()).isTrue();
  }

  @Test
  void delegatesIsFile() {
    Resource delegate = mock(Resource.class);
    when(delegate.isFile()).thenReturn(true);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.isFile()).isTrue();
  }

  @Test
  void delegatesGetFile() throws IOException {
    Resource delegate = mock(Resource.class);
    File expected = new File("test.txt");
    when(delegate.getFile()).thenReturn(expected);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.getFile()).isSameAs(expected);
  }

  @Test
  void delegatesGetURL() throws IOException {
    Resource delegate = mock(Resource.class);
    URL expected = new URL("file:test.txt");
    when(delegate.getURL()).thenReturn(expected);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.getURL()).isSameAs(expected);
  }

  @Test
  void delegatesGetURI() throws IOException {
    Resource delegate = mock(Resource.class);
    URI expected = URI.create("file:test.txt");
    when(delegate.getURI()).thenReturn(expected);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.getURI()).isSameAs(expected);
  }

  @Test
  void delegatesGetDescription() {
    Resource delegate = mock(Resource.class);
    when(delegate.toString()).thenReturn("Test Resource");

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.toString()).isEqualTo("Test Resource");
  }

  @Test
  void delegatesContentLength() throws IOException {
    Resource delegate = mock(Resource.class);
    when(delegate.contentLength()).thenReturn(42L);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.contentLength()).isEqualTo(42L);
  }

  @Test
  void delegatesLastModified() throws IOException {
    Resource delegate = mock(Resource.class);
    when(delegate.lastModified()).thenReturn(123L);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.lastModified()).isEqualTo(123L);
  }

  @Test
  void delegatesCreateRelative() throws IOException {
    Resource delegate = mock(Resource.class);
    Resource expected = mock(Resource.class);
    when(delegate.createRelative("test.txt")).thenReturn(expected);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.createRelative("test.txt")).isSameAs(expected);
  }

  @Test
  void delegatesGetName() {
    Resource delegate = mock(Resource.class);
    when(delegate.getName()).thenReturn("test.txt");

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.getName()).isEqualTo("test.txt");
  }

  @Test
  void delegatesIsOpen() {
    Resource delegate = mock(Resource.class);
    when(delegate.isOpen()).thenReturn(true);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.isOpen()).isTrue();
  }

  @Test
  void delegatesGetReader() throws IOException {
    Resource delegate = mock(Resource.class);
    Reader expected = mock(Reader.class);
    when(delegate.getReader()).thenReturn(expected);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.getReader()).isSameAs(expected);
  }

  @Test
  void delegatesGetReaderWithEncoding() throws IOException {
    Resource delegate = mock(Resource.class);
    Reader expected = mock(Reader.class);
    when(delegate.getReader(StandardCharsets.UTF_8)).thenReturn(expected);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.getReader(StandardCharsets.UTF_8)).isSameAs(expected);
  }

  @Test
  void delegatesReadableChannel() throws IOException {
    Resource delegate = mock(Resource.class);
    ReadableByteChannel expected = mock(ReadableByteChannel.class);
    when(delegate.readableChannel()).thenReturn(expected);

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.readableChannel()).isSameAs(expected);
  }

  @Test
  void existsReturnsFalseOnIOException() throws IOException {
    Resource delegate = new ResourceDecorator(mock(Resource.class)) {
      @Override
      public boolean exists() {
        throw ExceptionUtils.sneakyThrow(new IOException());
      }
    };

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThat(decorator.exists()).isFalse();
  }

  @Test
  void existsRethrowsNonIOException() {
    Resource delegate = mock(Resource.class);
    when(delegate.exists()).thenThrow(new RuntimeException());

    TestResourceDecorator decorator = new TestResourceDecorator(delegate);
    assertThrows(RuntimeException.class, decorator::exists);
  }

  @Test
  void setDelegateChangesDelegate() {
    Resource originalDelegate = mock(Resource.class);
    Resource newDelegate = mock(Resource.class);

    TestResourceDecorator decorator = new TestResourceDecorator(originalDelegate);
    decorator.setDelegate(newDelegate);

    assertThat(decorator.getDelegate()).isSameAs(newDelegate);
  }

  @Test
  void equalsWithSameDelegate() {
    Resource delegate = mock(Resource.class);
    TestResourceDecorator decorator1 = new TestResourceDecorator(delegate);
    TestResourceDecorator decorator2 = new TestResourceDecorator(delegate);

    assertThat(decorator1).isEqualTo(decorator2);
  }

  @Test
  void equalsWithDifferentDelegate() {
    TestResourceDecorator decorator1 = new TestResourceDecorator(mock(Resource.class));
    TestResourceDecorator decorator2 = new TestResourceDecorator(mock(Resource.class));

    assertThat(decorator1).isNotEqualTo(decorator2);
  }

  static class TestResourceDecorator extends ResourceDecorator {
    TestResourceDecorator(Resource delegate) {
      super(delegate);
    }
  }
}