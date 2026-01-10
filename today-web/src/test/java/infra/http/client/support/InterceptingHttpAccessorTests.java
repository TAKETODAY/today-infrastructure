/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.client.support;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.http.HttpRequest;
import infra.http.client.ClientHttpRequestExecution;
import infra.http.client.ClientHttpRequestInterceptor;
import infra.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InterceptingHttpAccessor}.
 *
 * @author Brian Clozel
 */
public class InterceptingHttpAccessorTests {

  @Test
  public void getInterceptors() {
    TestInterceptingHttpAccessor accessor = new TestInterceptingHttpAccessor();
    List<ClientHttpRequestInterceptor> interceptors = Arrays.asList(
            new SecondClientHttpRequestInterceptor(),
            new ThirdClientHttpRequestInterceptor(),
            new FirstClientHttpRequestInterceptor()

    );
    accessor.setInterceptors(interceptors);

    assertThat(accessor.getInterceptors().get(0)).isInstanceOf(FirstClientHttpRequestInterceptor.class);
    assertThat(accessor.getInterceptors().get(1)).isInstanceOf(SecondClientHttpRequestInterceptor.class);
    assertThat(accessor.getInterceptors().get(2)).isInstanceOf(ThirdClientHttpRequestInterceptor.class);
  }

  private class TestInterceptingHttpAccessor extends InterceptingHttpAccessor {
  }

  @Order(1)
  private class FirstClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
      return null;
    }
  }

  private class SecondClientHttpRequestInterceptor implements ClientHttpRequestInterceptor, Ordered {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
      return null;
    }

    @Override
    public int getOrder() {
      return 2;
    }
  }

  private class ThirdClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
      return null;
    }
  }

}
