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

package infra.http.client.config;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.HttpComponentsClientHttpRequestFactory;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.http.client.ReactorClientHttpRequestFactory;
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
