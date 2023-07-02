/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.web.reactive.client;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/2 15:18
 */
class ReactorNettyHttpClientMapperTests {

  @Test
  void ofWithCollectionCreatesComposite() {
    ReactorNettyHttpClientMapper one = (httpClient) -> new TestHttpClient(httpClient, "1");
    ReactorNettyHttpClientMapper two = (httpClient) -> new TestHttpClient(httpClient, "2");
    ReactorNettyHttpClientMapper three = (httpClient) -> new TestHttpClient(httpClient, "3");
    ReactorNettyHttpClientMapper compose = ReactorNettyHttpClientMapper.forCompose(List.of(one, two, three));
    TestHttpClient httpClient = (TestHttpClient) compose.apply(new TestHttpClient());
    assertThat(httpClient.getContent()).isEqualTo("123");
  }

  @Test
  void ofWhenCollectionIsNullThrowsException() {
    Collection<ReactorNettyHttpClientMapper> mappers = null;
    assertThatIllegalArgumentException().isThrownBy(() -> ReactorNettyHttpClientMapper.forCompose(mappers))
            .withMessage("Mappers must not be null");
  }

  @Test
  void ofWithArrayCreatesComposite() {
    ReactorNettyHttpClientMapper one = (httpClient) -> new TestHttpClient(httpClient, "1");
    ReactorNettyHttpClientMapper two = (httpClient) -> new TestHttpClient(httpClient, "2");
    ReactorNettyHttpClientMapper three = (httpClient) -> new TestHttpClient(httpClient, "3");
    ReactorNettyHttpClientMapper compose = ReactorNettyHttpClientMapper.forCompose(one, two, three);
    TestHttpClient httpClient = (TestHttpClient) compose.apply(new TestHttpClient());
    assertThat(httpClient.getContent()).isEqualTo("123");
  }

  @Test
  void ofWhenArrayIsNullThrowsException() {
    ReactorNettyHttpClientMapper[] mappers = null;
    assertThatIllegalArgumentException().isThrownBy(() -> ReactorNettyHttpClientMapper.forCompose(mappers))
            .withMessage("Mappers must not be null");
  }

  private static class TestHttpClient extends HttpClient {

    private final String content;

    TestHttpClient() {
      this.content = "";
    }

    TestHttpClient(HttpClient httpClient, String content) {
      this.content = (httpClient instanceof TestHttpClient testHttpClient)
                     ? testHttpClient.content + content
                     : content;
    }

    @Override
    public HttpClientConfig configuration() {
      throw new UnsupportedOperationException("Auto-generated method stub");
    }

    @Override
    protected HttpClient duplicate() {
      throw new UnsupportedOperationException("Auto-generated method stub");
    }

    String getContent() {
      return this.content;
    }

  }

}