/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.app.test.http.server;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Map;
import java.util.function.Supplier;

import infra.util.function.SingletonSupplier;
import infra.web.util.UriBuilder;
import infra.web.util.UriBuilderFactory;

/**
 * Lazy {@link UriBuilderFactory} that only obtains the delegate on first call.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class LazyUriBuilderFactory implements UriBuilderFactory {

  private final Supplier<UriBuilderFactory> supplier;

  LazyUriBuilderFactory(Supplier<UriBuilderFactory> supplier) {
    this.supplier = SingletonSupplier.of(supplier);
  }

  @Override
  public URI expand(String uriTemplate, Map<String, ? extends @Nullable Object> uriVariables) {
    return delegate().expand(uriTemplate, uriVariables);
  }

  @Override
  public URI expand(String uriTemplate, @Nullable Object... uriVariables) {
    return delegate().expand(uriTemplate, uriVariables);
  }

  @Override
  public UriBuilder uriString(String uriTemplate) {
    return delegate().uriString(uriTemplate);
  }

  @Override
  public UriBuilder builder() {
    return delegate().builder();
  }

  private UriBuilderFactory delegate() {
    return this.supplier.get();
  }

}
