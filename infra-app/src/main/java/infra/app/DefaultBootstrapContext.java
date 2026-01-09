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
  @SuppressWarnings("NullAway")
  public <T> T getOrElse(Class<T> type, @Nullable T other) {
    return getOrElseSupply(type, () -> other);
  }

  @Override
  @Nullable
  public <T> T getOrElseSupply(Class<T> type, Supplier<@Nullable T> other) {
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
