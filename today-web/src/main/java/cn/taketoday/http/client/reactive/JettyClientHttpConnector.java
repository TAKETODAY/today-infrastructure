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

package cn.taketoday.http.client.reactive;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.io.Content;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntPredicate;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.core.io.buffer.PooledDataBuffer;
import cn.taketoday.core.io.buffer.TouchableDataBuffer;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link ClientHttpConnector} for the Jetty Reactive Streams HttpClient.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">Jetty ReactiveStreams HttpClient</a>
 * @since 4.0
 */
public class JettyClientHttpConnector implements ClientHttpConnector {

  private final HttpClient httpClient;

  private DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;

  /**
   * Default constructor that creates a new instance of {@link HttpClient}.
   */
  public JettyClientHttpConnector() {
    this(new HttpClient());
  }

  /**
   * Constructor with an initialized {@link HttpClient}.
   */
  public JettyClientHttpConnector(HttpClient httpClient) {
    this(httpClient, null);
  }

  /**
   * Constructor with an initialized {@link HttpClient} and configures it
   * with the given {@link JettyResourceFactory}.
   *
   * @param httpClient the {@link HttpClient} to use
   * @param resourceFactory the {@link JettyResourceFactory} to use
   */
  public JettyClientHttpConnector(HttpClient httpClient, @Nullable JettyResourceFactory resourceFactory) {
    Assert.notNull(httpClient, "HttpClient is required");
    if (resourceFactory != null) {
      httpClient.setExecutor(resourceFactory.getExecutor());
      httpClient.setByteBufferPool(resourceFactory.getByteBufferPool());
      httpClient.setScheduler(resourceFactory.getScheduler());
    }
    this.httpClient = httpClient;
  }

  /**
   * Set the buffer factory to use.
   */
  public void setBufferFactory(DataBufferFactory bufferFactory) {
    this.bufferFactory = bufferFactory;
  }

  @Override
  public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
          Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

    if (!uri.isAbsolute()) {
      return Mono.error(new IllegalArgumentException("URI is not absolute: " + uri));
    }

    if (!httpClient.isStarted()) {
      try {
        httpClient.start();
      }
      catch (Exception ex) {
        return Mono.error(ex);
      }
    }

    Request jettyRequest = httpClient.newRequest(uri).method(method.toString());
    JettyClientHttpRequest request = new JettyClientHttpRequest(jettyRequest, this.bufferFactory);

