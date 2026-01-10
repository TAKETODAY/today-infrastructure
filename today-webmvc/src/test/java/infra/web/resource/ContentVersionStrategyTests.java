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

package infra.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.util.DigestUtils;
import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContentVersionStrategy}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
class ContentVersionStrategyTests {

  private final ContentVersionStrategy versionStrategy = new ContentVersionStrategy();

  @BeforeEach
  public void setup() {
    VersionResourceResolver versionResourceResolver = new VersionResourceResolver();
    versionResourceResolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
  }

  @Test
  public void extractVersion() {
    String hash = "7fbe76cdac6093784895bb4989203e5a";
    String path = "font-awesome/css/font-awesome.min-" + hash + ".css";

    assertThat(this.versionStrategy.extractVersion(path)).isEqualTo(hash);
    assertThat(this.versionStrategy.extractVersion("foo/bar.css")).isNull();
  }

  @Test
  public void removeVersion() {
    String hash = "7fbe76cdac6093784895bb4989203e5a";
    String file = "font-awesome/css/font-awesome.min%s%s.css";

    assertThat(this.versionStrategy.removeVersion(String.format(file, "-", hash), hash)).isEqualTo(String.format(file, "", ""));
  }

  @Test
  public void getResourceVersion() throws IOException {
    Resource expected = new ClassPathResource("test/bar.css", getClass());
    String hash = DigestUtils.md5DigestAsHex(FileCopyUtils.copyToByteArray(expected.getInputStream()));

    assertThat(this.versionStrategy.getResourceVersion(expected)).isEqualTo(hash);
  }

  @Test
  public void addVersionToUrl() {
    assertThat(this.versionStrategy.addVersion("test/bar.css", "123")).isEqualTo("test/bar-123.css");
  }

  @Test
  void constructorShouldInitializeWithFileNameVersionPathStrategy() {
    ContentVersionStrategy strategy = new ContentVersionStrategy();

    // Test by calling methods that depend on the strategy being properly initialized
    String path = "css/style-e36d2e05253c6c7085a91522ce43a0b4.css";
    String version = strategy.extractVersion(path);

    // This verifies the FileNameVersionPathStrategy is properly set up
    assertThat(version).isEqualTo("e36d2e05253c6c7085a91522ce43a0b4");
  }

  @Test
  void getResourceVersionShouldCalculateMD5Hash() throws IOException {
    ContentVersionStrategy strategy = new ContentVersionStrategy();
    Resource resource = new ClassPathResource("test/bar.css", getClass());

    String version = strategy.getResourceVersion(resource);

    // Calculate expected hash manually to verify correctness
    byte[] content = FileCopyUtils.copyToByteArray(resource.getInputStream());
    String expectedVersion = DigestUtils.md5DigestAsHex(content);

    assertThat(version).isEqualTo(expectedVersion);
    assertThat(version).hasSize(32); // MD5 hash is 32 characters long
  }

  @Test
  void getResourceVersionShouldThrowIllegalStateExceptionOnIOException() throws IOException {
    ContentVersionStrategy strategy = new ContentVersionStrategy();
    Resource resource = mock(Resource.class);
    when(resource.getInputStream()).thenThrow(new IOException("Test exception"));

    assertThatThrownBy(() -> strategy.getResourceVersion(resource))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to calculate hash for")
            .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void extractVersionShouldReturnNullWhenNoVersionInPath() {
    ContentVersionStrategy strategy = new ContentVersionStrategy();

    String version = strategy.extractVersion("css/style.css");

    assertThat(version).isNull();
  }

  @Test
  void extractVersionShouldReturnVersionWhenValidVersionInPath() {
    ContentVersionStrategy strategy = new ContentVersionStrategy();
    String hash = "a1b2c3d4e5f6789012345678901234ab";
    String path = "js/script-" + hash + ".js";

    String version = strategy.extractVersion(path);

    assertThat(version).isEqualTo(hash);
  }

  @Test
  void removeVersionShouldRemoveVersionFromPath() {
    ContentVersionStrategy strategy = new ContentVersionStrategy();
    String hash = "7fbe76cdac6093784895bb4989203e5a";
    String pathWithVersion = "font-awesome/css/font-awesome.min-" + hash + ".css";
    String expectedPath = "font-awesome/css/font-awesome.min.css";

    String result = strategy.removeVersion(pathWithVersion, hash);

    assertThat(result).isEqualTo(expectedPath);
  }

  @Test
  void removeVersionShouldHandleComplexPaths() {
    ContentVersionStrategy strategy = new ContentVersionStrategy();
    String hash = "a1b2c3d4e5f6789012345678901234ab";
    String pathWithVersion = "webjars/bootstrap/3.2.0/css/bootstrap-" + hash + ".min.css";
    String expectedPath = "webjars/bootstrap/3.2.0/css/bootstrap.min.css";

    String result = strategy.removeVersion(pathWithVersion, hash);

    assertThat(result).isEqualTo(expectedPath);
  }

  @Test
  void addVersionToUrlShouldAppendVersionCorrectly() {
    ContentVersionStrategy strategy = new ContentVersionStrategy();
    String path = "css/style.css";
    String version = "1234567890abcdef1234567890abcdef";
    String expectedPath = "css/style-1234567890abcdef1234567890abcdef.css";

    String result = strategy.addVersion(path, version);

    assertThat(result).isEqualTo(expectedPath);
  }

  @Test
  void addVersionToUrlShouldHandlePathsInSubdirectories() {
    ContentVersionStrategy strategy = new ContentVersionStrategy();
    String path = "static/js/components/button.js";
    String version = "abcdef1234567890abcdef1234567890";
    String expectedPath = "static/js/components/button-abcdef1234567890abcdef1234567890.js";

    String result = strategy.addVersion(path, version);

    assertThat(result).isEqualTo(expectedPath);
  }

}
