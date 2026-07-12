/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.filter;

import org.junit.jupiter.api.Test;

import infra.core.Ordered;
import infra.core.env.Environment;
import infra.web.FilterChain;
import infra.web.HttpContext;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GenericFilterBean}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class GenericFilterBeanTests {

  @Test
  void defaultOrderIsLowestPrecedence() {
    var filter = new GenericFilterBean() {
      @Override
      public void doFilter(HttpContext request, FilterChain chain) {
      }
    };
    assertThat(filter.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
  }

  @Test
  void setOrder() {
    var filter = new GenericFilterBean() {
      @Override
      public void doFilter(HttpContext request, FilterChain chain) {
      }
    };
    filter.setOrder(42);
    assertThat(filter.getOrder()).isEqualTo(42);
  }

  @Test
  void getFilterNameReturnsBeanName() {
    var filter = new GenericFilterBean() {
      @Override
      public void doFilter(HttpContext request, FilterChain chain) {
      }
    };
    filter.setBeanName("myFilter");
    assertThat(filter.getFilterName()).isEqualTo("myFilter");
  }

  @Test
  void getFilterNameReturnsNullByDefault() {
    var filter = new GenericFilterBean() {
      @Override
      public void doFilter(HttpContext request, FilterChain chain) {
      }
    };
    assertThat(filter.getFilterName()).isNull();
  }

  @Test
  void getEnvironmentReturnsSetValue() {
    var filter = new GenericFilterBean() {
      @Override
      public void doFilter(HttpContext request, FilterChain chain) {
      }
    };
    Environment env = mockEnvironment();
    filter.setEnvironment(env);
    assertThat(filter.getEnvironment()).isSameAs(env);
  }

  @Test
  void getEnvironmentReturnsNullByDefault() {
    var filter = new GenericFilterBean() {
      @Override
      public void doFilter(HttpContext request, FilterChain chain) {
      }
    };
    assertThat(filter.getEnvironment()).isNotNull();
  }

  @Test
  void afterPropertiesSetInvokesInitFilterBean() throws Exception {
    var invoked = new boolean[] { false };
    var filter = new GenericFilterBean() {
      @Override
      public void doFilter(HttpContext request, FilterChain chain) {
      }

      @Override
      protected void initFilterBean() {
        invoked[0] = true;
      }
    };
    filter.afterPropertiesSet();
    assertThat(invoked[0]).isTrue();
  }

  @Test
  void afterPropertiesSetDoesNotThrowByDefault() {
    var filter = new GenericFilterBean() {
      @Override
      public void doFilter(HttpContext request, FilterChain chain) {
      }
    };
    assertThatCode(filter::afterPropertiesSet).doesNotThrowAnyException();
  }

  @Test
  void destroyDoesNotThrowByDefault() {
    var filter = new GenericFilterBean() {
      @Override
      public void doFilter(HttpContext request, FilterChain chain) {
      }
    };
    assertThatCode(filter::destroy).doesNotThrowAnyException();
  }

  @Test
  void loggerIsAvailable() {
    var filter = new GenericFilterBean() {
      @Override
      public void doFilter(HttpContext request, FilterChain chain) {
      }
    };
    assertThat(filter.logger).isNotNull();
  }

  @Test
  void subclassDoFilterIsInvoked() throws Throwable {
    var invoked = new boolean[] { false };
    var filter = new GenericFilterBean() {
      @Override
      public void doFilter(HttpContext request, FilterChain chain) {
        invoked[0] = true;
      }
    };
    filter.doFilter(new MockHttpContext(), null);
    assertThat(invoked[0]).isTrue();
  }

  private static Environment mockEnvironment() {
    return mock(Environment.class);
  }

}
