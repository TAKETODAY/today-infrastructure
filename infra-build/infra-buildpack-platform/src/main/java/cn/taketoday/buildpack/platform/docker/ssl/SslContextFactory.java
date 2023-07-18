/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.docker.ssl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import cn.taketoday.lang.Assert;

/**
 * Builds an {@link SSLContext} for use with an HTTP connection.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SslContextFactory {

  private static final char[] NO_PASSWORD = {};

  private static final String KEY_STORE_ALIAS = "infra-app-docker";

  /**
   * Create an {@link SSLContext} from files in the specified directory. The directory
   * must contain files with the names 'key.pem', 'cert.pem', and 'ca.pem'.
   *
   * @param directory the path to a directory containing certificate and key files
   * @return the {@code SSLContext}
   */
  public SSLContext forDirectory(String directory) {
    try {
      Path keyPath = Paths.get(directory, "key.pem");
      Path certPath = Paths.get(directory, "cert.pem");
      Path caPath = Paths.get(directory, "ca.pem");
      Path caKeyPath = Paths.get(directory, "ca-key.pem");
      verifyCertificateFiles(keyPath, certPath, caPath);
      KeyManagerFactory keyManagerFactory = getKeyManagerFactory(keyPath, certPath);
      TrustManagerFactory trustManagerFactory = getTrustManagerFactory(caPath, caKeyPath);
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
      return sslContext;
    }
    catch (RuntimeException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }

  private KeyManagerFactory getKeyManagerFactory(Path keyPath, Path certPath) throws Exception {
    KeyStore store = KeyStoreFactory.create(certPath, keyPath, KEY_STORE_ALIAS);
    KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    factory.init(store, NO_PASSWORD);
    return factory;
  }

  private TrustManagerFactory getTrustManagerFactory(Path caPath, Path caKeyPath)
          throws NoSuchAlgorithmException, KeyStoreException {
    KeyStore store = KeyStoreFactory.create(caPath, caKeyPath, KEY_STORE_ALIAS);
    TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    factory.init(store);
    return factory;
  }

  private static void verifyCertificateFiles(Path... paths) {
    for (Path path : paths) {
      Assert.state(Files.exists(path) && Files.isRegularFile(path),
              "Certificate path must contain the files 'ca.pem', 'cert.pem', and 'key.pem' files");
    }
  }

}
