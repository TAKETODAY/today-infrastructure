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

package infra.docker.compose.service.connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Map;

import infra.core.env.StandardEnvironment;
import infra.core.ssl.SslBundle;
import infra.docker.compose.core.DockerComposeFile;
import infra.docker.compose.core.ImageReference;
import infra.docker.compose.core.RunningService;
import infra.docker.compose.service.connection.DockerComposeConnectionDetailsFactory.DockerComposeConnectionDetails;
import infra.origin.Origin;
import infra.origin.OriginProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class DockerComposeConnectionDetailsFactoryTests {

  @TempDir
  Path directory;

  @Test
  void acceptsMatchingImageAndPreservesServiceOrigin() {
    Origin origin = mock();
    OriginTrackedService service = mock();
    given(service.getOrigin()).willReturn(origin);
    given(service.image()).willReturn(ImageReference.of("postgres:17"));
    var source = new DockerComposeConnectionSource(service, new StandardEnvironment());

    var details = new TestFactory("postgres").getConnectionDetails(source);

    assertThat(details).isNotNull();
    assertThat(details.getOrigin()).isSameAs(origin);
    assertThat(details.getSslBundle(service)).isNull();
  }

  @Test
  void acceptsAlternateConnectionNameWhenRequiredClassIsPresent() {
    RunningService service = service(Map.of("infra.service-connection", "postgres"));
    var source = new DockerComposeConnectionSource(service, new StandardEnvironment());
    var factory = new TestFactory(new String[] { "mysql", "postgres" }, RunningService.class.getName());

    assertThat(factory.getConnectionDetails(source)).isNotNull();
  }

  @Test
  void rejectsUnmatchedNameOrMissingRequiredClass() {
    RunningService service = service(Map.of("infra.service-connection", "postgres"));
    var source = new DockerComposeConnectionSource(service, new StandardEnvironment());

    assertThat(new TestFactory("mysql").getConnectionDetails(source)).isNull();
    assertThat(new TestFactory(new String[] { "postgres" }, "example.MissingDriver").getConnectionDetails(source)).isNull();
  }

  @Test
  void loadsJksStoreRelativeToComposeFileAndCachesBundle() throws Exception {
    KeyStore store = KeyStore.getInstance("JKS");
    store.load(null);
    try (var output = Files.newOutputStream(this.directory.resolve("service.jks"))) {
      store.store(output, "store-secret".toCharArray());
    }
    var service = service(Map.of(
            "infra.sslbundle.jks.keystore.type", "JKS",
            "infra.sslbundle.jks.keystore.location", "service.jks",
            "infra.sslbundle.jks.keystore.password", "store-secret",
            "infra.sslbundle.jks.protocol", "TLSv1.2",
            "infra.sslbundle.jks.options.ciphers", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "infra.sslbundle.jks.options.enabled-protocols", "TLSv1.2,TLSv1.3"));
    given(service.composeFile()).willReturn(DockerComposeFile.of(Files.createFile(this.directory.resolve("compose.yaml")).toFile()));
    var details = new DockerComposeConnectionDetails(service);

    SslBundle bundle = details.getSslBundle(service);

    assertThat(bundle).isNotNull();
    assertThat(bundle.getProtocol()).isEqualTo("TLSv1.2");
    assertThat(bundle.getOptions().getEnabledProtocols()).containsExactly("TLSv1.2", "TLSv1.3");
    assertThat(bundle.getOptions().getCiphers()).containsExactly(
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
    assertThat(bundle.getStores().getKeyStore().getType()).isEqualTo("JKS");
    assertThat(bundle.getStores().getKeyStorePassword()).isEqualTo("store-secret");
    assertThat(details.getSslBundle(service)).isSameAs(bundle);
  }

  @Test
  void loadsPemTrustStoreWithDefaultOptions() throws Exception {
    RunningService service = service(Map.of(
            "infra.sslbundle.pem.truststore.type", "PKCS12",
            "infra.sslbundle.pem.truststore.certificate", "classpath:infra/docker/compose/service/connection/test-cert.pem"));

    SslBundle bundle = new DockerComposeConnectionDetails(service).getSslBundle(service);

    assertThat(bundle).isNotNull();
    assertThat(bundle.getOptions().isSpecified()).isFalse();
    assertThat(bundle.getStores().getKeyStore()).isNull();
    KeyStore trustStore = bundle.getStores().getTrustStore();
    assertThat(trustStore.getType()).isEqualTo("PKCS12");
    assertThat(trustStore.isCertificateEntry("ssl")).isTrue();
  }

  @Test
  void rejectsSimultaneousJksAndPemConfiguration() {
    RunningService service = service(Map.of(
            "infra.sslbundle.jks.keystore.location", "service.jks",
            "infra.sslbundle.pem.truststore.certificate", "classpath:infra/docker/compose/service/connection/test-cert.pem"));
    var details = new DockerComposeConnectionDetails(service);

    assertThatIllegalStateException().isThrownBy(() -> details.getSslBundle(service))
            .withMessage("Mutually exclusive JKS and PEM ssl bundles have been configured");
  }

  private static RunningService service(Map<String, String> labels) {
    RunningService service = mock();
    given(service.labels()).willReturn(labels);
    return service;
  }

  interface OriginTrackedService extends RunningService, OriginProvider {
  }

  private static class TestFactory extends DockerComposeConnectionDetailsFactory<DockerComposeConnectionDetails> {

    TestFactory(String connectionName) {
      super(connectionName);
    }

    TestFactory(String[] connectionNames, String... requiredClasses) {
      super(connectionNames, requiredClasses);
    }

    @Override
    protected DockerComposeConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
      return new DockerComposeConnectionDetails(source.getRunningService());
    }
  }
}
