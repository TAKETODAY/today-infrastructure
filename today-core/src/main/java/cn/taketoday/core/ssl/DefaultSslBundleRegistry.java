/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.ssl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Default {@link SslBundleRegistry} implementation.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultSslBundleRegistry implements SslBundleRegistry, SslBundles {

  private static final Logger logger = LoggerFactory.getLogger(DefaultSslBundleRegistry.class);

  private final Map<String, RegisteredSslBundle> registeredBundles = new ConcurrentHashMap<>();

  public DefaultSslBundleRegistry() { }

  public DefaultSslBundleRegistry(String name, SslBundle bundle) {
    registerBundle(name, bundle);
  }

  @Override
  public void registerBundle(String name, SslBundle bundle) {
    Assert.notNull(name, "Name must not be null");
    Assert.notNull(bundle, "Bundle must not be null");
    RegisteredSslBundle previous = this.registeredBundles.putIfAbsent(name, new RegisteredSslBundle(name, bundle));
    Assert.state(previous == null, () -> "Cannot replace existing SSL bundle '%s'".formatted(name));
  }

  @Override
  public void updateBundle(String name, SslBundle updatedBundle) {
    getRegistered(name).update(updatedBundle);
  }

  @Override
  public SslBundle getBundle(String name) {
    return getRegistered(name).getBundle();
  }

  @Override
  public void addBundleUpdateHandler(String name, Consumer<SslBundle> updateHandler) throws NoSuchSslBundleException {
    getRegistered(name).addUpdateHandler(updateHandler);
  }

  private RegisteredSslBundle getRegistered(String name) throws NoSuchSslBundleException {
    Assert.notNull(name, "Name must not be null");
    RegisteredSslBundle registered = this.registeredBundles.get(name);
    if (registered == null) {
      throw new NoSuchSslBundleException(name, "SSL bundle name '%s' cannot be found".formatted(name));
    }
    return registered;
  }

  private static class RegisteredSslBundle {

    private final String name;

    private final List<Consumer<SslBundle>> updateHandlers = new CopyOnWriteArrayList<>();

    private volatile SslBundle bundle;

    RegisteredSslBundle(String name, SslBundle bundle) {
      this.name = name;
      this.bundle = bundle;
    }

    void update(SslBundle updatedBundle) {
      Assert.notNull(updatedBundle, "UpdatedBundle must not be null");
      this.bundle = updatedBundle;
      if (this.updateHandlers.isEmpty()) {
        logger.warn("SSL bundle '{}' has been updated but may be in use by a technology that doesn't support SSL reloading", this.name);
      }
      this.updateHandlers.forEach((handler) -> handler.accept(updatedBundle));
    }

    SslBundle getBundle() {
      return this.bundle;
    }

    void addUpdateHandler(Consumer<SslBundle> updateHandler) {
      Assert.notNull(updateHandler, "UpdateHandler must not be null");
      this.updateHandlers.add(updateHandler);
    }

  }

}
