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
			ExceptionResolver exceptionResolver, //
			ResourceMappingRegistry registry, //
			@Autowired(required = false) PathMatcher pathMatcher, //
			HandlerInterceptorRegistry handlerInterceptorRegistry,
			@Autowired(required = false) ResourceResolver resourceResolver) {

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

			final String path = request.getRequestURI().substring(contextPathLength);

			final ResourceMapping resourceMapping = //
					lookupResourceHandlerMapping(path, pathMatcher, registry.getResourceHandlerMappings());

			if (resourceMapping == null) {
				log.info("resourceHandlerMapping == null 404");
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

			if (resource == null || resource.isDirectory()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} // TODO Directory
			else {
				WebResourceWriter.newInstance(resource, resourceMapping)//
						.write(request, response);
			}
		}
		catch (Throwable exception) {
			WebUtils.resolveException(request, response, //
					getServletConfig().getServletContext(), exceptionResolver, exception);
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

	public static class WebResourceWriter {

		private final WebResource resource;
		private String contentType;

		private final ResourceMapping resourceHandlerMapping;

		private WebResourceWriter(WebResource resource, ResourceMapping resourceHandlerMapping) {

			this.resource = resource;
			final String contentType = resource.getContentType();
			if (StringUtils.isEmpty(contentType)) {
				this.contentType = WebUtils.getServletContext().getMimeType(resource.getName());
				if (StringUtils.isEmpty(this.contentType)) {
					this.contentType = Constant.BLANK;
				}
			}
			else {
				this.contentType = contentType;
			}
			this.resourceHandlerMapping = resourceHandlerMapping;
		}

		static WebResourceWriter newInstance(WebResource resource, ResourceMapping resourceHandlerMapping) {
			return new WebResourceWriter(resource, resourceHandlerMapping);
		}

		/**
		 * Send the resource to the client. The methods will check the request method,
		 * if the request method is head the resource will be send without content.
		 *
		 * @param request
		 *            current request
		 * @param response
		 *            current response
		 * @throws IOException
		 *             If an input or output exception occurs
		 */
		public final void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
			prepareWrite(request, response, !isHeadRequest(request));
		}

		private static boolean isHeadRequest(HttpServletRequest request) {
			return "HEAD".equalsIgnoreCase(request.getMethod());
		}

		private void prepareWrite(final HttpServletRequest request, //
				final HttpServletResponse response, final boolean send) throws IOException //
		{
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
//			if (ifNoneMatch == null && ge(ifModifiedSince, lastModified)) {
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
			applyHeaders(request, response, lastModified, eTag);

			if (send) {
				if (isGZipEnabled(request)) {
					writeCompressed(resource, response);
				}
				else {
					write(resource, response);
				}
			}
		}

		/**
		 * Whether gZip enable
		 * 
		 * @param request
		 *            current request
		 * @return whether gZip enable
		 * @throws IOException
		 *             if any IO exception occurred
		 */
		private boolean isGZipEnabled(HttpServletRequest request) throws IOException {
			return resourceHandlerMapping.isGzip() && isContentCompressable() && isGZipNecessary();
		}

		private boolean isGZipNecessary() throws IOException {
			long contentLength = resource.contentLength();
			if (contentLength != 0 && resourceHandlerMapping.getGzipMinLength() > 0) {
				return contentLength > resourceHandlerMapping.getGzipMinLength();
			}
			return true;
		}

		private boolean isContentCompressable() {
			return "image/svg+xml".equals(contentType) //
					|| !contentType.startsWith("image") //
							&& !contentType.startsWith("video");
		}

		/**
		 * Create a {@link GZIPOutputStream} with given response
		 * 
		 * @param response
		 *            current response
		 * @return a {@link GZIPOutputStream}
		 * @throws IOException
		 *             if any IO exception occurred
		 */
		private GZIPOutputStream gzipOutputStream(HttpServletResponse response) throws IOException {
			return new GZIPOutputStream(response.getOutputStream(), resourceHandlerMapping.getBufferSize());
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
		private void writeCompressed(Resource resource, HttpServletResponse response) throws IOException {

			response.setHeader(Constant.CONTENT_ENCODING, Constant.GZIP);

			try (InputStream source = resource.getInputStream(); //
					OutputStream outputStream = gzipOutputStream(response)) {

				WebUtils.writeToOutputStream(source, outputStream, resourceHandlerMapping.getBufferSize());
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
		private void write(Resource resource, HttpServletResponse response) throws IOException {

			response.setContentLengthLong(resource.contentLength());

			try (InputStream source = resource.getInputStream(); //
					OutputStream sink = response.getOutputStream()) {

				WebUtils.writeToOutputStream(source, sink, resourceHandlerMapping.getBufferSize());
			}
		}

		private static boolean matches(String matchHeader, String etag) {
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
		private void applyHeaders(HttpServletRequest request, //
				HttpServletResponse response, long lastModified, String eTag) throws IOException //
		{
			response.setHeader(Constant.CONTENT_TYPE, contentType);
			if (lastModified > 0) {
				response.setDateHeader(Constant.LAST_MODIFIED, lastModified);
			}
			if (StringUtils.isNotEmpty(eTag)) {
				response.setHeader(Constant.ETAG, eTag);
			}
			if (resourceHandlerMapping.getCacheControl() != null) {
				response.setHeader(Constant.CACHE_CONTROL, resourceHandlerMapping.getCacheControl().toString());
			}
			if (resourceHandlerMapping.getExpires() > 0) {
				response.setDateHeader(Constant.EXPIRES, System.currentTimeMillis() + resourceHandlerMapping.getExpires());
			}
		}

	}

}
