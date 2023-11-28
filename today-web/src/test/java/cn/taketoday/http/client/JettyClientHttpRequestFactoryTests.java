/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.http.client;

import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import cn.taketoday.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/1 23:40
 */
class JettyClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

  @Override
  protected ClientHttpRequestFactory createRequestFactory() {
    return new JettyClientHttpRequestFactory();
  }

  @Override
  @Test
  public void httpMethods() throws Exception {
    super.httpMethods();
    assertHttpMethod("patch", HttpMethod.PATCH);
  }

  @Test
  void setConnectTimeout() {
    JettyClientHttpRequestFactory requestFactory = (JettyClientHttpRequestFactory) factory;
    assertThatThrownBy(() -> requestFactory.setConnectTimeout(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Timeout must be a non-negative value");

    requestFactory.setConnectTimeout(1);

    requestFactory.setConnectTimeout(Duration.ZERO);

    assertThatThrownBy(() -> requestFactory.setConnectTimeout(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ConnectTimeout is required");

  }

  @Test
  void setReadTimeout() {
    JettyClientHttpRequestFactory requestFactory = (JettyClientHttpRequestFactory) factory;
    assertThatThrownBy(() -> requestFactory.setReadTimeout(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Timeout must be a positive value");

    requestFactory.setReadTimeout(1);

    requestFactory.setReadTimeout(Duration.ZERO);

    assertThatThrownBy(() -> requestFactory.setReadTimeout(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ReadTimeout is required");

  }

  @Test
  void startHttpClientFailed() throws Exception {
    HttpClient httpClient = mock();

    doThrow(new Exception("test msg")).when(httpClient).start();

    JettyClientHttpRequestFactory requestFactory = new JettyClientHttpRequestFactory(httpClient);

    assertThatThrownBy(requestFactory::afterPropertiesSet)
            .isInstanceOf(IOException.class)
            .hasMessage("Could not start HttpClient: test msg");
  }

}
