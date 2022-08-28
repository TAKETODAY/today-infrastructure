/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.http.codec.multipart;

import org.reactivestreams.Subscription;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferLimitException;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.FastByteArrayOutputStream;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Scheduler;
import reactor.util.context.Context;

/**
 * Subscribes to a token stream (i.e. the result of
 * {@link MultipartParser#parse(Flux, byte[], int, Charset)}, and produces a flux of {@link Part} objects.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
final class PartGenerator extends BaseSubscriber<MultipartParser.Token> {
  private static final Logger log = LoggerFactory.getLogger(PartGenerator.class);

  private final MonoSink<Part> sink;
  private final AtomicBoolean requestOutstanding = new AtomicBoolean();
  private final AtomicReference<State> state = new AtomicReference<>(new InitialState());

  private final boolean streaming;
  private final int maxInMemorySize;
  private final long maxDiskUsagePerPart;
  private final Mono<Path> fileStorageDirectory;
  private final Scheduler blockingOperationScheduler;

  private PartGenerator(MonoSink<Part> sink, int maxInMemorySize, long maxDiskUsagePerPart,
          boolean streaming, Mono<Path> fileStorageDirectory, Scheduler blockingOperationScheduler) {

    this.sink = sink;
    this.maxInMemorySize = maxInMemorySize;
    this.maxDiskUsagePerPart = maxDiskUsagePerPart;
    this.streaming = streaming;
    this.fileStorageDirectory = fileStorageDirectory;
    this.blockingOperationScheduler = blockingOperationScheduler;
  }

  /**
   * Creates parts from a given stream of tokens.
   */
  public static Mono<Part> createPart(Flux<MultipartParser.Token> tokens, int maxInMemorySize,
          long maxDiskUsagePerPart, boolean streaming, Mono<Path> fileStorageDirectory,
          Scheduler blockingOperationScheduler) {

    return Mono.create(sink -> {
      PartGenerator generator = new PartGenerator(sink, maxInMemorySize, maxDiskUsagePerPart, streaming,
              fileStorageDirectory, blockingOperationScheduler);

      sink.onCancel(generator);
      sink.onRequest(l -> generator.requestToken());
      tokens.subscribe(generator);
    });
  }

  @Override
  public Context currentContext() {
    return Context.of(this.sink.contextView());
  }

  @Override
  protected void hookOnSubscribe(Subscription subscription) {
    requestToken();
  }

  @Override
  protected void hookOnNext(MultipartParser.Token token) {
    this.requestOutstanding.set(false);
    State state = this.state.get();
    if (token instanceof MultipartParser.HeadersToken) {
      newPart(state, token.getHeaders());
    }
    else {
      state.applyBody(token.getBuffer());
    }
  }

  private void newPart(State currentState, HttpHeaders headers) {
    if (MultipartUtils.isFormField(headers)) {
      changeState(currentState, new FormFieldState(headers));
      requestToken();
    }
    else if (!this.streaming) {
      changeState(currentState, new InMemoryState(headers));
      requestToken();
    }
    else {
      Flux<DataBuffer> streamingContent = Flux.create(contentSink -> {
        State newState = new StreamingState(contentSink);
        if (changeState(currentState, newState)) {
          contentSink.onRequest(l -> requestToken());
          requestToken();
        }
      });
      emitPart(DefaultParts.part(headers, streamingContent));
    }
  }

  @Override
  protected void hookOnComplete() {
    this.state.get().onComplete();
  }

  @Override
  protected void hookOnError(Throwable throwable) {
    this.state.get().error(throwable);
    changeStateInternal(DisposedState.INSTANCE);
    this.sink.error(throwable);
  }

  @Override
  public void dispose() {
    changeStateInternal(DisposedState.INSTANCE);
    cancel();
  }

  boolean changeState(State oldState, State newState) {
    if (this.state.compareAndSet(oldState, newState)) {
      if (log.isDebugEnabled()) {
        log.trace("Changed state: {} -> {}", oldState, newState);
      }
      oldState.dispose();
      return true;
    }
    else {
      log.warn("Could not switch from {} to {}; current state: {}",
              oldState, newState, this.state.get());
      return false;
    }
  }

  private void changeStateInternal(State newState) {
    if (this.state.get() == DisposedState.INSTANCE) {
      return;
    }
    State oldState = this.state.getAndSet(newState);
    if (log.isDebugEnabled()) {
      log.trace("Changed state: {} -> {}", oldState, newState);
    }
    oldState.dispose();
  }

  void emitPart(Part part) {
    if (log.isDebugEnabled()) {
      log.trace("Emitting: {}", part);
    }
    sink.success(part);
  }

  void emitError(Throwable t) {
    cancel();
    this.sink.error(t);
  }

  void requestToken() {
    if (upstream() != null
            && this.requestOutstanding.compareAndSet(false, true)) {
      request(1);
    }
  }

  /**
   * Represents the internal state of the {@link PartGenerator} for
   * creating a single {@link Part}.
   * {@link State} instances are stateful, and created when a new
   * {@link MultipartParser.HeadersToken} is accepted (see
   * {@link #newPart(State, HttpHeaders)}.
   * The following rules determine which state the creator will have:
   * <ol>
   * <li>If the part is a {@linkplain MultipartUtils#isFormField(HttpHeaders) form field},
   * the creator will be in the {@link FormFieldState}.</li>
   * <li>If {@linkplain #streaming} is enabled, the creator will be in the
   * {@link StreamingState}.</li>
   * <li>Otherwise, the creator will initially be in the
   * {@link InMemoryState}, but will switch over to {@link CreateFileState}
   * when the part byte count exceeds {@link #maxInMemorySize},
   * then to {@link WritingFileState} (to write the memory contents),
   * and finally {@link IdleFileState}, which switches back to
   * {@link WritingFileState} when more body data comes in.</li>
   * </ol>
   */
  private abstract static class State {

    /**
     * Invoked when a {@link MultipartParser.BodyToken} is received.
     */
    abstract void applyBody(DataBuffer dataBuffer);

    /**
     * Invoked when all tokens for the part have been received.
     */
    void onComplete() {

    }

    /**
     * Invoked when an error has been received.
     */
    void error(Throwable throwable) { }

    /**
     * Cleans up any state.
     */
    void dispose() { }
  }

  /**
   * The initial state of the creator. Throws an exception for {@link #applyBody(DataBuffer)}.
   */
  private final class InitialState extends State {

    private InitialState() { }

    @Override
    public void applyBody(DataBuffer dataBuffer) {
      DataBufferUtils.release(dataBuffer);
      emitError(new IllegalStateException("Body token not expected"));
    }

    @Override
    public String toString() {
      return "INITIAL";
    }
  }

  /**
   * The creator state when a {@linkplain MultipartUtils#isFormField(HttpHeaders) form field} is received.
   * Stores all body buffers in memory (up until {@link #maxInMemorySize}).
   */
  private final class FormFieldState extends State {

    private final HttpHeaders headers;
    private final FastByteArrayOutputStream value = new FastByteArrayOutputStream();

    public FormFieldState(HttpHeaders headers) {
      this.headers = headers;
    }

    @Override
    public void applyBody(DataBuffer dataBuffer) {
      int size = this.value.size() + dataBuffer.readableByteCount();
      if (PartGenerator.this.maxInMemorySize == -1
              || size < PartGenerator.this.maxInMemorySize) {
        store(dataBuffer);
        requestToken();
      }
      else {
        DataBufferUtils.release(dataBuffer);
        emitError(new DataBufferLimitException(
                "Form field value exceeded the memory usage limit of " +
                        PartGenerator.this.maxInMemorySize + " bytes"));
      }
    }

    private void store(DataBuffer dataBuffer) {
      try {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        this.value.write(bytes);
      }
      catch (IOException ex) {
        emitError(ex);
      }
      finally {
        DataBufferUtils.release(dataBuffer);
      }
    }

    @Override
    public void onComplete() {
      byte[] bytes = this.value.toByteArrayUnsafe();
      String value = new String(bytes, MultipartUtils.charset(this.headers));
      emitPart(DefaultParts.formFieldPart(this.headers, value));
    }

    @Override
    public String toString() {
      return "FORM-FIELD";
    }

  }

  /**
   * The creator state when {@link #streaming} is {@code true} (and not
   * handling a form field). Relays all received buffers to a sink.
   */
  private final class StreamingState extends State {

    private final FluxSink<DataBuffer> bodySink;

    public StreamingState(FluxSink<DataBuffer> bodySink) {
      this.bodySink = bodySink;
    }

    @Override
    public void applyBody(DataBuffer dataBuffer) {
      if (!bodySink.isCancelled()) {
        bodySink.next(dataBuffer);
        if (bodySink.requestedFromDownstream() > 0) {
          requestToken();
        }
      }
      else {
        DataBufferUtils.release(dataBuffer);
        // even though the body sink is canceled, the (outer) part sink
        // might not be, so request another token
        requestToken();
      }
    }

    @Override
    public void onComplete() {
      if (!bodySink.isCancelled()) {
        bodySink.complete();
      }
    }

    @Override
    public void error(Throwable throwable) {
      if (!this.bodySink.isCancelled()) {
        this.bodySink.error(throwable);
      }
    }

    @Override
    public String toString() {
      return "STREAMING";
    }

  }

  /**
   * The creator state when {@link #streaming} is {@code false} (and not
   * handling a form field). Stores all received buffers in a queue.
   * If the byte count exceeds {@link #maxInMemorySize}, the creator state
   * is changed to {@link CreateFileState}, and eventually to
   * {@link CreateFileState}.
   */
  private final class InMemoryState extends State {

    private final HttpHeaders headers;
    private volatile boolean releaseOnDispose = true;
    private final AtomicLong byteCount = new AtomicLong();
    private final ConcurrentLinkedQueue<DataBuffer> content = new ConcurrentLinkedQueue<>();

    public InMemoryState(HttpHeaders headers) {
      this.headers = headers;
    }

    @Override
    public void applyBody(DataBuffer dataBuffer) {
      long prevCount = this.byteCount.get();
      long count = this.byteCount.addAndGet(dataBuffer.readableByteCount());
      if (PartGenerator.this.maxInMemorySize == -1
              || count <= PartGenerator.this.maxInMemorySize) {
        storeBuffer(dataBuffer);
      }
      else if (prevCount <= PartGenerator.this.maxInMemorySize) {
        switchToFile(dataBuffer, count);
      }
      else {
        DataBufferUtils.release(dataBuffer);
        emitError(new IllegalStateException("Body token not expected"));
      }
    }

    private void storeBuffer(DataBuffer dataBuffer) {
      this.content.add(dataBuffer);
      requestToken();
    }

    private void switchToFile(DataBuffer current, long byteCount) {
      ArrayList<DataBuffer> content = new ArrayList<>(this.content);
      content.add(current);
      this.releaseOnDispose = false;

      CreateFileState newState = new CreateFileState(this.headers, content, byteCount);
      if (changeState(this, newState)) {
        newState.createFile();
      }
      else {
        content.forEach(DataBufferUtils::release);
      }
    }

    @Override
    public void onComplete() {
      emitMemoryPart();
    }

    private void emitMemoryPart() {
      byte[] bytes = new byte[(int) byteCount.get()];
      int idx = 0;
      for (DataBuffer buffer : content) {
        int len = buffer.readableByteCount();
        buffer.read(bytes, idx, len);
        idx += len;
        DataBufferUtils.release(buffer);
      }
      content.clear();
      Flux<DataBuffer> content = Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
      emitPart(DefaultParts.part(headers, content));
    }

    @Override
    public void dispose() {
      if (releaseOnDispose) {
        content.forEach(DataBufferUtils::release);
      }
    }

    @Override
    public String toString() {
      return "IN-MEMORY";
    }

  }

  /**
   * The creator state when waiting for a temporary file to be created.
   * {@link InMemoryState} initially switches to this state when the byte
   * count exceeds {@link #maxInMemorySize}, and then calls
   * {@link #createFile()} to switch to {@link WritingFileState}.
   */
  private final class CreateFileState extends State {

    private final HttpHeaders headers;
    private final Collection<DataBuffer> content;

    private final long byteCount;
    private volatile boolean completed;
    private volatile boolean releaseOnDispose = true;

    public CreateFileState(HttpHeaders headers, Collection<DataBuffer> content, long byteCount) {
      this.headers = headers;
      this.content = content;
      this.byteCount = byteCount;
    }

    @Override
    public void applyBody(DataBuffer dataBuffer) {
      DataBufferUtils.release(dataBuffer);
      emitError(new IllegalStateException("Body token not expected"));
    }

    @Override
    public void onComplete() {
      this.completed = true;
    }

    public void createFile() {
      PartGenerator.this.fileStorageDirectory
              .map(this::createFileState)
              .subscribeOn(PartGenerator.this.blockingOperationScheduler)
              .subscribe(this::fileCreated, PartGenerator.this::emitError);
    }

    private WritingFileState createFileState(Path directory) {
      try {
        Path tempFile = Files.createTempFile(directory, null, ".multipart");
        if (log.isDebugEnabled()) {
          log.trace("Storing multipart data in file {}", tempFile);
        }
        WritableByteChannel channel = Files.newByteChannel(tempFile, StandardOpenOption.WRITE);
        return new WritingFileState(this, tempFile, channel);
      }
      catch (IOException ex) {
        throw new UncheckedIOException("Could not create temp file in " + directory, ex);
      }
    }

    private void fileCreated(WritingFileState newState) {
      this.releaseOnDispose = false;
      if (changeState(this, newState)) {
        newState.writeBuffers(this.content);
        if (this.completed) {
          newState.onComplete();
        }
      }
      else {
        MultipartUtils.closeChannel(newState.channel);
        MultipartUtils.deleteFile(newState.file);
        this.content.forEach(DataBufferUtils::release);
      }
    }

    @Override
    public void dispose() {
      if (this.releaseOnDispose) {
        this.content.forEach(DataBufferUtils::release);
      }
    }

    @Override
    public String toString() {
      return "CREATE-FILE";
    }

  }

  private final class IdleFileState extends State {

    private final Path file;
    private final HttpHeaders headers;
    private final AtomicLong byteCount;
    private final WritableByteChannel channel;
    private volatile boolean closeOnDispose = true;
    private volatile boolean deleteOnDispose = true;

    public IdleFileState(WritingFileState state) {
      this.headers = state.headers;
      this.file = state.file;
      this.channel = state.channel;
      this.byteCount = state.byteCount;
    }

    @Override
    public void applyBody(DataBuffer dataBuffer) {
      long count = this.byteCount.addAndGet(dataBuffer.readableByteCount());
      if (PartGenerator.this.maxDiskUsagePerPart == -1 || count <= PartGenerator.this.maxDiskUsagePerPart) {

        this.closeOnDispose = false;
        this.deleteOnDispose = false;

        WritingFileState newState = new WritingFileState(this);
        if (changeState(this, newState)) {
          newState.writeBuffer(dataBuffer);
        }
        else {
          MultipartUtils.closeChannel(this.channel);
          MultipartUtils.deleteFile(this.file);
          DataBufferUtils.release(dataBuffer);
        }
      }
      else {
        MultipartUtils.closeChannel(this.channel);
        MultipartUtils.deleteFile(this.file);
        DataBufferUtils.release(dataBuffer);
        emitError(new DataBufferLimitException(
                "Part exceeded the disk usage limit of " + PartGenerator.this.maxDiskUsagePerPart +
                        " bytes"));
      }
    }

    @Override
    public void onComplete() {
      MultipartUtils.closeChannel(this.channel);
      this.deleteOnDispose = false;
      emitPart(DefaultParts.part(this.headers, this.file, PartGenerator.this.blockingOperationScheduler));
    }

    @Override
    public void dispose() {
      if (this.closeOnDispose) {
        MultipartUtils.closeChannel(this.channel);
      }
      if (this.deleteOnDispose) {
        MultipartUtils.deleteFile(this.file);
      }
    }

    @Override
    public String toString() {
      return "IDLE-FILE";
    }

  }

  private final class WritingFileState extends State {

    private final Path file;
    private final HttpHeaders headers;
    private final AtomicLong byteCount;
    private final WritableByteChannel channel;

    private volatile boolean completed;
    private volatile boolean disposed;

    public WritingFileState(CreateFileState state, Path file, WritableByteChannel channel) {
      this.headers = state.headers;
      this.file = file;
      this.channel = channel;
      this.byteCount = new AtomicLong(state.byteCount);
    }

    public WritingFileState(IdleFileState state) {
      this.headers = state.headers;
      this.file = state.file;
      this.channel = state.channel;
      this.byteCount = state.byteCount;
    }

    @Override
    public void applyBody(DataBuffer dataBuffer) {
      DataBufferUtils.release(dataBuffer);
      emitError(new IllegalStateException("Body token not expected"));
    }

    @Override
    public void onComplete() {
      this.completed = true;
    }

    public void writeBuffer(DataBuffer dataBuffer) {
      Mono.just(dataBuffer)
              .flatMap(this::writeInternal)
              .subscribeOn(PartGenerator.this.blockingOperationScheduler)
              .subscribe(null,
                      PartGenerator.this::emitError,
                      this::writeComplete);
    }

    public void writeBuffers(Iterable<DataBuffer> dataBuffers) {
      Flux.fromIterable(dataBuffers)
              .concatMap(this::writeInternal)
              .then()
              .subscribeOn(PartGenerator.this.blockingOperationScheduler)
              .subscribe(null,
                      PartGenerator.this::emitError,
                      this::writeComplete);
    }

    private void writeComplete() {
      IdleFileState newState = new IdleFileState(this);
      if (this.completed) {
        newState.onComplete();
      }
      else if (this.disposed) {
        newState.dispose();
      }
      else if (changeState(this, newState)) {
        requestToken();
      }
      else {
        MultipartUtils.closeChannel(this.channel);
        MultipartUtils.deleteFile(this.file);
      }
    }

    @SuppressWarnings("BlockingMethodInNonBlockingContext")
    private Mono<Void> writeInternal(DataBuffer dataBuffer) {
      try {
        ByteBuffer byteBuffer = dataBuffer.toByteBuffer();
        while (byteBuffer.hasRemaining()) {
          this.channel.write(byteBuffer);
        }
        return Mono.empty();
      }
      catch (IOException ex) {
        MultipartUtils.closeChannel(this.channel);
        MultipartUtils.deleteFile(this.file);
        return Mono.error(ex);
      }
      finally {
        DataBufferUtils.release(dataBuffer);
      }
    }

    @Override
    public void dispose() {
      this.disposed = true;
    }

    @Override
    public String toString() {
      return "WRITE-FILE";
    }
  }

  private static final class DisposedState extends State {
    public static final DisposedState INSTANCE = new DisposedState();

    private DisposedState() { }

    @Override
    public void applyBody(DataBuffer dataBuffer) {
      DataBufferUtils.release(dataBuffer);
    }

    @Override
    public String toString() {
      return "DISPOSED";
    }

  }

}
