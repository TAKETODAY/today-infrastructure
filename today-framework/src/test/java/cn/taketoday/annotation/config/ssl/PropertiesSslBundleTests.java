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

import java.util.Set;

import cn.taketoday.core.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/1 23:07
 */
class PropertiesSslBundleTests {

  @Test
  void pemPropertiesAreMappedToSslBundle() {
    PemSslBundleProperties properties = new PemSslBundleProperties();
    properties.getKey().setAlias("alias");
    properties.getKey().setPassword("secret");
    properties.getOptions().setCiphers(Set.of("cipher1", "cipher2", "cipher3"));
    properties.getOptions().setEnabledProtocols(Set.of("protocol1", "protocol2"));
    properties.getKeystore().setCertificate("cert1.pem");
    properties.getKeystore().setPrivateKey("key1.pem");
    properties.getKeystore().setPrivateKeyPassword("keysecret1");
    properties.getKeystore().setType("PKCS12");
    properties.getTruststore().setCertificate("cert2.pem");
    properties.getTruststore().setPrivateKey("key2.pem");
    properties.getTruststore().setPrivateKeyPassword("keysecret2");
    properties.getTruststore().setType("JKS");
    SslBundle sslBundle = PropertiesSslBundle.get(properties);
    assertThat(sslBundle.getKey().getAlias()).isEqualTo("alias");
    assertThat(sslBundle.getKey().getPassword()).isEqualTo("secret");
    assertThat(sslBundle.getOptions().getCiphers()).containsExactlyInAnyOrder("cipher1", "cipher2", "cipher3");
    assertThat(sslBundle.getOptions().getEnabledProtocols()).containsExactlyInAnyOrder("protocol1", "protocol2");
    assertThat(sslBundle.getStores()).isNotNull();
    assertThat(sslBundle.getStores()).extracting("keyStoreDetails")
            .extracting("certificate", "privateKey", "privateKeyPassword", "type")
            .containsExactly("cert1.pem", "key1.pem", "keysecret1", "PKCS12");
    assertThat(sslBundle.getStores()).extracting("trustStoreDetails")
            .extracting("certificate", "privateKey", "privateKeyPassword", "type")
            .containsExactly("cert2.pem", "key2.pem", "keysecret2", "JKS");
  }

  @Test
  void jksPropertiesAreMappedToSslBundle() {
    JksSslBundleProperties properties = new JksSslBundleProperties();
    properties.getKey().setAlias("alias");
    properties.getKey().setPassword("secret");
    properties.getOptions().setCiphers(Set.of("cipher1", "cipher2", "cipher3"));
    properties.getOptions().setEnabledProtocols(Set.of("protocol1", "protocol2"));
    properties.getKeystore().setLocation("cert1.p12");
    properties.getKeystore().setPassword("secret1");
    properties.getKeystore().setProvider("provider1");
    properties.getKeystore().setType("JKS");
    properties.getTruststore().setLocation("cert2.jks");
    properties.getTruststore().setPassword("secret2");
    properties.getTruststore().setProvider("provider2");
    properties.getTruststore().setType("PKCS12");
    SslBundle sslBundle = PropertiesSslBundle.get(properties);
    assertThat(sslBundle.getKey().getAlias()).isEqualTo("alias");
    assertThat(sslBundle.getKey().getPassword()).isEqualTo("secret");
    assertThat(sslBundle.getOptions().getCiphers()).containsExactlyInAnyOrder("cipher1", "cipher2", "cipher3");
    assertThat(sslBundle.getOptions().getEnabledProtocols()).containsExactlyInAnyOrder("protocol1", "protocol2");
    assertThat(sslBundle.getStores()).isNotNull();
    assertThat(sslBundle.getStores()).extracting("keyStoreDetails")
            .extracting("location", "password", "provider", "type")
            .containsExactly("cert1.p12", "secret1", "provider1", "JKS");
    assertThat(sslBundle.getStores()).extracting("trustStoreDetails")
            .extracting("location", "password", "provider", "type")
            .containsExactly("cert2.jks", "secret2", "provider2", "PKCS12");
  }

}