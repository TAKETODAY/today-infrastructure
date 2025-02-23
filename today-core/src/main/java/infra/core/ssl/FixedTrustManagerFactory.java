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

import java.security.KeyStore;
import java.security.Provider;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;

import infra.lang.Version;

/**
 * {@link TrustManagerFactory} which uses a fixed set of {@link TrustManager
 * TrustManagers}.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
final class FixedTrustManagerFactory extends TrustManagerFactory {

  private static final Provider PROVIDER = new Provider("FixedTrustManagerFactory", Version.instance.implementationVersion(), "") {

  };

  private FixedTrustManagerFactory(FixedTrustManagersSpi spi, String algorithm) {
    super(spi, PROVIDER, algorithm);
  }

  static FixedTrustManagerFactory of(TrustManagerFactory trustManagerFactory, TrustManager... trustManagers) {
    return new FixedTrustManagerFactory(new FixedTrustManagersSpi(trustManagers),
            trustManagerFactory.getAlgorithm());
  }

  private static final class FixedTrustManagersSpi extends TrustManagerFactorySpi {

    private final TrustManager[] trustManagers;

    private FixedTrustManagersSpi(TrustManager[] trustManagers) {
      this.trustManagers = trustManagers;
    }

    @Override
    protected void engineInit(KeyStore ks) {
    }

    @Override
    protected void engineInit(ManagerFactoryParameters spec) {
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
      return this.trustManagers;
    }

  }

}
