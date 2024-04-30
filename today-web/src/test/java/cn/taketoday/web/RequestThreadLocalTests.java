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

package cn.taketoday.web;

import org.junit.jupiter.api.Test;

import cn.taketoday.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/4/30 21:43
 */
class RequestThreadLocalTests {

  @Test
  void netty() {
    RequestThreadLocal requestThreadLocal = RequestThreadLocal.lookup();
    assertThat(requestThreadLocal).isInstanceOf(RequestThreadLocal.Netty.class);
  }

  @Test
  @ClassPathExclusions("netty-comm*")
  void nettyNotPresent() {
    RequestThreadLocal requestThreadLocal = RequestThreadLocal.lookup();
    assertThat(requestThreadLocal).isInstanceOf(RequestThreadLocal.Default.class);
  }

}