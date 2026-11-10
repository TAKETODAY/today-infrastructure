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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import infra.context.ApplicationContext;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.InvalidMediaTypeException;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.server.ServerHttpResponse;
import infra.http.support.NettyHeaders;
import infra.lang.Constant;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.util.InfraStrategies;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import infra.web.AbstractHttpContext;
import infra.web.DispatcherHandler;
import infra.web.HttpContext;
import infra.web.HttpContextHolder;
import infra.web.async.AsyncWebRequest;
import infra.web.async.WebAsyncManager;
import infra.web.multipart.MultipartRequest;
import infra.web.server.RequestBodySizeExceededException;
import infra.web.server.ResponseStatusException;
import infra.web.server.error.SendErrorHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.DefaultHeaders;
import io.netty.handler.codec.Headers;
import io.netty.handler.codec.HeadersUtils;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AsciiString;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import io.netty.util.ReferenceCountUtil;

import static infra.http.HttpHeaders.isNotEmpty;
import static io.netty.handler.codec.http.HttpMethod.CONNECT;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.HEAD;

/**
 * Netty-based implementation of the {@link HttpContext}, providing access to the
 * underlying Netty {@link Channel}, {@link HttpRequest}, and response handling mechanisms.
 * <p>
 * This class serves as the bridge between the Netty transport layer and the web framework,
 * managing request parsing, response commitment, header manipulation, cookie handling,
 * and asynchronous processing. It supports both HTTP/1.1 and HTTP/2 protocols.
 * <p>
 * Key features include:
 * <ul>
 *   <li>Lazy initialization of response body buffers and output streams</li>
 *   <li>Support for chunked file transfers with SSL awareness</li>
 *   <li>Configurable query parameter parsing (HTML vs RFC 3986 rules)</li>
 *   <li>Built-in protection against HashDOS via query parameter limits</li>
 *   <li>Integration with {@link SendErrorHandler} for standardized error responses</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-04 21:24
 */
public abstract class NettyHttpContext extends AbstractHttpContext {

  /**
   * System property to configure the nio file chunk size when ssl is enabled.
   * <p>Can also be configured via the {@link InfraStrategies} mechanism.
   *
   * @since 5.0
   */
  private static final int nioFileChunkSize = InfraStrategies.getInt("infra.web.ssl.nio-file-chunked-size", 4096);

  /**
   * RFC 3986 (the URI standard) makes no mention of using '+' to encode a space in a URI query component. The
   * whatwg HTML standard, however, defines the query to be encoded with the
   * {@code application/x-www-form-urlencoded} serializer defined in the whatwg URL standard, which does use '+'
   * to encode a space instead of {@code %20}.
   * <p>This flag controls whether the decoding should happen according to HTML rules, which decodes the '+' to a
   * space. The default is {@code true}.
   *
   * @since 5.0
   */
  private static final boolean htmlQueryDecoding = InfraStrategies.getFlag("infra.web.htmlQueryDecoding", true);

  /**
   * Maximum number of query parameters allowed, to mitigate HashDOS.
   * 1024 by default.
   *
   * @since 5.0
   */
  private static final int maxQueryParams = InfraStrategies.getInt("infra.web.maxQueryParams", 1024);

  /**
   * {@code false} by default. If set to {@code true}, instead of allowing query parameters to be separated by
   * semicolons, treat the semicolon as a normal character in a query value.
   *
   * @since 5.0
   */
  private static final boolean semicolonAsNormalChar = InfraStrategies.getFlag("infra.web.semicolonAsNormalChar", false);

  /**
   * For Chunk file written
   *
   * @see ChunkedWriteHandler
   * @since 5.0
   */
  private static final String CHUNKED_WRITER_NAME = "chunkedWriter";

  private static final AttributeKey<@Nullable NettyHttpContext> KEY = AttributeKey.valueOf(NettyHttpContext.class, "KEY");

  private static final CookieHeaderNames.SameSite[] SAME_SITES = CookieHeaderNames.SameSite.values();

  /**
   * Shared logger for transport-level diagnostics.
   */
  protected static final Logger log = LoggerFactory.getLogger(NettyHttpContext.class);

  protected final Channel channel;

  protected final NettyRequestConfig config;

  /**
   * response headers
   */
  protected final DefaultHeaders<CharSequence, @Nullable CharSequence, ?> nettyResponseHeaders;

  protected final HttpTrafficHandler httpTrafficHandler;

  private final long requestTimeMillis = System.currentTimeMillis();

  private HttpResponseStatus status = HttpResponseStatus.OK;

  /**
   * The response body waiting to be written to the pipeline: a {@link ByteBuf},
   * a {@link FileRegion} or a {@link ChunkedInput}. These representations are
   * mutually exclusive and consumed together by {@link #flush()}.
   */
  private @Nullable Object pendingBody;

  private @Nullable ServerHttpResponse outputMessage;

  private final @Nullable BodyInputStream requestBody;

  private final io.netty.handler.codec.http.HttpMethod method;

  private final long contentLength;

  private long receivedBytes;

  /**
   * Whether this request paused channel auto-read for manual body reading.
   */
  private volatile boolean readPaused;

