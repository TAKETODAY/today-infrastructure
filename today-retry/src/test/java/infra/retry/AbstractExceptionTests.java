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

package infra.retry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractExceptionTests {

  @Test
  public void testExceptionString() throws Exception {
    Exception exception = getException("foo");
    assertThat(exception.getMessage()).isEqualTo("foo");
  }

  @Test
  public void testExceptionStringThrowable() throws Exception {
    Exception exception = getException("foo", new IllegalStateException());
    assertThat(exception.getMessage().substring(0, 3)).isEqualTo("foo");
  }

  public abstract Exception getException(String msg);

  public abstract Exception getException(String msg, Throwable t);

}
