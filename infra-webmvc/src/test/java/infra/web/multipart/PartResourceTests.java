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

package infra.web.multipart;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 10:24
 */
class PartResourceTests {

  @Test
  void testConstructor() {
    Part Part = mock(Part.class);
    PartResource resource = new PartResource(Part);

    assertThat(resource).isNotNull();
  }

  @Test
  void testExists() {
    Part Part = mock(Part.class);
    PartResource resource = new PartResource(Part);

    assertThat(resource.exists()).isTrue();
  }

  @Test
  void testIsOpen() {
    Part Part = mock(Part.class);
    PartResource resource = new PartResource(Part);

    assertThat(resource.isOpen()).isTrue();
  }

  @Test
  void testContentLength() {
    Part Part = mock(Part.class);
    when(Part.getContentLength()).thenReturn(1024L);
    PartResource resource = new PartResource(Part);

    assertThat(resource.contentLength()).isEqualTo(1024L);
  }

  @Test
  void testGetName() {
    Part Part = mock(Part.class);
    when(Part.getOriginalFilename()).thenReturn("test.txt");
    PartResource resource = new PartResource(Part);

    assertThat(resource.getName()).isEqualTo("test.txt");
  }

  @Test
  void testGetInputStream() throws IOException, IllegalStateException {
    Part Part = mock(Part.class);
    InputStream inputStream = mock(InputStream.class);
    when(Part.getInputStream()).thenReturn(inputStream);
    PartResource resource = new PartResource(Part);

    assertThat(resource.getInputStream()).isEqualTo(inputStream);
  }

  @Test
  void testToString() {
    Part Part = mock(Part.class);
    when(Part.getName()).thenReturn("test-file");
    PartResource resource = new PartResource(Part);

    assertThat(resource.toString()).isEqualTo("Part resource [test-file]");
  }

  @Test
  void testEquals() {
    Part Part = mock(Part.class);
    PartResource resource1 = new PartResource(Part);
    PartResource resource2 = new PartResource(Part);

    assertThat(resource1).isEqualTo(resource2);
    assertThat(resource1).isEqualTo(resource1);
    assertThat(resource1).isNotEqualTo(new Object());
  }

  @Test
  void testHashCode() {
    Part Part = mock(Part.class);
    PartResource resource = new PartResource(Part);

    assertThat(resource.hashCode()).isEqualTo(Part.hashCode());
  }

}