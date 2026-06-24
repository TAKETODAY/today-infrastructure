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

package infra.mock.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import java.io.InputStream;
import java.net.URL;

import infra.core.io.FileSystemResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MockContextImpl}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
@DisplayName("MockContext unit tests")
class MockContextTests {

  /**
   * @since 4.0
   */
  @Nested
  @DisplayName("with FileSystemResourceLoader")
  class MockContextWithFileSystemResourceLoaderTests {

    private final MockContextImpl mockContext =
            new MockContextImpl("infra/mock", new FileSystemResourceLoader());

    @Test
    void getResourceWithRelativePathToWindowsCDrive() throws Exception {
      URL resource = mockContext.getResource("C:\\temp");
      assertThat(resource).isNull();
    }

    @Test
    void getResourceAsStreamWithRelativePathToWindowsCDrive() {
      InputStream inputStream = mockContext.getResourceAsStream("C:\\temp");
      assertThat(inputStream).isNull();
    }

    @Test
    void getRealPathWithRelativePathToWindowsCDrive() {
      String realPath = mockContext.getRealPath("C:\\temp");

      if (OS.WINDOWS.isCurrentOs()) {
        assertThat(realPath).isNull();
      }
      else {
        assertThat(realPath).isNotNull();
      }
    }

  }

}
