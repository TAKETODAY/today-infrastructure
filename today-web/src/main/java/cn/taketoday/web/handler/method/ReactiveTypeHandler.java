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

package cn.taketoday.web.handler.method;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.task.SyncTaskExecutor;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.codec.ServerSentEvent;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.async.DeferredResult;

/**
 * Private helper class to assist with handling "reactive" return values types
 * that can be adapted to a Reactive Streams {@link Publisher} through the
 * {@link ReactiveAdapterRegistry}.
 *
 * <p>Such return values may be bridged to a {@link ResponseBodyEmitter} for
 * streaming purposes at the presence of a streaming media type or based on the
 * generic type.
 *
 * <p>For all other cases {@code Publisher} output is collected and bridged to
 * {@link DeferredResult} for standard async request processing.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:54
 */
final class ReactiveTypeHandler {
  private static final Logger log = LoggerFactory.getLogger(ReactiveTypeHandler.class);

  private static final long STREAMING_TIMEOUT_VALUE = -1;
  private static final MediaType WILDCARD_SUBTYPE_SUFFIXED_BY_NDJSON = MediaType.valueOf("application/*+x-ndjson");

  private final TaskExecutor taskExecutor;
  private final ReactiveAdapterRegistry adapterRegistry;
  private final ContentNegotiationManager contentNegotiationManager;

  public ReactiveTypeHandler() {
    this(ReactiveAdapterRegistry.getSharedInstance(), new SyncTaskExecutor(), new ContentNegotiationManager());
  }

  public ReactiveTypeHandler(ContentNegotiationManager manager) {
    this(ReactiveAdapterRegistry.getSharedInstance(), new SyncTaskExecutor(), manager);
  }

  public ReactiveTypeHandler(ReactiveAdapterRegistry registry,
          TaskExecutor executor, ContentNegotiationManager manager) {
    Assert.notNull(executor, "TaskExecutor is required");
    Assert.notNull(registry, "ReactiveAdapterRegistry is required");
    Assert.notNull(manager, "ContentNegotiationManager is required");
    this.adapterRegistry = registry;
    this.taskExecutor = executor;
    this.contentNegotiationManager = manager;
  }

  /**
   * Whether the type can be adapted to a Reactive Streams {@link Publisher}.
   */
  public boolean isReactiveType(Class<?> type) {
    return adapterRegistry.getAdapter(type) != null;
  }

  /**
   * Process the given reactive return value and decide whether to adapt it
   * to a {@link ResponseBodyEmitter} or a {@link DeferredResult}.
   *
   * @return an emitter for streaming, or {@code null} if handled internally
   * with a {@link DeferredResult}
   */
  @Nullable
  public ResponseBodyEmitter handleValue(
          Object returnValue, MethodParameter returnType, RequestContext request) throws Exception {
    Assert.notNull(returnValue, "Expected return value");

    ReactiveAdapter adapter = adapterRegistry.getAdapter(returnValue.getClass());
    if (adapter == null) {
      throw new IllegalStateException("Unexpected return value: " + returnValue);
    }

    ResolvableType elementType = ResolvableType.forMethodParameter(returnType).getGeneric();
    Class<?> elementClass = elementType.toClass();

    if (adapter.isMultiValue()) {
      Collection<MediaType> mediaTypes = getMediaTypes(request);
      if (mediaTypes.stream().anyMatch(MediaType.TEXT_EVENT_STREAM::includes)
              || ServerSentEvent.class.isAssignableFrom(elementClass)) {
        var emitter = new SseEmitter(STREAMING_TIMEOUT_VALUE);
        new SseEmitterSubscriber(emitter, taskExecutor).connect(adapter, returnValue);
        return emitter;
      }
      if (CharSequence.class.isAssignableFrom(elementClass)) {
        Optional<MediaType> mediaType = mediaTypes.stream()
                .filter(MimeType::isConcrete)
                .findFirst();
        var emitter = getEmitter(mediaType.orElse(MediaType.TEXT_PLAIN));
        new TextEmitterSubscriber(emitter, taskExecutor).connect(adapter, returnValue);
        return emitter;
      }
      MediaType streamingResponseType = findConcreteStreamingMediaType(mediaTypes);
      if (streamingResponseType != null) {
        ResponseBodyEmitter emitter = getEmitter(streamingResponseType);
        new JsonEmitterSubscriber(emitter, this.taskExecutor).connect(adapter, returnValue);
        return emitter;
      }
    }

    // Not streaming...
    DeferredResult<Object> result = new DeferredResult<>();
    new DeferredResultSubscriber(result, adapter, elementType)
            .connect(adapter, returnValue);

    request.getAsyncManager()
            .startDeferredResultProcessing(result);

    return null;
  }

