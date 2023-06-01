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

package cn.taketoday.http.client;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
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
 * <p><b>NOTE:</b> Requires Apache HttpComponents 4.3 or higher
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

  @Nullable
  private RequestConfig requestConfig;

  private boolean bufferRequestBody = true;

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
    Assert.notNull(httpClient, "HttpClient must not be null");
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
   * use the {@link org.apache.http.config.SocketConfig} on the
   * {@link HttpClient} itself.
   *
   * @param timeout the timeout value in milliseconds
   * @see RequestConfig#getConnectTimeout()
   * @see org.apache.http.config.SocketConfig#getSoTimeout
   */
  public void setConnectTimeout(int timeout) {
    Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
    this.requestConfig = requestConfigBuilder().setConnectTimeout(timeout).build();
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
    this.requestConfig = requestConfigBuilder()
            .setConnectionRequestTimeout(connectionRequestTimeout).build();
  }

  /**
   * Set the socket read timeout for the underlying {@link RequestConfig}.
   * A timeout value of 0 specifies an infinite timeout.
   * <p>Additional properties can be configured by specifying a
   * {@link RequestConfig} instance on a custom {@link HttpClient}.
   *
   * @param timeout the timeout value in milliseconds
   * @see RequestConfig#getSocketTimeout()
   */
  public void setReadTimeout(int timeout) {
    Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
    this.requestConfig = requestConfigBuilder().setSocketTimeout(timeout).build();
  }

  /**
   * Indicates whether this request factory should buffer the request body internally.
   * <p>Default is {@code true}. When sending large amounts of data via POST or PUT, it is
   * recommended to change this property to {@code false}, so as not to run out of memory.
   */
  @Deprecated(forRemoval = true)
  public void setBufferRequestBody(boolean bufferRequestBody) {
    this.bufferRequestBody = bufferRequestBody;
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
  public void setHttpContextFactory(@Nullable BiFunction<HttpMethod, URI, HttpContext> httpContextFactory) {
    this.httpContextFactory = httpContextFactory;
  }

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
    HttpClient client = getHttpClient();

    HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
    postProcessHttpRequest(httpRequest);
    HttpContext context = createHttpContext(httpMethod, uri);
    if (context == null) {
      context = HttpClientContext.create();
    }

    // Request configuration not set in the context
    if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
      // Use request configuration given by the user, when available
      RequestConfig config = null;
      if (httpRequest instanceof Configurable) {
        config = ((Configurable) httpRequest).getConfig();
      }
      if (config == null) {
        config = createRequestConfig(client);
      }
      if (config != null) {
        context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
      }
    }

    if (this.bufferRequestBody) {
      return new HttpComponentsClientHttpRequest(client, httpRequest, context);
    }
    else {
      return new HttpComponentsStreamingClientHttpRequest(client, httpRequest, context);
    }
  }

  /**
   * Return a builder for modifying the factory-level {@link RequestConfig}.
   */
  private RequestConfig.Builder requestConfigBuilder() {
    return this.requestConfig != null ? RequestConfig.copy(this.requestConfig) : RequestConfig.custom();
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
    if (client instanceof Configurable) {
      RequestConfig clientRequestConfig = ((Configurable) client).getConfig();
      return mergeRequestConfig(clientRequestConfig);
    }
    return this.requestConfig;
  }

  /**
   * Merge the given {@link HttpClient}-level {@link RequestConfig} with
   * the factory-level {@link RequestConfig}, if necessary.
   *
   * @param clientConfig the config held by the current
   * @return the merged request config
   */
  protected RequestConfig mergeRequestConfig(RequestConfig clientConfig) {
    if (this.requestConfig == null) {  // nothing to merge
      return clientConfig;
    }

    RequestConfig.Builder builder = RequestConfig.copy(clientConfig);
    int connectTimeout = this.requestConfig.getConnectTimeout();
    if (connectTimeout >= 0) {
      builder.setConnectTimeout(connectTimeout);
    }
    int connectionRequestTimeout = this.requestConfig.getConnectionRequestTimeout();
    if (connectionRequestTimeout >= 0) {
      builder.setConnectionRequestTimeout(connectionRequestTimeout);
    }
    int socketTimeout = this.requestConfig.getSocketTimeout();
    if (socketTimeout >= 0) {
      builder.setSocketTimeout(socketTimeout);
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
  protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
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
   * Template method that allows for manipulating the {@link HttpUriRequest} before it is
   * returned as part of a {@link HttpComponentsClientHttpRequest}.
   * <p>The default implementation is empty.
   *
   * @param request the request to process
   */
  protected void postProcessHttpRequest(HttpUriRequest request) { }

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
    return this.httpContextFactory != null ? this.httpContextFactory.apply(httpMethod, uri) : null;
  }

  /**
   * Shutdown hook that closes the underlying
   * {@link org.apache.http.conn.HttpClientConnectionManager ClientConnectionManager}'s
   * connection pool, if any.
   */
  @Override
  public void destroy() throws Exception {
    HttpClient httpClient = getHttpClient();
    if (httpClient instanceof Closeable closeable) {
      closeable.close();
    }
  }

  /**
   * An alternative to {@link org.apache.http.client.methods.HttpDelete} that
   * extends {@link org.apache.http.client.methods.HttpEntityEnclosingRequestBase}
   * rather than {@link org.apache.http.client.methods.HttpRequestBase} and
   * hence allows HTTP delete with a request body. For use with the RestTemplate
   * exchange methods which allow the combination of HTTP DELETE with an entity.
   */
  private static class HttpDelete extends HttpEntityEnclosingRequestBase {

    public HttpDelete(URI uri) {
      super();
      setURI(uri);
    }

    @Override
    public String getMethod() {
      return HttpMethod.DELETE.name();
    }
  }

}
