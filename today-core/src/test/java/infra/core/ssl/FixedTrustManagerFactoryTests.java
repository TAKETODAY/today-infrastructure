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

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/23 18:01
 */
class FixedTrustManagerFactoryTests {

  @Test
  void shouldReturnTrustManagers() throws Exception {
    TrustManager trustManager1 = mock(TrustManager.class);
    TrustManager trustManager2 = mock(TrustManager.class);
    FixedTrustManagerFactory factory = FixedTrustManagerFactory.of(getDefaultTrustManagerFactory(), trustManager1,
            trustManager2);
    assertThat(factory.getTrustManagers()).containsExactly(trustManager1, trustManager2);
  }

  private static TrustManagerFactory getDefaultTrustManagerFactory() throws NoSuchAlgorithmException {
    return TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
  }

}