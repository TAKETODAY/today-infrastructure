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

import static org.assertj.core.api.Assertions.*;

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