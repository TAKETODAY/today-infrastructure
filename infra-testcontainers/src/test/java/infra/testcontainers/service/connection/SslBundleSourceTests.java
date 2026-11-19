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

package infra.testcontainers.service.connection;

import org.junit.jupiter.api.Test;

import java.util.Set;

import infra.beans.factory.BeanFactory;
import infra.core.annotation.MergedAnnotations;
import infra.core.ssl.SslBundle;
import infra.core.ssl.jks.JksSslStoreBundle;
import infra.core.ssl.pem.PemSslStoreBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class SslBundleSourceTests {

  @Test
  void pemStoresUseDefaultSslSettings() throws Exception {
    SslBundleSource source = SslBundleSource.get(annotations("pem"));
    assertThat(source).isNotNull();
    assertThat(source.pemKeyStore().certificate()).isEqualTo("client.pem");
    assertThat(source.pemTrustStore().certificate()).isEqualTo("ca.pem");

    SslBundle bundle = source.getSslBundle();
    assertThat(bundle).isNotNull();
    assertThat(bundle.getStores()).isInstanceOf(PemSslStoreBundle.class);
    assertThat(bundle.getProtocol()).isEqualTo(SslBundle.DEFAULT_PROTOCOL);
    assertThat(bundle.getOptions().isSpecified()).isFalse();
    assertThat(bundle.getKey().getAlias()).isNull();
  }

  @Test
  void pemTrustStoreWorksWithoutAKeyStore() throws Exception {
    SslBundleSource source = SslBundleSource.get(annotations("pemTrustOnly"));
    assertThat(source).isNotNull();
    SslBundle bundle = source.getSslBundle();
    assertThat(bundle).isNotNull();
    assertThat(bundle.getStores().getKeyStore()).isNull();
  }

  @Test
  void keyStoresWorkWithoutATrustStore() throws Exception {
    for (String field : new String[] { "pemKeyOnly", "jksKeyOnly" }) {
      SslBundleSource source = SslBundleSource.get(annotations(field));
      assertThat(source).isNotNull();
      SslBundle bundle = source.getSslBundle();
      assertThat(bundle).isNotNull();
      assertThat(bundle.getStores().getTrustStore()).as(field).isNull();
    }
  }

  @Test
  void jksStoresUseExplicitSslSettingsAndMergeRepeatedMetadata() throws Exception {
    MergedAnnotations annotations = annotations("jks");
    BeanFactory factory = mock(BeanFactory.class);
    given(factory.findAllAnnotationsOnBean("database", Ssl.class, false))
            .willReturn(Set.of(annotations.get(Ssl.class).synthesize()));

    SslBundleSource source = SslBundleSource.get(factory, "database", annotations);
    assertThat(source).isNotNull();
    assertThat(source.jksKeyStore().location()).isEqualTo("client.jks");
    assertThat(source.jksTrustStore().location()).isEqualTo("ca.jks");
    SslBundle bundle = source.getSslBundle();
    assertThat(bundle).isNotNull();
    assertThat(bundle.getStores()).isInstanceOf(JksSslStoreBundle.class);
    assertThat(bundle.getProtocol()).isEqualTo("TLSv1.3");
    assertThat(bundle.getOptions().getEnabledProtocols()).containsExactly("TLSv1.3");
    assertThat(bundle.getOptions().getCiphers()).containsExactly("TLS_AES_128_GCM_SHA256");
    assertThat(bundle.getKey().getAlias()).isEqualTo("client");
  }

  @Test
  void sslOptionsAloneDoNotCreateAStoreBundle() throws Exception {
    SslBundleSource source = SslBundleSource.get(annotations("optionsOnly"));
    assertThat(source).isNotNull();
    assertThat(source.getSslBundle()).isNull();
  }

  @Test
  void rejectsMixedStoreFormats() throws Exception {
    MergedAnnotations annotations = annotations("mixed");
    assertThatIllegalStateException().isThrownBy(() -> SslBundleSource.get(annotations))
            .withMessageContaining("PEM and JKS store annotations cannot be used together");
  }

  @Test
  void rejectsConflictingAnnotationsOnTheSameBean() throws Exception {
    MergedAnnotations annotations = annotations("jks");
    BeanFactory factory = mock(BeanFactory.class);
    given(factory.findAllAnnotationsOnBean("database", Ssl.class, false))
            .willReturn(Set.of(annotations("optionsOnly").get(Ssl.class).synthesize()));

    assertThatIllegalStateException().isThrownBy(() -> SslBundleSource.get(factory, "database", annotations))
            .withMessage("Expected single %s annotation, but found 2", Ssl.class.getName());
  }

  private MergedAnnotations annotations(String name) throws NoSuchFieldException {
    return MergedAnnotations.from(Declarations.class.getDeclaredField(name));
  }

  static class Declarations {

    @PemKeyStore(value = "client.pem", privateKey = "client-key.pem")
    @PemTrustStore("ca.pem")
    Object pem;

    @Ssl(protocol = "TLSv1.3", enabledProtocols = "TLSv1.3", ciphers = "TLS_AES_128_GCM_SHA256", keyAlias = "client")
    @JksKeyStore("client.jks")
    @JksTrustStore("ca.jks")
    Object jks;

    @Ssl
    Object optionsOnly;

    @PemKeyStore("client.pem")
    @JksTrustStore("ca.jks")
    Object mixed;

    @PemTrustStore("ca.pem")
    Object pemTrustOnly;

    @PemKeyStore("client.pem")
    Object pemKeyOnly;

    @JksKeyStore("client.jks")
    Object jksKeyOnly;

  }

}
