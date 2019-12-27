/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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

import static cn.taketoday.context.exception.ConfigurationException.nonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.resolver.WebResourceResolver;
import cn.taketoday.web.resource.CacheControl;
import cn.taketoday.web.resource.WebResource;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-12-25 16:12
 */
public class ResourceRequestHandler extends InterceptableRequestHandler {

    private final ResourceMapping mapping;
    private final WebResourceResolver resourceResolver;

    public ResourceRequestHandler(ResourceMapping mapping, WebResourceResolver resourceResolver) {
        this.mapping = nonNull(mapping, "resource metadata must not be null");
        this.resourceResolver = nonNull(resourceResolver, "resource resolver must not be null");
    }

    @Override
    public Object handleRequest(RequestContext context) throws Throwable {
        final WebResource resource = (WebResource) super.handleRequest(context);

        if (resource == null || resource.isDirectory()) {// TODO Directory listing
            context.sendError(404);
        }
        else {
            handleResult(context, resource);
        }
        return null;
    }

    @Override
    public HandlerInterceptor[] getInterceptors() {
        return getMapping().getInterceptors();
    }

    @Override
    protected Object handleInternal(RequestContext context) throws Throwable {

        final ResourceMappingMatchResult matchResult = //
                (ResourceMappingMatchResult) context.attribute(Constant.RESOURCE_MAPPING_MATCH_RESULT);

        return resourceResolver.resolveResource(matchResult);
    }

    /**
     * Handling resource result to client
     * 
     * @param context
     *            Current request context
     * @param resource
     *            {@link Resource}
     * @param resourceMapping
     *            {@link ResourceMapping}
     * @throws IOException
     *             If an input or output exception occurs
     */
    protected void handleResult(final RequestContext context, final WebResource resource) throws IOException {
        final String contentType = getContentType(resource);
        context.contentType(contentType);

        final String eTag = resource.getETag();
        final long lastModified = resource.lastModified();

        // lastModified
        if (WebUtils.checkNotModified(eTag, lastModified, context)) {
            return;
        }

        context.status(200);

        final ResourceMapping resourceMapping = getMapping();
        applyHeaders(context, lastModified, eTag, resourceMapping);

        if (WebUtils.isHeadRequest(context)) {
            return;
        }

        if (isGZipEnabled(resource, resourceMapping, contentType)) {
            writeCompressed(resource, context, resourceMapping);
        }
        else {
            write(resource, context, resourceMapping);
        }
    }

    protected String getContentType(final WebResource resource) {

        String contentType = resource.getContentType();
        if (StringUtils.isEmpty(contentType)) {
            contentType = getContentTypeInternal(resource);
            if (StringUtils.isEmpty(contentType)) {
                contentType = Constant.BLANK;
            }
        }
        return contentType;
    }

    private String getContentTypeInternal(WebResource resource) {
        return null;
    }

    /**
     * Whether gZip enable
     * 
     * @param resource
     * @param mapping
     * @param contentType
     * @return whether gZip enable
     * @throws IOException
     *             If any IO exception occurred
     */
    protected boolean isGZipEnabled(final WebResource resource,
                                    final ResourceMapping mapping, final String contentType) throws IOException //
    {
        return mapping.isGzip()
               && isContentCompressable(contentType)
               && resource.contentLength() > mapping.getGzipMinLength();
    }

    protected boolean isContentCompressable(final String contentType) {
        return "image/svg+xml".equals(contentType)
               || !contentType.startsWith("image")
                  && !contentType.startsWith("video");
    }

    /**
     * Write compressed {@link Resource} to the client
     * 
     * @param resource
     *            {@link Resource}
     * @param requestContext
     *            Current request context
     * @throws IOException
     *             If any IO exception occurred
     */
    protected void writeCompressed(final Resource resource,
                                   final RequestContext requestContext, //
                                   final ResourceMapping resourceMapping) throws IOException //
    {
        requestContext.responseHeader(Constant.CONTENT_ENCODING, Constant.GZIP);

        final int bufferSize = resourceMapping.getBufferSize();

        try (final InputStream source = resource.getInputStream()) {

            // ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
            // GZIPOutputStream gzip = new GZIPOutputStream(baos);
            // WebUtils.writeToOutputStream(source, gzip, bufferSize);
            // final byte[] byteArray = baos.toByteArray();
            // requestContext.contentLength(byteArray.length);
            // baos.writeTo(requestContext.getOutputStream());

            WebUtils.writeToOutputStream(source,
                                         new GZIPOutputStream(requestContext.getOutputStream(), bufferSize), bufferSize);
        }
    }

    /**
     * Write compressed {@link Resource} to the client
     * 
     * @param resource
     *            {@link Resource}
     * @param context
     *            Current request context
     * @throws IOException
     *             If any IO exception occurred
     */
    protected void write(final Resource resource,
                         final RequestContext context,
                         final ResourceMapping resourceMapping) throws IOException //
    {
        context.contentLength(resource.contentLength());

        try (final InputStream source = resource.getInputStream()) {
            WebUtils.writeToOutputStream(source, context.getOutputStream(), resourceMapping.getBufferSize());
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
     * 
     * @param context
     *            Current request context
     * @throws IOException
     *             If last modify read error
     */
    protected void applyHeaders(final RequestContext context,
                                final long lastModified,
                                final String eTag,
                                final ResourceMapping resourceMapping) throws IOException //
    {
        if (lastModified > 0) {
            context.responseDateHeader(Constant.LAST_MODIFIED, lastModified);
        }
        if (StringUtils.isNotEmpty(eTag)) {
            context.responseHeader(Constant.ETAG, eTag);
        }
        final CacheControl cacheControl = resourceMapping.getCacheControl();
        if (cacheControl != null) {
            context.responseHeader(Constant.CACHE_CONTROL, cacheControl.toString());
        }
        if (resourceMapping.getExpires() > 0) {
            context.responseDateHeader(Constant.EXPIRES, System.currentTimeMillis() + resourceMapping.getExpires());
        }
    }

    public ResourceMapping getMapping() {
        return mapping;
    }
}
