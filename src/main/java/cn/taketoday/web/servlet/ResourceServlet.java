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
package cn.taketoday.web.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.AntPathMatcher;
import cn.taketoday.context.PathMatcher;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.mapping.ResourceHandlerRegistry;
import cn.taketoday.web.mapping.ResourceMapping;
import cn.taketoday.web.mapping.ResourceMappingMatchResult;
import cn.taketoday.web.resolver.DefaultResourceResolver;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.resolver.WebResourceResolver;
import cn.taketoday.web.resource.CacheControl;
import cn.taketoday.web.resource.WebResource;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-05-14 17:59
 * @since 2.3.7
 */
@Deprecated
@SuppressWarnings("serial")
public class ResourceServlet extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(ResourceServlet.class);

    private final int contextPathLength;
    private final PathMatcher pathMatcher;
    private final ResourceHandlerRegistry registry;
    private final WebResourceResolver resourceResolver;
    /** exception resolver */
    private final ExceptionResolver exceptionResolver;

    /** @off*/
    @Autowired
    public ResourceServlet( ResourceHandlerRegistry registry,
                            ExceptionResolver exceptionResolver,
                            WebApplicationContext applicationContext,
                            @Autowired(required = false) PathMatcher pathMatcher,
                            @Autowired(required = false) WebResourceResolver resourceResolver) //@on
    {
        this.exceptionResolver = exceptionResolver;
        this.registry = registry;
        this.contextPathLength = applicationContext.getContextPath().length();

        this.pathMatcher = pathMatcher != null ? pathMatcher : new AntPathMatcher();
        this.resourceResolver = resourceResolver != null ? resourceResolver : new DefaultResourceResolver();
    }

    @Override
    public void service(final ServletRequest req, final ServletResponse res) throws ServletException, IOException {
        service((HttpServletRequest) req, (HttpServletResponse) res);
    }

    public final void service(final HttpServletRequest req,
                              final HttpServletResponse res) throws ServletException, IOException {

        final String path = requestPath(req, contextPathLength);

        final ResourceMappingMatchResult matchResult = //
                lookupResourceMapping(path, pathMatcher, registry.getResourceMappings());

        if (matchResult == null) {
            res.sendError(404);
            return;
        }

        final ResourceMapping mapping = matchResult.getResourceMapping();
        final RequestContext requestContext = DispatcherServlet.prepareContext(req, res);

        try {

            final WebResource resource;
            if (mapping.hasInterceptor()) {
                final HandlerInterceptor[] interceptors = mapping.getInterceptors();
                // invoke intercepter
                for (final HandlerInterceptor interceptor : interceptors) {
                    if (!interceptor.beforeProcess(requestContext, mapping)) {
                        log.debug("Resource Interceptor: [{}] return false", interceptor);
                        return;
                    }
                }
                resource = resourceResolver.resolveResource(matchResult); // may be null
                for (final HandlerInterceptor interceptor : interceptors) {
                    interceptor.afterProcess(requestContext, mapping, resource);
                }
            }
            else {
                resource = resourceResolver.resolveResource(matchResult); // may be null
            }

            if (resource == null || resource.isDirectory()) {// TODO Directory listing
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            else {
                resolveResult(requestContext, resource, mapping);
            }
        }
        catch (Throwable exception) {
            try {
                WebUtils.resolveException(mapping, exception, requestContext, exceptionResolver);
            }
            catch (Throwable e1) {
                throw new ServletException(e1);
            }
        }
    }

    /**
     * Get request path
     * 
     * @param req
     *            Current {@link HttpServletRequest}
     * @param length
     *            context path length
     * @return Decoded request path
     */
    protected String requestPath(final HttpServletRequest req, final int length) {
        if (length == 0) {
            return StringUtils.decodeUrl(req.getRequestURI());
        }
        return StringUtils.decodeUrl(req.getRequestURI().substring(length));
    }

    /**
     * Looking for {@link ResourceMapping}
     * 
     * @param path
     *            Request path
     * @param pathMatcher
     *            {@link PathMatcher}
     * @param resourceMappings
     *            All mappings
     * @return Mapped {@link ResourceMapping}
     */
    protected ResourceMappingMatchResult lookupResourceMapping(final String path,
                                                               final PathMatcher pathMatcher,
                                                               final List<ResourceMapping> resourceMappings) //
    {
        for (final ResourceMapping resourceMapping : resourceMappings) {
            for (final String pathPattern : resourceMapping.getPathPatterns()) {
                if (pathMatcher.match(pathPattern, path)) {
                    return new ResourceMappingMatchResult(path, pathPattern, pathMatcher, resourceMapping);
                }
            }
        }
        log.debug("NOT FOUND -> [{}]", path);
        return null;
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
    protected void resolveResult(final RequestContext context, //
                                 final WebResource resource,
                                 final ResourceMapping resourceMapping) throws IOException//
    {
        final String contentType = getContentType(resource);
        context.contentType(contentType);

        final String eTag = resource.getETag();
        final long lastModified = resource.lastModified();

        // lastModified
        if (WebUtils.checkNotModified(eTag, lastModified, context)) {
            return;
        }

        context.status(HttpServletResponse.SC_OK);

        applyHeaders(context, lastModified, eTag, resourceMapping);

        if (isHeadRequest(context)) {
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
            contentType = getServletConfig().getServletContext().getMimeType(resource.getName());
            if (StringUtils.isEmpty(contentType)) {
                contentType = Constant.BLANK;
            }
        }
        return contentType;
    }

    protected static boolean isHeadRequest(RequestContext requestContext) {
        return "HEAD".equalsIgnoreCase(requestContext.method());
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

    public final String getServletName() {
        return "ResourceServlet";
    }

    @Override
    public String getServletInfo() {
        return "ResourceServlet, Copyright © TODAY & 2017 - 2019 All Rights Reserved";
    }

    public final WebResourceResolver getResourceResolver() {
        return resourceResolver;
    }
}