  /**
   * Attempts to find a concrete {@code MediaType} that can be streamed (as json separated
   * by newlines in the response body). This method considers two concrete types
   * {@code APPLICATION_NDJSON} and {@code APPLICATION_STREAM_JSON}) as well as any
   * subtype of application that has the {@code +x-ndjson} suffix. In the later case,
   * the media type MUST be concrete for it to be considered.
   *
   * <p>For example {@code application/vnd.myapp+x-ndjson} is considered a streaming type
   * while {@code application/*+x-ndjson} isn't.
   *
   * @param acceptedMediaTypes the collection of acceptable media types in the request
   * @return the concrete streaming {@code MediaType} if one could be found or {@code null}
   * if none could be found
   */
  @Nullable
  static MediaType findConcreteStreamingMediaType(Collection<MediaType> acceptedMediaTypes) {
    for (MediaType acceptedType : acceptedMediaTypes) {
      if (WILDCARD_SUBTYPE_SUFFIXED_BY_NDJSON.includes(acceptedType)) {
        if (acceptedType.isConcrete()) {
          return acceptedType;
        }
        else {
          // if not concrete, it must be application/*+x-ndjson: we assume
          // that the requester is only interested in the ndjson nature of
          // the underlying representation and can parse any example of that
          // underlying representation, so we use the ndjson media type.
          return MediaType.APPLICATION_NDJSON;
        }
      }
      else if (MediaType.APPLICATION_NDJSON.includes(acceptedType)) {
        return MediaType.APPLICATION_NDJSON;
      }
      else if (MediaType.APPLICATION_STREAM_JSON.includes(acceptedType)) {
        return MediaType.APPLICATION_STREAM_JSON;
      }
    }
    return null; // not a concrete streaming type
  }

