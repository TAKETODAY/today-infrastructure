/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.annotation.config.ssl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Set;

import cn.taketoday.core.ssl.SslBundleRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/22 13:26
 */
class SslPropertiesBundleRegistrarTests {

  private SslPropertiesBundleRegistrar registrar;

  private FileWatcher fileWatcher;

  private SslProperties properties;

  private SslBundleRegistry registry;

  @BeforeEach
  void setUp() {
    this.properties = new SslProperties();
    this.fileWatcher = Mockito.mock(FileWatcher.class);
    this.registrar = new SslPropertiesBundleRegistrar(this.properties, this.fileWatcher);
    this.registry = Mockito.mock(SslBundleRegistry.class);
  }

  @Test
  void shouldWatchJksBundles() {
    JksSslBundleProperties jks = new JksSslBundleProperties();
    jks.setReloadOnUpdate(true);
    jks.getKeystore().setLocation("classpath:test.jks");
    jks.getKeystore().setPassword("secret");
    jks.getTruststore().setLocation("classpath:test.jks");
    jks.getTruststore().setPassword("secret");
    this.properties.getBundle().getJks().put("bundle1", jks);
    this.registrar.registerBundles(this.registry);
    then(this.registry).should(times(1)).registerBundle(eq("bundle1"), any());
    then(this.fileWatcher).should().watch(assertArg((set) -> pathEndingWith(set, "test.jks")), any());
  }

  @Test
  void shouldWatchPemBundles() {
    PemSslBundleProperties pem = new PemSslBundleProperties();
    pem.setReloadOnUpdate(true);
    pem.getKeystore().setCertificate("classpath:cn/taketoday/annotation/config/ssl/rsa-cert.pem");
    pem.getKeystore().setPrivateKey("classpath:cn/taketoday/annotation/config/ssl/rsa-key.pem");
    pem.getTruststore().setCertificate("classpath:cn/taketoday/annotation/config/ssl/ed25519-cert.pem");
    pem.getTruststore().setPrivateKey("classpath:cn/taketoday/annotation/config/ssl/ed25519-key.pem");
    this.properties.getBundle().getPem().put("bundle1", pem);
    this.registrar.registerBundles(this.registry);
    then(this.registry).should(times(1)).registerBundle(eq("bundle1"), any());
    then(this.fileWatcher).should()
            .watch(assertArg((set) -> pathEndingWith(set, "rsa-cert.pem", "rsa-key.pem")), any());
  }

  @Test
  void shouldFailIfPemKeystoreCertificateIsEmbedded() {
    PemSslBundleProperties pem = new PemSslBundleProperties();
    pem.setReloadOnUpdate(true);
    pem.getKeystore().setCertificate("""
            -----BEGIN CERTIFICATE-----
            MIICCzCCAb2gAwIBAgIUZbDi7G5czH+Yi0k2EMWxdf00XagwBQYDK2VwMHsxCzAJ
            BgNVBAYTAlhYMRIwEAYDVQQIDAlTdGF0ZU5hbWUxETAPBgNVBAcMCENpdHlOYW1l
            MRQwEgYDVQQKDAtDb21wYW55TmFtZTEbMBkGA1UECwwSQ29tcGFueVNlY3Rpb25O
            YW1lMRIwEAYDVQQDDAlsb2NhbGhvc3QwHhcNMjMwOTExMTIxNDMwWhcNMzMwOTA4
            MTIxNDMwWjB7MQswCQYDVQQGEwJYWDESMBAGA1UECAwJU3RhdGVOYW1lMREwDwYD
            VQQHDAhDaXR5TmFtZTEUMBIGA1UECgwLQ29tcGFueU5hbWUxGzAZBgNVBAsMEkNv
            bXBhbnlTZWN0aW9uTmFtZTESMBAGA1UEAwwJbG9jYWxob3N0MCowBQYDK2VwAyEA
            Q/DDA4BSgZ+Hx0DUxtIRjVjN+OcxXVURwAWc3Gt9GUyjUzBRMB0GA1UdDgQWBBSv
            EdpoaBMBoxgO96GFbf03k07DSTAfBgNVHSMEGDAWgBSvEdpoaBMBoxgO96GFbf03
            k07DSTAPBgNVHRMBAf8EBTADAQH/MAUGAytlcANBAHMXDkGd57d4F4cRk/8UjhxD
            7OtRBZfdfznSvlhJIMNfH5q0zbC2eO3hWCB3Hrn/vIeswGP8Ov4AJ6eXeX44BQM=
            -----END CERTIFICATE-----
            """.strip());
    this.properties.getBundle().getPem().put("bundle1", pem);
    assertThatIllegalStateException().isThrownBy(() -> this.registrar.registerBundles(this.registry))
            .withMessageContaining("Unable to register SSL bundle 'bundle1'")
            .havingCause()
            .withMessage("Unable to watch for reload on update");
  }

