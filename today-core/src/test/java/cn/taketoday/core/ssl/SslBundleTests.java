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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SslBundle}.
 *
 * @author Phillip Webb
 */
class SslBundleTests {

  @Test
  void createSslContextDelegatesToManagers() {
    SslManagerBundle managers = mock(SslManagerBundle.class);
    SslBundle bundle = SslBundle.of(null, null, null, "testprotocol", managers);
    bundle.createSslContext();
    then(managers).should().createSslContext("testprotocol");
  }

  @Test
  void ofCreatesSslBundle() {
    SslStoreBundle stores = mock(SslStoreBundle.class);
    SslBundleKey key = mock(SslBundleKey.class);
    SslOptions options = mock(SslOptions.class);
    String protocol = "test";
    SslManagerBundle managers = mock(SslManagerBundle.class);
    SslBundle bundle = SslBundle.of(stores, key, options, protocol, managers);
    assertThat(bundle.getStores()).isSameAs(stores);
    assertThat(bundle.getKey()).isSameAs(key);
    assertThat(bundle.getOptions()).isSameAs(options);
    assertThat(bundle.getProtocol()).isSameAs(protocol);
    assertThat(bundle.getManagers()).isSameAs(managers);
  }

}
