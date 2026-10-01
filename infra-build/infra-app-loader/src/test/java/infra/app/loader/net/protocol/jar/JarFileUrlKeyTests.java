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

package infra.app.loader.net.protocol.jar;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;

import infra.app.loader.net.protocol.Handlers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarFileUrlKey}.
 *
 * @author Phillip Webb
 */
class JarFileUrlKeyTests {

  @Test
  void directKeyEqualsUrlKey() throws Exception {
    JarFileUrlKey fromUrl = new JarFileUrlKey(new URL("jar:nested:/my.jar/!mynested.jar!/my/path"));
    JarFileUrlKey direct = new JarFileUrlKey("jar", "", -1, "nested:/my.jar/!mynested.jar!/my/path", false);
    assertThat(direct).isEqualTo(fromUrl);
    assertThat(direct.hashCode()).isEqualTo(fromUrl.hashCode());
  }

  @Test
  void directKeyWithRuntimeRefEqualsUrlKey() throws Exception {
    JarFileUrlKey fromUrl = new JarFileUrlKey(new URL("jar:nested:/my.jar/!mynested.jar!/my/path#runtime"));
    JarFileUrlKey direct = new JarFileUrlKey("jar", "", -1, "nested:/my.jar/!mynested.jar!/my/path", true);
    assertThat(direct).isEqualTo(fromUrl);
    assertThat(direct.hashCode()).isEqualTo(fromUrl.hashCode());
  }

  @Test
  void directKeyWithRuntimeRefNotEqualToKeyWithout() {
    JarFileUrlKey first = new JarFileUrlKey("jar", "", -1, "nested:/my.jar/!mynested.jar!/my/path", true);
    JarFileUrlKey second = new JarFileUrlKey("jar", "", -1, "nested:/my.jar/!mynested.jar!/my/path", false);
    assertThat(first).isNotEqualTo(second);
  }

  @BeforeAll
  static void setup() {
    Handlers.register();
  }

  @Test
  void getCreatesKey() throws Exception {
    URL url = new URL("jar:nested:/my.jar/!mynested.jar!/my/path");
    JarFileUrlKey key = new JarFileUrlKey(url);
    assertThat(key).isEqualTo(key).isEqualTo(new JarFileUrlKey(url))
            .isNotEqualTo(new JarFileUrlKey(new URL("jar:nested:/my.jar/!mynested.jar!/my/path2")));
    assertThat(key.hashCode()).isEqualTo("nested:/my.jar/!mynested.jar!/my/path".hashCode());
  }

  @Test
  void getWhenUppercaseProtocolCreatesKey() throws Exception {
    URL url = new URL("JAR:nested:/my.jar/!mynested.jar!/my/path");
    assertThat(new JarFileUrlKey(url)).isEqualTo(new JarFileUrlKey(new URL("jar:nested:/my.jar/!mynested.jar!/my/path")));
  }

  @Test
  void getWhenHasHostAndPortCreatesKey() throws Exception {
    URL url = new URL("https://example.com:1234/test");
    assertThat(new JarFileUrlKey(url)).isEqualTo(new JarFileUrlKey(new URL("https://example.com:1234/test")));
  }

  @Test
  void getWhenHasUppercaseHostCreatesKey() throws Exception {
    URL url = new URL("https://EXAMPLE.com:1234/test");
    assertThat(new JarFileUrlKey(url)).isEqualTo(new JarFileUrlKey(new URL("https://example.com:1234/test")));
  }

  @Test
  void getWhenHasNoPortCreatesKeyWithDefaultPort() throws Exception {
    URL url = new URL("https://EXAMPLE.com/test");
    assertThat(new JarFileUrlKey(url)).isEqualTo(new JarFileUrlKey(new URL("https://example.com:443/test")));
  }

  @Test
  void getWhenHasNoFileCreatesKey() throws Exception {
    URL url = new URL("https://EXAMPLE.com");
    assertThat(new JarFileUrlKey(url)).isEqualTo(new JarFileUrlKey(new URL("https://example.com:443")));
  }

  @Test
  void getWhenHasRuntimeRefCreatesKey() throws Exception {
    URL url = new URL("jar:nested:/my.jar/!mynested.jar!/my/path#runtime");
    assertThat(new JarFileUrlKey(url)).isEqualTo(new JarFileUrlKey(new URL("jar:nested:/my.jar/!mynested.jar!/my/path#runtime")));
  }

  @Test
  void getWhenHasOtherRefCreatesKeyWithoutRef() throws Exception {
    URL url = new URL("jar:nested:/my.jar/!mynested.jar!/my/path#example");
    assertThat(new JarFileUrlKey(url)).isEqualTo(new JarFileUrlKey(new URL("jar:nested:/my.jar/!mynested.jar!/my/path")));
  }

}
