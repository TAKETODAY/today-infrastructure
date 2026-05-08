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

package infra.docker.compose.service.connection;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import infra.app.io.ApplicationResourceLoader;
import infra.context.service.connection.ConnectionDetails;
import infra.context.service.connection.ConnectionDetailsFactory;
import infra.core.io.ResourceLoader;
import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundleKey;
import infra.core.ssl.SslOptions;
import infra.core.ssl.jks.JksSslStoreBundle;
import infra.core.ssl.jks.JksSslStoreDetails;
import infra.core.ssl.pem.PemSslStore;
import infra.core.ssl.pem.PemSslStoreBundle;
import infra.core.ssl.pem.PemSslStoreDetails;
import infra.docker.compose.core.DockerComposeFile;
import infra.docker.compose.core.RunningService;
import infra.lang.Assert;
import infra.lang.TodayStrategies;
import infra.origin.Origin;
import infra.origin.OriginProvider;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;

/**
 * Base class for {@link ConnectionDetailsFactory} implementations that provide
 * {@link ConnectionDetails} from a {@link DockerComposeConnectionSource}.
 *
 * @param <D> the connection details type
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 5.0
 */
public abstract class DockerComposeConnectionDetailsFactory<D extends ConnectionDetails>
        implements ConnectionDetailsFactory<DockerComposeConnectionSource, D> {

  private final Predicate<DockerComposeConnectionSource> predicate;

  private final String[] requiredClassNames;

  /**
   * Create a new {@link DockerComposeConnectionDetailsFactory} instance.
   *
   * @param connectionName the required connection name
   * @param requiredClassNames the names of classes that must be present
   */
  protected DockerComposeConnectionDetailsFactory(String connectionName, String... requiredClassNames) {
    this(new ConnectionNamePredicate(connectionName), requiredClassNames);
  }

  /**
   * Create a new {@link DockerComposeConnectionDetailsFactory} instance.
   *
   * @param connectionNames the required connection name
   * @param requiredClassNames the names of classes that must be present
   */
  protected DockerComposeConnectionDetailsFactory(String[] connectionNames, String... requiredClassNames) {
    this(new ConnectionNamePredicate(connectionNames), requiredClassNames);
  }

  /**
   * Create a new {@link DockerComposeConnectionDetailsFactory} instance.
   *
   * @param predicate a predicate used to check when a service is accepted
   * @param requiredClassNames the names of classes that must be present
   */
  protected DockerComposeConnectionDetailsFactory(Predicate<DockerComposeConnectionSource> predicate, String... requiredClassNames) {
    this.predicate = predicate;
    this.requiredClassNames = requiredClassNames;
  }

  @Override
  public final @Nullable D getConnectionDetails(DockerComposeConnectionSource source) {
    return (!accept(source)) ? null : getDockerComposeConnectionDetails(source);
  }

  private boolean accept(DockerComposeConnectionSource source) {
    return hasRequiredClasses() && this.predicate.test(source);
  }

  private boolean hasRequiredClasses() {
    return ObjectUtils.isEmpty(this.requiredClassNames) || Arrays.stream(this.requiredClassNames)
            .allMatch(ClassUtils::isPresent);
  }

  /**
   * Get the {@link ConnectionDetails} from the given {@link RunningService}
   * {@code source}. May return {@code null} if no connection can be created. Result
   * types should consider extending {@link DockerComposeConnectionDetails}.
   *
   * @param source the source
   * @return the service connection or {@code null}.
   */
  protected abstract @Nullable D getDockerComposeConnectionDetails(DockerComposeConnectionSource source);

  /**
   * Convenient base class for {@link ConnectionDetails} results that are backed by a
   * {@link RunningService}.
   */
  protected static class DockerComposeConnectionDetails implements ConnectionDetails, OriginProvider {

    private final @Nullable Origin origin;

    private volatile @Nullable SslBundle sslBundle;

    /**
     * Create a new {@link DockerComposeConnectionDetails} instance.
     *
     * @param runningService the source {@link RunningService}
     */
    protected DockerComposeConnectionDetails(RunningService runningService) {
      Assert.notNull(runningService, "'runningService' is required");
      this.origin = Origin.from(runningService);
    }

    @Override
    public @Nullable Origin getOrigin() {
      return this.origin;
    }

    protected @Nullable SslBundle getSslBundle(RunningService service) {
      if (this.sslBundle != null) {
        return this.sslBundle;
      }
      SslBundle jksSslBundle = getJksSslBundle(service);
      SslBundle pemSslBundle = getPemSslBundle(service);
      if (jksSslBundle == null && pemSslBundle == null) {
        return null;
      }
      if (jksSslBundle != null && pemSslBundle != null) {
        throw new IllegalStateException("Mutually exclusive JKS and PEM ssl bundles have been configured");
      }
      SslBundle sslBundle = (jksSslBundle != null) ? jksSslBundle : pemSslBundle;
      this.sslBundle = sslBundle;
      return sslBundle;
    }

    private @Nullable SslBundle getJksSslBundle(RunningService service) {
      JksSslStoreDetails keyStoreDetails = getJksSslStoreDetails(service, "keystore");
      JksSslStoreDetails trustStoreDetails = getJksSslStoreDetails(service, "truststore");
      if (keyStoreDetails == null && trustStoreDetails == null) {
        return null;
      }
      Map<String, String> labels = service.labels();
      SslBundleKey key = SslBundleKey.of(labels.get("infra.sslbundle.jks.key.alias"), labels.get("infra.sslbundle.jks.key.password"));
      SslOptions options = createSslOptions(labels.get("infra.sslbundle.jks.options.ciphers"), labels.get("infra.sslbundle.jks.options.enabled-protocols"));
      String protocol = labels.get("infra.sslbundle.jks.protocol");
      Path workingDirectory = getWorkingDirectory(service);
      return SslBundle.of(new JksSslStoreBundle(keyStoreDetails, trustStoreDetails, getResourceLoader(workingDirectory)),
              key, options, protocol);
    }

    private ResourceLoader getResourceLoader(@Nullable Path workingDirectory) {
      ClassLoader classLoader = ApplicationResourceLoader.of().getClassLoader();
      return ApplicationResourceLoader.of(classLoader,
              TodayStrategies.forDefaultResourceLocation(classLoader), workingDirectory);
    }

    private @Nullable JksSslStoreDetails getJksSslStoreDetails(RunningService service, String storeType) {
      Map<String, String> labels = service.labels();
      String type = labels.get("infra.sslbundle.jks.%s.type".formatted(storeType));
      String provider = labels.get("infra.sslbundle.jks.%s.provider".formatted(storeType));
      String location = labels.get("infra.sslbundle.jks.%s.location".formatted(storeType));
      String password = labels.get("infra.sslbundle.jks.%s.password".formatted(storeType));
      if (location == null) {
        return null;
      }
      return new JksSslStoreDetails(type, provider, location, password);
    }

    private @Nullable Path getWorkingDirectory(RunningService runningService) {
      DockerComposeFile composeFile = runningService.composeFile();
      if (composeFile == null || CollectionUtils.isEmpty(composeFile.getFiles())) {
        return Path.of(".");
      }
      return composeFile.getFiles().get(0).toPath().getParent();
    }

    private SslOptions createSslOptions(@Nullable String ciphers, @Nullable String enabledProtocols) {
      Set<String> ciphersSet = null;
      if (StringUtils.isNotEmpty(ciphers)) {
        ciphersSet = StringUtils.commaDelimitedListToSet(ciphers);
      }
      Set<String> enabledProtocolsSet = null;
      if (StringUtils.isNotEmpty(enabledProtocols)) {
        enabledProtocolsSet = StringUtils.commaDelimitedListToSet(enabledProtocols);
      }
      return SslOptions.of(ciphersSet, enabledProtocolsSet);
    }

    private @Nullable SslBundle getPemSslBundle(RunningService service) {
      PemSslStoreDetails keyStoreDetails = getPemSslStoreDetails(service, "keystore");
      PemSslStoreDetails trustStoreDetails = getPemSslStoreDetails(service, "truststore");
      if (keyStoreDetails == null && trustStoreDetails == null) {
        return null;
      }
      SslBundleKey key = SslBundleKey.of(service.labels().get("infra.sslbundle.pem.key.alias"),
              service.labels().get("infra.sslbundle.pem.key.password"));
      SslOptions options = createSslOptions(
              service.labels().get("infra.sslbundle.pem.options.ciphers"),
              service.labels().get("infra.sslbundle.pem.options.enabled-protocols"));
      String protocol = service.labels().get("infra.sslbundle.pem.protocol");
      Path workingDirectory = getWorkingDirectory(service);
      ResourceLoader resourceLoader = getResourceLoader(workingDirectory);
      return SslBundle.of(new PemSslStoreBundle(PemSslStore.load(keyStoreDetails, resourceLoader),
              PemSslStore.load(trustStoreDetails, resourceLoader)), key, options, protocol);
    }

    private @Nullable PemSslStoreDetails getPemSslStoreDetails(RunningService service, String storeType) {
      String type = service.labels().get("infra.sslbundle.pem.%s.type".formatted(storeType));
      String certificate = service.labels().get("infra.sslbundle.pem.%s.certificate".formatted(storeType));
      String privateKey = service.labels().get("infra.sslbundle.pem.%s.private-key".formatted(storeType));
      String privateKeyPassword = service.labels().get("infra.sslbundle.pem.%s.private-key-password".formatted(storeType));
      if (certificate == null && privateKey == null) {
        return null;
      }
      return new PemSslStoreDetails(type, certificate, privateKey, privateKeyPassword);
    }

  }

}
