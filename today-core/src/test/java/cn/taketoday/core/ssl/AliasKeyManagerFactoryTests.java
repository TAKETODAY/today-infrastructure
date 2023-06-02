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

package cn.taketoday.core.ssl;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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

}
