/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.lang.Assert;

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

  private final ArrayList<ApplicationListener<BootstrapContextClosedEvent>> listeners = new ArrayList<>();

  @Override
  public <T> void register(Class<T> type, InstanceSupplier<T> instanceSupplier) {
    register(type, instanceSupplier, true);
  }

  @Override
  public <T> void registerIfAbsent(Class<T> type, InstanceSupplier<T> instanceSupplier) {
    register(type, instanceSupplier, false);
  }

  private <T> void register(Class<T> type, InstanceSupplier<T> instanceSupplier, boolean replaceExisting) {
    Assert.notNull(type, "Type must not be null");
    Assert.notNull(instanceSupplier, "InstanceSupplier must not be null");
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
  public <T> InstanceSupplier<T> getRegisteredInstanceSupplier(Class<T> type) {
    synchronized(this.instanceSuppliers) {
      return (InstanceSupplier<T>) this.instanceSuppliers.get(type);
    }
  }

  @Override
  public void addCloseListener(ApplicationListener<BootstrapContextClosedEvent> listener) {
    listeners.add(listener);
  }

  @Override
  public <T> T get(Class<T> type) throws IllegalStateException {
    return getOrElseThrow(type, () -> new IllegalStateException(type.getName() + " has not been registered"));
  }

  @Override
  public <T> T getOrElse(Class<T> type, T other) {
    return getOrElseSupply(type, () -> other);
  }

  @Override
  public <T> T getOrElseSupply(Class<T> type, Supplier<T> other) {
    synchronized(this.instanceSuppliers) {
      InstanceSupplier<?> instanceSupplier = this.instanceSuppliers.get(type);
      return (instanceSupplier != null) ? getInstance(type, instanceSupplier) : other.get();
    }
  }

  @Override
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
  private <T> T getInstance(Class<T> type, InstanceSupplier<?> instanceSupplier) {
    T instance = (T) this.instances.get(type);
    if (instance == null) {
      instance = (T) instanceSupplier.get(this);
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
