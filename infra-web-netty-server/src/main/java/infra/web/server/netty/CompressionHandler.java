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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import infra.http.InvalidMediaTypeException;
import infra.http.MediaType;
import infra.web.server.Compression;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * Netty HTTP/1 response compressor configured from {@link Compression}.
 * <p>
 * This handler is created per channel because it keeps request-specific state
 * such as the current user agent and whether the request method is {@code HEAD}.
 * Requests are tracked in order so pipelined and asynchronous responses are
 * compressed according to the request they belong to.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class CompressionHandler extends HttpContentCompressor {

  private final Compression compression;

  private final Deque<RequestState> pendingRequests = new ArrayDeque<>();

  @Nullable
  private RequestState currentRequest;

  CompressionHandler(Compression compression) {
    this.compression = compression;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, HttpRequest msg, List<Object> out) throws Exception {
    pendingRequests.addLast(new RequestState(
            HttpMethod.HEAD.equals(msg.method()),
            msg.headers().get(HttpHeaderNames.USER_AGENT)));
    super.decode(ctx, msg, out);
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
    if (msg instanceof HttpResponse response) {
      int code = response.status().code();
      // Interim 1xx responses do not complete a request; consume the request
      // state only for final responses so pipelined requests stay aligned.
      if (code == 101 || code >= 200) {
        this.currentRequest = pendingRequests.pollFirst();
      }
    }
    super.encode(ctx, msg, out);
  }

  @Override
  protected Result beginEncode(HttpResponse response, String acceptEncoding) throws Exception {
    RequestState request = this.currentRequest;
    if (request == null) {
      request = RequestState.EMPTY;
    }

    if (request.headRequest
            || response instanceof NoCompressionResponse
            || !isCompressible(response)) {
      return null;
    }

    String[] excludedUserAgents = compression.getExcludedUserAgents();
    if (excludedUserAgents != null && excludedUserAgents.length > 0) {
      appendVary(response.headers(), HttpHeaderNames.USER_AGENT);
      if (isExcludedUserAgent(excludedUserAgents, request.userAgent)) {
        return null;
      }
    }

    // The representation varies by Accept-Encoding even when the current
    // request does not accept a supported encoding.
    appendVary(response.headers(), HttpHeaderNames.ACCEPT_ENCODING);
    return super.beginEncode(response, acceptEncoding);
  }

  private boolean isExcludedUserAgent(String[] excludedUserAgents, @Nullable String userAgent) {
    if (userAgent == null) {
      return false;
    }
    for (String excludedUserAgent : excludedUserAgents) {
      if (excludedUserAgent != null && excludedUserAgent.equalsIgnoreCase(userAgent)) {
        return true;
      }
    }
    return false;
  }

  private boolean isCompressible(HttpResponse response) {
    int code = response.status().code();
    if (code < 200 || code == 204 || code == 304 || code == 206) {
      return false;
    }

    HttpHeaders headers = response.headers();
    if (headers.contains(HttpHeaderNames.CONTENT_ENCODING) || headers.contains(HttpHeaderNames.CONTENT_RANGE)) {
      return false;
    }
    if (hasNoTransform(headers)) {
      return false;
    }

    String contentLength = headers.get(HttpHeaderNames.CONTENT_LENGTH);
    if (contentLength != null) {
      try {
        long length = Long.parseLong(contentLength);
        if (length == 0) {
          return false;
        }
        long minResponseSize = compression.getMinResponseSize().toBytes();
        if (!compression.getMinResponseSize().isNegative() && length < minResponseSize) {
          return false;
        }
      }
      catch (NumberFormatException ex) {
        return false;
      }
    }

    String contentType = headers.get(HttpHeaderNames.CONTENT_TYPE);
    if (contentType == null) {
      return false;
    }

    MediaType contentMimeType;
    try {
      contentMimeType = MediaType.parseMediaType(contentType);
    }
    catch (InvalidMediaTypeException ex) {
      return false;
    }

    if (MediaType.TEXT_EVENT_STREAM.isCompatibleWith(contentMimeType)) {
      return false;
    }

    String[] mimeTypes = compression.getMimeTypes();
    if (mimeTypes == null || mimeTypes.length == 0) {
      return true;
    }
    for (String mimeType : mimeTypes) {
      try {
        if (MediaType.parseMediaType(mimeType).isCompatibleWith(contentMimeType)) {
          return true;
        }
      }
      catch (InvalidMediaTypeException ignored) {
      }
    }
    return false;
  }

  private static void appendVary(HttpHeaders headers, CharSequence value) {
    for (String vary : headers.getAll(HttpHeaderNames.VARY)) {
      for (String token : vary.split(",")) {
        String candidate = token.trim();
        if ("*".equals(candidate) || value.toString().equalsIgnoreCase(candidate)) {
          return;
        }
      }
    }
    headers.add(HttpHeaderNames.VARY, value);
  }

  private static boolean hasNoTransform(HttpHeaders headers) {
    for (String cacheControl : headers.getAll(HttpHeaderNames.CACHE_CONTROL)) {
      for (String token : cacheControl.split(",")) {
        if ("no-transform".equalsIgnoreCase(token.trim())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * A response that must not be compressed, used for zero-copy and chunked file
   * transfers which bypass the {@code HttpContent} pipeline.
   */
  static final class NoCompressionResponse extends DefaultHttpResponse {

    NoCompressionResponse(HttpVersion version, HttpResponseStatus status, HttpHeaders headers) {
      super(version, status, headers);
    }

  }

  private record RequestState(boolean headRequest, @Nullable String userAgent) {

    static final RequestState EMPTY = new RequestState(false, null);

  }

}
