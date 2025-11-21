/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core.ssl;

import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.X509ExtendedKeyManager;

import infra.core.ssl.AliasKeyManagerFactory.AliasX509ExtendedKeyManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AliasKeyManagerFactory}.
 *
 * @author Phillip Webb
 */
class AliasKeyManagerFactoryTests {

  @Test
  void chooseEngineServerAliasReturnsAlias() throws Exception {
    KeyManagerFactory delegate = mock(KeyManagerFactory.class);
    given(delegate.getKeyManagers()).willReturn(new KeyManager[] { mock(X509ExtendedKeyManager.class) });
    AliasKeyManagerFactory factory = new AliasKeyManagerFactory(delegate, "test-alias",
            KeyManagerFactory.getDefaultAlgorithm());
    factory.init(null, null);
    KeyManager[] keyManagers = factory.getKeyManagers();
    X509ExtendedKeyManager x509KeyManager = (X509ExtendedKeyManager) Arrays.stream(keyManagers)
            .filter(X509ExtendedKeyManager.class::isInstance)
            .findAny()
            .get();
    String chosenAlias = x509KeyManager.chooseEngineServerAlias(null, null, null);
    assertThat(chosenAlias).isEqualTo("test-alias");
  }

  @Test
  void constructorCreatesFactoryWithCorrectAlgorithm() throws Exception {
    KeyManagerFactory delegate = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    AliasKeyManagerFactory factory = new AliasKeyManagerFactory(delegate, "test-alias", KeyManagerFactory.getDefaultAlgorithm());
    assertThat(factory.getAlgorithm()).isEqualTo(KeyManagerFactory.getDefaultAlgorithm());
  }

  @Test
  void engineInitWithKeyStoreInitializesDelegate() throws Exception {
    KeyManagerFactory delegate = mock(KeyManagerFactory.class);
    AliasKeyManagerFactory factory = new AliasKeyManagerFactory(delegate, "test-alias", KeyManagerFactory.getDefaultAlgorithm());
    KeyStore keyStore = mock(KeyStore.class);
    char[] password = "password".toCharArray();

    factory.init(keyStore, password);

    verify(delegate).init(keyStore, password);
  }

  @Test
  void engineInitWithManagerFactoryParametersThrowsException() {
    KeyManagerFactory delegate = mock(KeyManagerFactory.class);
    AliasKeyManagerFactory factory = new AliasKeyManagerFactory(delegate, "test-alias", KeyManagerFactory.getDefaultAlgorithm());
    ManagerFactoryParameters params = mock(ManagerFactoryParameters.class);

    assertThatThrownBy(() -> factory.init(params))
            .isInstanceOf(InvalidAlgorithmParameterException.class)
            .hasMessage("Unsupported ManagerFactoryParameters");
  }

  @Test
  void engineGetKeyManagersReturnsWrappedKeyManagers() throws Exception {
    X509ExtendedKeyManager keyManager = mock(X509ExtendedKeyManager.class);
    KeyManagerFactory delegate = mock(KeyManagerFactory.class);
    given(delegate.getKeyManagers()).willReturn(new KeyManager[] { keyManager });

    AliasKeyManagerFactory factory = new AliasKeyManagerFactory(delegate, "test-alias", KeyManagerFactory.getDefaultAlgorithm());
    factory.init(null, null);

    KeyManager[] keyManagers = factory.getKeyManagers();
    assertThat(keyManagers).hasSize(1);
    assertThat(keyManagers[0]).isInstanceOf(AliasX509ExtendedKeyManager.class);
  }

  @Test
  void engineGetKeyManagersFiltersNonX509KeyManagers() throws Exception {
    KeyManager nonX509KeyManager = mock(KeyManager.class);
    X509ExtendedKeyManager x509KeyManager = mock(X509ExtendedKeyManager.class);
    KeyManagerFactory delegate = mock(KeyManagerFactory.class);
    given(delegate.getKeyManagers()).willReturn(new KeyManager[] { nonX509KeyManager, x509KeyManager });

    AliasKeyManagerFactory factory = new AliasKeyManagerFactory(delegate, "test-alias", KeyManagerFactory.getDefaultAlgorithm());
    factory.init(null, null);

    KeyManager[] keyManagers = factory.getKeyManagers();
    assertThat(keyManagers).hasSize(1);
    assertThat(keyManagers[0]).isInstanceOf(AliasX509ExtendedKeyManager.class);
  }

