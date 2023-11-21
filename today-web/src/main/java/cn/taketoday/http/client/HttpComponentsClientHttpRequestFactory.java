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

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpTrace;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link cn.taketoday.http.client.ClientHttpRequestFactory} implementation that
 * uses <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents
 * HttpClient</a> to create requests.
 *
 * <p>Allows to use a pre-configured {@link HttpClient} instance -
 * potentially with authentication, HTTP connection pooling, etc.
 *
 * <p><b>NOTE:</b> Requires Apache HttpComponents 5.1 or higher.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HttpComponentsClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {

  private HttpClient httpClient;

  private long connectTimeout = -1;

  private long connectionRequestTimeout = -1;

  @Nullable
  private BiFunction<HttpMethod, URI, HttpContext> httpContextFactory;

  /**
   * Create a new instance of the {@code HttpComponentsClientHttpRequestFactory}
   * with a default {@link HttpClient} based on system properties.
   */
  public HttpComponentsClientHttpRequestFactory() {
    this.httpClient = HttpClients.createSystem();
  }

  /**
   * Create a new instance of the {@code HttpComponentsClientHttpRequestFactory}
   * with the given {@link HttpClient} instance.
   *
   * @param httpClient the HttpClient instance to use for this request factory
   */
  public HttpComponentsClientHttpRequestFactory(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Set the {@code HttpClient} used for
   * {@linkplain #createRequest(URI, HttpMethod) synchronous execution}.
   */
  public void setHttpClient(HttpClient httpClient) {
    Assert.notNull(httpClient, "HttpClient is required");
    this.httpClient = httpClient;
  }

  /**
   * Return the {@code HttpClient} used for
   * {@linkplain #createRequest(URI, HttpMethod) synchronous execution}.
   */
  public HttpClient getHttpClient() {
    return this.httpClient;
  }

  /**
   * Set the connection timeout for the underlying {@link RequestConfig}.
   * A timeout value of 0 specifies an infinite timeout.
   * <p>Additional properties can be configured by specifying a
   * {@link RequestConfig} instance on a custom {@link HttpClient}.
   * <p>This options does not affect connection timeouts for SSL
   * handshakes or CONNECT requests; for that, it is required to
   * use the {@link SocketConfig} on the
   * {@link HttpClient} itself.
   *
   * @param connectTimeout the timeout value in milliseconds
   * @see ConnectionConfig#getConnectTimeout()
   * @see SocketConfig#getSoTimeout
   */
  public void setConnectTimeout(int connectTimeout) {
    Assert.isTrue(connectTimeout >= 0, "Timeout must be a non-negative value");
    this.connectTimeout = connectTimeout;
  }

  /**
   * Set the connection timeout for the underlying {@link RequestConfig}.
   * A timeout value of 0 specifies an infinite timeout.
   * <p>Additional properties can be configured by specifying a
   * {@link RequestConfig} instance on a custom {@link HttpClient}.
   * <p>This options does not affect connection timeouts for SSL
   * handshakes or CONNECT requests; for that, it is required to
   * use the {@link SocketConfig} on the
   * {@link HttpClient} itself.
   *
   * @param connectTimeout the timeout value in milliseconds
   * @see ConnectionConfig#getConnectTimeout()
   * @see SocketConfig#getSoTimeout
   */
  public void setConnectTimeout(Duration connectTimeout) {
    Assert.notNull(connectTimeout, "ConnectTimeout is required");
    Assert.isTrue(!connectTimeout.isNegative(), "Timeout must be a non-negative value");
    this.connectTimeout = connectTimeout.toMillis();
  }

  /**
   * Set the timeout in milliseconds used when requesting a connection
   * from the connection manager using the underlying {@link RequestConfig}.
   * A timeout value of 0 specifies an infinite timeout.
   * <p>Additional properties can be configured by specifying a
   * {@link RequestConfig} instance on a custom {@link HttpClient}.
   *
   * @param connectionRequestTimeout the timeout value to request a connection in milliseconds
   * @see RequestConfig#getConnectionRequestTimeout()
   */
  public void setConnectionRequestTimeout(int connectionRequestTimeout) {
    Assert.isTrue(connectionRequestTimeout >= 0, "Timeout must be a non-negative value");
    this.connectionRequestTimeout = connectionRequestTimeout;
  }

  /**
   * Set the timeout in milliseconds used when requesting a connection
   * from the connection manager using the underlying {@link RequestConfig}.
   * A timeout value of 0 specifies an infinite timeout.
   * <p>Additional properties can be configured by specifying a
   * {@link RequestConfig} instance on a custom {@link HttpClient}.
   *
   * @param connectionRequestTimeout the timeout value to request a connection in milliseconds
   * @see RequestConfig#getConnectionRequestTimeout()
   */
  public void setConnectionRequestTimeout(Duration connectionRequestTimeout) {
    Assert.notNull(connectionRequestTimeout, "ConnectionRequestTimeout is required");
    Assert.isTrue(!connectionRequestTimeout.isNegative(), "Timeout must be a non-negative value");
    this.connectionRequestTimeout = connectionRequestTimeout.toMillis();
  }

  /**
   * Configure a factory to pre-create the {@link HttpContext} for each request.
   * <p>This may be useful for example in mutual TLS authentication where a
   * different {@code RestTemplate} for each client certificate such that
   * all calls made through a given {@code RestTemplate} instance as associated
   * for the same client identity. {@link HttpClientContext#setUserToken(Object)}
   * can be used to specify a fixed user token for all requests.
   *
   * @param httpContextFactory the context factory to use
   */
  public void setHttpContextFactory(BiFunction<HttpMethod, URI, HttpContext> httpContextFactory) {
    this.httpContextFactory = httpContextFactory;
  }

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
    HttpClient client = getHttpClient();

    ClassicHttpRequest httpRequest = createHttpUriRequest(httpMethod, uri);
    postProcessHttpRequest(httpRequest);
    HttpContext context = createHttpContext(httpMethod, uri);
    if (context == null) {
      context = HttpClientContext.create();
    }

    // Request configuration not set in the context
    if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
      // Use request configuration given by the user, when available
      RequestConfig config = null;
      if (httpRequest instanceof Configurable configurable) {
        config = configurable.getConfig();
      }
      if (config == null) {
        config = createRequestConfig(client);
      }
      if (config != null) {
        context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
      }
    }
    return new HttpComponentsClientHttpRequest(client, httpRequest, context);
  }

  /**
   * Create a default {@link RequestConfig} to use with the given client.
   * Can return {@code null} to indicate that no custom request config should
   * be set and the defaults of the {@link HttpClient} should be used.
   * <p>The default implementation tries to merge the defaults of the client
   * with the local customizations of this factory instance, if any.
   *
   * @param client the {@link HttpClient} (or {@code HttpAsyncClient}) to check
   * @return the actual RequestConfig to use (may be {@code null})
   * @see #mergeRequestConfig(RequestConfig)
   */
  @Nullable
  protected RequestConfig createRequestConfig(Object client) {
    if (client instanceof Configurable configurableClient) {
      RequestConfig clientRequestConfig = configurableClient.getConfig();
      return mergeRequestConfig(clientRequestConfig);
    }
    return mergeRequestConfig(RequestConfig.DEFAULT);
  }

  /**
   * Merge the given {@link HttpClient}-level {@link RequestConfig} with
   * the factory-level configuration, if necessary.
   *
   * @param clientConfig the config held by the current
   * @return the merged request config
   */
  @SuppressWarnings("deprecation")  // setConnectTimeout
  protected RequestConfig mergeRequestConfig(RequestConfig clientConfig) {
    if (this.connectTimeout == -1 && this.connectionRequestTimeout == -1) {  // nothing to merge
      return clientConfig;
    }

    RequestConfig.Builder builder = RequestConfig.copy(clientConfig);
    if (this.connectTimeout >= 0) {
      builder.setConnectTimeout(this.connectTimeout, TimeUnit.MILLISECONDS);
    }
    if (this.connectionRequestTimeout >= 0) {
      builder.setConnectionRequestTimeout(this.connectionRequestTimeout, TimeUnit.MILLISECONDS);
    }
    return builder.build();
  }

  /**
   * Create a Commons HttpMethodBase object for the given HTTP method and URI specification.
   *
   * @param httpMethod the HTTP method
   * @param uri the URI
   * @return the Commons HttpMethodBase object
   */
  protected ClassicHttpRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
    return switch (httpMethod) {
      case GET -> new HttpGet(uri);
      case PUT -> new HttpPut(uri);
      case HEAD -> new HttpHead(uri);
      case POST -> new HttpPost(uri);
      case TRACE -> new HttpTrace(uri);
      case PATCH -> new HttpPatch(uri);
      case DELETE -> new HttpDelete(uri);
      case OPTIONS -> new HttpOptions(uri);
      default -> throw new UnsupportedOperationException("Unsupported httpMethod '" + httpMethod + "'");
    };
  }

  /**
   * Template method that allows for manipulating the {@link ClassicHttpRequest} before it is
   * returned as part of a {@link HttpComponentsClientHttpRequest}.
   * <p>The default implementation is empty.
   *
   * @param request the request to process
   */
  protected void postProcessHttpRequest(ClassicHttpRequest request) { }

  /**
   * Template methods that creates a {@link HttpContext} for the given HTTP method and URI.
   * <p>The default implementation returns {@code null}.
   *
   * @param httpMethod the HTTP method
   * @param uri the URI
   * @return the http context
   */
  @Nullable
  protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
    return httpContextFactory != null ? httpContextFactory.apply(httpMethod, uri) : null;
  }

  /**
   * Shutdown hook that closes the underlying
   * {@link HttpClientConnectionManager ClientConnectionManager}'s
   * connection pool, if any.
   */
  @Override
  public void destroy() throws Exception {
    HttpClient httpClient = getHttpClient();
    if (httpClient instanceof Closeable closeable) {
      closeable.close();
    }
  }
}