    return requestCallback.apply(request).then(execute(request));
  }

  private Mono<ClientHttpResponse> execute(JettyClientHttpRequest request) {
    return Mono.fromDirect(request.toReactiveRequest()
            .response((reactiveResponse, chunkPublisher) -> {
              Flux<DataBuffer> content = Flux.from(chunkPublisher).map(this::toDataBuffer);
              return Mono.just(new JettyClientHttpResponse(reactiveResponse, content));
            }));
  }

  private DataBuffer toDataBuffer(Content.Chunk chunk) {
    DataBuffer delegate = this.bufferFactory.wrap(chunk.getByteBuffer());
    return new JettyDataBuffer(delegate, chunk);
  }

  private static final class JettyDataBuffer implements PooledDataBuffer {

    private final DataBuffer delegate;

    private final Content.Chunk chunk;

    private final AtomicInteger refCount = new AtomicInteger(1);

    public JettyDataBuffer(DataBuffer delegate, Content.Chunk chunk) {
      Assert.notNull(delegate, "Delegate must not be null");
      Assert.notNull(chunk, "Chunk must not be null");

      this.delegate = delegate;
      this.chunk = chunk;
    }

    @Override
    public boolean isAllocated() {
      return this.refCount.get() > 0;
    }

    @Override
    public PooledDataBuffer retain() {
      if (this.delegate instanceof PooledDataBuffer pooledDelegate) {
        pooledDelegate.retain();
      }
      this.chunk.retain();
      this.refCount.getAndUpdate(c -> {
        if (c != 0) {
          return c + 1;
        }
        else {
          return 0;
        }
      });
      return this;
    }

    @Override
    public boolean release() {
      if (this.delegate instanceof PooledDataBuffer pooledDelegate) {
        pooledDelegate.release();
      }
      this.chunk.release();
      int refCount = this.refCount.updateAndGet(c -> {
        if (c != 0) {
          return c - 1;
        }
        else {
          throw new IllegalStateException("already released " + this);
        }
      });
      return refCount == 0;
    }

    @Override
    public PooledDataBuffer touch(Object hint) {
      if (this.delegate instanceof TouchableDataBuffer touchableDelegate) {
        touchableDelegate.touch(hint);
      }
      return this;
    }

    // delegation

    @Override
    public DataBufferFactory factory() {
      return this.delegate.factory();
    }

    @Override
    public int indexOf(IntPredicate predicate, int fromIndex) {
      return this.delegate.indexOf(predicate, fromIndex);
    }

    @Override
    public int lastIndexOf(IntPredicate predicate, int fromIndex) {
      return this.delegate.lastIndexOf(predicate, fromIndex);
    }

    @Override
    public int readableByteCount() {
      return this.delegate.readableByteCount();
    }

    @Override
    public int writableByteCount() {
      return this.delegate.writableByteCount();
    }

    @Override
    public int capacity() {
      return this.delegate.capacity();
    }

    @Override
    @Deprecated
    public DataBuffer capacity(int capacity) {
      this.delegate.capacity(capacity);
      return this;
    }

    @Override
    public DataBuffer ensureWritable(int capacity) {
      this.delegate.ensureWritable(capacity);
      return this;
    }

    @Override
    public int readPosition() {
      return this.delegate.readPosition();
    }

    @Override
    public DataBuffer readPosition(int readPosition) {
      this.delegate.readPosition(readPosition);
      return this;
    }

    @Override
    public int writePosition() {
      return this.delegate.writePosition();
    }

    @Override
    public DataBuffer writePosition(int writePosition) {
      this.delegate.writePosition(writePosition);
      return this;
    }

    @Override
    public byte getByte(int index) {
      return this.delegate.getByte(index);
    }

    @Override
    public byte read() {
      return this.delegate.read();
    }

    @Override
    public DataBuffer read(byte[] destination) {
      this.delegate.read(destination);
      return this;
    }

    @Override
    public DataBuffer read(byte[] destination, int offset, int length) {
      this.delegate.read(destination, offset, length);
      return this;
    }

    @Override
    public DataBuffer write(byte b) {
      this.delegate.write(b);
      return this;
    }

    @Override
    public DataBuffer write(byte[] source) {
      this.delegate.write(source);
      return this;
    }

    @Override
    public DataBuffer write(byte[] source, int offset, int length) {
      this.delegate.write(source, offset, length);
      return this;
    }

    @Override
    public DataBuffer write(DataBuffer... buffers) {
      this.delegate.write(buffers);
      return this;
    }

    @Override
    public DataBuffer write(ByteBuffer... buffers) {
      this.delegate.write(buffers);
      return this;
    }

    @Override
    @Deprecated
    public DataBuffer slice(int index, int length) {
      DataBuffer delegateSlice = this.delegate.slice(index, length);
      this.chunk.retain();
      return new JettyDataBuffer(delegateSlice, this.chunk);
    }

    @Override
    public DataBuffer split(int index) {
      DataBuffer delegateSplit = this.delegate.split(index);
      this.chunk.retain();
      return new JettyDataBuffer(delegateSplit, this.chunk);
    }

    @Override
    @Deprecated
    public ByteBuffer asByteBuffer() {
      return this.delegate.asByteBuffer();
    }

    @Override
    @Deprecated
    public ByteBuffer asByteBuffer(int index, int length) {
      return this.delegate.asByteBuffer(index, length);
    }

    @Override
    @Deprecated
    public ByteBuffer toByteBuffer(int index, int length) {
      return this.delegate.toByteBuffer(index, length);
    }

    @Override
    public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
      this.delegate.toByteBuffer(srcPos, dest, destPos, length);
    }

    @Override
    public ByteBufferIterator readableByteBuffers() {
      ByteBufferIterator delegateIterator = this.delegate.readableByteBuffers();
      return new JettyByteBufferIterator(delegateIterator, this.chunk);
    }

    @Override
    public ByteBufferIterator writableByteBuffers() {
      ByteBufferIterator delegateIterator = this.delegate.writableByteBuffers();
      return new JettyByteBufferIterator(delegateIterator, this.chunk);
    }

    @Override
    public String toString(int index, int length, Charset charset) {
      return this.delegate.toString(index, length, charset);
    }

    private static final class JettyByteBufferIterator implements ByteBufferIterator {

      private final ByteBufferIterator delegate;

      private final Content.Chunk chunk;

      public JettyByteBufferIterator(ByteBufferIterator delegate, Content.Chunk chunk) {
        Assert.notNull(delegate, "Delegate must not be null");
        Assert.notNull(chunk, "Chunk must not be null");

        this.delegate = delegate;
        this.chunk = chunk;
        this.chunk.retain();
      }

      @Override
      public void close() {
        this.delegate.close();
        this.chunk.release();
      }

      @Override
      public boolean hasNext() {
        return this.delegate.hasNext();
      }

      @Override
      public ByteBuffer next() {
        return this.delegate.next();
      }
    }
  }

}