  /**
   * Create a context with the given response headers container.
   * <p>Subclasses that use a protocol-specific container, such as HTTP/2
   * {@code Http2Headers}, can supply it through this constructor.
   *
   * @param nettyResponseHeaders the response headers container
   */
  protected NettyHttpContext(ApplicationContext context, Channel channel, NettyRequestConfig config,
          DispatcherHandler dispatcherHandler, HttpTrafficHandler httpTrafficHandler,
          long contentLength, io.netty.handler.codec.http.HttpMethod method,
          DefaultHeaders<CharSequence, CharSequence, ?> nettyResponseHeaders) {
    super(context, dispatcherHandler);
    this.config = config;
    this.channel = channel;
    this.httpTrafficHandler = httpTrafficHandler;
    this.nettyResponseHeaders = nettyResponseHeaders;
    this.contentLength = contentLength;
    this.method = method;
    this.requestBody = createRequestBody(contentLength, method);
  }

  /**
   * Normalize a response header name before accessing the response headers.
   * <p>HTTP/2 requires lower-case field names, so the HTTP/2 context overrides
   * this method to lower-case names.
   */
  protected CharSequence normalizeHeaderName(String name) {
    return name;
  }

  /**
   * Whether this protocol supports the {@code Transfer-Encoding} response header.
   * <p>Chunked transfer coding was introduced in HTTP/1.1; HTTP/1.0 responses
   * must be delimited by closing the connection instead. HTTP/2 also forbids
   * connection-specific headers, so the HTTP/2 context overrides this method
   * to return {@code false}.
   */
  protected boolean supportsTransferEncoding() {
    return version().compareTo(HttpVersion.HTTP_1_1) >= 0;
  }

  /**
   * Whether this transport supports writing a file as a zero-copy
   * {@link DefaultFileRegion}.
   * <p>HTTP/2 stream channels only accept
   * {@link io.netty.handler.codec.http2.Http2StreamFrame} outbound messages,
   * so {@link Http2HttpContext} overrides this method to return {@code false};
   * files are then always sent through a {@link ChunkedWriteHandler}.
   *
   * @since 5.0
   */
  protected boolean supportsFileRegion() {
    return true;
  }

  /**
   * Create the request body stream for this exchange.
   * <p>A zero-length body, or an unknown length for a method that carries no body,
   * leaves the stream unset so reads return an empty input stream. When
   * {@link NettyRequestConfig#autoRead} is disabled, the channel stops reading
   * automatically and the body stream requests more data on demand.
   *
   * @param contentLength the declared request content length
   * @param method the request method
   */
  private @Nullable BodyInputStream createRequestBody(long contentLength, io.netty.handler.codec.http.HttpMethod method) {
    if (contentLength == 0L || (contentLength == -1L && (method == GET || method == HEAD))) {
      return null;
    }
    if (config.autoRead) {
      return new BodyInputStream(config.dataReceivedQueueCapacity);
    }
    channel.config().setAutoRead(false);
    this.readPaused = true;
    return new ManualReadingBodyInputStream(config.dataReceivedQueueCapacity);
  }

  @Override
  public long getRequestTimeMillis() {
    return requestTimeMillis;
  }

  @Override
  protected final String readMethod() {
    return method.name();
  }

  @Override
  public final long getContentLength() {
    return contentLength;
  }

  /**
   * Return the HTTP protocol version of the current request.
   *
   * @return the request protocol version, for example {@code HTTP/1.1} or {@code HTTP/2.0}
   */
  public abstract HttpVersion version();

  @Override
  public boolean isSecure() {
    return config.secure;
  }

  @Override
  public String getScheme() {
    return isSecure() ? Constant.HTTPS : Constant.HTTP;
  }

  @Override
  public InetSocketAddress remoteAddress() {
    return (InetSocketAddress) channel.remoteAddress();
  }

  @Override
  public SocketAddress localAddress() {
    return channel.localAddress();
  }

  @Override
  protected final String readRequestURI() {
    String uri = requestUri();
    int index = uri.indexOf('?');
    return index == -1 ? uri : uri.substring(0, index);
  }

  @Override
  public final @Nullable String readQueryString() {
    String uri = requestUri();
    int index = uri.indexOf('?');
    return index == -1 ? null : uri.substring(index + 1);
  }

  /**
   * Return the request target, including the query string.
   * <p>The raw target is returned as received; path and query string are split
   * lazily by {@link #readRequestURI()} and {@link #readQueryString()}.
   *
   * @return the raw request target, never {@code null}
   */
  protected abstract String requestUri();

  @Override
  protected PrintWriter createWriter() throws IOException {
    return new PrintWriter(getOutputStream(), config.writerAutoFlush, config.writerCharset);
  }

  @Override
  public @Nullable String getResponseContentType() {
    String contentType = this.responseContentType;
    if (contentType == null) {
      CharSequence value = nettyResponseHeaders.get(HttpHeaderNames.CONTENT_TYPE);
      if (value != null) {
        contentType = value.toString();
        this.responseContentType = contentType;
      }
    }
    return contentType;
  }

  /**
   * Adapt the Netty response headers to the framework {@link infra.http.HttpHeaders}.
   * <p>The returned adapter is a live view over {@link #nettyResponseHeaders}, so
   * header writes performed directly on this context remain visible through
   * {@link #responseHeaders()}.
   */
  @Override
  protected infra.http.HttpHeaders createResponseHeaders() {
    return new NettyHeaders(nettyResponseHeaders);
  }

  @Override
  public void setHeader(String name, @Nullable String value) {
    CharSequence headerName = normalizeHeaderName(name);
    if (value == null) {
      nettyResponseHeaders.remove(headerName);
    }
    else {
      nettyResponseHeaders.set(headerName, value);
    }
  }

