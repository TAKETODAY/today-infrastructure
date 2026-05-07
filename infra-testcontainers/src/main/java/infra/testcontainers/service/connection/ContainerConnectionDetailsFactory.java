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

package infra.testcontainers.service.connection;

import org.jspecify.annotations.Nullable;
import infra.testcontainers.lifecycle.TestcontainersStartup;
import org.testcontainers.containers.Container;
import org.testcontainers.lifecycle.Startable;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.beans.factory.InitializingBean;
import infra.context.service.connection.ConnectionDetails;
import infra.context.service.connection.ConnectionDetailsFactory;
import infra.core.ResolvableType;
import infra.core.ssl.SslBundle;
import infra.lang.Assert;
import infra.lang.TodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.origin.Origin;
import infra.origin.OriginProvider;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;

/**
 * Base class for {@link ConnectionDetailsFactory} implementations that provide
 * {@link ConnectionDetails} from a {@link ContainerConnectionSource}.
 *
 * @param <D> the connection details type
 * @param <C> the container type
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 5.0
 */
public abstract class ContainerConnectionDetailsFactory<C extends Container<?>, D extends ConnectionDetails>
        implements ConnectionDetailsFactory<ContainerConnectionSource<C>, D> {

  /**
   * Constant passed to the constructor when any connection name is accepted.
   */
  protected static final @Nullable String ANY_CONNECTION_NAME = null;

  private final List<String> connectionNames;

  private final String[] requiredClassNames;

  /**
   * Create a new {@link ContainerConnectionDetailsFactory} instance that accepts
   * {@link #ANY_CONNECTION_NAME any connection name}.
   */
  protected ContainerConnectionDetailsFactory() {
    this(ANY_CONNECTION_NAME);
  }

  /**
   * Create a new {@link ContainerConnectionDetailsFactory} instance with the given
   * connection name restriction.
   *
   * @param connectionName the required connection name or {@link #ANY_CONNECTION_NAME}
   * @param requiredClassNames the names of classes that must be present
   */
  protected ContainerConnectionDetailsFactory(@Nullable String connectionName, String... requiredClassNames) {
    this(Arrays.asList(connectionName), requiredClassNames);
  }

  /**
   * Create a new {@link ContainerConnectionDetailsFactory} instance with the given
   * supported connection names.
   *
   * @param connectionNames the supported connection names
   * @param requiredClassNames the names of classes that must be present
   */
  protected ContainerConnectionDetailsFactory(List<String> connectionNames, String... requiredClassNames) {
    Assert.notEmpty(connectionNames, "'connectionNames' must not be empty");
    this.connectionNames = connectionNames;
    this.requiredClassNames = requiredClassNames;
  }

  @Override
  public final @Nullable D getConnectionDetails(ContainerConnectionSource<C> source) {
    if (!hasRequiredClasses()) {
      return null;
    }
    try {
      @Nullable Class<?>[] generics = resolveGenerics();
      Class<?> requiredContainerType = generics[0];
      Class<?> requiredConnectionDetailsType = generics[1];
      Assert.state(requiredContainerType != null, "'requiredContainerType' is required");
      Assert.state(requiredConnectionDetailsType != null, "'requiredConnectionDetailsType' is required");
      if (sourceAccepts(source, requiredContainerType, requiredConnectionDetailsType)) {
        return getContainerConnectionDetails(source);
      }
    }
    catch (NoClassDefFoundError ex) {
      // Ignore
    }
    return null;
  }

  /**
   * Return if the given source accepts the connection. By default this method checks
   * each connection name.
   *
   * @param source the container connection source
   * @param requiredContainerType the required container type
   * @param requiredConnectionDetailsType the required connection details type
   * @return if the source accepts the connection
   */
  protected boolean sourceAccepts(ContainerConnectionSource<C> source, Class<?> requiredContainerType,
          Class<?> requiredConnectionDetailsType) {
    for (String requiredConnectionName : this.connectionNames) {
      if (source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasRequiredClasses() {
    return ObjectUtils.isEmpty(this.requiredClassNames) || Arrays.stream(this.requiredClassNames)
            .allMatch(ClassUtils::isPresent);
  }

  private @Nullable Class<?>[] resolveGenerics() {
    return ResolvableType.forClass(ContainerConnectionDetailsFactory.class, getClass()).resolveGenerics();
  }

  /**
   * Get the {@link ConnectionDetails} from the given {@link ContainerConnectionSource}
   * {@code source}. May return {@code null} if no connection can be created. Result
   * types should consider extending {@link ContainerConnectionDetails}.
   *
   * @param source the source
   * @return the service connection or {@code null}.
   */
  protected abstract @Nullable D getContainerConnectionDetails(ContainerConnectionSource<C> source);

  /**
   * Base class for {@link ConnectionDetails} results that are backed by a
   * {@link ContainerConnectionSource}.
   *
   * @param <C> the container type
   */
  protected static class ContainerConnectionDetails<C extends Container<?>>
          implements ConnectionDetails, OriginProvider, InitializingBean {

    private final ContainerConnectionSource<C> source;

    private volatile @Nullable C container;

    private volatile @Nullable SslBundle sslBundle;

    /**
     * Create a new {@link ContainerConnectionDetails} instance.
     *
     * @param source the source {@link ContainerConnectionSource}
     */
    protected ContainerConnectionDetails(ContainerConnectionSource<C> source) {
      Assert.notNull(source, "'source' is required");
      this.source = source;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
      this.container = this.source.getContainerSupplier().get();
    }

    /**
     * Return the container that back this connection details instance. This method
     * can only be called once the connection details bean has been initialized.
     *
     * @return the container instance
     */
    protected final C getContainer() {
      C container = this.container;
      Assert.state(container != null,
              "Container cannot be obtained before the connection details bean has been initialized");
      if (container instanceof Startable startable) {
        TestcontainersStartup.start(startable);
      }
      return container;
    }

    /**
     * Return the {@link SslBundle} to use with this connection or {@code null}.
     *
     * @return the ssl bundle or {@code null}
     */
    protected @Nullable SslBundle getSslBundle() {
      if (this.source.getSslBundleSource() == null) {
        return null;
      }
      SslBundle sslBundle = this.sslBundle;
      if (sslBundle == null) {
        sslBundle = this.source.getSslBundleSource().getSslBundle();
        this.sslBundle = sslBundle;
      }
      return sslBundle;
    }

    /**
     * Whether the field or bean is annotated with the given annotation.
     *
     * @param annotationType the annotation to check
     * @return whether the field or bean is annotated with the annotation
     */
    protected boolean hasAnnotation(Class<? extends Annotation> annotationType) {
      return this.source.hasAnnotation(annotationType);
    }

    @Override
    public Origin getOrigin() {
      return this.source.getOrigin();
    }

  }

  static class ContainerConnectionDetailsFactoriesRuntimeHints implements RuntimeHintsRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(ContainerConnectionDetailsFactoriesRuntimeHints.class);

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      TodayStrategies.forDefaultResourceLocation(classLoader)
              .load(ConnectionDetailsFactory.class, TodayStrategies.FailureHandler.logging(logger))
              .stream()
              .flatMap(this::requiredClassNames)
              .forEach((requiredClassName) -> hints.reflection()
                      .registerTypeIfPresent(classLoader, requiredClassName));
    }

    private Stream<String> requiredClassNames(ConnectionDetailsFactory<?, ?> connectionDetailsFactory) {
      return (connectionDetailsFactory instanceof ContainerConnectionDetailsFactory<?, ?> containerConnectionDetailsFactory)
              ? Stream.of(containerConnectionDetailsFactory.requiredClassNames) : Stream.empty();
    }

  }

}
