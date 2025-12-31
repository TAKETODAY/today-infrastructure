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

package infra.web.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 12:38
 */
class TransformedResourceTests {

  @Test
  void constructorShouldPreserveOriginalResourceProperties() throws IOException {
    Resource original = mock(Resource.class);
    byte[] transformedContent = "transformed content".getBytes();
    String filename = "test.txt";
    long lastModified = System.currentTimeMillis();

    when(original.getName()).thenReturn(filename);
    when(original.lastModified()).thenReturn(lastModified);

    TransformedResource transformedResource = new TransformedResource(original, transformedContent);

    assertThat(transformedResource.getName()).isEqualTo(filename);
    assertThat(transformedResource.lastModified()).isEqualTo(lastModified);
    assertThat(transformedResource.getByteArray()).isEqualTo(transformedContent);
  }

  @Test
  void constructorShouldThrowIllegalArgumentExceptionWhenLastModifiedFails() throws IOException {
    Resource original = mock(Resource.class);
    byte[] transformedContent = "transformed content".getBytes();

    when(original.getName()).thenReturn("test.txt");
    when(original.lastModified()).thenThrow(new IOException("Failed to get last modified"));

    assertThatIllegalArgumentException()
            .isThrownBy(() -> new TransformedResource(original, transformedContent))
            .withCauseInstanceOf(IOException.class);
  }

  @Test
  void getNameShouldReturnOriginalFilename() throws IOException {
    Resource original = mock(Resource.class);
    byte[] transformedContent = "content".getBytes();
    String filename = "original.txt";

    when(original.getName()).thenReturn(filename);
    when(original.lastModified()).thenReturn(1000L);

    TransformedResource transformedResource = new TransformedResource(original, transformedContent);

    assertThat(transformedResource.getName()).isEqualTo(filename);
  }

  @Test
  void getNameShouldReturnNullWhenOriginalHasNullName() throws IOException {
    Resource original = mock(Resource.class);
    byte[] transformedContent = "content".getBytes();

    when(original.getName()).thenReturn(null);
    when(original.lastModified()).thenReturn(1000L);

    TransformedResource transformedResource = new TransformedResource(original, transformedContent);

    assertThat(transformedResource.getName()).isNull();
  }

  @Test
  void lastModifiedShouldReturnOriginalLastModified() throws IOException {
    Resource original = mock(Resource.class);
    byte[] transformedContent = "content".getBytes();
    long lastModified = 1234567890L;

    when(original.getName()).thenReturn("test.txt");
    when(original.lastModified()).thenReturn(lastModified);

    TransformedResource transformedResource = new TransformedResource(original, transformedContent);

    assertThat(transformedResource.lastModified()).isEqualTo(lastModified);
  }

  @Test
  void getByteArrayShouldReturnTransformedContent() throws IOException {
    Resource original = mock(Resource.class);
    byte[] transformedContent = "modified content".getBytes();

    when(original.getName()).thenReturn("test.txt");
    when(original.lastModified()).thenReturn(1000L);

    TransformedResource transformedResource = new TransformedResource(original, transformedContent);

    assertThat(transformedResource.getByteArray()).isEqualTo(transformedContent);
  }

}