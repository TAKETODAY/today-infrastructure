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

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;

import cn.taketoday.http.HttpMethod;

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
    HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(httpClient);
    hrf.setConnectTimeout(1234);
    hrf.setConnectionRequestTimeout(4321);
    hrf.setReadTimeout(4567);

    URI uri = new URI(baseUrl + "/status/ok");
    HttpComponentsClientHttpRequest request = (HttpComponentsClientHttpRequest)
            hrf.createRequest(uri, HttpMethod.GET);

    Object config = request.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG);
    assertThat(config).as("Request config should be set").isNotNull();
    assertThat(config).as("Wrong request config type " + config.getClass().getName()).isInstanceOf(RequestConfig.class);
    RequestConfig requestConfig = (RequestConfig) config;
    assertThat(requestConfig.getConnectTimeout()).as("Wrong custom connection timeout").isEqualTo(1234);
    assertThat(requestConfig.getConnectionRequestTimeout()).as("Wrong custom connection request timeout").isEqualTo(4321);
    assertThat(requestConfig.getSocketTimeout()).as("Wrong custom socket timeout").isEqualTo(4567);
  }

  @Test
  public void defaultSettingsOfHttpClientMergedOnExecutorCustomization() throws Exception {
    RequestConfig defaultConfig = RequestConfig.custom().setConnectTimeout(1234).build();
    CloseableHttpClient client = mock(CloseableHttpClient.class,
            withSettings().extraInterfaces(Configurable.class));
    Configurable configurable = (Configurable) client;
    given(configurable.getConfig()).willReturn(defaultConfig);

    HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(client);
    assertThat(retrieveRequestConfig(hrf)).as("Default client configuration is expected").isSameAs(defaultConfig);

    hrf.setConnectionRequestTimeout(4567);
    RequestConfig requestConfig = retrieveRequestConfig(hrf);
    assertThat(requestConfig).isNotNull();
    assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(4567);
    // Default connection timeout merged
    assertThat(requestConfig.getConnectTimeout()).isEqualTo(1234);
  }

  @Test
  public void localSettingsOverrideClientDefaultSettings() throws Exception {
    RequestConfig defaultConfig = RequestConfig.custom()
            .setConnectTimeout(1234).setConnectionRequestTimeout(6789).build();
    CloseableHttpClient client = mock(CloseableHttpClient.class,
            withSettings().extraInterfaces(Configurable.class));
    Configurable configurable = (Configurable) client;
    given(configurable.getConfig()).willReturn(defaultConfig);

    HttpComponentsClientHttpRequestFactory hrf = new HttpComponentsClientHttpRequestFactory(client);
    hrf.setConnectTimeout(5000);

    RequestConfig requestConfig = retrieveRequestConfig(hrf);
    assertThat(requestConfig.getConnectTimeout()).isEqualTo(5000);
    assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(6789);
    assertThat(requestConfig.getSocketTimeout()).isEqualTo(-1);
  }

  @Test
  public void mergeBasedOnCurrentHttpClient() throws Exception {
    RequestConfig defaultConfig = RequestConfig.custom()
            .setSocketTimeout(1234).build();
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
    hrf.setReadTimeout(5000);

    RequestConfig requestConfig = retrieveRequestConfig(hrf);
    assertThat(requestConfig.getConnectTimeout()).isEqualTo(-1);
    assertThat(requestConfig.getConnectionRequestTimeout()).isEqualTo(-1);
    assertThat(requestConfig.getSocketTimeout()).isEqualTo(5000);

    // Update the Http client so that it returns an updated config
    RequestConfig updatedDefaultConfig = RequestConfig.custom()
            .setConnectTimeout(1234).build();
    given(configurable.getConfig()).willReturn(updatedDefaultConfig);
    hrf.setReadTimeout(7000);
    RequestConfig requestConfig2 = retrieveRequestConfig(hrf);
    assertThat(requestConfig2.getConnectTimeout()).isEqualTo(1234);
    assertThat(requestConfig2.getConnectionRequestTimeout()).isEqualTo(-1);
    assertThat(requestConfig2.getSocketTimeout()).isEqualTo(7000);
  }

  private RequestConfig retrieveRequestConfig(HttpComponentsClientHttpRequestFactory factory) throws Exception {
    URI uri = new URI(baseUrl + "/status/ok");
    HttpComponentsClientHttpRequest request = (HttpComponentsClientHttpRequest)
            factory.createRequest(uri, HttpMethod.GET);
    return (RequestConfig) request.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG);
  }

  @Test
  public void createHttpUriRequest() throws Exception {
    URI uri = new URI("https://example.com");
    testRequestBodyAllowed(uri, HttpMethod.GET, false);
    testRequestBodyAllowed(uri, HttpMethod.HEAD, false);
    testRequestBodyAllowed(uri, HttpMethod.OPTIONS, false);
    testRequestBodyAllowed(uri, HttpMethod.TRACE, false);
    testRequestBodyAllowed(uri, HttpMethod.PUT, true);
    testRequestBodyAllowed(uri, HttpMethod.POST, true);
    testRequestBodyAllowed(uri, HttpMethod.PATCH, true);
    testRequestBodyAllowed(uri, HttpMethod.DELETE, true);

  }

  private void testRequestBodyAllowed(URI uri, HttpMethod method, boolean allowed) {
    HttpUriRequest request = ((HttpComponentsClientHttpRequestFactory) this.factory).createHttpUriRequest(method, uri);
    Object actual = request instanceof HttpEntityEnclosingRequest;
    assertThat(actual).isEqualTo(allowed);
  }

}
