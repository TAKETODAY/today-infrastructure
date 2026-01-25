/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.server.netty;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import infra.context.properties.ConfigurationProperties;
import infra.context.properties.NestedConfigurationProperty;
import infra.util.DataSize;
import infra.web.RequestContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.handler.logging.LogLevel;

/**
 * Netty server properties.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/13 00:03
 */
@ConfigurationProperties(prefix = "server.netty", ignoreUnknownFields = true)
public class NettyServerProperties {

  /**
   * the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For child {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public @Nullable Integer workerThreads;

  /**
   * the number of threads that will be used by
   * {@link io.netty.util.concurrent.MultithreadEventExecutorGroup}
   *
   * For parent {@link EventLoopGroup}
   *
   * @see io.netty.util.concurrent.MultithreadEventExecutorGroup
   */
  public @Nullable Integer acceptorThreads;

  /**
   * The worker thread pool name
   *
   * @since 5.0
   */
  public @Nullable String workerPoolName;

  /**
   * The acceptor thread pool name
   *
   * @since 5.0
   */
  public @Nullable String acceptorPoolName;

  /**
   * The SOMAXCONN value of the current machine. If failed to get the value, {@code 200} is used as a
   * default value for Windows and {@code 128} for others.
   */
  public @Nullable Integer maxConnection;

  /**
   * The ServerChannel class to be used by the Netty server.
   * <p>
   * This allows customization of the underlying socket channel implementation
   * that Netty will use for accepting incoming connections.
   */
  public @Nullable Class<? extends ServerChannel> socketChannel;

  /**
   * Set netty LoggingHandler logging Level. If that loggingLevel
   * is null will not register logging handler
   */
  public @Nullable LogLevel loggingLevel;

  /**
   * The maximum length of the request body content.
   * <p>
   * This setting controls the maximum amount of data that can be received
   * in a single HTTP request body. Requests exceeding this limit will
   * typically result in an error being returned to the client.
   */
  public DataSize maxContentLength = DataSize.ofMegabytes(100);

  /**
   * The maximum chunk size.
   * <p>
   * HTTP requests and responses can be quite large, in which case
   * it's better to process the data as a stream of chunks. This
   * sets the limit, in bytes, at which Netty will send a chunk
   * down the pipeline.
   */
  public DataSize maxChunkSize = DataSize.ofBytes(8192);

  /**
   * Initial buffer size for HTTP request decoding.
   * <p>
   * This setting determines the initial capacity of the buffer used when
   * decoding incoming HTTP requests. A larger buffer may improve performance
   * for requests with large headers or bodies, while a smaller buffer may
   * reduce memory usage for applications handling many concurrent requests.
   */
  public DataSize initialBufferSize = DataSize.ofBytes(128);

  /**
   * The maximum line length of header lines.
   * <p>
   * This limits how much memory Netty will use when parsing
   * HTTP header key-value pairs.
   */
  public DataSize maxHeaderSize = DataSize.ofBytes(8192);

  /**
   * The maximum length of the first line of the HTTP header.
   * <p>
   * This limits how much memory Netty will use when parsed the
   * initial HTTP header line.
   */
  public int maxInitialLineLength = 4096;

  /**
   * Whether header validation should be enabled or not.
   * <p>
   * You usually want header validation enabled (which is the default)
   * in order to prevent request-/response-splitting attacks.
   */
  public boolean validateHeaders = true;

  /**
   * Set whether {@code Transfer-Encoding: Chunked} should be supported.
   * if {@code false}, then a {@code Transfer-Encoding: Chunked} header will produce an error,
   * instead of a stream of chunks.
   */
  public boolean chunkedSupported = true;

  /**
   * Set whether chunks can be split into multiple messages, if their
   * chunk size exceeds the size of the input buffer. If set to {@code false}
   * to only allow sending whole chunks down the pipeline.
   */
  public boolean allowPartialChunks = true;

  /**
   * Set whether more than one {@code Content-Length} header is allowed.
   * You usually want to disallow this (which is the default) as multiple
   * {@code Content-Length} headers can indicate a request- or response-splitting attack.
   * if set to {@code true} to allow multiple content length headers.
   */
  public boolean allowDuplicateContentLengths = false;

  /**
   * Whether the PrintWriter should auto-flush after each write operation.
   * <p>
   * When set to {@code true}, the PrintWriter returned by
   * {@link RequestContext#getWriter()} will automatically flush its output
   * buffer after each write operation. This ensures that data is immediately
   * sent to the client, which can be important for streaming responses.
   * <p>
   * Defaults to {@code false}, meaning manual flushing is required via
   * {@link java.io.PrintWriter#flush()} or closing the writer.
   *
   * @since 5.0
   */
  public boolean writerAutoFlush = false;

  /**
   * The capacity of the queue used to store received data chunks.
   * <p>
   * This setting controls how many data chunks can be queued for processing
   * before backpressure is applied to the data source. A larger queue can
   * help smooth out bursts of data but will consume more memory.
   *
   * @since 5.0
   */
  public int dataReceivedQueueCapacity = 256;

  /**
   * Whether to read data automatically from the channel.
   * <p>
   * When set to {@code true}, the channel will automatically read data when available.
   * When set to {@code false}, data will only be read when explicitly requested,
   * allowing for more fine-grained control over the reading process and potentially
   * enabling better flow control in high-throughput scenarios.
   * <p>
   * Defaults to {@code true}, meaning the channel will automatically read data.
   *
   * @since 5.0
   */
  public boolean autoRead = true;

  /**
   * shutdown details
   */
  @NestedConfigurationProperty
  public final Shutdown shutdown = new Shutdown();

  public static class Shutdown {

    /**
     * Graceful shutdown ensures that no tasks are submitted for
     * 'the quiet period' (usually a couple seconds) before it shuts
     * itself down. If a task is submitted during the quiet period,
     * it is guaranteed to be accepted and the quiet period will start over.
     */
    public long quietPeriod = 1;

    /**
     * The maximum amount of time to wait until the executor is
     * shutdown() regardless if a task was submitted during the quiet period
     */
    public long timeout = 10;

    /**
     * The unit of quietPeriod and timeout
     */
    public TimeUnit unit = TimeUnit.SECONDS;

  }

}
