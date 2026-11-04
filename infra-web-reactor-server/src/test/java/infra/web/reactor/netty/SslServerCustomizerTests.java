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

package infra.web.reactor.netty;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import infra.core.ssl.SslBundle;
import infra.core.ssl.pem.PemSslStoreBundle;
import infra.core.ssl.pem.PemSslStoreDetails;
import infra.test.classpath.resources.WithPackageResources;
import infra.web.server.Ssl;
import reactor.netty.tcp.SslProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslServerCustomizer}.
 *
 * @author Daeho Kwon
 */
class SslServerCustomizerTests {

  @Test
  @WithPackageResources({ "1.key", "1.crt", "2.key", "2.crt" })
  void getSslProviderReturnsMappedProviderForKnownServerName() {
    SslBundle defaultBundle = createBundle("1.key", "1.crt");
    SslBundle mappedBundle = createBundle("2.key", "2.crt");
    SslServerCustomizer customizer = new SslServerCustomizer(false, new Ssl(), defaultBundle,
            Map.of("mapped.example", mappedBundle));
    SslProvider mapped = customizer.getSslProvider("mapped.example");
    assertThat(mapped).isNotNull().isNotSameAs(customizer.getSslProvider(null));
  }

  @Test
  @WithPackageResources({ "1.key", "1.crt", "2.key", "2.crt" })
  void getSslProviderFallsBackToDefaultWhenServerNameIsUnmapped() {
    SslBundle defaultBundle = createBundle("1.key", "1.crt");
    SslBundle mappedBundle = createBundle("2.key", "2.crt");
    SslServerCustomizer customizer = new SslServerCustomizer(false, new Ssl(), defaultBundle,
            Map.of("mapped.example", mappedBundle));
    assertThat(customizer.getSslProvider("unmapped.example")).isSameAs(customizer.getSslProvider(null));
  }

  @Test
  @WithPackageResources({ "1.key", "1.crt", "2.key", "2.crt" })
  void getSslProviderIgnoresCaseOfConfiguredServerName() {
    SslBundle defaultBundle = createBundle("1.key", "1.crt");
    SslBundle mappedBundle = createBundle("2.key", "2.crt");
    SslServerCustomizer customizer = new SslServerCustomizer(false, new Ssl(), defaultBundle,
            Map.of("Mapped.Example", mappedBundle));
    SslProvider mapped = customizer.getSslProvider("mapped.example");
    assertThat(mapped).isNotSameAs(customizer.getSslProvider(null));
    assertThat(customizer.getSslProvider("MAPPED.EXAMPLE")).isSameAs(mapped);
  }

  @Test
  @WithPackageResources({ "1.key", "1.crt", "2.key", "2.crt" })
  void getSslProviderIgnoresCaseOfUpdatedServerName() {
    SslBundle defaultBundle = createBundle("1.key", "1.crt");
    SslBundle mappedBundle = createBundle("2.key", "2.crt");
    SslServerCustomizer customizer = new SslServerCustomizer(false, new Ssl(), defaultBundle,
            Map.of("Mapped.Example", mappedBundle));
    SslProvider initial = customizer.getSslProvider("mapped.example");
    customizer.updateSslBundle("MAPPED.example", mappedBundle);
    assertThat(customizer.getSslProvider("mapped.example")).isNotSameAs(initial);
  }

  @Test
  @WithPackageResources({ "1.key", "1.crt" })
  void getSslProviderReturnsDefaultWhenServerNameIsNull() {
    SslBundle defaultBundle = createBundle("1.key", "1.crt");
    SslServerCustomizer customizer = new SslServerCustomizer(false, new Ssl(), defaultBundle,
            Collections.emptyMap());
    assertThat(customizer.getSslProvider(null)).isNotNull();
  }

  private static SslBundle createBundle(String key, String certificate) {
    return SslBundle.of(new PemSslStoreBundle(
            new PemSslStoreDetails(null, "classpath:" + certificate, "classpath:" + key), null));
  }

}
