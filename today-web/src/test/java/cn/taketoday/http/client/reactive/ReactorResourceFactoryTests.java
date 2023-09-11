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

package cn.taketoday.http.client.reactive;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import reactor.netty.http.HttpResources;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link ReactorResourceFactory}.
 *
 * @author Rossen Stoyanchev
 */
public class ReactorResourceFactoryTests {

  private final ReactorResourceFactory resourceFactory = new ReactorResourceFactory();

  private final ConnectionProvider connectionProvider = mock();

  private final LoopResources loopResources = mock();

  @Test
  void globalResources() {

    this.resourceFactory.setUseGlobalResources(true);
    this.resourceFactory.afterPropertiesSet();

    HttpResources globalResources = HttpResources.get();
    assertThat(this.resourceFactory.getConnectionProvider()).isSameAs(globalResources);
    assertThat(this.resourceFactory.getLoopResources()).isSameAs(globalResources);
    assertThat(globalResources.isDisposed()).isFalse();

    this.resourceFactory.destroy();

    assertThat(globalResources.isDisposed()).isTrue();
  }

  @Test
  void globalResourcesWithConsumer() {

    AtomicBoolean invoked = new AtomicBoolean();

    this.resourceFactory.addGlobalResourcesConsumer(httpResources -> invoked.set(true));
    this.resourceFactory.afterPropertiesSet();

    assertThat(invoked.get()).isTrue();
    this.resourceFactory.destroy();
  }

  @Test
  void localResources() {

    this.resourceFactory.setUseGlobalResources(false);
    this.resourceFactory.afterPropertiesSet();

    ConnectionProvider connectionProvider = this.resourceFactory.getConnectionProvider();
    LoopResources loopResources = this.resourceFactory.getLoopResources();

    assertThat(connectionProvider).isNotSameAs(HttpResources.get());
    assertThat(loopResources).isNotSameAs(HttpResources.get());

    // The below does not work since ConnectionPoolProvider simply checks if pool is empty.
    // assertFalse(connectionProvider.isDisposed());
    assertThat(loopResources.isDisposed()).isFalse();

    this.resourceFactory.destroy();

    assertThat(connectionProvider.isDisposed()).isTrue();
    assertThat(loopResources.isDisposed()).isTrue();
  }

  @Test
  void localResourcesViaSupplier() {

    this.resourceFactory.setUseGlobalResources(false);
    this.resourceFactory.setConnectionProviderSupplier(() -> this.connectionProvider);
    this.resourceFactory.setLoopResourcesSupplier(() -> this.loopResources);
    this.resourceFactory.afterPropertiesSet();

    ConnectionProvider connectionProvider = this.resourceFactory.getConnectionProvider();
    LoopResources loopResources = this.resourceFactory.getLoopResources();

    assertThat(connectionProvider).isSameAs(this.connectionProvider);
    assertThat(loopResources).isSameAs(this.loopResources);

    verifyNoMoreInteractions(this.connectionProvider, this.loopResources);

    this.resourceFactory.destroy();

    // Managed (destroy disposes)..
    verify(this.connectionProvider).disposeLater();
    verify(this.loopResources).disposeLater(eq(Duration.ofSeconds(LoopResources.DEFAULT_SHUTDOWN_QUIET_PERIOD)), eq(Duration.ofSeconds(LoopResources.DEFAULT_SHUTDOWN_TIMEOUT)));
    verifyNoMoreInteractions(this.connectionProvider, this.loopResources);
  }

  @Test
  void customShutdownDurations() {
    Duration quietPeriod = Duration.ofMillis(500);
    Duration shutdownTimeout = Duration.ofSeconds(1);
    this.resourceFactory.setUseGlobalResources(false);
    this.resourceFactory.setConnectionProviderSupplier(() -> this.connectionProvider);
    this.resourceFactory.setLoopResourcesSupplier(() -> this.loopResources);
    this.resourceFactory.setShutdownQuietPeriod(quietPeriod);
    this.resourceFactory.setShutdownTimeout(shutdownTimeout);
    this.resourceFactory.afterPropertiesSet();
    this.resourceFactory.destroy();

    verify(this.connectionProvider).disposeLater();
    verify(this.loopResources).disposeLater(eq(quietPeriod), eq(shutdownTimeout));
    verifyNoMoreInteractions(this.connectionProvider, this.loopResources);
  }

  @Test
  void externalResources() {

    this.resourceFactory.setUseGlobalResources(false);
    this.resourceFactory.setConnectionProvider(this.connectionProvider);
    this.resourceFactory.setLoopResources(this.loopResources);
    this.resourceFactory.afterPropertiesSet();

    ConnectionProvider connectionProvider = this.resourceFactory.getConnectionProvider();
    LoopResources loopResources = this.resourceFactory.getLoopResources();

    assertThat(connectionProvider).isSameAs(this.connectionProvider);
    assertThat(loopResources).isSameAs(this.loopResources);

    verifyNoMoreInteractions(this.connectionProvider, this.loopResources);

    this.resourceFactory.destroy();

    // Not managed (destroy has no impact)..
    verifyNoMoreInteractions(this.connectionProvider, this.loopResources);
  }

  @Test
  void restartWithGlobalResources() {

    this.resourceFactory.setUseGlobalResources(true);
    this.resourceFactory.afterPropertiesSet();
    this.resourceFactory.stop();
    this.resourceFactory.start();

    HttpResources globalResources = HttpResources.get();
    assertThat(this.resourceFactory.getConnectionProvider()).isSameAs(globalResources);
    assertThat(this.resourceFactory.getLoopResources()).isSameAs(globalResources);
    assertThat(globalResources.isDisposed()).isFalse();

    this.resourceFactory.destroy();

    assertThat(globalResources.isDisposed()).isTrue();
  }

  @Test
  void restartWithLocalResources() {

    this.resourceFactory.setUseGlobalResources(false);
    this.resourceFactory.afterPropertiesSet();
    this.resourceFactory.stop();
    this.resourceFactory.start();

    ConnectionProvider connectionProvider = this.resourceFactory.getConnectionProvider();
    LoopResources loopResources = this.resourceFactory.getLoopResources();

    assertThat(connectionProvider).isNotSameAs(HttpResources.get());
    assertThat(loopResources).isNotSameAs(HttpResources.get());

    // The below does not work since ConnectionPoolProvider simply checks if pool is empty.
    // assertFalse(connectionProvider.isDisposed());
    assertThat(loopResources.isDisposed()).isFalse();

    this.resourceFactory.destroy();

    assertThat(connectionProvider.isDisposed()).isTrue();
    assertThat(loopResources.isDisposed()).isTrue();
  }

  @Test
  void restartWithExternalResources() {

    this.resourceFactory.setUseGlobalResources(false);
    this.resourceFactory.setConnectionProvider(this.connectionProvider);
    this.resourceFactory.setLoopResources(this.loopResources);
    this.resourceFactory.afterPropertiesSet();
    this.resourceFactory.stop();
    this.resourceFactory.start();

    ConnectionProvider connectionProvider = this.resourceFactory.getConnectionProvider();
    LoopResources loopResources = this.resourceFactory.getLoopResources();

    assertThat(connectionProvider).isSameAs(this.connectionProvider);
    assertThat(loopResources).isSameAs(this.loopResources);

    verifyNoMoreInteractions(this.connectionProvider, this.loopResources);

    this.resourceFactory.destroy();

    // Not managed (destroy has no impact)...
    verifyNoMoreInteractions(this.connectionProvider, this.loopResources);
  }

}
