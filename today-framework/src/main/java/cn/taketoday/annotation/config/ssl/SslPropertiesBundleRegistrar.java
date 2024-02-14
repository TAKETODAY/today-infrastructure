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

import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleRegistry;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * A {@link SslBundleRegistrar} that registers SSL bundles based
 * {@link SslProperties#getBundle() configuration properties}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class SslPropertiesBundleRegistrar implements SslBundleRegistrar {

  private static final Pattern PEM_CONTENT = Pattern.compile("-+BEGIN\\s+[^-]*-+", Pattern.CASE_INSENSITIVE);

  private final SslProperties.Bundles properties;

  @Nullable
  private FileWatcher fileWatcher;

  SslPropertiesBundleRegistrar(SslProperties properties) {
    this.properties = properties.getBundle();
  }

  SslPropertiesBundleRegistrar(SslProperties properties, @Nullable FileWatcher fileWatcher) {
    this.properties = properties.getBundle();
    this.fileWatcher = fileWatcher;
  }

  @Override
  public void registerBundles(SslBundleRegistry registry) {
    registerBundles(registry, this.properties.getPem(), PropertiesSslBundle::get, this::getLocations);
    registerBundles(registry, this.properties.getJks(), PropertiesSslBundle::get, this::getLocations);
  }

  private <P extends SslBundleProperties> void registerBundles(SslBundleRegistry registry,
          Map<String, P> properties, Function<P, SslBundle> bundleFactory, Function<P, Set<Location>> locationsSupplier) {

    for (Map.Entry<String, P> entry : properties.entrySet()) {
      String bundleName = entry.getKey();
      P bundleProperties = entry.getValue();
      SslBundle bundle = bundleFactory.apply(bundleProperties);
      registry.registerBundle(bundleName, bundle);
      if (bundleProperties.isReloadOnUpdate()) {
        Set<Path> paths = locationsSupplier.apply(bundleProperties)
                .stream()
                .filter(Location::hasValue)
                .map(location -> toPath(bundleName, location))
                .collect(Collectors.toSet());

        fileWatcher().watch(paths,
                () -> registry.updateBundle(bundleName, bundleFactory.apply(bundleProperties)));
      }
    }
  }

  private FileWatcher fileWatcher() {
    if (fileWatcher == null) {
      fileWatcher = new FileWatcher(properties.getWatch().getFile().getQuietPeriod());
    }
    return fileWatcher;
  }

  private Set<Location> getLocations(JksSslBundleProperties properties) {
    var keystore = properties.getKeystore();
    var truststore = properties.getTruststore();
    Set<Location> locations = new LinkedHashSet<>();
    locations.add(new Location("keystore.location", keystore.getLocation()));
    locations.add(new Location("truststore.location", truststore.getLocation()));
    return locations;
  }

  private Set<Location> getLocations(PemSslBundleProperties properties) {
    var keystore = properties.getKeystore();
    var truststore = properties.getTruststore();
    var locations = new LinkedHashSet<Location>();
    locations.add(new Location("keystore.private-key", keystore.getPrivateKey()));
    locations.add(new Location("keystore.certificates", keystore.getCertificate()));
    locations.add(new Location("truststore.private-key", truststore.getPrivateKey()));
    locations.add(new Location("truststore.certificates", truststore.getCertificate()));
    return locations;
  }

  private Path toPath(String bundleName, Location watchableLocation) {
    String value = watchableLocation.value();
    String field = watchableLocation.field();
    Assert.state(!PEM_CONTENT.matcher(value).find(),
            () -> "SSL bundle '%s' '%s' is not a URL and can't be watched".formatted(bundleName, field));
    try {
      URL url = ResourceUtils.getURL(value);
      Assert.state("file".equalsIgnoreCase(url.getProtocol()),
              () -> "SSL bundle '%s' '%s' URL '%s' doesn't point to a file".formatted(bundleName, field, url));
      return Path.of(url.toURI()).toAbsolutePath();
    }
    catch (Exception ex) {
      throw new IllegalStateException(
              "SSL bundle '%s' '%s' location '%s' cannot be watched".formatted(bundleName, field, value), ex);
    }
  }

  private record Location(String field, String value) {

    boolean hasValue() {
      return StringUtils.hasText(this.value);
    }

  }

}
