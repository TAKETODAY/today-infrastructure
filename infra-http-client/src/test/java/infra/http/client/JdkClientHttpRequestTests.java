/*
 * Copyright 2002-present the original author or authors.
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

package infra.http.client;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for restricted header filtering in {@link JdkClientHttpRequest}.
 */
class JdkClientHttpRequestTests {

  @Test
  void disallowedHeadersRespectSystemProperty() {
    String original = System.getProperty("jdk.httpclient.allowRestrictedHeaders");
    try {
      System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Expect,Host");
      Set<String> headers = JdkClientHttpRequest.disallowedHeaders();
      assertThat(headers).contains("connection", "content-length", "upgrade");
      assertThat(headers).doesNotContain("expect", "host");
    }
    finally {
      if (original == null) {
        System.clearProperty("jdk.httpclient.allowRestrictedHeaders");
      }
      else {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", original);
      }
    }
  }

  @Test
  void disallowedHeadersWithManyMixedCaseHeadersAllowed() {
    assertThat(disallowedHeaders("Expect,Host,Connection,Upgrade,Content-Length")).isEmpty();
  }

  @Test
  void disallowedHeadersWithWhitespaceAroundPropertyValue() {
    assertThat(disallowedHeaders(" expect,host "))
            .containsExactlyInAnyOrder("connection", "content-length", "upgrade");
  }

  @Test
  void disallowedHeadersWithWhitespaceAroundIndividualHeaders() {
    assertThat(disallowedHeaders("expect, host"))
            .containsExactlyInAnyOrder("connection", "content-length", "host", "upgrade");
    assertThat(disallowedHeaders("expect ,host"))
            .containsExactlyInAnyOrder("connection", "content-length", "expect", "upgrade");
  }

  private static Set<String> disallowedHeaders(String value) {
    String key = "jdk.httpclient.allowRestrictedHeaders";
    String original = System.getProperty(key);
    try {
      System.setProperty(key, value);
      return JdkClientHttpRequest.disallowedHeaders();
    }
    finally {
      if (original == null) {
        System.clearProperty(key);
      }
      else {
        System.setProperty(key, original);
      }
    }
  }
}
