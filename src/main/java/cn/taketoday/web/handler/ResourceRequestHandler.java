/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.web.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import cn.taketoday.lang.Assert;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebUtils;
import cn.taketoday.web.http.CacheControl;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.http.HttpStatus;
import cn.taketoday.web.http.NotFoundException;
import cn.taketoday.web.http.ResourceNotFoundException;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.resource.WebResource;
import cn.taketoday.web.resource.WebResourceResolver;

/**
 * @author TODAY 2019-12-25 16:12
 */
public class ResourceRequestHandler extends InterceptableRequestHandler {

  private final ResourceMapping mapping;
  private final WebResourceResolver resourceResolver;

  public ResourceRequestHandler(ResourceMapping mapping, WebResourceResolver resourceResolver) {
    Assert.notNull(mapping, "resource metadata must not be null");
    Assert.notNull(resourceResolver, "resource resolver must not be null");
    this.mapping = mapping;
    this.resourceResolver = resourceResolver;
  }

  @Override
  public Object handleRequest(final RequestContext context) throws Throwable {
    final Object ret = super.handleRequest(context);
    if (ret == null) {
      final ResourceMatchResult matchResult = getResourceMatchResult(context);
      throw ResourceNotFoundException.notFound(matchResult);
    }
    else if (ret instanceof WebResource) {
      final WebResource resource = (WebResource) ret;
      if (resource.isDirectory()) {// TODO Directory listing
        throw ResourceNotFoundException.notFound();
      }
      else {
        handleResult(context, resource);
      }
    }
    return HandlerAdapter.NONE_RETURN_VALUE;
  }

  private ResourceMatchResult getResourceMatchResult(RequestContext context) {
    final Object attribute = context.getAttribute(ResourceMatchResult.RESOURCE_MATCH_RESULT);
    if (attribute == null) {
      throw new NotFoundException("Resource Not Found");
    }
    return (ResourceMatchResult) attribute;
  }

  @Override
  public HandlerInterceptor[] getInterceptors() {
    return getMapping().getInterceptors();
  }

  @Override
  protected Object handleInternal(final RequestContext context) {
    return resourceResolver.resolveResource((ResourceMatchResult) context.getAttribute(ResourceMatchResult.RESOURCE_MATCH_RESULT));
  }

  /**
   * Handling resource result to client
   *
   * @param context
   *         Current request context
   * @param resource
   *         {@link Resource}
   *
   * @throws IOException
   *         If an input or output exception occurs
   */
  protected void handleResult(final RequestContext context, final WebResource resource) throws IOException {
    final String contentType = getContentType(resource);

    if (StringUtils.isNotEmpty(contentType)) {
      context.setContentType(contentType);
    }

    final String eTag = resource.getETag();
    final long lastModified = resource.lastModified();

    // lastModified
    if (WebUtils.checkNotModified(eTag, lastModified, context)) {
      return;
    }

    context.setStatus(HttpStatus.OK);

    final ResourceMapping resourceMapping = getMapping();
    applyHeaders(context.responseHeaders(), lastModified, eTag, resourceMapping);

    if (WebUtils.isHeadRequest(context)) {
      return;
    }

    write(resource, context, resourceMapping);
    context.flush();
  }

  protected String getContentType(final WebResource resource) {
    String contentType = resource.getContentType();
    if (StringUtils.isEmpty(contentType)) {
      contentType = getContentTypeInternal(resource);
    }
    return contentType;
  }

  private String getContentTypeInternal(WebResource resource) {
    return null;
  }

  /**
   * Write compressed {@link Resource} to the client
   *
   * @param resource
   *         {@link Resource}
   * @param requestContext
   *         Current request context
   *
   * @throws IOException
   *         If any IO exception occurred
   */
  protected void writeCompressed(final Resource resource,
                                 final RequestContext requestContext, //
                                 final ResourceMapping resourceMapping) throws IOException //
  {
    final HttpHeaders requestHeaders = requestContext.requestHeaders();
    requestHeaders.set(HttpHeaders.CONTENT_ENCODING, HttpHeaders.GZIP);

    final int bufferSize = resourceMapping.getBufferSize();

    try (final InputStream source = resource.getInputStream()) {

      // ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
      // GZIPOutputStream gzip = new GZIPOutputStream(baos);
      // WebUtils.writeToOutputStream(source, gzip, bufferSize);
      // final byte[] byteArray = baos.toByteArray();
      // requestContext.contentLength(byteArray.length);
      // baos.writeTo(requestContext.getOutputStream());

      StreamUtils.copy(
              source, new GZIPOutputStream(requestContext.getOutputStream(), bufferSize), bufferSize);
    }
  }

  /**
   * Write compressed {@link Resource} to the client
   *
   * @param resource
   *         {@link Resource}
   * @param context
   *         Current request context
   *
   * @throws IOException
   *         If any IO exception occurred
   */
  protected void write(final Resource resource,
                       final RequestContext context,
                       final ResourceMapping resourceMapping) throws IOException //
  {
    context.setContentLength(resource.contentLength());

    try (final InputStream source = resource.getInputStream()) {
      StreamUtils.copy(source, context.getOutputStream(), resourceMapping.getBufferSize());
    }
  }

  protected boolean matches(final String matchHeader, final String etag) {
    if (matchHeader != null && StringUtils.isNotEmpty(etag)) {
      return "*".equals(etag) || matchHeader.equals(etag);
    }
    return false;
  }

  /**
   * Apply the Content-Type, Last-Modified, ETag, Cache-Control, Expires
   */
  protected void applyHeaders(
          HttpHeaders responseHeaders,
          final long lastModified,
          final String eTag, final ResourceMapping resourceMapping)  //
  {
    if (lastModified > 0) {
      responseHeaders.setLastModified(lastModified);
    }
    if (StringUtils.isNotEmpty(eTag)) {
      responseHeaders.setETag(eTag);
    }
    final CacheControl cacheControl = resourceMapping.getCacheControl();
    if (cacheControl != null) {
      responseHeaders.setCacheControl(cacheControl);
    }
    if (resourceMapping.getExpires() > 0) {
      responseHeaders.setExpires(System.currentTimeMillis() + resourceMapping.getExpires());
    }
  }

  public ResourceMapping getMapping() {
    return mapping;
  }

  @Override
  public String toString() {
    return mapping.toString();
  }
}
