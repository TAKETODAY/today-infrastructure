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

package cn.taketoday.web.servlet.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.DigestUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.servlet.ContentCachingResponseWrapper;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.view.View;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link jakarta.servlet.Filter} that generates an {@code ETag} value based on the
 * content on the response. This ETag is compared to the {@code If-None-Match}
 * header of the request. If these headers are equal, the response content is
 * not sent, but rather a {@code 304 "Not Modified"} status instead.
 *
 * <p>Since the ETag is based on the response content, the response
 * (e.g. a {@link View}) is still rendered.
 * As such, this filter only saves bandwidth, not server performance.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/9 12:10
 */
public class ShallowEtagHeaderFilter extends OncePerRequestFilter {

  private static final String DIRECTIVE_NO_STORE = "no-store";

  private static final String STREAMING_ATTRIBUTE = ShallowEtagHeaderFilter.class.getName() + ".STREAMING";

  private boolean writeWeakETag = false;

  /**
   * Set whether the ETag value written to the response should be weak, as per RFC 7232.
   * <p>Should be configured using an {@code <init-param>} for parameter name
   * "writeWeakETag" in the filter definition in {@code web.xml}.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">RFC 7232 section 2.3</a>
   */
  public void setWriteWeakETag(boolean writeWeakETag) {
    this.writeWeakETag = writeWeakETag;
  }

  /**
   * Return whether the ETag value written to the response should be weak, as per RFC 7232.
   */
  public boolean isWriteWeakETag() {
    return this.writeWeakETag;
  }

  /**
   * The default value is {@code false} so that the filter may delay the generation
   * of an ETag until the last asynchronously dispatched thread.
   */
  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {

    HttpServletResponse responseToUse = response;
    if (!isAsyncDispatch(request) && !(response instanceof ConditionalContentCachingResponseWrapper)) {
      responseToUse = new ConditionalContentCachingResponseWrapper(response, request);
    }

    filterChain.doFilter(request, responseToUse);

    if (!isAsyncStarted(request) && !isContentCachingDisabled(request)) {
      updateResponse(request, responseToUse);
    }
  }

  private void updateResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ConditionalContentCachingResponseWrapper wrapper =
            ServletUtils.getNativeResponse(response, ConditionalContentCachingResponseWrapper.class);
    Assert.notNull(wrapper, "ContentCachingResponseWrapper not found");
    HttpServletResponse rawResponse = (HttpServletResponse) wrapper.getResponse();

    if (isEligibleForEtag(request, wrapper, wrapper.getStatus(), wrapper.getContentInputStream())) {
      String eTag = wrapper.getHeader(HttpHeaders.ETAG);
      if (!StringUtils.hasText(eTag)) {
        eTag = generateETagHeaderValue(wrapper.getContentInputStream(), this.writeWeakETag);
        rawResponse.setHeader(HttpHeaders.ETAG, eTag);
      }
      if (new ServletRequestContext(null, request, rawResponse).checkNotModified(eTag)) {
        return;
      }
    }

    wrapper.copyBodyToResponse();
  }

  /**
   * Whether an ETag should be calculated for the given request and response
   * exchange. By default this is {@code true} if all of the following match:
   * <ul>
   * <li>Response is not committed.</li>
   * <li>Response status codes is in the {@code 2xx} series.</li>
   * <li>Request method is a GET.</li>
   * <li>Response Cache-Control header does not contain "no-store" (or is not present at all).</li>
   * </ul>
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param responseStatusCode the HTTP response status code
   * @param inputStream the response body
   * @return {@code true} if eligible for ETag generation, {@code false} otherwise
   */
  protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response,
          int responseStatusCode, InputStream inputStream) {

    if (!response.isCommitted()
            && responseStatusCode >= 200 && responseStatusCode < 300
            && HttpMethod.GET.matches(request.getMethod())) {

      String cacheControl = response.getHeader(HttpHeaders.CACHE_CONTROL);
      return (cacheControl == null || !cacheControl.contains(DIRECTIVE_NO_STORE));
    }

    return false;
  }

  /**
   * Generate the ETag header value from the given response body byte array.
   * <p>The default implementation generates an MD5 hash.
   *
   * @param inputStream the response body as an InputStream
   * @param isWeak whether the generated ETag should be weak
   * @return the ETag header value
   * @see DigestUtils
   */
  protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException {
    // length of W/ + " + 0 + 32bits md5 hash + "
    StringBuilder builder = new StringBuilder(37);
    if (isWeak) {
      builder.append("W/");
    }
    builder.append("\"0");
    DigestUtils.appendMd5DigestAsHex(inputStream, builder);
    builder.append('"');
    return builder.toString();
  }

  /**
   * This method can be used to suppress the content caching response wrapper
   * of the ShallowEtagHeaderFilter. The main reason for this is streaming
   * scenarios which are not to be cached and do not need an eTag.
   * <p><strong>Note:</strong> This method must be called before the response
   * is written to in order for the entire response content to be written
   * without caching.
   */
  public static void disableContentCaching(ServletRequest request) {
    Assert.notNull(request, "ServletRequest must not be null");
    request.setAttribute(STREAMING_ATTRIBUTE, true);
  }

  private static boolean isContentCachingDisabled(HttpServletRequest request) {
    return request.getAttribute(STREAMING_ATTRIBUTE) != null;
  }

  /**
   * Returns the raw OutputStream, instead of the one that does caching,
   * if {@link #isContentCachingDisabled}.
   */
  private static class ConditionalContentCachingResponseWrapper extends ContentCachingResponseWrapper {

    private final HttpServletRequest request;

    ConditionalContentCachingResponseWrapper(HttpServletResponse response, HttpServletRequest request) {
      super(response);
      this.request = request;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      return isContentCachingDisabled(request) || hasETag()
             ? getResponse().getOutputStream() : super.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      return isContentCachingDisabled(this.request) || hasETag()
             ? getResponse().getWriter() : super.getWriter();
    }

    private boolean hasETag() {
      return StringUtils.hasText(getHeader(HttpHeaders.ETAG));
    }
  }

}
