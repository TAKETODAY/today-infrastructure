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

package infra.core.ssl;

import org.junit.jupiter.api.Test;

import infra.core.ssl.NoSuchSslBundleException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoSuchSslBundleException}.
 *
 * @author Phillip Webb
 */
class NoSuchSslBundleExceptionTests {

  @Test
  void createCreatesException() {
    Throwable cause = new RuntimeException();
    NoSuchSslBundleException exception = new NoSuchSslBundleException("name", "badness", cause);
    assertThat(exception).hasMessage("badness").hasCause(cause);
    assertThat(exception.getBundleName()).isEqualTo("name");
  }

}
