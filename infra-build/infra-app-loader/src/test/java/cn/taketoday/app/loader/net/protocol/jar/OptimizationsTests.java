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

package cn.taketoday.app.loader.net.protocol.jar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Optimizations}.
 *
 * @author Phillip Webb
 */
class OptimizationsTests {

  @AfterEach
  void reset() {
    Optimizations.disable();
  }

  @Test
  void defaultIsNotEnabled() {
    assertThat(Optimizations.isEnabled()).isFalse();
    assertThat(Optimizations.isEnabled(true)).isFalse();
    assertThat(Optimizations.isEnabled(false)).isFalse();
  }

  @Test
  void enableWithReadContentsEnables() {
    Optimizations.enable(true);
    assertThat(Optimizations.isEnabled()).isTrue();
    assertThat(Optimizations.isEnabled(true)).isTrue();
    assertThat(Optimizations.isEnabled(false)).isFalse();
  }

  @Test
  void enableWithoutReadContentsEnables() {
    Optimizations.enable(false);
    assertThat(Optimizations.isEnabled()).isTrue();
    assertThat(Optimizations.isEnabled(true)).isFalse();
    assertThat(Optimizations.isEnabled(false)).isTrue();
  }

  @Test
  void enableIsByThread() throws InterruptedException {
    Optimizations.enable(true);
    boolean[] enabled = new boolean[1];
    Thread thread = new Thread(() -> enabled[0] = Optimizations.isEnabled());
    thread.start();
    thread.join();
    assertThat(enabled[0]).isFalse();
  }

  @Test
  void disableDisables() {
    Optimizations.enable(true);
    Optimizations.disable();
    assertThat(Optimizations.isEnabled()).isFalse();
    assertThat(Optimizations.isEnabled(true)).isFalse();
    assertThat(Optimizations.isEnabled(false)).isFalse();

  }

}
