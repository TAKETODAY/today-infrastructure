/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.annotation.config.ssl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleRegistry;

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

  SslPropertiesBundleRegistrar(SslProperties properties, FileWatcher fileWatcher) {
    this.properties = properties.getBundle();
    this.fileWatcher = fileWatcher;
  }

  @Override
  public void registerBundles(SslBundleRegistry registry) {
    registerBundles(registry, this.properties.getPem(), PropertiesSslBundle::get, this::watchedPemPaths);
    registerBundles(registry, this.properties.getJks(), PropertiesSslBundle::get, this::watchedJksPaths);
  }

  private <P extends SslBundleProperties> void registerBundles(SslBundleRegistry registry,
          Map<String, P> properties, Function<P, SslBundle> bundleFactory, Function<P, Set<Path>> watchedPaths) {
    for (Map.Entry<String, P> entry : properties.entrySet()) {
      String bundleName = entry.getKey();
      P bundleProperties = entry.getValue();
      Supplier<SslBundle> bundleSupplier = () -> bundleFactory.apply(bundleProperties);
      try {
        registry.registerBundle(bundleName, bundleSupplier.get());
        if (bundleProperties.isReloadOnUpdate()) {
          Supplier<Set<Path>> pathsSupplier = () -> watchedPaths.apply(bundleProperties);
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

  private Set<Path> watchedJksPaths(JksSslBundleProperties properties) {
    ArrayList<BundleContentProperty> watched = new ArrayList<>(2);
    watched.add(new BundleContentProperty("keystore.location", properties.getKeystore().getLocation()));
    watched.add(new BundleContentProperty("truststore.location", properties.getTruststore().getLocation()));
    return watchedPaths(watched);
  }

  private Set<Path> watchedPemPaths(PemSslBundleProperties properties) {
    ArrayList<BundleContentProperty> watched = new ArrayList<>(4);
    watched.add(new BundleContentProperty("keystore.private-key", properties.getKeystore().getPrivateKey()));
    watched.add(new BundleContentProperty("keystore.certificate", properties.getKeystore().getCertificate()));
    watched.add(new BundleContentProperty("truststore.private-key", properties.getTruststore().getPrivateKey()));
    watched.add(new BundleContentProperty("truststore.certificate", properties.getTruststore().getCertificate()));
    return watchedPaths(watched);
  }

  private Set<Path> watchedPaths(List<BundleContentProperty> properties) {
    return properties.stream()
            .filter(BundleContentProperty::hasValue)
            .map(BundleContentProperty::toWatchPath)
            .collect(Collectors.toSet());
  }

  @Override
  public void destroy() throws Exception {
    fileWatcher.destroy();
  }

}
