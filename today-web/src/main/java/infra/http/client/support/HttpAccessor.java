/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.http.client.support;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.http.HttpMethod;
import infra.http.HttpRequest;
import infra.http.client.BufferingClientHttpRequestFactory;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestInitializer;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Base class for {@link infra.web.client.RestTemplate}
 * and other HTTP accessing gateway helpers, defining common properties
 * such as the {@link ClientHttpRequestFactory} to operate on.
 *
 * <p>Not intended to be used directly.
 *
 * <p>See {@link infra.web.client.RestTemplate} for an entry point.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ClientHttpRequestFactory
 * @see infra.web.client.RestTemplate
 * @since 4.0
 */
public abstract class HttpAccessor {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final ArrayList<ClientHttpRequestInitializer> httpRequestInitializers = new ArrayList<>();

  private ClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();

  @Nullable
  private Predicate<HttpRequest> bufferingPredicate;

  /**
   * Set the request factory that this accessor uses for obtaining client request handles.
   * <p>The default is a {@link JdkClientHttpRequestFactory} based on the JDK's own
   * HTTP libraries ({@link java.net.HttpURLConnection}).
   * <p><b>Note that the standard JDK HTTP library does not support the HTTP PATCH method.
   * Configure the Apache HttpComponents or OkHttp request factory to enable PATCH.</b>
   *
   * @see #createRequest(URI, HttpMethod)
   * @see JdkClientHttpRequestFactory
   * @see infra.http.client.HttpComponentsClientHttpRequestFactory
   */
  public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
    Assert.notNull(requestFactory, "ClientHttpRequestFactory is required");
    this.requestFactory = requestFactory;
  }

  /**
   * Return the request factory that this accessor uses for obtaining client request handles.
   */
  public ClientHttpRequestFactory getRequestFactory() {
    return this.bufferingPredicate != null ? new BufferingClientHttpRequestFactory(
            this.requestFactory, this.bufferingPredicate) : this.requestFactory;
  }

  /**
   * Set the request initializers that this accessor should use.
   * <p>The initializers will get immediately sorted according to their
   * {@linkplain AnnotationAwareOrderComparator#sort(List) order}.
   */
  public void setHttpRequestInitializers(List<ClientHttpRequestInitializer> requestInitializers) {
    if (this.httpRequestInitializers != requestInitializers) {
      this.httpRequestInitializers.clear();
      this.httpRequestInitializers.addAll(requestInitializers);
      AnnotationAwareOrderComparator.sort(this.httpRequestInitializers);
      httpRequestInitializers.trimToSize();
    }
  }

  /**
   * Get the request initializers that this accessor uses.
   * <p>The returned {@link List} is active and may be modified. Note,
   * however, that the initializers will not be resorted according to their
   * {@linkplain AnnotationAwareOrderComparator#sort(List) order} before the
   * {@link ClientHttpRequest} is initialized.
   *
   * @see #setHttpRequestInitializers(List)
   */
  public List<ClientHttpRequestInitializer> getHttpRequestInitializers() {
    return this.httpRequestInitializers;
  }

  /**
   * Enable buffering of request and response, aggregating all content before
   * it is sent, and making it possible to read the response body repeatedly.
   *
   * @param predicate to determine whether to buffer for the given request
   * @since 5.0
   */
  public void setBufferingPredicate(@Nullable Predicate<HttpRequest> predicate) {
    this.bufferingPredicate = predicate;
  }

  /**
   * Return the {@link #setBufferingPredicate(Predicate) configured} predicate
   * to determine whether to buffer request and response content.
   *
   * @since 5.0
   */
  @Nullable
  public Predicate<HttpRequest> getBufferingPredicate() {
    return this.bufferingPredicate;
  }

  /**
   * Create a new {@link ClientHttpRequest} via this template's {@link ClientHttpRequestFactory}.
   *
   * @param url the URL to connect to
   * @param method the HTTP method to execute (GET, POST, etc)
   * @return the created request
   * @throws IOException in case of I/O errors
   * @see #getRequestFactory()
   * @see ClientHttpRequestFactory#createRequest(URI, HttpMethod)
   */
  protected ClientHttpRequest createRequest(URI url, HttpMethod method) throws IOException {
    ClientHttpRequest request = getRequestFactory().createRequest(url, method);
    List<ClientHttpRequestInitializer> requestInitializers = getHttpRequestInitializers();
    if (!requestInitializers.isEmpty()) {
      for (ClientHttpRequestInitializer initializer : requestInitializers) {
        initializer.initialize(request);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("HTTP {} {}", method.name(), url);
    }
    return request;
  }

}
