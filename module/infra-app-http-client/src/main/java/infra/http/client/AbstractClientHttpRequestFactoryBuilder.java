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

package infra.http.client;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import infra.http.client.ClientHttpRequestFactory;
import infra.lang.Assert;
import infra.util.LambdaSafe;

/**
 * Internal base class used for {@link ClientHttpRequestFactoryBuilder} implementations.
 *
 * @param <T> the {@link ClientHttpRequestFactory} type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
abstract class AbstractClientHttpRequestFactoryBuilder<T extends ClientHttpRequestFactory>
        implements ClientHttpRequestFactoryBuilder<T> {

  private final List<Consumer<T>> customizers;

  protected AbstractClientHttpRequestFactoryBuilder(@Nullable List<Consumer<T>> customizers) {
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
    ArrayList<E> merged = new ArrayList<>(list);
    merged.addAll(additional);
    return List.copyOf(merged);
  }

  @Override
  @SuppressWarnings("unchecked")
  public final T build(@Nullable HttpClientSettings settings) {
    T factory = createClientHttpRequestFactory(settings != null ? settings : HttpClientSettings.defaults());
    LambdaSafe.callbacks(Consumer.class, this.customizers, factory)
            .invoke(consumer -> consumer.accept(factory));
    return factory;
  }

  protected abstract T createClientHttpRequestFactory(HttpClientSettings settings);

}
