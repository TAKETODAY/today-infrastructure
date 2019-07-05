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
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.PathMatcher;
import cn.taketoday.web.mapping.HandlerInterceptorRegistry;
import cn.taketoday.web.mapping.ResourceMapping;
import cn.taketoday.web.mapping.ResourceMappingRegistry;
import cn.taketoday.web.resolver.DefaultResourceResolver;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.resolver.ResourceResolver;
import cn.taketoday.web.resource.WebResource;
import cn.taketoday.web.utils.AntPathMatcher;
import cn.taketoday.web.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 *         2019-05-14 17:59
 * @since 2.3.7
 */
@Slf4j
@SuppressWarnings("serial")
public class ResourceServlet extends GenericServlet {

    private final int contextPathLength;
    private final PathMatcher pathMatcher;
    /** exception resolver */
    private final ExceptionResolver exceptionResolver;
    private final ResourceResolver resourceResolver;
    private final ResourceMappingRegistry registry;
    /** intercepter registry */
    private final HandlerInterceptorRegistry handlerInterceptorRegistry;

    @Autowired
    public ResourceServlet(//
            ResourceMappingRegistry registry, //
            ExceptionResolver exceptionResolver, //
            @Autowired(required = false) PathMatcher pathMatcher, //
            HandlerInterceptorRegistry handlerInterceptorRegistry,
            @Autowired(required = false) ResourceResolver resourceResolver) //
    {

        this.registry = registry;
        registry.sortResourceMappings();

        if (pathMatcher == null) {
            pathMatcher = new AntPathMatcher();
        }
        this.pathMatcher = pathMatcher;
        if (resourceResolver == null) {
            this.resourceResolver = new DefaultResourceResolver(pathMatcher);
        }
        else {
            this.resourceResolver = resourceResolver;
        }
        this.exceptionResolver = exceptionResolver;
        this.handlerInterceptorRegistry = handlerInterceptorRegistry;
        this.contextPathLength = WebUtils.getServletContext().getContextPath().length();
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

        HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        try {
            // fix
            final String path = StringUtils.decodeUrl(request.getRequestURI().substring(contextPathLength));

            final ResourceMapping resourceMapping = //
                    lookupResourceHandlerMapping(path, pathMatcher, registry.getResourceHandlerMappings());

            if (resourceMapping == null) {
                log.debug("NOT FOUND -> [{}]", path);
                response.sendError(404);
                return;
            }

            final WebResource resource;
            if (resourceMapping.hasInterceptor()) {
                final int[] interceptors = resourceMapping.getInterceptors();
                // invoke intercepter
                final HandlerInterceptorRegistry handlerInterceptorRegistry = this.handlerInterceptorRegistry;
                for (final int interceptor : interceptors) {
                    if (!handlerInterceptorRegistry.get(interceptor).beforeProcess(request, response, null)) {
                        log.debug("Resource Interceptor: [{}] return false", handlerInterceptorRegistry.get(interceptor));
                        return;
                    }
                }
                resource = resourceResolver.resolveResource(path, resourceMapping); // may be null
                for (final int interceptor : interceptors) {
                    handlerInterceptorRegistry.get(interceptor).afterProcess(resource, request, response);
                }
            }
            else {
                resource = resourceResolver.resolveResource(path, resourceMapping); // may be null
            }

            if (resource == null || resource.isDirectory()) {// TODO Directory listing
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            else {
                resolveResult(request, response, resource, resourceMapping);
            }
        }
        catch (Throwable exception) {
            WebUtils.resolveException(request, response, //
                    getServletConfig().getServletContext(), exceptionResolver, null, exception);
        }

    }

    /**
     * Looking for {@link ResourceMapping}
     * 
     * @param request
     *            current request
     * @return mapped {@link ResourceMapping}
     */
    protected ResourceMapping lookupResourceHandlerMapping(final String path, //
            final PathMatcher pathMatcher, final List<ResourceMapping> resourceMappings)//
    {
        for (final ResourceMapping resourceHandlerMapping : resourceMappings) {
            for (final String pathPattern : resourceHandlerMapping.getPathPatterns()) {
                if (pathMatcher.match(pathPattern, path)) {
                    return resourceHandlerMapping;
                }
            }
        }
        return null;
    }

    protected void resolveResult(final HttpServletRequest request, //
            final HttpServletResponse response, WebResource resource, ResourceMapping resourceMapping) throws Throwable//
    {
        String contentType = resource.getContentType();
        if (StringUtils.isEmpty(contentType)) {
            contentType = WebUtils.getServletContext().getMimeType(resource.getName());
            if (StringUtils.isEmpty(contentType)) {
                contentType = Constant.BLANK;
            }
        }
        // Validate request headers for caching
        // ---------------------------------------------------

        // If-None-Match header should contain "*" or ETag. If so, then return 304
        final String ifNoneMatch = request.getHeader(Constant.IF_NONE_MATCH);
        final String eTag = resource.getETag();
        if (matches(ifNoneMatch, eTag)) {
            response.setHeader(Constant.ETAG, eTag); // 304.
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // If-Modified-Since header should be greater than LastModified
        // If so, then return 304
        // This header is ignored if any If-None-Match header is specified
        final long ifModifiedSince = request.getDateHeader(Constant.IF_MODIFIED_SINCE);// If-Modified-Since
        final long lastModified = resource.lastModified();
        if (ifNoneMatch == null && (ifModifiedSince > 0 && lastModified != 0 && ifModifiedSince >= lastModified)) {
//      if (ifNoneMatch == null && ge(ifModifiedSince, lastModified)) {
            response.setDateHeader(Constant.LAST_MODIFIED, lastModified); // 304
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // Validate request headers for resume
        // ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412
        final String ifMatch = request.getHeader(Constant.IF_MATCH);
        if (ifMatch != null && !matches(ifMatch, eTag)) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // If-Unmodified-Since header should be greater than LastModified.
        // If not, then return 412.
        final long ifUnmodifiedSince = request.getDateHeader(Constant.IF_UNMODIFIED_SINCE);// "If-Unmodified-Since"

        if (ifUnmodifiedSince > 0 && lastModified > 0 && ifUnmodifiedSince <= lastModified) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);

        applyHeaders(response, contentType, lastModified, eTag, resourceMapping);

        if (isHeadRequest(request)) {
            return;
        }

        if (isGZipEnabled(resource, resourceMapping, contentType)) {
            writeCompressed(resource, response, resourceMapping);
        }
        else {
            write(resource, response, resourceMapping);
        }
    }

    protected static boolean isHeadRequest(HttpServletRequest request) {
        return "HEAD".equalsIgnoreCase(request.getMethod());
    }

    /**
     * Whether gZip enable
     * 
     * @param resource
     * @param resourceMapping
     * @param contentType
     * @return whether gZip enable
     * @throws IOException
     *             if any IO exception occurred
     */
    protected static final boolean isGZipEnabled(final WebResource resource, //
            final ResourceMapping resourceMapping, final String contentType) throws IOException //
    {
        return resourceMapping.isGzip() //
                && isContentCompressable(contentType) //
                && resource.contentLength() > resourceMapping.getGzipMinLength();
    }

    protected static final boolean isContentCompressable(final String contentType) {
        return "image/svg+xml".equals(contentType) //
                || !contentType.startsWith("image") //
                        && !contentType.startsWith("video");
    }

    /**
     * Write compressed {@link Resource} to the client
     * 
     * @param resource
     *            {@link Resource}
     * @param response
     *            current response
     * @throws IOException
     *             if any IO exception occurred
     */
    protected static final void writeCompressed(final Resource resource, final HttpServletResponse response, //
            final ResourceMapping resourceMapping) throws IOException //
    {
        response.setHeader(Constant.CONTENT_ENCODING, Constant.GZIP);

        final int bufferSize = resourceMapping.getBufferSize();

        try (final InputStream source = resource.getInputStream(); //
                final OutputStream outputStream = //
                        new GZIPOutputStream(response.getOutputStream(), bufferSize)) {

            WebUtils.writeToOutputStream(source, outputStream, bufferSize);
        }
    }

    /**
     * Write compressed {@link Resource} to the client
     * 
     * @param resource
     *            {@link Resource}
     * @param response
     *            current response
     * @throws IOException
     *             if any IO exception occurred
     */
    protected static final void write(final Resource resource, final HttpServletResponse response, //
            final ResourceMapping resourceMapping) throws IOException //
    {
        response.setContentLengthLong(resource.contentLength());

        try (final InputStream source = resource.getInputStream(); //
                final OutputStream sink = response.getOutputStream()) {

            WebUtils.writeToOutputStream(source, sink, resourceMapping.getBufferSize());
        }
    }

    protected static final boolean matches(final String matchHeader, final String etag) {
        if (matchHeader != null && StringUtils.isNotEmpty(etag)) {
            return "*".equals(etag) || matchHeader.equals(etag);
        }
        return false;
    }

    /**
     * Apply the Content-Type, Last-Modified, ETag, Cache-Control, Expires
     * 
     * @param request
     *            current request
     * @param response
     *            current response
     * @throws IOException
     *             if last modify read error
     */
    protected static final void applyHeaders(final HttpServletResponse response, //
            final String contentType, final long lastModified, //
            final String eTag, final ResourceMapping resourceMapping) throws IOException //
    {
        response.setHeader(Constant.CONTENT_TYPE, contentType);
        if (lastModified > 0) {
            response.setDateHeader(Constant.LAST_MODIFIED, lastModified);
        }
        if (StringUtils.isNotEmpty(eTag)) {
            response.setHeader(Constant.ETAG, eTag);
        }
        if (resourceMapping.getCacheControl() != null) {
            response.setHeader(Constant.CACHE_CONTROL, resourceMapping.getCacheControl().toString());
        }
        if (resourceMapping.getExpires() > 0) {
            response.setDateHeader(Constant.EXPIRES, System.currentTimeMillis() + resourceMapping.getExpires());
        }
    }

}
