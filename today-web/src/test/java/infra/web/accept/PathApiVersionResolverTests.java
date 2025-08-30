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

package infra.web.accept;

import org.junit.jupiter.api.Test;

import infra.mock.api.http.HttpMockRequest;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/13 11:24
 */
class PathApiVersionResolverTests {

  @Test
  void resolve() {
    testResolve(0, "/1.0/path", "1.0");
    testResolve(1, "/app/1.1/path", "1.1");
  }

  @Test
  void insufficientPathSegments() {
    assertThatThrownBy(() -> testResolve(0, "/", "1.0")).isInstanceOf(InvalidApiVersionException.class);
  }

  private static void testResolve(int index, String requestUri, String expected) {
    HttpMockRequest request = new HttpMockRequestImpl("GET", requestUri);
    String actual = new PathApiVersionResolver(index).resolveVersion(
            new MockRequestContext(null, request, null));
    assertThat(actual).isEqualTo(expected);
  }

}