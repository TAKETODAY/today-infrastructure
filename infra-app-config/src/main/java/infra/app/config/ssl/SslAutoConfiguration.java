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

package infra.app.config.ssl;

import java.util.List;

import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.io.ResourceLoader;
import infra.core.ssl.DefaultSslBundleRegistry;
import infra.core.ssl.SslBundleRegistry;
import infra.core.ssl.SslBundles;
import infra.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SSL.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@DisableDIAutoConfiguration
@EnableConfigurationProperties(SslProperties.class)
public class SslAutoConfiguration {

  private SslAutoConfiguration() {
  }

  @Component
  public static SslPropertiesBundleRegistrar sslPropertiesSslBundleRegistrar(SslProperties properties, ResourceLoader resourceLoader) {
    FileWatcher fileWatcher = new FileWatcher(properties.getBundle().getWatch().getFile().getQuietPeriod());
    return new SslPropertiesBundleRegistrar(properties, fileWatcher, resourceLoader);
  }

  @Component
  @ConditionalOnMissingBean({ SslBundleRegistry.class, SslBundles.class })
  public static DefaultSslBundleRegistry sslBundleRegistry(List<SslBundleRegistrar> sslBundleRegistrars) {
    DefaultSslBundleRegistry registry = new DefaultSslBundleRegistry();
    for (SslBundleRegistrar registrar : sslBundleRegistrars) {
      registrar.registerBundles(registry);
    }
    return registry;
  }

}