  @Override
  public void setHeaders(infra.http.@Nullable HttpHeaders headers) {
    if (isNotEmpty(headers)) {
      for (var entry : headers.entries()) {
        nettyResponseHeaders.set(normalizeHeaderName(entry.getKey()), entry.getValue());
      }
    }
  }

  @Override
  public void addHeader(String name, @Nullable String value) {
    nettyResponseHeaders.add(normalizeHeaderName(name), value);
  }

  @Override
  public void addHeaders(infra.http.@Nullable HttpHeaders headers) {
    if (isNotEmpty(headers)) {
      for (var entry : headers.entries()) {
        nettyResponseHeaders.add(normalizeHeaderName(entry.getKey()), entry.getValue());
      }
    }
  }

  @Override
  public boolean removeHeader(String name) {
    if (!super.removeHeader(name)) {
      CharSequence headerName = normalizeHeaderName(name);
      boolean contains = nettyResponseHeaders.contains(headerName);
      nettyResponseHeaders.remove(headerName);
      return contains;
    }
    return true;
  }

  @Override
  public boolean containsResponseHeader(String name) {
    return nettyResponseHeaders.contains(normalizeHeaderName(name));
  }

  @Override
  public HttpCookie[] readCookies() {
    List<String> allCookie = getHeaders(HttpHeaders.COOKIE);
    if (CollectionUtils.isEmpty(allCookie)) {
      return EMPTY_COOKIES;
    }
    Set<Cookie> decoded;
    ServerCookieDecoder cookieDecoder = config.cookieDecoder;
    if (allCookie.size() == 1) {
      decoded = cookieDecoder.decode(allCookie.get(0));
    }
    else {
      decoded = new TreeSet<>();
      for (String header : allCookie) {
        decoded.addAll(cookieDecoder.decode(header));
      }
    }
    if (CollectionUtils.isEmpty(decoded)) {
      return EMPTY_COOKIES;
    }
    else {
      int i = 0;
      HttpCookie[] ret = new HttpCookie[decoded.size()];
      for (Cookie cookie : decoded) {
        ret[i++] = new HttpCookie(cookie.name(), cookie.value());
      }
      return ret;
    }
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected MultiValueMap<String, String> readParameters() {
    String queryString = getQueryString();
    MultiValueMap<String, String> params = MultiValueMap.forSmartListAdaptation(new LinkedHashMap<>());
    if (queryString != null) {
      parseParameters(params, queryString, semicolonAsNormalChar, Constant.DEFAULT_CHARSET);
    }

    if (getAttribute(FORM_URLENCODED_ATTRIBUTE) instanceof MultiValueMap cached) {
      params.addAll(cached);
    }
    else if (hasBody() && getMethod() != HttpMethod.GET
            && getMethod() != HttpMethod.HEAD && isFormUrlEncoded()) {
      try {
        Charset charset = formCharset();
        String s = getContentAsString(charset);
        if (StringUtils.isNotEmpty(s)) {
          MultiValueMap<String, String> body = MultiValueMap.forSmartListAdaptation(new LinkedHashMap<>());
          parseParameters(body, s, semicolonAsNormalChar, charset);
          params.addAll(body);
          setAttribute(FORM_URLENCODED_ATTRIBUTE, body);
        }
      }
      catch (IOException e) {
        throw new HttpMessageNotReadableException("'application/x-www-form-urlencoded' content read failed", e, this);
      }
    }
    return params;
  }

  @Override
  public void sendRedirect(String location) {
    this.status = HttpResponseStatus.FOUND;
    nettyResponseHeaders.set(HttpHeaderNames.LOCATION, location);
    commit();
  }

  /**
   * HTTP response body
   *
   * @return HTTP response body
   */
  private ByteBuf responseBody() {
    if (pendingBody instanceof ByteBuf body) {
      return body;
    }
    if (pendingBody != null) {
      throw new IllegalStateException("Cannot write response body: a response file has already been scheduled");
    }

    var bodyFactory = config.responseBodyFactory;
    ByteBuf body = bodyFactory != null ? bodyFactory.apply(this) : null; // may null
    if (body == null) {
      // fallback
      body = createResponseBody(config);
    }
    this.pendingBody = body;
    return body;
  }

  protected ByteBuf createResponseBody(NettyRequestConfig config) {
    return channel.alloc().ioBuffer(config.responseBodyInitialCapacity);
  }

  @Override
  public ServerHttpResponse asHttpOutputMessage() {
    ServerHttpResponse response = this.outputMessage;
    if (response == null) {
      response = new NettyHttpOutputMessage();
      this.outputMessage = response;
    }
    return response;
  }

  @Override
  public void flush() {
    writeHeaders();
    if (!isCommitted() || isCommitting()) {
      // The head was not written: committing is in progress (re-entrant call
      // from a committing callback) or it failed. Never write the body first.
      return;
    }

    Object body = this.pendingBody;
    this.pendingBody = null;
    if (body instanceof ByteBuf byteBuf) {
      writeResponseBody(byteBuf);
    }
    else if (body != null) {
      writeResponseFile(body);
    }
  }

  @Override
  protected OutputStream createOutputStream() {
    return getMethod() == HttpMethod.HEAD ? new NoBodyOutputStream() : new ResponseBodyOutputStream();
  }

  @Override
  protected InputStream createInputStream() {
    return requestBody != null ? requestBody : InputStream.nullInputStream();
  }

  @Override
  protected final void completeRequestInternal(@Nullable Throwable notHandled) {
    cleanup(null);
    if (notHandled == null) {
      flush();
      writeResponseEnd();
    }
    else {
      discardPendingResponse();
    }
  }

  private void discardPendingResponse() {
    Object body = this.pendingBody;
    this.pendingBody = null;
    if (body instanceof ByteBuf byteBuf) {
      ReferenceCountUtil.safeRelease(byteBuf);
    }
    else if (body != null) {
      releaseResponseFile(body);
    }
  }

  /**
   * Release an unsent response file or an input rejected by the pipeline.
   */
  protected final void releaseResponseFile(@Nullable Object pendingFile) {
    try {
      if (pendingFile instanceof ChunkedInput<?> input) {
        input.close();
      }
    }
    catch (Exception ex) {
      log.warn("Failed to close pending response file", ex);
    }
    finally {
      ReferenceCountUtil.safeRelease(pendingFile);
    }
  }

  /**
   * Adapt a pending zero-copy file when trailers were registered afterwards.
   */
  protected final Object prepareResponseFile(Object file) {
    if (file instanceof PendingFileRegion region && getTrailerFieldNames() != null) {
      try {
        return createChunkedResponseFile(region.file, region.position(), region.count());
      }
      catch (IOException ex) {
        throw new UncheckedIOException("Failed to prepare response file", ex);
      }
      finally {
        ReferenceCountUtil.safeRelease(region);
      }
    }
    return file;
  }

  private Object createChunkedResponseFile(File file, long position, long count) throws IOException {
    ChannelPipeline pipeline = channel.pipeline();
    if (pipeline.context(CHUNKED_WRITER_NAME) == null) {
      pipeline.addLast(CHUNKED_WRITER_NAME, new ChunkedWriteHandler());
    }
    FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
    try {
      ChunkedNioFile nioFile = new ChunkedNioFile(fileChannel, position, count, nioFileChunkSize);
      return modifyChunkedFile(nioFile);
    }
    catch (IOException | RuntimeException | Error ex) {
      try {
        fileChannel.close();
      }
      catch (IOException closeFailure) {
        ex.addSuppressed(closeFailure);
      }
      throw ex;
    }
  }

  /**
   * Restore channel auto-read if this request paused it.
   */
  private void restoreAutoRead() {
    if (readPaused) {
      readPaused = false;
      channel.config().setAutoRead(true);
    }
  }

  private void cleanup(@Nullable IOException error) {
    if (requestBody != null) {
      if (error != null) {
        requestBody.onError(error);
      }
      requestBody.close();
    }
    restoreAutoRead();
  }

  /**
   * Send HTTP message to the client
   *
   * @throws IllegalStateException If the response has been committed
   */
  private void commit() {
    assertNotCommitted();
    writeHeaders();
  }

  /**
   * Write the headers that frame the response content.
   * <p>When the length is known it is written as {@code Content-Length}. An
   * unknown-length {@link ChunkedInput} uses chunked transfer coding where the
   * protocol supports it; otherwise the message is delimited by the transport.
   * An explicit length or a chunked transfer coding wins, so the common response
   * path returns after the first lookup. For a {@code HEAD} request the length is
   * taken from the discarded body before it is released, so the representation
   * length is preserved.
   */
  private void writeContentFraming(DefaultHeaders<CharSequence, @Nullable CharSequence, ?> headers) {
    if (isTransferEncodingChunked(headers)) {
      // Explicit chunked framing wins over an inferred or stale Content-Length.
      headers.remove(HttpHeaderNames.CONTENT_LENGTH);
      return;
    }
    if (headers.contains(HttpHeaderNames.CONTENT_LENGTH)) {
      return;
    }

    Object body = this.pendingBody;
    if (body instanceof ByteBuf byteBuf) {
      headers.setInt(HttpHeaderNames.CONTENT_LENGTH, byteBuf.readableBytes());
    }
    else if (body instanceof FileRegion region) {
      headers.setLong(HttpHeaderNames.CONTENT_LENGTH, region.count());
    }
    else if (body instanceof ChunkedInput<?> input) {
      long length = input.length();
      if (length >= 0) {
        headers.setLong(HttpHeaderNames.CONTENT_LENGTH, length);
      }
      else if (method != HEAD && supportsTransferEncoding()) {
        // Unknown length cannot use Content-Length; let chunked coding delimit it.
        headers.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
      }
    }
    else if (outputStream instanceof NoBodyOutputStream nbStream) {
      headers.setInt(HttpHeaderNames.CONTENT_LENGTH, nbStream.contentLength);
    }
    else {
      headers.setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
    }
  }

  @Override
  protected void writeHeaders() {
    if (!tryBeginCommit()) {
      // Already committed, another thread / a re-entrant callback is in the
      // middle of committing, or a failed attempt has not fully unwound yet.
      // The head must be written exactly once.
      return;
    }

    // From the committing phase onwards the response headers are locked:
    // committing callbacks must not mutate headers or register trailers.
    boolean committedOk = false;
    try {
      onCommitting();
      var headers = nettyResponseHeaders;
      int code = status.code();
      if (code < 200 || code == 204 || code == 205 || code == 304
              || (method == CONNECT && code < 300)) {
        // these responses carry no body, even when a handler or sendFile()
        // prepared one
        clearTrailerFields();
        headers.remove(HttpHeaderNames.CONTENT_LENGTH);
        headers.remove(HttpHeaderNames.TRANSFER_ENCODING);
        headers.remove(HttpHeaderNames.TRAILER);
        discardPendingResponse();
        if (code == 205) {
          // 205 is not implicitly empty; clients need an explicit zero length
          headers.setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
        }
      }
      else if (method == HEAD) {
        clearTrailerFields();
        // preserve the representation length before the body is discarded
        writeContentFraming(headers);
        discardPendingResponse();
      }
      else {
        Set<String> trailerNames = getTrailerFieldNames();
        if (trailerNames != null) {
          if (supportsTransferEncoding()) {
            headers.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
          }
          headers.remove(HttpHeaderNames.CONTENT_LENGTH);
          headers.set(HttpHeaderNames.TRAILER, String.join(", ", trailerNames));
        }
        else {
          writeContentFraming(headers);
        }
      }

      // apply cookies
      if (responseCookies != null) {
        var cookieEncoder = config.cookieEncoder;
        for (HttpCookie cookie : responseCookies) {
          DefaultCookie nc = new DefaultCookie(cookie.getName(), cookie.getValue());
          if (cookie instanceof ResponseCookie rc) {
            nc.setPath(rc.getPath());
            nc.setDomain(rc.getDomain());
            nc.setMaxAge(rc.getMaxAge().getSeconds());
            nc.setSecure(rc.isSecure());
            nc.setHttpOnly(rc.isHttpOnly());
            nc.setSameSite(forSameSite(rc.getSameSite()));
            nc.setPartitioned(rc.isPartitioned());
          }

          headers.add(HttpHeaderNames.SET_COOKIE, cookieEncoder.encode(nc));
        }
      }

      trackResponseWrite(writeResponseHead(status, headers));
      committedOk = true;
    }
    finally {
      // keep COMMITTED when the head write was issued; roll back on failure
      finishCommit(committedOk);
    }
    onCommitted();
  }

  /**
   * Write the response head using the transport specific representation.
   *
   * @return the write future, so the caller can attach the failure listener
   */
  protected <T extends Headers<CharSequence, CharSequence, T>>
  ChannelFuture writeResponseHead(HttpResponseStatus status, DefaultHeaders<CharSequence, @Nullable CharSequence, T> headers) {
    return channel.write(new DefaultHttpResponse(version(), status, new Http1Headers(headers)));
  }

  /**
   * Write and flush a response body chunk.
   */
  protected void writeResponseBody(ByteBuf body) {
    trackResponseWrite(channel.writeAndFlush(body));
  }

  /**
   * Write and flush a file or chunked input.
   */
  protected void writeResponseFile(Object file) {
    Object prepared = prepareResponseFile(file);
    try {
      trackResponseWrite(channel.writeAndFlush(prepared));
    }
    catch (RuntimeException | Error failure) {
      releaseResponseFile(prepared);
      throw failure;
    }
  }

  /**
   * Track a response write so a failed write closes the connection or stream.
   * Trailers are evaluated when the response completes and are written after
   * every body chunk in pipeline order; an asynchronous write failure aborts
   * the exchange through {@link ChannelFutureListener#CLOSE_ON_FAILURE}.
   */
  protected final void trackResponseWrite(ChannelFuture future) {
    future.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
  }

  /**
   * Abort a response that cannot produce a valid terminating message.
   */
  protected final void abortResponse(Throwable failure) {
    HttpTrafficHandler.handleCommittedException(channel, failure);
  }

  /**
   * Complete the response, closing the connection when it is not kept alive.
   */
  protected void writeResponseEnd() {
  }

  @Override
  public void setContentLength(long length) {
    nettyResponseHeaders.setLong(HttpHeaderNames.CONTENT_LENGTH, length);
  }

  @Override
  public void reset() {
    assertNotCommitted();
    if (pendingBody instanceof ByteBuf body) {
      // keep the buffer for reuse
      body.clear();
    }
    else {
      discardPendingResponse();
    }

    clearTrailerFields();
    nettyResponseHeaders.clear();

    writer = null;
    responseContentType = null;
    status = HttpResponseStatus.OK;
  }

  @Override
  public void setStatus(int sc) {
    this.status = HttpResponseStatus.valueOf(sc);
  }

  /**
   * Set response status
   *
   * @param status http status
   * @since 5.0
   */
  public void setStatus(HttpResponseStatus status) {
    this.status = status;
  }

  @Override
  public int getStatus() {
    return status.code();
  }

  @Override
  public void sendError(int sc) throws IOException {
    sendError(sc, null);
  }

  /**
   * Sends an HTTP error response to the client with the specified status code and
   * an optional error message. This method resets the current response state before
   * setting the new status code and delegating the error handling logic to the
   * configured {@code sendErrorHandler}.
   *
   * <p>This method is typically used to notify the client of an error condition,
   * such as a missing resource (404 Not Found) or an invalid request (400 Bad Request).
   * The optional error message can provide additional details about the error, which
   * may be logged or included in the response depending on the implementation of the
   * {@code sendErrorHandler}.
   *
   * <p><strong>Usage Example:</strong></p>
   * <pre>{@code
   * try {
   *   processRequest(request);
   * }
   * catch (InvalidInputException e) {
   *   sendError(HttpResponseStatus.BAD_REQUEST.code(), "Invalid input: " + e.getMessage());
   * }
   * }</pre>
   *
   * <p>In the example above, if the {@code processRequest} method throws an
   * {@code InvalidInputException}, the server responds with a 400 status code and
   * includes a descriptive error message.
   *
   * <p><strong>Handling Internal Server Errors:</strong></p>
   * <pre>{@code
   * try {
   *   executeCriticalOperation();
   * }
   * catch (Exception e) {
   *   sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "An unexpected error occurred");
   * }
   * }</pre>
   *
   * <p>Here, the method is used to handle unexpected exceptions by sending a 500
   * status code and a generic error message to the client.
   *
   * @param sc the HTTP status code to send (e.g., 404 for Not Found, 500 for Internal Server Error)
   * @param msg an optional error message describing the issue; may be {@code null}
   * if no specific message is available
   * @throws IOException if an I/O error occurs while sending the error response
   * @see SendErrorHandler#handleError(HttpContext, String)
   */
  @Override
  public void sendError(int sc, @Nullable String msg) throws IOException {
    reset();
    this.status = HttpResponseStatus.valueOf(sc);
    config.sendErrorHandler.handleError(this, msg);
  }

  @Override
  protected MultipartRequest createMultipartRequest() {
    return config.multipartParser.parse(this);
  }

  @Override
  protected AsyncWebRequest createAsyncWebRequest() {
    return new NettyAsyncWebRequest(this);
  }

  /**
   * Consume a request body chunk decoded by the HTTP/1.x codec.
   * <p>Ownership of {@code content} is transferred to this method.
   *
   * @param content the decoded chunk, possibly the last chunk
   */
  protected void onDataReceived(HttpContent content) {
    onDataReceived(content.content(), content instanceof LastHttpContent);
  }

  /**
   * Consume a raw request body chunk.
   * <p>The chunk is forwarded to the request body stream, or released when it is
   * rejected or there is no body to read. Ownership of {@code data} is always
   * transferred to this method.
   *
   * @param data the body chunk to consume
   * @param last whether this chunk completes the request body
   */
  protected void onDataReceived(ByteBuf data, boolean last) {
    final int chunkSize = data.readableBytes();
    final long received = receivedBytes + chunkSize;

    if (received > config.maxContentLength) {
      data.release();
      if (requestBody != null) {
        requestBody.onError(new IOException(new RequestBodySizeExceededException(config.maxContentLength)));
      }
      return;
    }

    receivedBytes = received;

    if (requestBody != null) {
      if (chunkSize > 0) {
        requestBody.onDataReceived(data);
      }
      else {
        data.release();
      }
      if (last) {
        requestBody.onComplete();
      }
    }
    else {
      data.release();
    }
  }

  /**
   * Signal a stream-level failure to the request body reader, so pending or
   * subsequent reads fail instead of blocking.
   *
   * @param error the failure to propagate
   */
  public void onError(IOException error) {
    if (requestBody != null) {
      requestBody.onError(error);
    }
  }

  /**
   * Release request resources after the underlying channel or HTTP/2 stream
   * becomes inactive.
   */
  protected void onChannelInactive() {
    discardPendingResponse();
    cleanup(new ClosedChannelException());
  }

  /**
   * Adapt a chunked file input to the transport that writes it.
   * <p>Invoked when a file is sent over SSL/TLS, where zero-copy transfer is not
   * possible and the response is written through a {@link ChunkedWriteHandler}.
   * HTTP/2 contexts wrap the input so that it emits {@code DATA} frames.
   *
   * @param nioFile the chunked file input to adapt
   * @return the object written to the channel, by default {@code nioFile} itself
   */
  protected Object modifyChunkedFile(ChunkedInput<ByteBuf> nioFile) {
    return nioFile;
  }

  //

  /**
   * Dispatch the request on the request executor thread.
   * <p>Negotiates {@code 100 Continue} and enforces the configured maximum
   * content length before delegating to {@link #processRequest()}. Unhandled
   * failures are reported through {@link HttpTrafficHandler}, and the context is
   * removed from {@link HttpContextHolder} when the exchange completes.
   * <p>Protocol specific behavior is supplied by the {@code 100 Continue} and
   * request dispatch hooks, so subclasses only provide transport details.
   * <p>When {@link NettyRequestConfig#autoRead} is disabled, manual body reading
   * pauses channel auto-read for the request. The pause is released by
   * {@link #cleanup(IOException)} when the request completes:
   * {@link #processRequest()} guarantees that completion, while the early
   * returns above call {@code cleanup} directly because they bypass it.
   */
  public final void run() {
    HttpContextHolder.set(this);

    try {
      long contentLength = getContentLength();
      if (contentLength != -1 && contentLength > config.maxContentLength) {
        processException(new RequestBodySizeExceededException(config.maxContentLength));
        cleanup(null);
        return;
      }

      if (is100ContinueExpected()) {
        if (tryWriteResponseContinue()) {
          removeExpectHeader();
        }
        else {
          writeRejectResponseContinue();
          cleanup(null);
          return;
        }
      }

      processRequest();
    }
    catch (Throwable e) {
      discardPendingResponse();
      httpTrafficHandler.handleException(channel, e);
    }
    finally {
      HttpContextHolder.cleanup();
    }
  }

  /**
   * Dispatch the request to the {@link DispatcherHandler}.
   * <p>Subclasses may override this to inspect protocol state before dispatching,
   * for example to report a request decoding failure.
   * <p>Implementations must guarantee that the request lifecycle completes
   * before returning, including on failure, so that request-scoped resources and
   * the paused channel auto-read are released.
   *
   * @throws Exception if request handling fails
   */
  protected void processRequest() throws Exception {
    dispatcherHandler.handleRequest(this);
  }

  /**
   * Determine whether the request carries a {@code 100-continue} expectation.
   *
   * @return {@code true} if the client expects an interim {@code 100 Continue} response
   */
  protected abstract boolean is100ContinueExpected();

  /**
   * Remove the {@code Expect} request header once {@code 100 Continue} has been
   * accepted, so a later read does not negotiate the interim response again.
   * <p>The base implementation does nothing.
   */
  protected void removeExpectHeader() {
  }

  /**
   * Decide whether to accept the {@code 100-continue} expectation and, if so,
   * write the interim response.
   * <p>The expectation is rejected when the declared content length exceeds
   * {@link NettyRequestConfig#maxContentLength}, or when
   * {@link DispatcherHandler#requestContinueExpected} explicitly rejects it.
   *
   * @return {@code true} if the interim response was written and the request body
   * may be read, {@code false} if the expectation must be rejected
   */
  protected boolean tryWriteResponseContinue() {
    if (getContentLength() > config.maxContentLength) {
      return false;
    }
    Boolean continueExpected = dispatcherHandler.requestContinueExpected(this);
    if (continueExpected != null && !continueExpected) {
      return false;
    }

    writeResponseContinue();
    return true;
  }

  /**
   * Write the informational {@code 100 Continue} response without ending the
   * exchange, so the client can start sending the request body.
   * <p>Implementations must use the transport specific representation.
   */
  protected abstract void writeResponseContinue();

  /**
   * Write a {@code 417 Expectation Failed} response after the {@code 100-continue}
   * expectation was rejected.
   * <p>Implementations must use the transport specific representation and must
   * not read the request body afterwards.
   */
  protected abstract void writeRejectResponseContinue();

  protected boolean hasBody() {
    return requestBody != null;
  }

  private boolean isFormUrlEncoded() {
    try {
      MediaType contentType = getContentType();
      return contentType != null
              && contentType.equalsTypeAndSubtype(MediaType.APPLICATION_FORM_URLENCODED);
    }
    catch (InvalidMediaTypeException ex) {
      return false;
    }
  }

  /**
   * Return the charset for decoding form body values.
   * <p>The {@code charset} parameter of the request {@code Content-Type} wins;
   * UTF-8 is used when it is absent or when the content type cannot be parsed.
   */
  private Charset formCharset() {
    try {
      MediaType contentType = getContentType();
      if (contentType != null) {
        Charset charset = contentType.getCharset();
        if (charset != null) {
          return charset;
        }
      }
    }
    catch (InvalidMediaTypeException ex) {
      // fall through to the default charset
    }
    return Constant.DEFAULT_CHARSET;
  }

  @Override
  public String toString() {
    return super.toString() + " " + version();
  }

  static @Nullable NettyHttpContext lookup(AttributeMap map) {
    return map.attr(KEY).get();
  }

  /**
   * Attach this context to its channel so subsequent body frames can find it.
   * <p>Must be called after construction is complete; the constructor itself
   * must not publish {@code this}.
   */
  final void attach() {
    channel.attr(KEY).set(this);
  }

  protected static @Nullable NettyHttpContext getAndRemove(AttributeMap map) {
    return map.attr(KEY).getAndSet(null);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected static long contentLength(Headers headers) {
    return headers.getLong(HttpHeaderNames.CONTENT_LENGTH, -1);
  }

  /**
   * write result to client
   *
   * @param concurrentResult async result
   * @throws Exception dispatch error
   */
  void dispatchConcurrentResult(@Nullable Object concurrentResult) throws Exception {
    Object handler = WebAsyncManager.findHttpRequestHandler(this);
    dispatcherHandler.handleConcurrentResult(this, handler, concurrentResult);
  }

  /**
   * Return the enum value corresponding to the passed in same-site-flag, using a case insensitive comparison.
   *
   * @param name value for the SameSite Attribute
   * @return enum value for the provided name or null
   */
  static CookieHeaderNames.@Nullable SameSite forSameSite(@Nullable String name) {
    if (name != null) {
      for (var each : SAME_SITES) {
        if (each.name().equalsIgnoreCase(name)) {
          return each;
        }
      }
    }
    return null;
  }

  /**
   * Checks to see if the transfer encoding in the specified headers is chunked
   *
   * @return True if transfer encoding is chunked, otherwise false
   */
  private static boolean isTransferEncodingChunked(DefaultHeaders<CharSequence, @Nullable CharSequence, ?> responseHeaders) {
    return responseHeaders.contains(
            HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED, AsciiString.CASE_INSENSITIVE_HASHER);
  }

  /**
   * Parse URL-encoded parameters into the given map.
   *
   * @param params the target parameters
   * @param s the raw query string or form body
   * @param semicolonAsNormalChar whether a semicolon is a regular character
   * @param charset the charset used to decode percent escapes
   * @throws ResponseStatusException with {@code 400 Bad Request} if the input
   * contains a malformed escape sequence
   */
  static void parseParameters(MultiValueMap<String, String> params, String s,
          boolean semicolonAsNormalChar, Charset charset) {
    try {
      parseParametersRaw(params, s, semicolonAsNormalChar, charset);
    }
    catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URL-encoded parameters", e);
    }
  }

  private static void parseParametersRaw(MultiValueMap<String, String> params, String s,
          boolean semicolonAsNormalChar, Charset charset) {
    QueryStringDecoder decoder = QueryStringDecoder.builder()
            .hasPath(false)
            .charset(charset)
            .maxParams(maxQueryParams)
            .semicolonIsNormalChar(semicolonAsNormalChar)
            .htmlQueryDecoding(htmlQueryDecoding)
            .build(s);
    params.addAll(decoder.parameters());
  }

  static final class NoBodyOutputStream extends OutputStream {

    public int contentLength = 0;

    @Override
    public void write(int b) {
      contentLength++;
    }

    @Override
    public void write(byte[] buf, int offset, int len) {
      if (offset < 0 || len < 0 || offset + len > buf.length) {
        throw new IndexOutOfBoundsException("Invalid offset [%s] and / or length [%s] specified for array of size [%s]"
                .formatted(offset, len, buf.length));
      }

      contentLength += len;
    }

  }

  final class ManualReadingBodyInputStream extends BodyInputStream {

    private ManualReadingBodyInputStream(int capacity) {
      super(capacity);
    }

    @Override
    protected void requestNext() {
      channel.read();
    }

  }

  final class ResponseBodyOutputStream extends OutputStream {

    @Override
    public void write(int b) {
      responseBody().writeByte(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      if (len != 0) {
        responseBody().writeBytes(b, off, len);
      }
    }

    @Override
    public void flush() {
      NettyHttpContext.this.flush();
    }

  }

  private final class NettyHttpOutputMessage implements ServerHttpResponse {

    private @Nullable MediaType contentType;

    @Override
    public void setContentType(@Nullable MediaType mediaType) {
      this.contentType = mediaType;
      NettyHttpContext.this.setContentType(mediaType);
    }

    @Override
    public void setContentLength(long length) {
      NettyHttpContext.this.setContentLength(length);
    }

    @Override
    public void setHeader(String name, @Nullable String value) {
      NettyHttpContext.this.setHeader(name, value);
    }

    @Override
    public void setHeaders(infra.http.@Nullable HttpHeaders headers) {
      NettyHttpContext.this.setHeaders(headers);
    }

    @Override
    public void addHeader(String name, @Nullable String value) {
      NettyHttpContext.this.addHeader(name, value);
    }

    @Override
    public void addHeaders(infra.http.@Nullable HttpHeaders headers) {
      NettyHttpContext.this.addHeaders(headers);
    }

    @Override
    public boolean removeHeader(String name) {
      return NettyHttpContext.this.removeHeader(name);
    }

    @Override
    public boolean containsHeader(String name) {
      return NettyHttpContext.this.containsResponseHeader(name);
    }

    @Override
    public @Nullable MediaType getContentType() {
      MediaType contentType = this.contentType;
      if (contentType == null) {
        CharSequence value = nettyResponseHeaders.get(HttpHeaderNames.CONTENT_TYPE);
        if (value != null) {
          contentType = MediaType.parseMediaType(value.toString());
          this.contentType = contentType;
        }
      }
      return contentType;
    }

    @Override
    public boolean containsHeaderValue(String headerName, String value) {
      return nettyResponseHeaders.contains(normalizeHeaderName(headerName), value, AsciiString.CASE_INSENSITIVE_HASHER);
    }

    @Override
    public void setStatusCode(HttpStatusCode status) {
      setStatus(status);
    }

    @Override
    public void flush() {
      NettyHttpContext.this.flush();
    }

    @Override
    public void close() {
      writeHeaders();
    }

    @Override
    public OutputStream getBody() throws IOException {
      return getOutputStream();
    }

    @Override
    public infra.http.HttpHeaders getHeaders() {
      return responseHeaders();
    }

    @Override
    public @Nullable String getHeader(String name) {
      return HeadersUtils.getAsString(nettyResponseHeaders, normalizeHeaderName(name));
    }

    @Override
    public List<String> getHeaders(String name) {
      return HeadersUtils.getAllAsString(nettyResponseHeaders, normalizeHeaderName(name));
    }

    @Override
    public Collection<String> getHeaderNames() {
      return HeadersUtils.namesAsString(nettyResponseHeaders);
    }

    @Override
    public @Nullable String getContentTypeAsString() {
      return NettyHttpContext.this.getResponseContentType();
    }

    @Override
    public long getContentLength() {
      return contentLength(nettyResponseHeaders);
    }

    @Override
    public void sendFile(Path file, long position, long count) throws IOException {
      Assert.notNull(file, "File is required");
      sendFile(file.toFile(), position, count);
    }

    @Override
    public void sendFile(File file, long position, long count) throws IOException {
      assertNotCommitted();
      Assert.notNull(file, "File is required");
      Assert.isTrue(position >= 0, "Position must not be negative");
      Assert.isTrue(count >= 0, "Count must not be negative");
      if (file.isFile()) {
        long fileLength = file.length();
        Assert.isTrue(position <= fileLength, "Position exceeds file length");
        Assert.isTrue(count <= fileLength - position, "Count exceeds available file bytes");
      }

      if (pendingBody instanceof ByteBuf body && body.readableBytes() > 0) {
        throw new IllegalStateException("Cannot send file: response body has already been written");
      }

      discardPendingResponse();
      // chunked framing is required when trailers were registered, so a
      // zero-copy file region cannot be used
      if (config.secure || !supportsFileRegion() || getTrailerFieldNames() != null) {
        pendingBody = createChunkedResponseFile(file, position, count);
      }
      else {
        pendingBody = new PendingFileRegion(file, position, count);
      }

      setContentLength(count);
    }

  }

  private static final class PendingFileRegion extends DefaultFileRegion {

    private final File file;

    PendingFileRegion(File file, long position, long count) {
      super(file, position, count);
      this.file = file;
    }
  }

}
