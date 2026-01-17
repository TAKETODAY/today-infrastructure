/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.ssl.pem;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import infra.core.io.ResourceLoader;
import infra.lang.Assert;
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

  private final SingletonSupplier<List<X509Certificate>> certificatesSupplier;

  private final SingletonSupplier<PrivateKey> privateKeySupplier;

  @SuppressWarnings("NullAway")
  LoadedPemSslStore(PemSslStoreDetails details, ResourceLoader resourceLoader) {
    Assert.notNull(details, "Details is required");
    Assert.notNull(resourceLoader, "ResourceLoader is required");
    this.details = details;
    this.resourceLoader = resourceLoader;
    this.certificatesSupplier = supplier(() -> loadCertificates(details, resourceLoader));
    this.privateKeySupplier = supplier(() -> loadPrivateKey(details, resourceLoader));
  }

  private static <T> SingletonSupplier<T> supplier(ThrowingSupplier<T> supplier) {
    return SingletonSupplier.of(supplier.throwing(LoadedPemSslStore::asUncheckedIOException));
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
    Assert.state(CollectionUtils.isNotEmpty(certificates), "Loaded certificates are empty");
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

  @Nullable
  @Override
  public List<X509Certificate> certificates() {
    return this.certificatesSupplier.get();
  }

  @Nullable
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
