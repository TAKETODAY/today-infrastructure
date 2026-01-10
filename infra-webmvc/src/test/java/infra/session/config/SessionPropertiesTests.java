/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.session.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 20:48
 */
class SessionPropertiesTests {

  @Test
  void defaultValuesAreSet() {
    SessionProperties properties = new SessionProperties();
    assertThat(properties.getTimeout()).isEqualTo(Duration.ofMinutes(30));
    assertThat(properties.isPersistent()).isFalse();
    assertThat(properties.getStoreDir()).isNull();
    assertThat(properties.getSessionIdLength()).isEqualTo(32);
    assertThat(properties.getMaxSessions()).isEqualTo(10000);
    assertThat(properties.cookie).isNotNull();
  }

  @Test
  void setTimeoutToNullIsAllowed() {
    var properties = new SessionProperties();
    properties.setTimeout(null);
    assertThat(properties.getTimeout()).isNull();
  }

  @Test
  void setPersistentUpdatesPersistent() {
    var properties = new SessionProperties();
    properties.setPersistent(true);
    assertThat(properties.isPersistent()).isTrue();
  }

  @Test
  void setStoreDirUpdatesStoreDir(@TempDir File tempDir) {
    var properties = new SessionProperties();
    properties.setStoreDir(tempDir);
    assertThat(properties.getStoreDir()).isEqualTo(tempDir);
  }

  @Test
  void setSessionIdLengthUpdatesSessionIdLength() {
    var properties = new SessionProperties();
    properties.setSessionIdLength(64);
    assertThat(properties.getSessionIdLength()).isEqualTo(64);
  }

  @Test
  void setSessionIdLengthWithZeroThrowsException() {
    var properties = new SessionProperties();
    assertThatIllegalArgumentException().isThrownBy(() -> properties.setSessionIdLength(0));
  }

  @Test
  void setSessionIdLengthWithNegativeValueThrowsException() {
    var properties = new SessionProperties();
    assertThatIllegalArgumentException().isThrownBy(() -> properties.setSessionIdLength(-1));
  }

  @Test
  void setMaxSessionsUpdatesMaxSessions() {
    var properties = new SessionProperties();
    properties.setMaxSessions(20000);
    assertThat(properties.getMaxSessions()).isEqualTo(20000);
  }

  @Test
  void getValidStoreDirWhenStoreDirIsFileThrowsException(@TempDir File tempDir) throws IOException {
    var properties = new SessionProperties();
    var file = new File(tempDir, "file.txt");
    file.createNewFile();
    properties.setStoreDir(file);
    assertThatIllegalStateException().isThrownBy(() -> properties.getValidStoreDir(null))
            .withMessageContaining("points to a file");
  }

  @Test
  void getValidStoreDirWithRelativePathResolvesRelativeToApplicationHome() {
    // This test assumes ApplicationHome resolves to the project root, which is standard.
    var properties = new SessionProperties();
    var relativeDir = new File("build/sessions");
    properties.setStoreDir(relativeDir);
    var validStoreDir = properties.getValidStoreDir(null);
    assertThat(validStoreDir).isAbsolute();
    assertThat(validStoreDir.getAbsolutePath().endsWith(relativeDir.getPath())).isTrue();
    assertThat(validStoreDir).exists().isDirectory();
    // Cleanup
    validStoreDir.delete();
  }

  @Test
  void getValidStoreDirWhenMkdirsIsFalseAndDirExistsReturnsDir(@TempDir File tempDir) {
    var properties = new SessionProperties();
    properties.setStoreDir(tempDir);
    var validStoreDir = properties.getValidStoreDir(null, false);
    assertThat(validStoreDir).isEqualTo(tempDir);
  }

}
