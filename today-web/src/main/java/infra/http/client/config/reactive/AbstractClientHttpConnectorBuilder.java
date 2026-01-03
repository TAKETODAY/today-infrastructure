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

package infra.http.client.config.reactive;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import infra.http.client.reactive.ClientHttpConnector;
import infra.http.client.config.HttpClientSettings;
import infra.lang.Assert;
import infra.util.LambdaSafe;

/**
 * Internal base class used for {@link ClientHttpConnectorBuilder} implementations.
 *
 * @param <T> the {@link ClientHttpConnector} type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
abstract class AbstractClientHttpConnectorBuilder<T extends ClientHttpConnector>
        implements ClientHttpConnectorBuilder<T> {

  private final List<Consumer<T>> customizers;

  protected AbstractClientHttpConnectorBuilder(@Nullable List<Consumer<T>> customizers) {
    this.customizers = (customizers != null) ? customizers : Collections.emptyList();
  }

  protected final List<Consumer<T>> getCustomizers() {
    return this.customizers;
  }

  protected final List<Consumer<T>> mergedCustomizers(Consumer<T> customizer) {
    Assert.notNull(this.customizers, "'customizer' is required");
    return merge(this.customizers, List.of(customizer));
  }

  protected final List<Consumer<T>> mergedCustomizers(Collection<Consumer<T>> customizers) {
    Assert.notNull(customizers, "'customizers' is required");
    Assert.noNullElements(customizers, "'customizers' must not contain null elements");
    return merge(this.customizers, customizers);
  }

  private <E> List<E> merge(Collection<E> list, Collection<? extends E> additional) {
    List<E> merged = new ArrayList<>(list);
    merged.addAll(additional);
    return List.copyOf(merged);
  }

  @Override
  @SuppressWarnings("unchecked")
  public final T build(@Nullable HttpClientSettings settings) {
    T connector = createClientHttpConnector((settings != null) ? settings : HttpClientSettings.defaults());
    LambdaSafe.callbacks(Consumer.class, this.customizers, connector)
            .invoke((consumer) -> consumer.accept(connector));
    return connector;
  }

  protected abstract T createClientHttpConnector(HttpClientSettings settings);

}
