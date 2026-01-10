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
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 15:04
 */
class InputStreamResourceTests {

  @Test
  void singleReadOfInputStreamIsAllowed() throws IOException {
    InputStream inputStream = new ByteArrayInputStream("test".getBytes());
    InputStreamResource resource = new InputStreamResource(inputStream);

    assertThat(resource.getInputStream()).hasContent("test");
  }

  @Test
  void multipleReadsOfInputStreamThrowException() throws IOException {
    InputStream inputStream = new ByteArrayInputStream("test".getBytes());
    InputStreamResource resource = new InputStreamResource(inputStream);

    resource.getInputStream();
    assertThatIllegalStateException()
            .isThrownBy(resource::getInputStream)
            .withMessageContaining("InputStream has already been read");
  }

  @Test
  void inputStreamSourceProvidesStreamOnDemand() throws IOException {
    AtomicBoolean streamCreated = new AtomicBoolean();
    InputStreamSource source = () -> {
      streamCreated.set(true);
      return new ByteArrayInputStream("test".getBytes());
    };

    InputStreamResource resource = new InputStreamResource(source);
    assertThat(streamCreated).isFalse();

    try (InputStream stream = resource.getInputStream()) {
      assertThat(streamCreated).isTrue();
      assertThat(stream).hasContent("test");
    }
  }

  @Test
  void streamResourceAlwaysExistsAndIsOpen() {
    InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(new byte[0]));

    assertThat(resource.exists()).isTrue();
    assertThat(resource.isOpen()).isTrue();
  }

  @Test
  void descriptionIsIncludedInToString() {
    InputStreamResource resource = new InputStreamResource(
            new ByteArrayInputStream(new byte[0]), "test description");

    assertThat(resource.toString()).contains("test description");
  }

  @Test
  void equalityBasedOnUnderlyingStream() {
    InputStream stream1 = new ByteArrayInputStream(new byte[0]);
    InputStream stream2 = new ByteArrayInputStream(new byte[0]);

    InputStreamResource resource1 = new InputStreamResource(stream1);
    InputStreamResource resource2 = new InputStreamResource(stream1);
    InputStreamResource resource3 = new InputStreamResource(stream2);

    assertThat(resource1)
            .isEqualTo(resource1)
            .isEqualTo(resource2)
            .isNotEqualTo(resource3)
            .isNotEqualTo(null)
            .isNotEqualTo(new Object());
  }

  @Test
  void nullInputStreamThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new InputStreamResource((InputStream) null))
            .withMessage("InputStream is required");
  }

  @Test
  void nullInputStreamSourceThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new InputStreamResource((InputStreamSource) null))
            .withMessage("InputStreamSource is required");
  }

  @Test
  void equalsWithSameSourceReturnsSameHashCode() {
    InputStream stream = new ByteArrayInputStream(new byte[0]);
    InputStreamResource resource1 = new InputStreamResource(stream);
    InputStreamResource resource2 = new InputStreamResource(stream);

    assertThat(resource1.hashCode()).isEqualTo(resource2.hashCode());
  }

  @Test
  void inputStreamResourceWithNullDescriptionUsesEmptyString() {
    InputStreamResource resource = new InputStreamResource(
            new ByteArrayInputStream(new byte[0]), null);
    assertThat(resource.toString()).isEqualTo("InputStream resource []");
  }

  @Test
  void resourceEqualityBasedOnInputStreamSourceEquality() {
    InputStreamSource source1 = () -> new ByteArrayInputStream(new byte[0]);
    InputStreamSource source2 = () -> new ByteArrayInputStream(new byte[0]);

    InputStreamResource resource1 = new InputStreamResource(source1);
    InputStreamResource resource2 = new InputStreamResource(source1);
    InputStreamResource resource3 = new InputStreamResource(source2);

    assertThat(resource1)
            .isEqualTo(resource1)
            .isEqualTo(resource2)
            .isNotEqualTo(resource3);
  }

  @Test
  void earlyContentLengthReadPreventsSubsequentRead() throws IOException {
    InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(new byte[10]));
    resource.contentLength();

    assertThatIllegalStateException()
            .isThrownBy(resource::getInputStream)
            .withMessageContaining("InputStream has already been read");
  }

  @Test
  void multipleReadsFromSourceNotAllowed() throws IOException {
    AtomicBoolean firstRead = new AtomicBoolean();
    InputStreamSource source = () -> {
      if (firstRead.getAndSet(true)) {
        throw new AssertionError("Source should only be read once");
      }
      return new ByteArrayInputStream(new byte[0]);
    };

    InputStreamResource resource = new InputStreamResource(source);
    resource.getInputStream();

    assertThatIllegalStateException()
            .isThrownBy(resource::getInputStream)
            .withMessageContaining("InputStream has already been read");
  }
}