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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.core.codec.DecodingException;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferLimitException;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.FastByteArrayOutputStream;
import cn.taketoday.http.MediaType;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
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
  private static final Logger logger = LoggerFactory.getLogger(PartGenerator.class);

  private final FluxSink<Part> sink;
  private final AtomicInteger partCount = new AtomicInteger();
  private final AtomicBoolean requestOutstanding = new AtomicBoolean();
  private final AtomicReference<State> state = new AtomicReference<>(new InitialState());

  private final int maxParts;
  private final boolean streaming;
  private final int maxInMemorySize;
  private final long maxDiskUsagePerPart;
  private final Mono<Path> fileStorageDirectory;
  private final Scheduler blockingOperationScheduler;

  private PartGenerator(
          FluxSink<Part> sink, int maxParts, int maxInMemorySize, long maxDiskUsagePerPart,
          boolean streaming, Mono<Path> fileStorageDirectory, Scheduler blockingOperationScheduler) {

    this.sink = sink;
    this.maxParts = maxParts;
    this.streaming = streaming;
    this.maxInMemorySize = maxInMemorySize;
    this.maxDiskUsagePerPart = maxDiskUsagePerPart;
    this.fileStorageDirectory = fileStorageDirectory;
    this.blockingOperationScheduler = blockingOperationScheduler;
  }

  /**
   * Creates parts from a given stream of tokens.
   */
  public static Flux<Part> createParts(
          Flux<MultipartParser.Token> tokens, int maxParts,
          int maxInMemorySize, long maxDiskUsagePerPart, boolean streaming,
          Mono<Path> fileStorageDirectory, Scheduler blockingOperationScheduler) {

    return Flux.create(sink -> {
      PartGenerator generator = new PartGenerator(
              sink, maxParts, maxInMemorySize, maxDiskUsagePerPart, streaming,
              fileStorageDirectory, blockingOperationScheduler);

      sink.onCancel(generator::onSinkCancel);
      sink.onRequest(l -> generator.requestToken());
      tokens.subscribe(generator);
    });
  }

  @Override
  public Context currentContext() {
    return this.sink.currentContext();
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
      // finish previous part
      state.partComplete(false);
      if (tooManyParts()) {
        return;
      }
      newPart(state, token.getHeaders());
    }
    else {
      state.applyBody(token.getBuffer());
    }
  }

  private void newPart(State currentState, HttpHeaders headers) {
    if (isFormField(headers)) {
      changeStateInternal(new FormFieldState(headers));
      requestToken();
    }
    else if (!this.streaming) {
      changeStateInternal(new InMemoryState(headers));
      requestToken();
    }
    else {
      emitPart(DefaultParts.part(headers, Flux.create(contentSink -> {
        State newState = new StreamingState(contentSink);
        if (changeState(currentState, newState)) {
          contentSink.onRequest(l -> requestToken());
          requestToken();
        }
      })));
    }
  }

  @Override
  protected void hookOnComplete() {
    this.state.get().partComplete(true);
  }

  @Override
  protected void hookOnError(Throwable throwable) {
    this.state.get().error(throwable);
    changeStateInternal(DisposedState.INSTANCE);
    this.sink.error(throwable);
  }

  private void onSinkCancel() {
    changeStateInternal(DisposedState.INSTANCE);
    cancel();
  }

  boolean changeState(State oldState, State newState) {
    if (this.state.compareAndSet(oldState, newState)) {
      if (logger.isTraceEnabled()) {
        logger.trace("Changed state: {} -> {}", oldState, newState);
      }
      oldState.dispose();
      return true;
    }
    else {
      logger.warn("Could not switch from {} to {}; current state: {}",
                  oldState, newState, this.state.get());
      return false;
    }
  }

  private void changeStateInternal(State newState) {
    if (this.state.get() == DisposedState.INSTANCE) {
      return;
    }
    State oldState = this.state.getAndSet(newState);
    if (logger.isTraceEnabled()) {
      logger.trace("Changed state: {} -> {}", oldState, newState);
    }
    oldState.dispose();
  }

  void emitPart(Part part) {
    if (logger.isTraceEnabled()) {
      logger.trace("Emitting: {}", part);
    }
    this.sink.next(part);
  }

  void emitComplete() {
    this.sink.complete();
  }

  void emitError(Throwable t) {
    cancel();
    this.sink.error(t);
  }

  void requestToken() {
    if (upstream() != null
            && !this.sink.isCancelled()
            && this.sink.requestedFromDownstream() > 0
            && this.requestOutstanding.compareAndSet(false, true)) {
      request(1);
    }
  }

  private boolean tooManyParts() {
    int count = this.partCount.incrementAndGet();
    if (this.maxParts > 0 && count > this.maxParts) {
      emitError(new DecodingException("Too many parts (" + count + "/" + this.maxParts + " allowed)"));
      return true;
    }
    else {
      return false;
    }
  }

  private static boolean isFormField(HttpHeaders headers) {
    MediaType contentType = headers.getContentType();
    return (contentType == null || MediaType.TEXT_PLAIN.equalsTypeAndSubtype(contentType))
            && headers.getContentDisposition().getFilename() == null;
  }

  /**
   * Represents the internal state of the {@link PartGenerator} for
   * creating a single {@link Part}.
   * {@link State} instances are stateful, and created when a new
   * {@link MultipartParser.HeadersToken} is accepted (see
   * {@link #newPart(State, HttpHeaders)}.
   * The following rules determine which state the creator will have:
   * <ol>
   * <li>If the part is a {@linkplain #isFormField(HttpHeaders) form field},
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
     *
     * @param finalPart {@code true} if this was the last part (and
     * {@link #emitComplete()} should be called; {@code false} otherwise
     */
    abstract void partComplete(boolean finalPart);

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
    public void partComplete(boolean finalPart) {
      if (finalPart) {
        emitComplete();
      }
    }

    @Override
    public String toString() {
      return "INITIAL";
    }
  }

  /**
   * The creator state when a {@linkplain #isFormField(HttpHeaders) form field} is received.
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
    public void partComplete(boolean finalPart) {
      byte[] bytes = this.value.toByteArrayUnsafe();
      String value = new String(bytes, MultipartUtils.charset(this.headers));
      emitPart(DefaultParts.formFieldPart(this.headers, value));
      if (finalPart) {
        emitComplete();
      }
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
      if (!this.bodySink.isCancelled()) {
        this.bodySink.next(dataBuffer);
        if (this.bodySink.requestedFromDownstream() > 0) {
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
    public void partComplete(boolean finalPart) {
      if (!this.bodySink.isCancelled()) {
        this.bodySink.complete();
      }
      if (finalPart) {
        emitComplete();
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
    public void partComplete(boolean finalPart) {
      emitMemoryPart();
      if (finalPart) {
        emitComplete();
      }
    }

    private void emitMemoryPart() {
      byte[] bytes = new byte[(int) this.byteCount.get()];
      int idx = 0;
      for (DataBuffer buffer : this.content) {
        int len = buffer.readableByteCount();
        buffer.read(bytes, idx, len);
        idx += len;
        DataBufferUtils.release(buffer);
      }
      this.content.clear();
      Flux<DataBuffer> content = Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
      emitPart(DefaultParts.part(this.headers, content));
    }

    @Override
    public void dispose() {
      if (this.releaseOnDispose) {
        this.content.forEach(DataBufferUtils::release);
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
    private volatile boolean finalPart;
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
    public void partComplete(boolean finalPart) {
      this.completed = true;
      this.finalPart = finalPart;
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
        if (logger.isTraceEnabled()) {
          logger.trace("Storing multipart data in file {}", tempFile);
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
          newState.partComplete(this.finalPart);
        }
      }
      else {
        MultipartUtils.closeChannel(newState.channel);
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
        WritingFileState newState = new WritingFileState(this);
        if (changeState(this, newState)) {
          newState.writeBuffer(dataBuffer);
        }
        else {
          MultipartUtils.closeChannel(this.channel);
          DataBufferUtils.release(dataBuffer);
        }
      }
      else {
        DataBufferUtils.release(dataBuffer);
        emitError(new DataBufferLimitException(
                "Part exceeded the disk usage limit of " + PartGenerator.this.maxDiskUsagePerPart +
                        " bytes"));
      }
    }

    @Override
    public void partComplete(boolean finalPart) {
      MultipartUtils.closeChannel(this.channel);
      Flux<DataBuffer> content = partContent();
      emitPart(DefaultParts.part(this.headers, content));
      if (finalPart) {
        emitComplete();
      }
    }

    private Flux<DataBuffer> partContent() {
      return DataBufferUtils
              .readByteChannel(
                      () -> Files.newByteChannel(this.file, StandardOpenOption.READ),
                      DefaultDataBufferFactory.sharedInstance, 1024)
              .subscribeOn(PartGenerator.this.blockingOperationScheduler);
    }

    @Override
    public void dispose() {
      if (this.closeOnDispose) {
        MultipartUtils.closeChannel(this.channel);
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
    private volatile boolean finalPart;

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
    public void partComplete(boolean finalPart) {
      this.completed = true;
      this.finalPart = finalPart;
    }

    public void writeBuffer(DataBuffer dataBuffer) {
      Mono.just(dataBuffer)
              .flatMap(this::writeInternal)
              .subscribeOn(PartGenerator.this.blockingOperationScheduler)
              .subscribe(null, PartGenerator.this::emitError, this::writeComplete);
    }

    public void writeBuffers(Iterable<DataBuffer> dataBuffers) {
      Flux.fromIterable(dataBuffers)
              .concatMap(this::writeInternal)
              .then()
              .subscribeOn(PartGenerator.this.blockingOperationScheduler)
              .subscribe(null, PartGenerator.this::emitError, this::writeComplete);
    }

    private void writeComplete() {
      IdleFileState newState = new IdleFileState(this);
      if (this.completed) {
        newState.partComplete(this.finalPart);
      }
      else if (changeState(this, newState)) {
        requestToken();
      }
      else {
        MultipartUtils.closeChannel(this.channel);
      }
    }

    @SuppressWarnings("BlockingMethodInNonBlockingContext")
    private Mono<Void> writeInternal(DataBuffer dataBuffer) {
      try {
        ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
        while (byteBuffer.hasRemaining()) {
          this.channel.write(byteBuffer);
        }
        return Mono.empty();
      }
      catch (IOException ex) {
        return Mono.error(ex);
      }
      finally {
        DataBufferUtils.release(dataBuffer);
      }
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
    public void partComplete(boolean finalPart) { }

    @Override
    public String toString() {
      return "DISPOSED";
    }

  }

}
