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

package infra.annotation.config.ssl;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.context.properties.ConfigurationProperties;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundleRegistry;
import infra.core.ssl.SslBundles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslAutoConfiguration}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class SslAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class));

  @Test
  void sslBundlesCreatedWithNoConfiguration() {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(SslBundleRegistry.class));
  }

  @Test
  void sslBundlesCreatedWithCertificates() {
    List<String> propertyValues = new ArrayList<>();
    propertyValues.add("ssl.bundle.pem.first.key.alias=alias1");
    propertyValues.add("ssl.bundle.pem.first.key.password=secret1");
    propertyValues.add(
            "ssl.bundle.pem.first.keystore.certificate=classpath:infra/annotation/config/ssl/rsa-cert.pem");
    propertyValues.add(
            "ssl.bundle.pem.first.keystore.private-key=classpath:infra/annotation/config/ssl/rsa-key.pem");
    propertyValues.add("ssl.bundle.pem.first.keystore.type=PKCS12");
    propertyValues.add("ssl.bundle.pem.first.truststore.type=PKCS12");
    propertyValues.add(
            "ssl.bundle.pem.first.truststore.certificate=classpath:infra/annotation/config/ssl/rsa-cert.pem");
    propertyValues.add(
            "ssl.bundle.pem.first.truststore.private-key=classpath:infra/annotation/config/ssl/rsa-key.pem");
    propertyValues.add("ssl.bundle.pem.second.key.alias=alias2");
    propertyValues.add("ssl.bundle.pem.second.key.password=secret2");
    propertyValues.add(
            "ssl.bundle.pem.second.keystore.certificate=classpath:infra/annotation/config/ssl/ed25519-cert.pem");
    propertyValues.add(
            "ssl.bundle.pem.second.keystore.private-key=classpath:infra/annotation/config/ssl/ed25519-key.pem");
    propertyValues.add("ssl.bundle.pem.second.keystore.type=PKCS12");
    propertyValues.add(
            "ssl.bundle.pem.second.truststore.certificate=classpath:infra/annotation/config/ssl/ed25519-cert.pem");
    propertyValues.add(
            "ssl.bundle.pem.second.truststore.private-key=classpath:infra/annotation/config/ssl/ed25519-key.pem");
    propertyValues.add("ssl.bundle.pem.second.truststore.type=PKCS12");
    this.contextRunner.withPropertyValues(propertyValues.toArray(String[]::new)).run((context) -> {
      assertThat(context).hasSingleBean(SslBundles.class);
      SslBundles bundles = context.getBean(SslBundles.class);
      SslBundle first = bundles.getBundle("first");
      assertThat(first).isNotNull();
      assertThat(first.getStores()).isNotNull();
      assertThat(first.getManagers()).isNotNull();
      assertThat(first.getKey().getAlias()).isEqualTo("alias1");
      assertThat(first.getKey().getPassword()).isEqualTo("secret1");
      assertThat(first.getStores().getKeyStore().getType()).isEqualTo("PKCS12");
      assertThat(first.getStores().getTrustStore().getType()).isEqualTo("PKCS12");
      SslBundle second = bundles.getBundle("second");
      assertThat(second).isNotNull();
      assertThat(second.getStores()).isNotNull();
      assertThat(second.getManagers()).isNotNull();
      assertThat(second.getKey().getAlias()).isEqualTo("alias2");
      assertThat(second.getKey().getPassword()).isEqualTo("secret2");
      assertThat(second.getStores().getKeyStore().getType()).isEqualTo("PKCS12");
      assertThat(second.getStores().getTrustStore().getType()).isEqualTo("PKCS12");
    });
  }

  @Test
  void sslBundlesCreatedWithCustomSslBundle() {
    List<String> propertyValues = new ArrayList<>();
    propertyValues.add("custom.ssl.key.alias=alias1");
    propertyValues.add("custom.ssl.key.password=secret1");
    propertyValues.add("custom.ssl.keystore.certificate=classpath:infra/annotation/config/ssl/rsa-cert.pem");
    propertyValues.add("custom.ssl.keystore.keystore.private-key=classpath:infra/annotation/config/ssl/rsa-key.pem");
    propertyValues.add("custom.ssl.truststore.certificate=classpath:infra/annotation/config/ssl/rsa-cert.pem");
    propertyValues.add("custom.ssl.keystore.type=PKCS12");
    propertyValues.add("custom.ssl.truststore.type=PKCS12");
    this.contextRunner.withUserConfiguration(CustomSslBundleConfiguration.class)
            .withPropertyValues(propertyValues.toArray(String[]::new))
            .run((context) -> {
              assertThat(context).hasSingleBean(SslBundles.class);
              SslBundles bundles = context.getBean(SslBundles.class);
              SslBundle first = bundles.getBundle("custom");
              assertThat(first).isNotNull();
              assertThat(first.getStores()).isNotNull();
              assertThat(first.getManagers()).isNotNull();
              assertThat(first.getKey().getAlias()).isEqualTo("alias1");
              assertThat(first.getKey().getPassword()).isEqualTo("secret1");
              assertThat(first.getStores().getKeyStore().getType()).isEqualTo("PKCS12");
              assertThat(first.getStores().getTrustStore().getType()).isEqualTo("PKCS12");
            });
  }

  @Configuration
  @EnableConfigurationProperties(CustomSslProperties.class)
  public static class CustomSslBundleConfiguration {

    @Bean
    public SslBundleRegistrar customSslBundlesRegistrar(CustomSslProperties properties) {
      return new CustomSslBundlesRegistrar(properties);
    }

  }

  @ConfigurationProperties("custom.ssl")
  static class CustomSslProperties extends PemSslBundleProperties {

  }

  static class CustomSslBundlesRegistrar implements SslBundleRegistrar {

    private final CustomSslProperties properties;

    CustomSslBundlesRegistrar(CustomSslProperties properties) {
      this.properties = properties;
    }

    @Override
    public void registerBundles(SslBundleRegistry registry) {
      registry.registerBundle("custom", PropertiesSslBundle.get(this.properties));
    }

  }

}