  private Collection<MediaType> getMediaTypes(RequestContext request)
          throws HttpMediaTypeNotAcceptableException {
    HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata != null) {
      MediaType[] producibleMediaTypes = matchingMetadata.getProducibleMediaTypes();
      if (ObjectUtils.isNotEmpty(producibleMediaTypes)) {
        return Arrays.asList(producibleMediaTypes);
      }
    }
    return contentNegotiationManager.resolveMediaTypes(request);
  }

  private ResponseBodyEmitter getEmitter(MediaType mediaType) {
    return new ResponseBodyEmitter(STREAMING_TIMEOUT_VALUE) {
      @Override
      protected void extendResponse(RequestContext outputMessage) {
        // FIXME 不使用继承方式
        outputMessage.setContentType(mediaType.toString());
      }
    };
  }

  private abstract static class AbstractEmitterSubscriber implements Subscriber<Object>, Runnable {

    protected final ResponseBodyEmitter emitter;

    private final TaskExecutor taskExecutor;

    @Nullable
    private Subscription subscription;

    private final AtomicReference<Object> elementRef = new AtomicReference<>();

    @Nullable
    private Throwable error;

    private volatile boolean terminated;

    private final AtomicLong executing = new AtomicLong();

    private volatile boolean done;

    protected AbstractEmitterSubscriber(ResponseBodyEmitter emitter, TaskExecutor executor) {
      this.emitter = emitter;
      this.taskExecutor = executor;
    }

    public void connect(ReactiveAdapter adapter, Object returnValue) {
      Publisher<Object> publisher = adapter.toPublisher(returnValue);
      publisher.subscribe(this);
    }

    @Override
    public final void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
      emitter.onTimeout(() -> {
        if (log.isTraceEnabled()) {
          log.trace("Connection timeout for {}", emitter);
        }
        terminate();
        emitter.complete();
      });
      emitter.onError(emitter::completeWithError);
      subscription.request(1);
    }

    @Override
    public final void onNext(Object element) {
      elementRef.lazySet(element);
      trySchedule();
    }

    @Override
    public final void onError(Throwable ex) {
      this.error = ex;
      this.terminated = true;
      trySchedule();
    }

    @Override
    public final void onComplete() {
      this.terminated = true;
      trySchedule();
    }

    private void trySchedule() {
      if (executing.getAndIncrement() == 0) {
        schedule();
      }
    }

    private void schedule() {
      try {
        taskExecutor.execute(this);
      }
      catch (Throwable ex) {
        try {
          terminate();
        }
        finally {
          executing.decrementAndGet();
          elementRef.lazySet(null);
        }
      }
    }

    @Override
    public void run() {
      if (done) {
        elementRef.lazySet(null);
        return;
      }

      // Check terminal signal before processing element..
      boolean isTerminated = this.terminated;

      Object element = elementRef.get();
      if (element != null) {
        elementRef.lazySet(null);
        Assert.state(subscription != null, "No subscription");
        try {
          send(element);
          subscription.request(1);
        }
        catch (final Throwable ex) {
          if (log.isTraceEnabled()) {
            log.trace("Send for {} failed: {}", emitter, ex);
          }
          terminate();
          return;
        }
      }

      if (isTerminated) {
        this.done = true;
        Throwable ex = this.error;
        this.error = null;
        if (ex != null) {
          if (log.isTraceEnabled()) {
            log.trace("Publisher for {} failed: {}", emitter, ex);
          }
          emitter.completeWithError(ex);
        }
        else {
          if (log.isTraceEnabled()) {
            log.trace("Publisher for {} completed", emitter);
          }
          emitter.complete();
        }
        return;
      }

      if (this.executing.decrementAndGet() != 0) {
        schedule();
      }
    }

    protected abstract void send(Object element) throws IOException;

    private void terminate() {
      this.done = true;
      if (this.subscription != null) {
        this.subscription.cancel();
      }
    }
  }

  private static class SseEmitterSubscriber extends AbstractEmitterSubscriber {

    SseEmitterSubscriber(SseEmitter sseEmitter, TaskExecutor executor) {
      super(sseEmitter, executor);
    }

    @Override
    protected void send(Object element) throws IOException {
      if (element instanceof ServerSentEvent<?> event) {
        ((SseEmitter) emitter).send(adapt(event));
      }
      else {
        emitter.send(element, MediaType.APPLICATION_JSON);
      }
    }

    private SseEmitter.SseEventBuilder adapt(ServerSentEvent<?> sse) {
      SseEmitter.SseEventBuilder builder = SseEmitter.event();
      String id = sse.id();
      String event = sse.event();
      Duration retry = sse.retry();
      String comment = sse.comment();
      Object data = sse.data();
      if (id != null) {
        builder.id(id);
      }
      if (event != null) {
        builder.name(event);
      }
      if (data != null) {
        builder.data(data);
      }
      if (retry != null) {
        builder.reconnectTime(retry.toMillis());
      }
      if (comment != null) {
        builder.comment(comment);
      }
      return builder;
    }
  }

  private static class JsonEmitterSubscriber extends AbstractEmitterSubscriber {

    JsonEmitterSubscriber(ResponseBodyEmitter emitter, TaskExecutor executor) {
      super(emitter, executor);
    }

    @Override
    protected void send(Object element) throws IOException {
      emitter.send(element, MediaType.APPLICATION_JSON);
      emitter.send("\n", MediaType.TEXT_PLAIN);
    }

  }

  private static class TextEmitterSubscriber extends AbstractEmitterSubscriber {

    TextEmitterSubscriber(ResponseBodyEmitter emitter, TaskExecutor executor) {
      super(emitter, executor);
    }

    @Override
    protected void send(Object element) throws IOException {
      emitter.send(element, MediaType.TEXT_PLAIN);
    }

  }

  private static class DeferredResultSubscriber implements Subscriber<Object> {

    private final DeferredResult<Object> result;

    private final boolean multiValueSource;

    private final CollectedValuesList values;

    DeferredResultSubscriber(DeferredResult<Object> result, ReactiveAdapter adapter, ResolvableType elementType) {
      this.result = result;
      this.multiValueSource = adapter.isMultiValue();
      this.values = new CollectedValuesList(elementType);
    }

    public void connect(ReactiveAdapter adapter, Object returnValue) {
      Publisher<Object> publisher = adapter.toPublisher(returnValue);
      publisher.subscribe(this);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      this.result.onTimeout(subscription::cancel);
      subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Object element) {
      this.values.add(element);
    }

    @Override
    public void onError(Throwable ex) {
      this.result.setErrorResult(ex);
    }

    @Override
    public void onComplete() {
      if (this.values.size() > 1 || this.multiValueSource) {
        this.result.setResult(this.values);
      }
      else if (this.values.size() == 1) {
        this.result.setResult(this.values.get(0));
      }
      else {
        this.result.setResult(null);
      }
    }
  }

  /**
   * List of collect values where all elements are a specified type.
   */
  @SuppressWarnings("serial")
  static class CollectedValuesList extends ArrayList<Object> {

    private final ResolvableType elementType;

    CollectedValuesList(ResolvableType elementType) {
      this.elementType = elementType;
    }

    public ResolvableType getReturnType() {
      return ResolvableType.forClassWithGenerics(List.class, this.elementType);
    }
  }

}

