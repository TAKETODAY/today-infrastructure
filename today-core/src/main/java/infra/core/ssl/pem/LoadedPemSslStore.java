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

package infra.core.ssl.pem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Supplier;

import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.function.SingletonSupplier;
import infra.util.function.ThrowingSupplier;

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

  private final ResourceLoader resourceLoader;

  private final Supplier<List<X509Certificate>> certificatesSupplier;

  private final Supplier<PrivateKey> privateKeySupplier;

  LoadedPemSslStore(PemSslStoreDetails details, ResourceLoader resourceLoader) {
    Assert.notNull(details, "Details is required");
    Assert.notNull(resourceLoader, "ResourceLoader is required");
    this.details = details;
    this.resourceLoader = resourceLoader;
    this.certificatesSupplier = supplier(() -> loadCertificates(details, resourceLoader));
    this.privateKeySupplier = supplier(() -> loadPrivateKey(details, resourceLoader));
  }

  private static <T> Supplier<T> supplier(ThrowingSupplier<T> supplier) {
    return SingletonSupplier.from(supplier.throwing(LoadedPemSslStore::asUncheckedIOException));
  }

  private static UncheckedIOException asUncheckedIOException(String message, Throwable cause) {
    return new UncheckedIOException(message, (IOException) cause);
  }

  @Nullable
  private static List<X509Certificate> loadCertificates(PemSslStoreDetails details, ResourceLoader resourceLoader) throws IOException {
    PemContent pemContent = PemContent.load(details.certificates(), resourceLoader);
    if (pemContent == null) {
      return null;
    }
    List<X509Certificate> certificates = pemContent.getCertificates();
    Assert.state(!CollectionUtils.isEmpty(certificates), "Loaded certificates are empty");
    return certificates;
  }

  @Nullable
  private static PrivateKey loadPrivateKey(PemSslStoreDetails details, ResourceLoader resourceLoader) throws IOException {
    PemContent pemContent = PemContent.load(details.privateKey(), resourceLoader);
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

  @Override
  public PemSslStore withAlias(@Nullable String alias) {
    return new LoadedPemSslStore(this.details.withAlias(alias), resourceLoader);
  }

  @Override
  public PemSslStore withPassword(@Nullable String password) {
    return new LoadedPemSslStore(this.details.withPassword(password), resourceLoader);
  }

}