  @Test
  void shouldFailIfPemKeystorePrivateKeyIsEmbedded() {
    PemSslBundleProperties pem = new PemSslBundleProperties();
    pem.setReloadOnUpdate(true);
    pem.getKeystore().setCertificate("classpath:cn/taketoday/annotation/config/ssl/ed25519-cert.pem");
    pem.getKeystore().setPrivateKey("""
            -----BEGIN PRIVATE KEY-----
            MC4CAQAwBQYDK2VwBCIEIC29RnMVTcyqXEAIO1b/6p7RdbM6TiqvnztVQ4IxYxUh
            -----END PRIVATE KEY-----
            """.strip());
    this.properties.getBundle().getPem().put("bundle1", pem);
    assertThatIllegalStateException().isThrownBy(() -> this.registrar.registerBundles(this.registry))
            .withMessageContaining("Unable to register SSL bundle 'bundle1'")
            .havingCause()
            .withMessage("Unable to watch for reload on update");
  }

  @Test
  void shouldFailIfPemTruststoreCertificateIsEmbedded() {
    PemSslBundleProperties pem = new PemSslBundleProperties();
    pem.setReloadOnUpdate(true);
    pem.getTruststore().setCertificate("""
            -----BEGIN CERTIFICATE-----
            MIICCzCCAb2gAwIBAgIUZbDi7G5czH+Yi0k2EMWxdf00XagwBQYDK2VwMHsxCzAJ
            BgNVBAYTAlhYMRIwEAYDVQQIDAlTdGF0ZU5hbWUxETAPBgNVBAcMCENpdHlOYW1l
            MRQwEgYDVQQKDAtDb21wYW55TmFtZTEbMBkGA1UECwwSQ29tcGFueVNlY3Rpb25O
            YW1lMRIwEAYDVQQDDAlsb2NhbGhvc3QwHhcNMjMwOTExMTIxNDMwWhcNMzMwOTA4
            MTIxNDMwWjB7MQswCQYDVQQGEwJYWDESMBAGA1UECAwJU3RhdGVOYW1lMREwDwYD
            VQQHDAhDaXR5TmFtZTEUMBIGA1UECgwLQ29tcGFueU5hbWUxGzAZBgNVBAsMEkNv
            bXBhbnlTZWN0aW9uTmFtZTESMBAGA1UEAwwJbG9jYWxob3N0MCowBQYDK2VwAyEA
            Q/DDA4BSgZ+Hx0DUxtIRjVjN+OcxXVURwAWc3Gt9GUyjUzBRMB0GA1UdDgQWBBSv
            EdpoaBMBoxgO96GFbf03k07DSTAfBgNVHSMEGDAWgBSvEdpoaBMBoxgO96GFbf03
            k07DSTAPBgNVHRMBAf8EBTADAQH/MAUGAytlcANBAHMXDkGd57d4F4cRk/8UjhxD
            7OtRBZfdfznSvlhJIMNfH5q0zbC2eO3hWCB3Hrn/vIeswGP8Ov4AJ6eXeX44BQM=
            -----END CERTIFICATE-----
            """.strip());
    this.properties.getBundle().getPem().put("bundle1", pem);
    assertThatIllegalStateException().isThrownBy(() -> this.registrar.registerBundles(this.registry))
            .withMessageContaining("Unable to register SSL bundle 'bundle1'")
            .havingCause()
            .withMessage("Unable to watch for reload on update");
  }

  @Test
  void shouldFailIfPemTruststorePrivateKeyIsEmbedded() {
    PemSslBundleProperties pem = new PemSslBundleProperties();
    pem.setReloadOnUpdate(true);
    pem.getTruststore().setCertificate("classpath:cn/taketoday/annotation/config/ssl/ed25519-cert.pem");
    pem.getTruststore().setPrivateKey("""
            -----BEGIN PRIVATE KEY-----
            MC4CAQAwBQYDK2VwBCIEIC29RnMVTcyqXEAIO1b/6p7RdbM6TiqvnztVQ4IxYxUh
            -----END PRIVATE KEY-----
            """.strip());
    this.properties.getBundle().getPem().put("bundle1", pem);
    assertThatIllegalStateException().isThrownBy(() -> this.registrar.registerBundles(this.registry))
            .withMessageContaining("Unable to register SSL bundle 'bundle1'")
            .havingCause()
            .withMessage("Unable to watch for reload on update");
  }

  private void pathEndingWith(Set<Path> paths, String... suffixes) {
    for (String suffix : suffixes) {
      assertThat(paths).anyMatch((path) -> path.getFileName().toString().endsWith(suffix));
    }
  }

}