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
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.SyncTaskExecutor;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.codec.ServerSentEvent;
import cn.taketoday.http.server.ServerHttpResponse;
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
import cn.taketoday.web.context.async.DeferredResult;
import cn.taketoday.web.context.async.WebAsyncUtils;

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

  private static final long STREAMING_TIMEOUT_VALUE = -1;

  private static final MediaType[] JSON_STREAMING_MEDIA_TYPES = {
          MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_STREAM_JSON
  };

  private static final Logger log = LoggerFactory.getLogger(ReactiveTypeHandler.class);

  private final ReactiveAdapterRegistry adapterRegistry;

  private final TaskExecutor taskExecutor;

  private final ContentNegotiationManager contentNegotiationManager;

  private boolean taskExecutorWarning;

  public ReactiveTypeHandler() {
    this(ReactiveAdapterRegistry.getSharedInstance(), new SyncTaskExecutor(), new ContentNegotiationManager());
  }

  public ReactiveTypeHandler(ContentNegotiationManager manager) {
    this(ReactiveAdapterRegistry.getSharedInstance(), new SyncTaskExecutor(), manager);
  }

  public ReactiveTypeHandler(
          ReactiveAdapterRegistry registry, TaskExecutor executor, ContentNegotiationManager manager) {
    Assert.notNull(registry, "ReactiveAdapterRegistry is required");
    Assert.notNull(executor, "TaskExecutor is required");
    Assert.notNull(manager, "ContentNegotiationManager is required");
    this.adapterRegistry = registry;
    this.taskExecutor = executor;
    this.contentNegotiationManager = manager;
    this.taskExecutorWarning =
            executor instanceof SimpleAsyncTaskExecutor || executor instanceof SyncTaskExecutor;
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
        logExecutorWarning(returnType);
        SseEmitter emitter = new SseEmitter(STREAMING_TIMEOUT_VALUE);
        new SseEmitterSubscriber(emitter, taskExecutor).connect(adapter, returnValue);
        return emitter;
      }
      if (CharSequence.class.isAssignableFrom(elementClass)) {
        logExecutorWarning(returnType);
        Optional<MediaType> mediaType = mediaTypes.stream()
                .filter(MimeType::isConcrete)
                .findFirst();
        ResponseBodyEmitter emitter = getEmitter(mediaType.orElse(MediaType.TEXT_PLAIN));
        new TextEmitterSubscriber(emitter, taskExecutor).connect(adapter, returnValue);
        return emitter;
      }
      for (MediaType type : mediaTypes) {
        for (MediaType streamingType : JSON_STREAMING_MEDIA_TYPES) {
          if (streamingType.includes(type)) {
            logExecutorWarning(returnType);
            ResponseBodyEmitter emitter = getEmitter(streamingType);
            new JsonEmitterSubscriber(emitter, taskExecutor).connect(adapter, returnValue);
            return emitter;
          }
        }
      }
    }

    // Not streaming...
    DeferredResult<Object> result = new DeferredResult<>();
    new DeferredResultSubscriber(result, adapter, elementType)
            .connect(adapter, returnValue);

    WebAsyncUtils.getAsyncManager(request)
            .startDeferredResultProcessing(result);

    return null;
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
      protected void extendResponse(ServerHttpResponse outputMessage) {
        outputMessage.getHeaders().setContentType(mediaType);
      }
    };
  }

  @SuppressWarnings("ConstantConditions")
  private void logExecutorWarning(MethodParameter returnType) {
    if (this.taskExecutorWarning && log.isWarnEnabled()) {
      synchronized(this) {
        if (this.taskExecutorWarning) {
          String executorTypeName = this.taskExecutor.getClass().getSimpleName();
          log.warn("""
                          !!!
                          Streaming through a reactive type requires an Executor to write to the response.
                          Please, configure a TaskExecutor in the MVC config under "async support".
                          The {} currently in use is not suitable under load.
                          -------------------------------
                          Controller:\t{}
                          Method:\t\t{}
                          Returning:\t{}
                          !!!""",
                  executorTypeName,
                  returnType.getContainingClass().getName(),
                  returnType.getMethod().getName(),
                  ResolvableType.forMethodParameter(returnType));
          this.taskExecutorWarning = false;
        }
      }
    }
  }

  private abstract static class AbstractEmitterSubscriber implements Subscriber<Object>, Runnable {

    private final ResponseBodyEmitter emitter;

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

    protected ResponseBodyEmitter getEmitter() {
      return this.emitter;
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
      this.elementRef.lazySet(element);
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
      if (this.executing.getAndIncrement() == 0) {
        schedule();
      }
    }

    private void schedule() {
      try {
        this.taskExecutor.execute(this);
      }
      catch (Throwable ex) {
        try {
          terminate();
        }
        finally {
          this.executing.decrementAndGet();
          this.elementRef.lazySet(null);
        }
      }
    }

    @Override
    public void run() {
      if (this.done) {
        this.elementRef.lazySet(null);
        return;
      }

      // Check terminal signal before processing element..
      boolean isTerminated = this.terminated;

      Object element = this.elementRef.get();
      if (element != null) {
        this.elementRef.lazySet(null);
        Assert.state(this.subscription != null, "No subscription");
        try {
          send(element);
          this.subscription.request(1);
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
        ((SseEmitter) getEmitter()).send(adapt(event));
      }
      else {
        getEmitter().send(element, MediaType.APPLICATION_JSON);
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
      getEmitter().send(element, MediaType.APPLICATION_JSON);
      getEmitter().send("\n", MediaType.TEXT_PLAIN);
    }
  }

  private static class TextEmitterSubscriber extends AbstractEmitterSubscriber {

    TextEmitterSubscriber(ResponseBodyEmitter emitter, TaskExecutor executor) {
      super(emitter, executor);
    }

    @Override
    protected void send(Object element) throws IOException {
      getEmitter().send(element, MediaType.TEXT_PLAIN);
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
      return ResolvableType.fromClassWithGenerics(List.class, this.elementType);
    }
  }

}

