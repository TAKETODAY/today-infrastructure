/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.ssl;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleRegistry;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;

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
    propertyValues.add("ssl.bundle.pem.first.keystore.certificate=cert1.pem");
    propertyValues.add("ssl.bundle.pem.first.keystore.private-key=key1.pem");
    propertyValues.add("ssl.bundle.pem.first.keystore.type=JKS");
    propertyValues.add("ssl.bundle.pem.first.truststore.type=PKCS12");
    propertyValues.add("ssl.bundle.pem.second.key.alias=alias2");
    propertyValues.add("ssl.bundle.pem.second.key.password=secret2");
    propertyValues.add("ssl.bundle.pem.second.keystore.certificate=cert2.pem");
    propertyValues.add("ssl.bundle.pem.second.keystore.private-key=key2.pem");
    propertyValues.add("ssl.bundle.pem.second.keystore.type=PKCS12");
    propertyValues.add("ssl.bundle.pem.second.truststore.certificate=ca.pem");
    propertyValues.add("ssl.bundle.pem.second.truststore.private-key=ca-key.pem");
    propertyValues.add("ssl.bundle.pem.second.truststore.type=JKS");
    this.contextRunner.withPropertyValues(propertyValues.toArray(String[]::new)).run((context) -> {
      assertThat(context).hasSingleBean(SslBundles.class);
      SslBundles bundles = context.getBean(SslBundles.class);
      SslBundle first = bundles.getBundle("first");
      assertThat(first).isNotNull();
      assertThat(first.getStores()).isNotNull();
      assertThat(first.getManagers()).isNotNull();
      assertThat(first.getKey().getAlias()).isEqualTo("alias1");
      assertThat(first.getKey().getPassword()).isEqualTo("secret1");
      assertThat(first.getStores()).extracting("keyStoreDetails").extracting("type").isEqualTo("JKS");
      assertThat(first.getStores()).extracting("trustStoreDetails").extracting("type").isEqualTo("PKCS12");
      SslBundle second = bundles.getBundle("second");
      assertThat(second).isNotNull();
      assertThat(second.getStores()).isNotNull();
      assertThat(second.getManagers()).isNotNull();
      assertThat(second.getKey().getAlias()).isEqualTo("alias2");
      assertThat(second.getKey().getPassword()).isEqualTo("secret2");
      assertThat(second.getStores()).extracting("keyStoreDetails").extracting("type").isEqualTo("PKCS12");
      assertThat(second.getStores()).extracting("trustStoreDetails").extracting("type").isEqualTo("JKS");
    });
  }

  @Test
  void sslBundlesCreatedWithCustomSslBundle() {
    List<String> propertyValues = new ArrayList<>();
    propertyValues.add("custom.ssl.key.alias=alias1");
    propertyValues.add("custom.ssl.key.password=secret1");
    propertyValues.add("custom.ssl.keystore.type=JKS");
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
              assertThat(first.getStores()).extracting("keyStoreDetails").extracting("type").isEqualTo("JKS");
              assertThat(first.getStores()).extracting("trustStoreDetails").extracting("type").isEqualTo("PKCS12");
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
