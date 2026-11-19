/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.http.client;

import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.core.ssl.SslBundle;
import infra.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

public class ClientHttpRequestFactoryCustomizationTests {

  @Test
  void appendsJdkCustomizersInOrderWithoutChangingTheOriginalBuilder() {
    var calls = new ArrayList<String>();
    var original = ClientHttpRequestFactoryBuilder.jdk();
    var customized = original.withCustomizer(factory -> calls.add("first"))
            .withCustomizers(List.of(factory -> calls.add("second"), factory -> calls.add("third")));

    original.build();
    assertThat(calls).isEmpty();
    customized.build();
    assertThat(calls).containsExactly("first", "second", "third");
    assertThatIllegalArgumentException().isThrownBy(() -> original.withCustomizers(null));
    assertThatIllegalArgumentException().isThrownBy(() -> original.withCustomizers(Collections.singletonList(null)));
  }

  @Test
  void appliesApacheConnectionConfigurationAndFactoryCustomizers() throws Exception {
    var calls = new ArrayList<String>();
    var factory = ClientHttpRequestFactoryBuilder.httpComponents()
            .withConnectionConfigCustomizer(config -> {
              calls.add("connection");
              config.setConnectTimeout(Timeout.ofSeconds(1));
            })
            .withCustomizers(List.of(candidate -> calls.add("factory")))
            .build();
    try {
      assertThat(calls).containsExactly("connection", "factory");
    }
    finally {
      factory.destroy();
    }
  }

  @Test
  void appliesReactorCustomizersInOrder() {
    var calls = new ArrayList<String>();
    ClientHttpRequestFactoryBuilder.reactor().withCustomizer(factory -> calls.add("first"))
            .withCustomizers(List.of(factory -> calls.add("second")))
            .build();
    assertThat(calls).containsExactly("first", "second");
  }

  @Test
  void reflectiveBuilderSupportsOnlyBestEffortRedirectAndCookieSettings() {
    var builder = ClientHttpRequestFactoryBuilder.of(LegacyFactory.class);
    var defaults = HttpClientSettings.defaults();

    assertThatIllegalStateException().isThrownBy(() -> builder.build(defaults.withSslBundle(mock(SslBundle.class))))
            .withMessageContaining("SSL bundle");
    assertThat(builder.build(defaults.withRedirects(HttpRedirects.FOLLOW_WHEN_POSSIBLE)
            .withCookieHandling(HttpCookieHandling.ENABLE_WHEN_POSSIBLE))).isInstanceOf(LegacyFactory.class);
  }

  @Test
  void reflectiveBuilderFallsBackToMillisecondsWhenTheDurationSetterIsDeprecated() {
    var builder = ClientHttpRequestFactoryBuilder.of(LegacyFactory.class);
    var factory = builder.build(HttpClientSettings.defaults().withReadTimeout(Duration.ofMillis(250)));

    assertThat(factory.timeout).isEqualTo(250);
  }

  @Test
  void reflectiveBuilderReportsAMissingNoArgConstructor() {
    assertThatIllegalStateException().isThrownBy(() -> ClientHttpRequestFactoryBuilder.of(NoDefaultConstructor.class).build())
            .withCauseInstanceOf(NoSuchMethodException.class);
  }

  public static class LegacyFactory implements ClientHttpRequestFactory {

    int timeout;

    @Deprecated
    public void setReadTimeout(Duration timeout) {
      throw new AssertionError("The deprecated setter should not be called");
    }

    public void setReadTimeout(int timeout) {
      this.timeout = timeout;
    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod method) {
      throw new UnsupportedOperationException();
    }
  }

  static class NoDefaultConstructor extends LegacyFactory {

    NoDefaultConstructor(int timeout) {
      this.timeout = timeout;
    }
  }

}
