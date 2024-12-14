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

package infra.http.codec.multipart;

import org.reactivestreams.Subscription;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import infra.core.codec.DecodingException;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferLimitException;
import infra.core.io.buffer.DataBufferUtils;
import infra.http.HttpHeaders;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.util.context.Context;

/**
 * Subscribes to a buffer stream and produces a flux of {@link Token} instances.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class MultipartParser extends BaseSubscriber<DataBuffer> {

  private static final Logger log = LoggerFactory.getLogger(MultipartParser.class);

  private static final byte CR = '\r';
  private static final byte LF = '\n';

  private static final byte[] CR_LF = { CR, LF };
  private static final byte[] DOUBLE_CR_LF = { CR, LF, CR, LF };
  private static final byte HYPHEN = '-';
  private static final byte[] TWO_HYPHENS = { HYPHEN, HYPHEN };
  private static final String HEADER_ENTRY_SEPARATOR = "\\r\\n";

  private final FluxSink<Token> sink;

  private final AtomicReference<State> state;

  private final byte[] boundary;

  private final int maxHeadersSize;

  private final Charset headersCharset;

  private final AtomicBoolean requestOutstanding = new AtomicBoolean();

  private MultipartParser(FluxSink<Token> sink, byte[] boundary, int maxHeadersSize, Charset headersCharset) {
    this.sink = sink;
    this.boundary = boundary;
    this.maxHeadersSize = maxHeadersSize;
    this.headersCharset = headersCharset;
    this.state = new AtomicReference<>(new PreambleState());
  }

  /**
   * Parses the given stream of {@link DataBuffer} objects into a stream of {@link Token} objects.
   *
   * @param buffers the input buffers
   * @param boundary the multipart boundary, as found in the {@code Content-Type} header
   * @param maxHeadersSize the maximum buffered header size
   * @param headersCharset the charset to use for decoding headers
   * @return a stream of parsed tokens
   */
  public static Flux<Token> parse(Flux<DataBuffer> buffers, byte[] boundary, int maxHeadersSize, Charset headersCharset) {
    return Flux.create(sink -> {
      MultipartParser parser = new MultipartParser(sink, boundary, maxHeadersSize, headersCharset);
      sink.onCancel(parser::onSinkCancel);
      sink.onRequest(n -> parser.requestBuffer());
      buffers.subscribe(parser);
    });
  }

  @Override
  public Context currentContext() {
    return Context.of(sink.contextView());
  }

  @Override
  protected void hookOnSubscribe(Subscription subscription) {
    requestBuffer();
  }

  @Override
  protected void hookOnNext(DataBuffer value) {
    requestOutstanding.set(false);
    state.get().onNext(value);
  }

  @Override
  protected void hookOnComplete() {
    state.get().onComplete();
  }

  @Override
  protected void hookOnError(Throwable throwable) {
    State oldState = state.getAndSet(DisposedState.INSTANCE);
    oldState.dispose();
    sink.error(throwable);
  }

  private void onSinkCancel() {
    State oldState = state.getAndSet(DisposedState.INSTANCE);
    oldState.dispose();
    cancel();
  }

  boolean changeState(State oldState, State newState, @Nullable DataBuffer remainder) {
    if (state.compareAndSet(oldState, newState)) {
      if (log.isTraceEnabled()) {
        log.trace("Changed state: {} -> {}", oldState, newState);
      }
      oldState.dispose();
      if (remainder != null) {
        if (remainder.readableBytes() > 0) {
          newState.onNext(remainder);
        }
        else {
          remainder.release();
          requestBuffer();
        }
      }
      return true;
    }
    else if (remainder != null) {
      remainder.release();
    }
    return false;
  }

  void emitHeaders(HttpHeaders headers) {
    if (log.isTraceEnabled()) {
      log.trace("Emitting headers: {}", headers);
    }
    sink.next(new HeadersToken(headers));
  }

  void emitBody(DataBuffer buffer, boolean last) {
    if (log.isTraceEnabled()) {
      log.trace("Emitting body: {}", buffer);
    }
    sink.next(new BodyToken(buffer, last));
  }

  void emitError(Throwable t) {
    cancel();
    sink.error(t);
  }

  void emitComplete() {
    cancel();
    sink.complete();
  }

  private void requestBuffer() {
    if (upstream() != null
            && !sink.isCancelled()
            && sink.requestedFromDownstream() > 0
            && requestOutstanding.compareAndSet(false, true)) {
      request(1);
    }
  }

  /**
   * Represents the output of {@link #parse(Flux, byte[], int, Charset)}.
   */
  public abstract static class Token {

    public abstract DataBuffer getBuffer();

    public abstract HttpHeaders getHeaders();

    public abstract boolean isLast();
  }

  /**
   * Represents a token that contains {@link HttpHeaders}.
   */
  public final static class HeadersToken extends Token {

    private final HttpHeaders headers;

    public HeadersToken(HttpHeaders headers) {
      this.headers = headers;
    }

    @Override
    public HttpHeaders getHeaders() {
      return headers;
    }

    @Override
    public DataBuffer getBuffer() {
      throw new IllegalStateException();
    }

    @Override
    public boolean isLast() {
      return false;
    }
  }

  /**
   * Represents a token that contains {@link DataBuffer}.
   */
  public final static class BodyToken extends Token {

    private final boolean last;

    private final DataBuffer buffer;

    public BodyToken(DataBuffer buffer, boolean last) {
      this.buffer = buffer;
      this.last = last;
    }

    @Override
    public HttpHeaders getHeaders() {
      throw new IllegalStateException();
    }

    @Override
    public DataBuffer getBuffer() {
      return buffer;
    }

    @Override
    public boolean isLast() {
      return last;
    }

  }

  /**
   * Represents the internal state of the {@link MultipartParser}.
   * The flow for well-formed multipart messages is shown below:
   * <p><pre>
   *     PREAMBLE
   *         |
   *         v
   *  +-->HEADERS--->DISPOSED
   *  |      |
   *  |      v
   *  +----BODY
   *  </pre>
   * For malformed messages the flow ends in DISPOSED, and also when the
   * sink is {@linkplain #onSinkCancel() cancelled}.
   */
  private abstract static class State {

    abstract void onNext(DataBuffer buf);

    abstract void onComplete();

    void dispose() { }
  }

  /**
   * The initial state of the parser. Looks for the first boundary of the
   * multipart message. Note that the first boundary is not necessarily
   * prefixed with {@code CR LF}; only the prefix {@code --} is required.
   */
  private final class PreambleState extends State {

    private final DataBufferUtils.Matcher firstBoundary;

    public PreambleState() {
      this.firstBoundary = DataBufferUtils.matcher(
              MultipartUtils.concat(TWO_HYPHENS, MultipartParser.this.boundary));
    }

    /**
     * Looks for the first boundary in the given buffer. If found, changes
     * state to {@link HeadersState}, and passes on the remainder of the
     * buffer.
     */
    @Override
    public void onNext(DataBuffer buf) {
      int endIdx = this.firstBoundary.match(buf);
      if (endIdx != -1) {
        if (log.isTraceEnabled()) {
          log.trace("First boundary found @{} in {}", endIdx, buf);
        }
        DataBuffer preambleBuffer = buf.split(endIdx + 1);
        preambleBuffer.release();

        changeState(this, new HeadersState(), buf);
      }
      else {
        buf.release();
        requestBuffer();
      }
    }

    @Override
    public void onComplete() {
      if (changeState(this, DisposedState.INSTANCE, null)) {
        emitError(new DecodingException("Could not find first boundary"));
      }
    }

    @Override
    public String toString() {
      return "PREAMBLE";
    }

  }

  /**
   * The state of the parser dealing with part headers. Parses header
   * buffers into a {@link HttpHeaders} instance, making sure that
   * the amount does not exceed {@link #maxHeadersSize}.
   */
  private final class HeadersState extends State {

    private final DataBufferUtils.Matcher endHeaders = DataBufferUtils.matcher(DOUBLE_CR_LF);

    private final AtomicInteger byteCount = new AtomicInteger();

    private final ArrayList<DataBuffer> buffers = new ArrayList<>();

    /**
     * First checks whether the multipart boundary leading to this state
     * was the final boundary, or whether {@link #maxHeadersSize} is
     * exceeded. Then looks for the header-body boundary
     * ({@code CR LF CR LF}) in the given buffer. If found, convert
     * all buffers collected so far into a {@link HttpHeaders} object
     * and changes to {@link BodyState}, passing the remainder of the
     * buffer. If the boundary is not found, the buffer is collected.
     */
    @Override
    public void onNext(DataBuffer buf) {
      if (isLastBoundary(buf)) {
        if (log.isTraceEnabled()) {
          log.trace("Last boundary found in {}", buf);
        }

        if (changeState(this, DisposedState.INSTANCE, buf)) {
          emitComplete();
        }
        return;
      }
      int endIdx = endHeaders.match(buf);
      if (endIdx != -1) {
        if (log.isTraceEnabled()) {
          log.trace("End of headers found @{} in {}", endIdx, buf);
        }
        long count = this.byteCount.addAndGet(endIdx);
        if (belowMaxHeaderSize(count)) {
          DataBuffer headerBuf = buf.split(endIdx + 1);
          this.buffers.add(headerBuf);
          emitHeaders(parseHeaders());

          changeState(this, new BodyState(), buf);
        }
      }
      else {
        long count = this.byteCount.addAndGet(buf.readableBytes());
        if (belowMaxHeaderSize(count)) {
          this.buffers.add(buf);
          requestBuffer();
        }
      }
    }

    /**
     * If the given buffer is the first buffer, check whether it starts with {@code --}.
     * If it is the second buffer, check whether it makes up {@code --} together with the first buffer.
     */
    private boolean isLastBoundary(DataBuffer buf) {
      return (
              buffers.isEmpty()
                      && buf.readableBytes() >= 2
                      && buf.getByte(0) == HYPHEN && buf.getByte(1) == HYPHEN
      ) || (
              buffers.size() == 1
                      && buffers.get(0).readableBytes() == 1
                      && buffers.get(0).getByte(0) == HYPHEN
                      && buf.readableBytes() >= 1
                      && buf.getByte(0) == HYPHEN
      );
    }

    /**
     * Checks whether the given {@code count} is below or equal to {@link #maxHeadersSize}
     * and emits a {@link DataBufferLimitException} if not.
     */
    private boolean belowMaxHeaderSize(long count) {
      if (count <= MultipartParser.this.maxHeadersSize) {
        return true;
      }
      else {
        emitError(new DataBufferLimitException("Part headers exceeded the memory usage limit of " +
                MultipartParser.this.maxHeadersSize + " bytes"));
        return false;
      }
    }

    /**
     * Parses the list of buffers into a {@link HttpHeaders} instance.
     * Converts the joined buffers into a string using ISO=8859-1, and parses
     * that string into key and values.
     */
    private HttpHeaders parseHeaders() {
      if (buffers.isEmpty()) {
        return HttpHeaders.empty();
      }
      DataBuffer joined = buffers.get(0).factory().join(buffers);
      buffers.clear();
      String string = joined.toString(MultipartParser.this.headersCharset);
      joined.release();
      HttpHeaders result = HttpHeaders.forWritable();
      for (String line : string.split(HEADER_ENTRY_SEPARATOR)) {
        int idx = line.indexOf(':');
        if (idx != -1) {
          String name = line.substring(0, idx);
          String value = line.substring(idx + 1);
          while (value.startsWith(" ")) {
            value = value.substring(1);
          }
          result.add(name, value);
        }
      }
      return result;
    }

    @Override
    public void onComplete() {
      if (changeState(this, DisposedState.INSTANCE, null)) {
        emitError(new DecodingException("Could not find end of headers"));
      }
    }

    @Override
    public void dispose() {
      for (DataBuffer buffer : buffers) {
        buffer.release();
      }
    }

    @Override
    public String toString() {
      return "HEADERS";
    }

  }

  /**
   * The state of the parser dealing with multipart bodies. Relays
   * data buffers as {@link BodyToken} until the boundary is found (or
   * rather: {@code CR LF - - boundary}.
   */
  private final class BodyState extends State {

    private final DataBufferUtils.Matcher boundary;

    private final int boundaryLength;

    private final Deque<DataBuffer> queue = new ConcurrentLinkedDeque<>();

    public BodyState() {
      byte[] delimiter = MultipartUtils.concat(CR_LF, TWO_HYPHENS, MultipartParser.this.boundary);
      this.boundary = DataBufferUtils.matcher(delimiter);
      this.boundaryLength = delimiter.length;
    }

    /**
     * Checks whether the (end of the) needle {@code CR LF - - boundary}
     * can be found in {@code buffer}. If found, the needle can overflow into the
     * previous buffer, so we calculate the length and slice the current
     * and previous buffers accordingly. We then change to {@link HeadersState}
     * and pass on the remainder of {@code buffer}. If the needle is not found, we
     * enqueue {@code buffer}.
     */
    @Override
    public void onNext(DataBuffer buffer) {
      int endIdx = this.boundary.match(buffer);
      if (endIdx != -1) {
        if (log.isTraceEnabled()) {
          log.trace("Boundary found @{} in {}", endIdx, buffer);
        }
        DataBuffer boundaryBuffer = buffer.split(endIdx + 1);
        int len = endIdx - this.boundaryLength + 1 - boundaryBuffer.readPosition();
        if (len > 0) {
          // whole boundary in buffer.
          // slice off the body part, and flush
          DataBuffer body = boundaryBuffer.split(len);
          boundaryBuffer.release();
          enqueue(body);
          flush();
        }
        else if (len < 0) {
          // boundary spans multiple buffers, and we've just found the end
          // iterate over buffers in reverse order
          boundaryBuffer.release();
          DataBuffer prev;
          while ((prev = this.queue.pollLast()) != null) {
            int prevByteCount = prev.readableBytes();
            int prevLen = prevByteCount + len;
            if (prevLen >= 0) {
              // slice body part of previous buffer, and flush it
              DataBuffer body = prev.split(prevLen + prev.readPosition());
              prev.release();
              enqueue(body);
              flush();
              break;
            }
            else {
              // previous buffer only contains boundary bytes
              prev.release();
              len += prevByteCount;
            }
          }
        }
        else /* if (len == 0) */ {
          // buffer starts with complete delimiter, flush out the previous buffers
          boundaryBuffer.release();
          flush();
        }

        changeState(this, new HeadersState(), buffer);
      }
      else {
        enqueue(buffer);
        requestBuffer();
      }
    }

    /**
     * Store the given buffer. Emit buffers that cannot contain boundary bytes,
     * by iterating over the queue in reverse order, and summing buffer sizes.
     * The first buffer that passes the boundary length and subsequent buffers
     * are emitted (in the correct, non-reverse order).
     */
    private void enqueue(DataBuffer buf) {
      queue.add(buf);

      int len = 0;
      ArrayDeque<DataBuffer> emit = new ArrayDeque<>();
      for (Iterator<DataBuffer> iterator = queue.descendingIterator(); iterator.hasNext(); ) {
        DataBuffer previous = iterator.next();
        if (len > this.boundaryLength) {
          // addFirst to negate iterating in reverse order
          emit.addFirst(previous);
          iterator.remove();
        }
        len += previous.readableBytes();
      }

      for (DataBuffer buffer : emit) {
        MultipartParser.this.emitBody(buffer, false);
      }
    }

    private void flush() {
      for (Iterator<DataBuffer> iterator = queue.iterator(); iterator.hasNext(); ) {
        DataBuffer buffer = iterator.next();
        boolean last = !iterator.hasNext();
        MultipartParser.this.emitBody(buffer, last);
      }
      this.queue.clear();
    }

    @Override
    public void onComplete() {
      if (changeState(this, DisposedState.INSTANCE, null)) {
        String msg = "Could not find end of body (␍␊--" +
                new String(MultipartParser.this.boundary, StandardCharsets.UTF_8) + ")";
        emitError(new DecodingException(msg));
      }
    }

    @Override
    public void dispose() {
      this.queue.forEach(DataBuffer.RELEASE_CONSUMER);
      this.queue.clear();
    }

    @Override
    public String toString() {
      return "BODY";
    }
  }

  /**
   * The state of the parser when finished, either due to seeing the final
   * boundary or to a malformed message. Releases all incoming buffers.
   */
  private static final class DisposedState extends State {

    public static final DisposedState INSTANCE = new DisposedState();

    private DisposedState() { }

    @Override
    public void onNext(DataBuffer buf) {
      buf.release();
    }

    @Override
    public void onComplete() { }

    @Override
    public String toString() {
      return "DISPOSED";
    }
  }

}
