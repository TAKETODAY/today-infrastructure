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

package cn.taketoday.framework.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/13 11:44
 */
class HikariCheckpointRestoreLifecycleTests {

  private final HikariCheckpointRestoreLifecycle lifecycle;

  private final HikariDataSource dataSource;

  HikariCheckpointRestoreLifecycleTests() {
    HikariConfig config = new HikariConfig();
    config.setAllowPoolSuspension(true);
    config.setJdbcUrl("jdbc:hsqldb:mem:test-" + UUID.randomUUID());
    config.setPoolName("lifecycle-tests");
    this.dataSource = new HikariDataSource(config);
    this.lifecycle = new HikariCheckpointRestoreLifecycle(this.dataSource);
  }

  @Test
  void startedWhenStartedShouldSucceed() {
    assertThat(this.lifecycle.isRunning()).isTrue();
    this.lifecycle.start();
    assertThat(this.lifecycle.isRunning()).isTrue();
  }

  @Test
  void stopWhenStoppedShouldSucceed() {
    assertThat(this.lifecycle.isRunning()).isTrue();
    this.lifecycle.stop();
    assertThat(this.dataSource.isRunning()).isFalse();
    assertThatNoException().isThrownBy(this.lifecycle::stop);
  }

  @Test
  void whenStoppedAndStartedDataSourceShouldPauseAndResume() {
    assertThat(this.lifecycle.isRunning()).isTrue();
    this.lifecycle.stop();
    assertThat(this.dataSource.isRunning()).isFalse();
    assertThat(this.dataSource.isClosed()).isFalse();
    assertThat(this.lifecycle.isRunning()).isFalse();
    assertThat(this.dataSource.getHikariPoolMXBean().getTotalConnections()).isZero();
    this.lifecycle.start();
    assertThat(this.dataSource.isRunning()).isTrue();
    assertThat(this.dataSource.isClosed()).isFalse();
    assertThat(this.lifecycle.isRunning()).isTrue();
  }

  @Test
  void whenDataSourceIsClosedThenStartShouldThrow() {
    this.dataSource.close();
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(this.lifecycle::start);
  }

}