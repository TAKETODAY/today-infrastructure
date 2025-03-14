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

package infra.annotation.config.ssl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import infra.beans.factory.DisposableBean;
import infra.core.io.ResourceLoader;
import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundleRegistry;

/**
 * A {@link SslBundleRegistrar} that registers SSL bundles based
 * {@link SslProperties#getBundle() configuration properties}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class SslPropertiesBundleRegistrar implements SslBundleRegistrar, DisposableBean {

  private final SslProperties.Bundles properties;

  private final FileWatcher fileWatcher;

  private final ResourceLoader resourceLoader;

  SslPropertiesBundleRegistrar(SslProperties properties, FileWatcher fileWatcher, ResourceLoader resourceLoader) {
    this.properties = properties.getBundle();
    this.fileWatcher = fileWatcher;
    this.resourceLoader = resourceLoader;
  }

  @Override
  public void registerBundles(SslBundleRegistry registry) {
    registerBundles(registry, this.properties.getPem(), PropertiesSslBundle::get, this::watchedPemPaths);
    registerBundles(registry, this.properties.getJks(), PropertiesSslBundle::get, this::watchedJksPaths);
  }

  private <P extends SslBundleProperties> void registerBundles(SslBundleRegistry registry, Map<String, P> properties,
          BiFunction<P, ResourceLoader, SslBundle> bundleFactory, Function<Bundle<P>, Set<Path>> watchedPaths) {
    for (Map.Entry<String, P> entry : properties.entrySet()) {
      String bundleName = entry.getKey();
      P bundleProperties = entry.getValue();
      Supplier<SslBundle> bundleSupplier = () -> bundleFactory.apply(bundleProperties, this.resourceLoader);
      try {
        registry.registerBundle(bundleName, bundleSupplier.get());
        if (bundleProperties.isReloadOnUpdate()) {
          Supplier<Set<Path>> pathsSupplier = () -> watchedPaths.apply(new Bundle<>(bundleName, bundleProperties));
          watchForUpdates(registry, bundleName, pathsSupplier, bundleSupplier);
        }
      }
      catch (IllegalStateException ex) {
        throw new IllegalStateException("Unable to register SSL bundle '%s'".formatted(bundleName), ex);
      }
    }
  }

  private void watchForUpdates(SslBundleRegistry registry, String bundleName,
          Supplier<Set<Path>> pathsSupplier, Supplier<SslBundle> bundleSupplier) {
    try {
      fileWatcher.watch(pathsSupplier.get(), () -> registry.updateBundle(bundleName, bundleSupplier.get()));
    }
    catch (RuntimeException ex) {
      throw new IllegalStateException("Unable to watch for reload on update", ex);
    }
  }

  private Set<Path> watchedJksPaths(Bundle<JksSslBundleProperties> bundle) {
    List<BundleContentProperty> watched = new ArrayList<>();
    watched.add(new BundleContentProperty("keystore.location", bundle.properties().getKeystore().getLocation()));
    watched.add(new BundleContentProperty("truststore.location", bundle.properties().getTruststore().getLocation()));
    return watchedPaths(bundle.name(), watched);
  }

  private Set<Path> watchedPemPaths(Bundle<PemSslBundleProperties> bundle) {
    List<BundleContentProperty> watched = new ArrayList<>();
    watched.add(new BundleContentProperty("keystore.private-key", bundle.properties().getKeystore().getPrivateKey()));
    watched.add(new BundleContentProperty("keystore.certificate", bundle.properties().getKeystore().getCertificate()));
    watched.add(new BundleContentProperty("truststore.private-key", bundle.properties().getTruststore().getPrivateKey()));
    watched.add(new BundleContentProperty("truststore.certificate", bundle.properties().getTruststore().getCertificate()));
    return watchedPaths(bundle.name(), watched);
  }

  private Set<Path> watchedPaths(String bundleName, List<BundleContentProperty> properties) {
    try {
      return properties.stream()
              .filter(BundleContentProperty::hasValue)
              .map((content) -> content.toWatchPath(this.resourceLoader))
              .collect(Collectors.toSet());
    }
    catch (BundleContentNotWatchableException ex) {
      throw ex.withBundleName(bundleName);
    }
  }

  @Override
  public void destroy() throws Exception {
    fileWatcher.destroy();
  }

  private record Bundle<P>(String name, P properties) {
  }

}
