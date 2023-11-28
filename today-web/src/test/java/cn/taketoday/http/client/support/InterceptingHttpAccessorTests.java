/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.http.client.support;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.client.ClientHttpRequestExecution;
import cn.taketoday.http.client.ClientHttpRequestInterceptor;
import cn.taketoday.http.client.ClientHttpResponse;

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
