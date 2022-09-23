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

package cn.taketoday.http.client;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;

import java.net.URI;

import cn.taketoday.http.HttpMethod;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * @author Stephane Nicoll
 */
public class HttpComponentsClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

  @Override
  protected ClientHttpRequestFactory createRequestFactory() {
    return new HttpComponentsClientHttpRequestFactory();
  }

  @Override
  @Test
  public void httpMethods() throws Exception {
    super.httpMethods();
    assertHttpMethod("patch", HttpMethod.PATCH);
  }

  @Test
  public void assertCustomConfig() throws Exception {
    HttpClient httpClient = HttpClientBuilder.create().build();
    HttpComponentsClientHttpRequest request;
    try (HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(httpClient)) {
      hrf.setConnectTimeout(1234);
      hrf.setConnectionRequestTimeout(4321);

      URI uri = new URI(baseUrl + "/status/ok");
      request = (HttpComponentsClientHttpRequest)
              hrf.createRequest(uri, HttpMethod.GET);

      Object config = request.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG);
      assertThat(config).as("Request config should be set").isNotNull();
      assertThat(config).as("Wrong request config type " + config.getClass().getName()).isInstanceOf(RequestConfig.class);
      RequestConfig requestConfig = (RequestConfig) config;
      assertThat(requestConfig.getConnectTimeout()).as("Wrong custom connection timeout").isEqualTo(Timeout.of(1234, MILLISECONDS));
      assertThat(requestConfig.getConnectionRequestTimeout()).as("Wrong custom connection request timeout").isEqualTo(Timeout.of(4321, MILLISECONDS));
    }
  }

  @Test
  public void defaultSettingsOfHttpClientMergedOnExecutorCustomization() throws Exception {
    RequestConfig defaultConfig = RequestConfig.custom().setConnectTimeout(1234, MILLISECONDS).build();
    CloseableHttpClient client = mock(CloseableHttpClient.class,
            withSettings().extraInterfaces(Configurable.class));
    Configurable configurable = (Configurable) client;
    given(configurable.getConfig()).willReturn(defaultConfig);

    HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(client);
    assertThat(retrieveRequestConfig(hrf)).as("Default client configuration is expected").isSameAs(defaultConfig);

    hrf.setConnectionRequestTimeout(4567);
    RequestConfig requestConfig = retrieveRequestConfig(hrf);
    assertThat(requestConfig).isNotNull();
    assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(Timeout.of(4567, MILLISECONDS));
    // Default connection timeout merged
    assertThat(requestConfig.getConnectTimeout()).isEqualTo(Timeout.of(1234, MILLISECONDS));
  }

  @Test
  public void localSettingsOverrideClientDefaultSettings() throws Exception {
    RequestConfig defaultConfig = RequestConfig.custom()
            .setConnectTimeout(1234, MILLISECONDS)
            .setConnectionRequestTimeout(6789, MILLISECONDS)
            .build();
    CloseableHttpClient client = mock(CloseableHttpClient.class,
            withSettings().extraInterfaces(Configurable.class));
    Configurable configurable = (Configurable) client;
    given(configurable.getConfig()).willReturn(defaultConfig);

    HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(client);
    hrf.setConnectTimeout(5000);

    RequestConfig requestConfig = retrieveRequestConfig(hrf);
    assertThat(requestConfig.getConnectTimeout()).isEqualTo(Timeout.of(5000, MILLISECONDS));
    assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(Timeout.of(6789, MILLISECONDS));
  }

  @Test
  public void mergeBasedOnCurrentHttpClient() throws Exception {
    RequestConfig defaultConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(1234, MILLISECONDS)
            .build();
    final CloseableHttpClient client = mock(CloseableHttpClient.class,
            withSettings().extraInterfaces(Configurable.class));
    Configurable configurable = (Configurable) client;
    given(configurable.getConfig()).willReturn(defaultConfig);

    HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory() {
      @Override
      public HttpClient getHttpClient() {
        return client;
      }
    };
    hrf.setConnectionRequestTimeout(5000);

    RequestConfig requestConfig = retrieveRequestConfig(hrf);
    assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(Timeout.of(5000, MILLISECONDS));
    assertThat(requestConfig.getConnectTimeout()).isEqualTo(RequestConfig.DEFAULT.getConnectTimeout());

    // Update the Http client so that it returns an updated config
    RequestConfig updatedDefaultConfig = RequestConfig.custom()
            .setConnectTimeout(1234, MILLISECONDS).build();
    given(configurable.getConfig()).willReturn(updatedDefaultConfig);
    hrf.setConnectionRequestTimeout(7000);
    RequestConfig requestConfig2 = retrieveRequestConfig(hrf);
    assertThat(requestConfig2.getConnectTimeout()).isEqualTo(Timeout.of(1234, MILLISECONDS));
    assertThat(requestConfig2.getConnectionRequestTimeout()).isEqualTo(Timeout.of(7000, MILLISECONDS));
  }

  private RequestConfig retrieveRequestConfig(HttpComponentsClientHttpRequestFactory factory) throws Exception {
    URI uri = new URI(baseUrl + "/status/ok");
    HttpComponentsClientHttpRequest request = (HttpComponentsClientHttpRequest)
            factory.createRequest(uri, HttpMethod.GET);
    return (RequestConfig) request.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG);
  }

}
