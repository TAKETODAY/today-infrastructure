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

package infra.app;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Supplier;

import infra.context.ApplicationContext;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.lang.Assert;

/**
 * Default {@link ConfigurableBootstrapContext} implementation.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 10:48
 */
public class DefaultBootstrapContext implements ConfigurableBootstrapContext {

  private final Map<Class<?>, InstanceSupplier<?>> instanceSuppliers = new HashMap<>();

  private final Map<Class<?>, Object> instances = new HashMap<>();

  private final LinkedHashSet<ApplicationListener<BootstrapContextClosedEvent>>
          listeners = new LinkedHashSet<>();

  @Override
  public <T> void register(Class<T> type, InstanceSupplier<T> instanceSupplier) {
    register(type, instanceSupplier, true);
  }

  @Override
  public <T> void registerIfAbsent(Class<T> type, InstanceSupplier<T> instanceSupplier) {
    register(type, instanceSupplier, false);
  }

  private <T> void register(Class<T> type, InstanceSupplier<T> instanceSupplier, boolean replaceExisting) {
    Assert.notNull(type, "Type is required");
    Assert.notNull(instanceSupplier, "InstanceSupplier is required");
    synchronized(this.instanceSuppliers) {
      boolean alreadyRegistered = this.instanceSuppliers.containsKey(type);
      if (replaceExisting || !alreadyRegistered) {
        Assert.state(!this.instances.containsKey(type), () -> type.getName() + " has already been created");
        this.instanceSuppliers.put(type, instanceSupplier);
      }
    }
  }

  @Override
  public <T> boolean isRegistered(Class<T> type) {
    synchronized(this.instanceSuppliers) {
      return this.instanceSuppliers.containsKey(type);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  @Nullable
  public <T> InstanceSupplier<T> getRegisteredInstanceSupplier(Class<T> type) {
    synchronized(this.instanceSuppliers) {
      return (InstanceSupplier<T>) this.instanceSuppliers.get(type);
    }
  }

  @Override
  public void addCloseListener(ApplicationListener<BootstrapContextClosedEvent> listener) {
    listeners.add(listener);
  }

  @Nullable
  @Override
  public <T> T get(Class<T> type) throws IllegalStateException {
    return getOrElseThrow(type, () -> new IllegalStateException(type.getName() + " has not been registered"));
  }

  @Nullable
  @Override
  public <T> T getOrElse(Class<T> type, T other) {
    return getOrElseSupply(type, () -> other);
  }

  @Override
  @Nullable
  public <T> T getOrElseSupply(Class<T> type, Supplier<T> other) {
    synchronized(this.instanceSuppliers) {
      InstanceSupplier<?> instanceSupplier = this.instanceSuppliers.get(type);
      return (instanceSupplier != null) ? getInstance(type, instanceSupplier) : other.get();
    }
  }

  @Override
  @Nullable
  public <T, X extends Throwable> T getOrElseThrow(Class<T> type, Supplier<? extends X> exceptionSupplier) throws X {
    synchronized(this.instanceSuppliers) {
      InstanceSupplier<?> instanceSupplier = this.instanceSuppliers.get(type);
      if (instanceSupplier == null) {
        throw exceptionSupplier.get();
      }
      return getInstance(type, instanceSupplier);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> @Nullable T getInstance(Class<T> type, InstanceSupplier<?> instanceSupplier) {
    T instance = (T) this.instances.get(type);
    if (instance == null) {
      instance = (T) instanceSupplier.get(this);
      if (instance == null) {
        return null;
      }
      if (instanceSupplier.getScope() == Scope.SINGLETON) {
        this.instances.put(type, instance);
      }
    }
    return instance;
  }

  /**
   * Method to be called when {@link BootstrapContext} is closed and the
   * {@link ApplicationContext} is prepared.
   *
   * @param applicationContext the prepared context
   */
  public void close(ConfigurableApplicationContext applicationContext) {
    BootstrapContextClosedEvent event = new BootstrapContextClosedEvent(this, applicationContext);
    for (ApplicationListener<BootstrapContextClosedEvent> listener : listeners) {
      listener.onApplicationEvent(event);
    }
  }

}
