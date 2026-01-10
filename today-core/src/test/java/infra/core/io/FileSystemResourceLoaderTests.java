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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 14:58
 */
class FileSystemResourceLoaderTests {

  @Test
  void pathWithoutSlashResolvesToFileSystemResource() {
    FileSystemResourceLoader loader = new FileSystemResourceLoader();
    Resource resource = loader.getResource("test.txt");

    assertThat(resource).isInstanceOf(FileSystemResource.class);
    assertThat(resource.getName()).isEqualTo("test.txt");
  }

  @Test
  void pathWithLeadingSlashStripsSlash() {
    FileSystemResourceLoader loader = new FileSystemResourceLoader();
    Resource resource = loader.getResource("/test.txt");

    assertThat(resource).isInstanceOf(FileSystemResource.class);
    assertThat(resource.getName()).isEqualTo("test.txt");
  }

  @Test
  void fileProtocolResolvesToFileSystemResource() {
    FileSystemResourceLoader loader = new FileSystemResourceLoader();
    Resource resource = loader.getResource("file:test.txt");

    assertThat(resource).isInstanceOf(FileUrlResource.class);
    assertThat(resource.getName()).isEqualTo("test.txt");
  }

  @Test
  void classPathProtocolResolvesToClassPathResource() {
    FileSystemResourceLoader loader = new FileSystemResourceLoader();
    Resource resource = loader.getResource("classpath:test.txt");

    assertThat(resource).isInstanceOf(ClassPathResource.class);
    assertThat(resource.getName()).isEqualTo("test.txt");
  }

  @Test
  void urlProtocolResolvesToUrlResource() {
    FileSystemResourceLoader loader = new FileSystemResourceLoader();
    Resource resource = loader.getResource("https://example.com/test.txt");

    assertThat(resource).isInstanceOf(UrlResource.class);
    assertThat(resource.getName()).isEqualTo("test.txt");
  }

  @Test
  void contextResourceProvidesPathWithinContext() {
    FileSystemResourceLoader loader = new FileSystemResourceLoader();
    Resource resource = loader.getResource("/path/test.txt");

    assertThat(resource).isInstanceOf(ContextResource.class);
    assertThat(((ContextResource) resource).getPathWithinContext()).isEqualTo("path/test.txt");
  }

  @Test
  void emptyPathResolvesToEmptyFileSystemResource() {
    FileSystemResourceLoader loader = new FileSystemResourceLoader();
    Resource resource = loader.getResource("");

    assertThat(resource).isInstanceOf(FileSystemResource.class);
    assertThat(resource.getName()).isEmpty();
  }

  @Test
  void nullLocationThrowsIllegalArgumentException() {
    FileSystemResourceLoader loader = new FileSystemResourceLoader();

    assertThatIllegalArgumentException()
            .isThrownBy(() -> loader.getResource(null))
            .withMessage("Location is required");
  }

}