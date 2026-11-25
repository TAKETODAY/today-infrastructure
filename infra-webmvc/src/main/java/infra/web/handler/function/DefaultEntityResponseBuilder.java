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

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.lang.reflect.Type;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import infra.core.ReactiveAdapter;
import infra.core.ReactiveAdapterRegistry;
import infra.core.ResolvableType;
import infra.http.CacheControl;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.util.Feature;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.HttpContext;
import infra.web.async.DeferredResult;

import static infra.util.FeatureDetector.isPresent;

/**
 * Default {@link EntityResponse.Builder} implementation.
 *
 * @param <T> the entity type
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DefaultEntityResponseBuilder<T> implements EntityResponse.Builder<T> {

  private final T entity;

  private final Type entityType;

  private HttpStatusCode status = HttpStatus.OK;

  private final HttpHeaders headers = HttpHeaders.forWritable();

  private final LinkedMultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

  public DefaultEntityResponseBuilder(T entity, @Nullable Type entityType) {
    this.entity = entity;
    this.entityType = (entityType != null) ? entityType : entity.getClass();
  }

  @Override
  public EntityResponse.Builder<T> status(HttpStatusCode status) {
    Assert.notNull(status, "HttpStatusCode is required");
    this.status = status;
    return this;
  }

  @Override
  public EntityResponse.Builder<T> status(int status) {
    return status(HttpStatusCode.valueOf(status));
  }

  @Override
  public EntityResponse.Builder<T> cookie(ResponseCookie cookie) {
    Assert.notNull(cookie, "Cookie is required");
    this.cookies.add(cookie.getName(), cookie);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> cookie(String name, String... values) {
    for (String value : values) {
      this.cookies.add(name, ResponseCookie.builder(name, value).build());
    }
    return this;
  }

  @Override
  public EntityResponse.Builder<T> cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer) {
    cookiesConsumer.accept(this.cookies);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> cookies(@Nullable Collection<ResponseCookie> cookies) {
    if (CollectionUtils.isNotEmpty(cookies)) {
      for (ResponseCookie cookie : cookies) {
        this.cookies.add(cookie.getName(), cookie);
      }
    }
    return this;
  }

  @Override
  public EntityResponse.Builder<T> cookies(@Nullable MultiValueMap<String, ResponseCookie> cookies) {
    this.cookies.setAll(cookies);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> header(String headerName, String @Nullable ... headerValues) {
    headers.setOrRemove(headerName, headerValues);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> headers(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(this.headers);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> headers(@Nullable HttpHeaders headers) {
    this.headers.setAll(headers);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> allow(HttpMethod... allowedMethods) {
    this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
    return this;
  }

  @Override
  public EntityResponse.Builder<T> allow(Set<HttpMethod> allowedMethods) {
    this.headers.setAllow(allowedMethods);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> contentLength(long contentLength) {
    this.headers.setContentLength(contentLength);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> contentType(MediaType contentType) {
    this.headers.setContentType(contentType);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> eTag(@Nullable String etag) {
    this.headers.setETag(etag);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> lastModified(ZonedDateTime lastModified) {
    this.headers.setLastModified(lastModified);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> lastModified(Instant lastModified) {
    this.headers.setLastModified(lastModified);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> location(URI location) {
    this.headers.setLocation(location);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> cacheControl(CacheControl cacheControl) {
    this.headers.setCacheControl(cacheControl);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> varyBy(String... requestHeaders) {
    this.headers.setVary(Arrays.asList(requestHeaders));
    return this;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public EntityResponse<T> build() {
    if (this.entity instanceof CompletionStage completionStage) {
      return new CompletionStageEntityResponse(this.status, this.headers, this.cookies,
              completionStage, this.entityType);
    }
    else if (isPresent(Feature.REACTIVE_STREAMS)) {
      ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(this.entity.getClass());
      if (adapter != null) {
        Publisher<T> publisher = adapter.toPublisher(this.entity);
        return new PublisherEntityResponse(this.status, this.headers, this.cookies, publisher, this.entityType);
      }
    }
    return new DefaultEntityResponse<>(this.status, this.headers, this.cookies, this.entity, this.entityType);
  }

  /**
   * Default {@link EntityResponse} implementation for synchronous bodies.
   */
  private static class DefaultEntityResponse<T> extends AbstractServerResponse implements EntityResponse<T> {

    private final T entity;

    private final Type entityType;

    public DefaultEntityResponse(HttpStatusCode statusCode, HttpHeaders headers,
            MultiValueMap<String, ResponseCookie> cookies, T entity, Type entityType) {

      super(statusCode, headers, cookies);
      this.entity = entity;
      this.entityType = entityType;
    }

    @Override
    public T entity() {
      return this.entity;
    }

    @Override
    protected Object writeToInternal(HttpContext request) {
      return new Body(this.entity, this.entityType, this);
    }

    final Type elementType(Class<?> container) {
      ResolvableType elementType = ResolvableType.forType(this.entityType).as(container).getGeneric();
      return elementType.resolve() != null ? elementType.getType() : Object.class;
    }

  }

  /**
   * {@link EntityResponse} implementation for asynchronous {@link CompletionStage} bodies.
   */
  private static class CompletionStageEntityResponse<T> extends DefaultEntityResponse<CompletionStage<T>> {

    public CompletionStageEntityResponse(HttpStatusCode statusCode, HttpHeaders headers,
            MultiValueMap<String, ResponseCookie> cookies, CompletionStage<T> entity, Type entityType) {

      super(statusCode, headers, cookies, entity, entityType);
    }

    @Override
    protected Object writeToInternal(HttpContext request) {
      DeferredResult<Object> result = new DeferredResult<>();
      entity().whenComplete((value, ex) -> {
        if (ex != null) {
          if (ex instanceof CompletionException && ex.getCause() != null) {
            ex = ex.getCause();
          }
          try {
            result.setResult(handleError(ex, request));
          }
          catch (Throwable failure) {
            result.setErrorResult(failure);
          }
        }
        else {
          result.setResult(value != null ? new Body(value, elementType(CompletionStage.class), this) : NONE_RETURN_VALUE);
        }
      });
      return result;
    }

  }

  /**
   * {@link EntityResponse} implementation for asynchronous {@link Publisher} bodies.
   */
  private static class PublisherEntityResponse<T> extends DefaultEntityResponse<Publisher<T>> {

    public PublisherEntityResponse(HttpStatusCode statusCode, HttpHeaders headers,
            MultiValueMap<String, ResponseCookie> cookies, Publisher<T> entity, Type entityType) {
      super(statusCode, headers, cookies, entity, entityType);
    }

    @Override
    protected Object writeToInternal(HttpContext request) {
      return new PublisherBody(entity(), elementType(Publisher.class), this);
    }

  }

  record Body(Object value, Type type, ErrorHandlingServerResponse response) { }

  record PublisherBody(Publisher<?> publisher, Type elementType, ErrorHandlingServerResponse response) { }

}
