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

package cn.taketoday.core.ssl.pem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.function.SingletonSupplier;
import cn.taketoday.util.function.ThrowingSupplier;

/**
 * {@link PemSslStore} loaded from {@link PemSslStoreDetails}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PemSslStore#load(PemSslStoreDetails)
 * @since 4.0
 */
final class LoadedPemSslStore implements PemSslStore {

  private final PemSslStoreDetails details;

  private final Supplier<List<X509Certificate>> certificatesSupplier;

  private final Supplier<PrivateKey> privateKeySupplier;

  LoadedPemSslStore(PemSslStoreDetails details) {
    Assert.notNull(details, "Details is required");
    this.details = details;
    this.certificatesSupplier = supplier(() -> loadCertificates(details));
    this.privateKeySupplier = supplier(() -> loadPrivateKey(details));
  }

  private static <T> Supplier<T> supplier(@Nullable ThrowingSupplier<T> supplier) {
    return SingletonSupplier.from(supplier.throwing(LoadedPemSslStore::asUncheckedIOException));
  }

  private static UncheckedIOException asUncheckedIOException(String message, Exception cause) {
    return new UncheckedIOException(message, (IOException) cause);
  }

  @Nullable
  private static List<X509Certificate> loadCertificates(PemSslStoreDetails details) throws IOException {
    PemContent pemContent = PemContent.load(details.certificates());
    if (pemContent == null) {
      return null;
    }
    List<X509Certificate> certificates = pemContent.getCertificates();
    Assert.state(!CollectionUtils.isEmpty(certificates), "Loaded certificates are empty");
    return certificates;
  }

  private static PrivateKey loadPrivateKey(PemSslStoreDetails details) throws IOException {
    PemContent pemContent = PemContent.load(details.privateKey());
    return (pemContent != null) ? pemContent.getPrivateKey(details.privateKeyPassword()) : null;
  }

  @Nullable
  @Override
  public String type() {
    return this.details.type();
  }

  @Nullable
  @Override
  public String alias() {
    return this.details.alias();
  }

  @Nullable
  @Override
  public String password() {
    return this.details.password();
  }

  @Override
  public List<X509Certificate> certificates() {
    return this.certificatesSupplier.get();
  }

  @Override
  public PrivateKey privateKey() {
    return this.privateKeySupplier.get();
  }

}
