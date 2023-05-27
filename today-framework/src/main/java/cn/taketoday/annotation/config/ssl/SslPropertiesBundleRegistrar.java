/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.ssl;

import java.util.Map;
import java.util.function.Function;

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
class SslPropertiesBundleRegistrar implements SslBundleRegistrar {

  private final SslProperties.Bundles properties;

  SslPropertiesBundleRegistrar(SslProperties properties) {
    this.properties = properties.getBundle();
  }

  @Override
  public void registerBundles(SslBundleRegistry registry) {
    registerBundles(registry, this.properties.getPem(), PropertiesSslBundle::get);
    registerBundles(registry, this.properties.getJks(), PropertiesSslBundle::get);
  }

  private <P extends SslBundleProperties> void registerBundles(
          SslBundleRegistry registry, Map<String, P> properties, Function<P, SslBundle> bundleFactory) {

    for (Map.Entry<String, P> entry : properties.entrySet()) {
      String bundleName = entry.getKey();
      P bundleProperties = entry.getValue();
      registry.registerBundle(bundleName, bundleFactory.apply(bundleProperties));
    }
  }

}
