/*
 * Copyright 2012-present the original author or authors.
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

package infra.test.web.mock.client;

import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.JdkClientHttpRequestFactory;

/**
 * Testing utility that trusts all TLS connections during tests.
 * <p>
 * This can be configured on a test client when binding to a live server using
 * {@link RestTestClient#bindToServer(ClientHttpRequestFactory)}.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public abstract class TrustAllTlsRequestFactory {

  public static HttpClient.Builder httpClientBuilder() {
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[] { new TrustAllX509TrustManager() }, new SecureRandom());
      return HttpClient.newBuilder().sslContext(sslContext);
    }
    catch (Exception ex) {
      throw new IllegalStateException("Failed to create trust-all SSL request factory", ex);
    }
  }

  public static ClientHttpRequestFactory create() {
    return new JdkClientHttpRequestFactory(httpClientBuilder().build());
  }

  private static final class TrustAllX509TrustManager implements X509TrustManager {

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

  }

}
