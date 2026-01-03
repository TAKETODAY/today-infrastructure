/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.client.config.reactive;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test customizer that can assert that it has been called.
 *
 * @param <T> type being customized
 * @author Phillip Webb
 */
class TestCustomizer<T> implements Consumer<T> {

  private boolean called;

  @Override
  public void accept(T t) {
    this.called = true;
  }

  void assertCalled() {
    assertThat(this.called).isTrue();
  }

}
