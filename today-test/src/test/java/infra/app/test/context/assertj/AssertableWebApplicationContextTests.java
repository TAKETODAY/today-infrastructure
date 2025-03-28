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

package infra.app.test.context.assertj;

import org.junit.jupiter.api.Test;

import infra.web.mock.ConfigurableWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/23 10:58
 */
class AssertableWebApplicationContextTests {

  @Test
  void getShouldReturnProxy() {
    AssertableWebApplicationContext context = AssertableWebApplicationContext
            .get(() -> mock(ConfigurableWebApplicationContext.class));
    assertThat(context).isInstanceOf(ConfigurableWebApplicationContext.class);
  }

}