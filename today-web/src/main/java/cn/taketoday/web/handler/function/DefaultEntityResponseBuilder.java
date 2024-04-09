/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.handler.function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.ReactiveStreams;
import cn.taketoday.core.io.InputStreamResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceRegion;
import cn.taketoday.http.CacheControl;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRange;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.InvalidMediaTypeException;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.GenericHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.async.DeferredResult;

/**
 * Default {@link EntityResponse.Builder} implementation.
 *
 * @param <T> the entity type
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DefaultEntityResponseBuilder<T> implements EntityResponse.Builder<T> {

  private static final Type RESOURCE_REGION_LIST_TYPE =
          new ParameterizedTypeReference<List<ResourceRegion>>() { }.getType();

  private final T entity;

  private final Type entityType;

  private HttpStatusCode status = HttpStatus.OK;

  private final HttpHeaders headers = HttpHeaders.forWritable();

  private final LinkedMultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

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
  public EntityResponse.Builder<T> cookie(HttpCookie cookie) {
    Assert.notNull(cookie, "Cookie is required");
    this.cookies.add(cookie.getName(), cookie);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> cookies(
          Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer) {
    cookiesConsumer.accept(this.cookies);
    return this;
  }

  @Override
  public EntityResponse.Builder<T> header(String headerName, String... headerValues) {
    for (String headerValue : headerValues) {
      this.headers.add(headerName, headerValue);
    }
    return this;
  }

  @Override
  public EntityResponse.Builder<T> headers(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(this.headers);
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
  public EntityResponse.Builder<T> eTag(String etag) {
    if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
      etag = "\"" + etag;
    }
    if (!etag.endsWith("\"")) {
      etag = etag + "\"";
    }
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
    else if (ReactiveStreams.isPresent) {
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
            MultiValueMap<String, HttpCookie> cookies, T entity, Type entityType) {

      super(statusCode, headers, cookies);
      this.entity = entity;
      this.entityType = entityType;
    }

    @Override
    public T entity() {
      return this.entity;
    }

    @Nullable
    @Override
    protected Object writeToInternal(RequestContext request, Context context) throws Exception {
      writeEntityWithMessageConverters(this.entity, request, context);
      return NONE_RETURN_VALUE;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void writeEntityWithMessageConverters(Object entity,
            RequestContext request, ServerResponse.Context context) throws IOException {

      MediaType contentType = getContentType(request);
      Class<?> entityClass = entity.getClass();
      Type entityType = this.entityType;

      if (entityClass != InputStreamResource.class && Resource.class.isAssignableFrom(entityClass)) {
        request.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        String rangeHeader = request.requestHeaders().getFirst(HttpHeaders.RANGE);
        if (rangeHeader != null) {
          Resource resource = (Resource) entity;
          try {
            List<HttpRange> httpRanges = HttpRange.parseRanges(rangeHeader);
            request.setStatus(HttpStatus.PARTIAL_CONTENT.value());
            entity = HttpRange.toResourceRegions(httpRanges, resource);
            entityClass = entity.getClass();
            entityType = RESOURCE_REGION_LIST_TYPE;
          }
          catch (IllegalArgumentException ex) {
            request.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
            request.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
          }
        }
      }

      for (HttpMessageConverter<?> messageConverter : context.messageConverters()) {
        if (messageConverter instanceof GenericHttpMessageConverter genericMessageConverter) {
          if (genericMessageConverter.canWrite(entityType, entityClass, contentType)) {
            genericMessageConverter.write(
                    entity, entityType, contentType, request.asHttpOutputMessage());
            return;
          }
        }
        if (messageConverter.canWrite(entityClass, contentType)) {
          ((HttpMessageConverter<Object>) messageConverter).write(
                  entity, contentType, request.asHttpOutputMessage());
          return;
        }
      }

      List<MediaType> producibleMediaTypes = producibleMediaTypes(context.messageConverters(), entityClass);
      throw new HttpMediaTypeNotAcceptableException(producibleMediaTypes);
    }

    @Nullable
    private static MediaType getContentType(RequestContext response) {
      try {
        return MediaType.parseMediaType(response.getResponseContentType()).removeQualityValue();
      }
      catch (InvalidMediaTypeException ex) {
        return null;
      }
    }

    protected void tryWriteEntityWithMessageConverters(Object entity,
            RequestContext request, ServerResponse.Context context) throws Throwable {
      try {
        writeEntityWithMessageConverters(entity, request, context);
      }
      catch (IOException ex) {
        handleError(ex, request, context);
      }
    }

    private static List<MediaType> producibleMediaTypes(
            List<HttpMessageConverter<?>> messageConverters, Class<?> entityClass) {

      return messageConverters.stream()
              .filter(messageConverter -> messageConverter.canWrite(entityClass, null))
              .flatMap(messageConverter -> messageConverter.getSupportedMediaTypes(entityClass).stream())
              .toList();
    }

  }

  /**
   * {@link EntityResponse} implementation for asynchronous {@link CompletionStage} bodies.
   */
  private static class CompletionStageEntityResponse<T> extends DefaultEntityResponse<CompletionStage<T>> {

    public CompletionStageEntityResponse(HttpStatusCode statusCode, HttpHeaders headers,
            MultiValueMap<String, HttpCookie> cookies, CompletionStage<T> entity, Type entityType) {

      super(statusCode, headers, cookies, entity, entityType);
    }

    @Nullable
    @Override
    protected Object writeToInternal(RequestContext request, Context context) throws Exception {

      DeferredResult<ServerResponse> deferredResult = createDeferredResult(request, context);
      DefaultAsyncServerResponse.writeAsync(request, deferredResult);
      return NONE_RETURN_VALUE;
    }

    private DeferredResult<ServerResponse> createDeferredResult(
            RequestContext request, Context context) {

      DeferredResult<ServerResponse> result = new DeferredResult<>();
      entity().whenComplete((value, ex) -> {
        if (ex != null) {
          if (ex instanceof CompletionException && ex.getCause() != null) {
            ex = ex.getCause();
          }
          ServerResponse errorResponse = errorResponse(ex, request);
          if (errorResponse != null) {
            result.setResult(errorResponse);
          }
          else {
            result.setErrorResult(ex);
          }
        }
        else {
          try {
            tryWriteEntityWithMessageConverters(value, request, context);
            result.setResult(null);
          }
          catch (Throwable writeException) {
            result.setErrorResult(writeException);
          }
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
            MultiValueMap<String, HttpCookie> cookies, Publisher<T> entity, Type entityType) {
      super(statusCode, headers, cookies, entity, entityType);
    }

    @Nullable
    @Override
    protected Object writeToInternal(RequestContext request, Context context) throws Exception {
      DeferredResult<?> deferredResult = new DeferredResult<>();
      DefaultAsyncServerResponse.writeAsync(request, deferredResult);

      entity().subscribe(new DeferredResultSubscriber(request, context, deferredResult));
      return NONE_RETURN_VALUE;
    }

    private class DeferredResultSubscriber implements Subscriber<T> {

      private final Context context;
      @Nullable
      private Subscription subscription;
      private final RequestContext request;
      private final DeferredResult<?> deferredResult;

      public DeferredResultSubscriber(RequestContext request,
              Context context, DeferredResult<?> deferredResult) {
        this.request = request;
        this.context = context;
        this.deferredResult = deferredResult;
      }

      @Override
      public void onSubscribe(Subscription subscription) {
        if (this.subscription == null) {
          this.subscription = subscription;
          subscription.request(1);
        }
        else {
          subscription.cancel();
        }
      }

      @Override
      public void onNext(T t) {
        Assert.state(this.subscription != null, "No subscription");
        try {
          tryWriteEntityWithMessageConverters(t, request, context);
          request.getOutputStream().flush();
          this.subscription.request(1);
        }
        catch (Throwable ex) {
          this.subscription.cancel();
          this.deferredResult.setErrorResult(ex);
        }
      }

      @Override
      public void onError(Throwable t) {
        try {
          handleError(t, this.request, this.context);
        }
        catch (Throwable handlingThrowable) {
          this.deferredResult.setErrorResult(handlingThrowable);
        }
      }

      @Override
      public void onComplete() {
        try {
          request.getOutputStream().flush();
          this.deferredResult.setResult(null);
        }
        catch (IOException ex) {
          this.deferredResult.setErrorResult(ex);
        }

      }
    }

  }

}
