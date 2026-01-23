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

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import infra.lang.Assert;
import infra.util.LambdaSafe;

/**
 * Interface used to build a fully configured {@link ClientHttpRequestFactory}. Builders
 * for {@link #httpComponents() Apache HTTP Components},
 * {@link #reactor() Reactor}, {@link #jdk() JDK} can
 * be obtained using the factory methods on this interface. The {@link #of(Class)} and
 * {@link #of(Supplier)} methods may be used to instantiate other
 * {@link ClientHttpRequestFactory} instances using reflection.
 *
 * @param <T> the {@link ClientHttpRequestFactory} type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@FunctionalInterface
public interface ClientHttpRequestFactoryBuilder<T extends ClientHttpRequestFactory> {

  /**
   * Build a default configured {@link ClientHttpRequestFactory}.
   *
   * @return a default configured {@link ClientHttpRequestFactory}.
   */
  default T build() {
    return build(null);
  }

  /**
   * Build a fully configured {@link ClientHttpRequestFactory}, applying the given
   * {@code settings} if they are provided.
   *
   * @param settings the settings to apply or {@code null}
   * @return a fully configured {@link ClientHttpRequestFactory}.
   */
  T build(@Nullable HttpClientSettings settings);

  /**
   * Return a new {@link ClientHttpRequestFactoryBuilder} that applies the given
   * customizer to the {@link ClientHttpRequestFactory} after it has been built.
   *
   * @param customizer the customizers to apply
   * @return a new {@link ClientHttpRequestFactoryBuilder} instance
   */
  default ClientHttpRequestFactoryBuilder<T> withCustomizer(Consumer<T> customizer) {
    return withCustomizers(List.of(customizer));
  }

  /**
   * Return a new {@link ClientHttpRequestFactoryBuilder} that applies the given
   * customizers to the {@link ClientHttpRequestFactory} after it has been built.
   *
   * @param customizers the customizers to apply
   * @return a new {@link ClientHttpRequestFactoryBuilder} instance
   */
  @SuppressWarnings("unchecked")
  default ClientHttpRequestFactoryBuilder<T> withCustomizers(Collection<Consumer<T>> customizers) {
    Assert.notNull(customizers, "'customizers' is required");
    Assert.noNullElements(customizers, "'customizers' must not contain null elements");
    return (settings) -> {
      T factory = build(settings);
      LambdaSafe.callbacks(Consumer.class, customizers, factory)
              .invoke(consumer -> consumer.accept(factory));
      return factory;
    };
  }

  /**
   * Return a {@link HttpComponentsClientHttpRequestFactoryBuilder} that can be used to
   * build a {@link HttpComponentsClientHttpRequestFactory}.
   *
   * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder}
   */
  static HttpComponentsClientHttpRequestFactoryBuilder httpComponents() {
    return new HttpComponentsClientHttpRequestFactoryBuilder();
  }

  /**
   * Return a {@link ReactorClientHttpRequestFactoryBuilder} that can be used to build a
   * {@link ReactorClientHttpRequestFactory}.
   *
   * @return a new {@link ReactorClientHttpRequestFactoryBuilder}
   */
  static ReactorClientHttpRequestFactoryBuilder reactor() {
    return new ReactorClientHttpRequestFactoryBuilder();
  }

  /**
   * Return a {@link JdkClientHttpRequestFactoryBuilder} that can be used to build a
   * {@link JdkClientHttpRequestFactory} .
   *
   * @return a new {@link JdkClientHttpRequestFactoryBuilder}
   */
  static JdkClientHttpRequestFactoryBuilder jdk() {
    return new JdkClientHttpRequestFactoryBuilder();
  }

  /**
   * Return a new {@link ClientHttpRequestFactoryBuilder} for the given
   * {@code requestFactoryType}. The following implementations are supported without the
   * use of reflection:
   * <ul>
   * <li>{@link HttpComponentsClientHttpRequestFactory}</li>
   * <li>{@link JdkClientHttpRequestFactory}</li>
   * <li>{@link ReactorClientHttpRequestFactory}</li>
   * </ul>
   *
   * @param <T> the {@link ClientHttpRequestFactory} type
   * @param requestFactoryType the {@link ClientHttpRequestFactory} type
   * @return a new {@link ClientHttpRequestFactoryBuilder}
   */
  @SuppressWarnings("unchecked")
  static <T extends ClientHttpRequestFactory> ClientHttpRequestFactoryBuilder<T> of(Class<T> requestFactoryType) {
    Assert.notNull(requestFactoryType, "'requestFactoryType' is required");
    Assert.isTrue(requestFactoryType != ClientHttpRequestFactory.class,
            "'requestFactoryType' must be an implementation of ClientHttpRequestFactory");
    if (requestFactoryType == HttpComponentsClientHttpRequestFactory.class) {
      return (ClientHttpRequestFactoryBuilder<T>) httpComponents();
    }
    if (requestFactoryType == ReactorClientHttpRequestFactory.class) {
      return (ClientHttpRequestFactoryBuilder<T>) reactor();
    }
    if (requestFactoryType == JdkClientHttpRequestFactory.class) {
      return (ClientHttpRequestFactoryBuilder<T>) jdk();
    }
    return new ReflectiveComponentsClientHttpRequestFactoryBuilder<>(requestFactoryType);
  }

  /**
   * Return a new {@link ClientHttpRequestFactoryBuilder} from the given supplier, using
   * reflection to ultimately apply the {@link HttpClientSettings}.
   *
   * @param <T> the {@link ClientHttpRequestFactory} type
   * @param requestFactorySupplier the {@link ClientHttpRequestFactory} supplier
   * @return a new {@link ClientHttpRequestFactoryBuilder}
   */
  static <T extends ClientHttpRequestFactory> ClientHttpRequestFactoryBuilder<T> of(Supplier<T> requestFactorySupplier) {
    return new ReflectiveComponentsClientHttpRequestFactoryBuilder<>(requestFactorySupplier);
  }

  /**
   * Detect the most suitable {@link ClientHttpRequestFactoryBuilder} based on the
   * classpath. The method favors builders in the following order:
   * <ol>
   * <li>{@link #httpComponents()}</li>
   * <li>{@link #reactor()}</li>
   * <li>{@link #jdk()}</li>
   * </ol>
   *
   * @return the most suitable {@link ClientHttpRequestFactoryBuilder} for the classpath
   */
  static ClientHttpRequestFactoryBuilder<? extends ClientHttpRequestFactory> detect() {
    return detect(null);
  }

  /**
   * Detect the most suitable {@link ClientHttpRequestFactoryBuilder} based on the
   * classpath. The method favors builders in the following order:
   * <ol>
   * <li>{@link #httpComponents()}</li>
   * <li>{@link #reactor()}</li>
   * <li>{@link #jdk()}</li>
   * </ol>
   *
   * @param classLoader the class loader to use for detection
   * @return the most suitable {@link ClientHttpRequestFactoryBuilder} for the classpath
   */
  static ClientHttpRequestFactoryBuilder<? extends ClientHttpRequestFactory> detect(@Nullable ClassLoader classLoader) {
    if (HttpComponentsClientHttpRequestFactoryBuilder.Classes.present(classLoader)) {
      return httpComponents();
    }
    if (ReactorClientHttpRequestFactoryBuilder.Classes.present(classLoader)) {
      return reactor();
    }
    return jdk();
  }

}