  @Test
  void aliasX509ExtendedKeyManagerChooseEngineServerAliasReturnsConfiguredAlias() {
    X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
    AliasX509ExtendedKeyManager keyManager = new AliasX509ExtendedKeyManager(delegate, "test-alias");

    String alias = keyManager.chooseEngineServerAlias("RSA", null, null);
    assertThat(alias).isEqualTo("test-alias");
  }

  @Test
  void aliasX509ExtendedKeyManagerChooseEngineClientAliasDelegatesToOriginal() {
    X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
    given(delegate.chooseEngineClientAlias(any(), any(), any())).willReturn("delegated-alias");
    AliasX509ExtendedKeyManager keyManager = new AliasX509ExtendedKeyManager(delegate, "test-alias");

    String alias = keyManager.chooseEngineClientAlias(new String[] { "RSA" }, null, null);
    assertThat(alias).isEqualTo("delegated-alias");
  }

  @Test
  void aliasX509ExtendedKeyManagerChooseServerAliasDelegatesToOriginal() {
    X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
    given(delegate.chooseServerAlias(anyString(), any(), any())).willReturn("delegated-alias");
    AliasX509ExtendedKeyManager keyManager = new AliasX509ExtendedKeyManager(delegate, "test-alias");

    String alias = keyManager.chooseServerAlias("RSA", null, null);
    assertThat(alias).isEqualTo("delegated-alias");
  }

  @Test
  void aliasX509ExtendedKeyManagerChooseClientAliasDelegatesToOriginal() {
    X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
    given(delegate.chooseClientAlias(any(), any(), any())).willReturn("delegated-alias");
    AliasX509ExtendedKeyManager keyManager = new AliasX509ExtendedKeyManager(delegate, "test-alias");

    String alias = keyManager.chooseClientAlias(new String[] { "RSA" }, null, null);
    assertThat(alias).isEqualTo("delegated-alias");
  }

  @Test
  void aliasX509ExtendedKeyManagerGetCertificateChainDelegatesToOriginal() {
    X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
    X509Certificate[] certificates = new X509Certificate[0];
    given(delegate.getCertificateChain(anyString())).willReturn(certificates);
    AliasX509ExtendedKeyManager keyManager = new AliasX509ExtendedKeyManager(delegate, "test-alias");

    X509Certificate[] result = keyManager.getCertificateChain("test");
    assertThat(result).isSameAs(certificates);
  }

  @Test
  void aliasX509ExtendedKeyManagerGetPrivateKeyDelegatesToOriginal() {
    X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
    PrivateKey privateKey = mock(PrivateKey.class);
    given(delegate.getPrivateKey(anyString())).willReturn(privateKey);
    AliasX509ExtendedKeyManager keyManager = new AliasX509ExtendedKeyManager(delegate, "test-alias");

    PrivateKey result = keyManager.getPrivateKey("test");
    assertThat(result).isSameAs(privateKey);
  }

  @Test
  void aliasX509ExtendedKeyManagerGetClientAliasesDelegatesToOriginal() {
    X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
    String[] aliases = new String[] { "alias1", "alias2" };
    given(delegate.getClientAliases(anyString(), any())).willReturn(aliases);
    AliasX509ExtendedKeyManager keyManager = new AliasX509ExtendedKeyManager(delegate, "test-alias");

    String[] result = keyManager.getClientAliases("RSA", null);
    assertThat(result).isSameAs(aliases);
  }

  @Test
  void aliasX509ExtendedKeyManagerGetServerAliasesDelegatesToOriginal() {
    X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
    String[] aliases = new String[] { "alias1", "alias2" };
    given(delegate.getServerAliases(anyString(), any())).willReturn(aliases);
    AliasX509ExtendedKeyManager keyManager = new AliasX509ExtendedKeyManager(delegate, "test-alias");

    String[] result = keyManager.getServerAliases("RSA", null);
    assertThat(result).isSameAs(aliases);
  }

}
