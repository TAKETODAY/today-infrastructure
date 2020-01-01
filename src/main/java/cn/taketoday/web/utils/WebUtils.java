/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.NumberUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.HttpHeaders;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.exception.AccessForbiddenException;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.exception.FileSizeExceededException;
import cn.taketoday.web.exception.MethodNotAllowedException;
import cn.taketoday.web.exception.NotFoundException;
import cn.taketoday.web.exception.UnauthorizedException;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.validation.ValidationException;

/**
 * @author TODAY <br>
 *         2019-03-15 19:53
 * @since 2.3.7
 */
public abstract class WebUtils {

    private static WebApplicationContext applicationContext;

    /**
     * Get {@link WebApplicationContext}
     * 
     * @return WebApplicationContext
     */
    public final static WebApplicationContext getWebApplicationContext() {
        return applicationContext;
    }

    public static void setWebApplicationContext(WebApplicationContext applicationContext) {
        WebUtils.applicationContext = applicationContext;
    }

    /**
     * @param type
     *            type
     * @param methodParameterName
     *            parameter name
     */
    public final static BadRequestException newBadRequest(String type, MethodParameter parameter, Throwable ex) {
        return newBadRequest(type, parameter.getName(), ex);
    }

    /**
     * @param type
     *            type
     * @param methodParameterName
     *            parameter name
     */
    public final static BadRequestException newBadRequest(String type, String methodParameterName, Throwable ex) {
        StringBuilder msg = new StringBuilder(64);

        if (StringUtils.isNotEmpty(type)) {
            msg.append(type);
        }
        else {
            msg.append("Parameter");
        }

        msg.append(": [").append(methodParameterName).append("] is required and it can't be resolve, bad request.");

        return new BadRequestException(msg.toString(), ex);
    }

    /**
     * Write to {@link OutputStream}
     * 
     * @param source
     *            {@link InputStream}
     * @param out
     *            {@link OutputStream}
     * @param bufferSize
     *            buffer size
     * @throws IOException
     *             if any IO exception occurred
     */
    public static void writeToOutputStream(final InputStream source,
                                           final OutputStream out, final int bufferSize) throws IOException //
    {
        final byte[] buff = new byte[bufferSize];
        int len = 0;
        while ((len = source.read(buff)) != -1) {
            out.write(buff, 0, len);
        }
    }

    /**
     * Resolves the content type of the file.
     *
     * @param filename
     *            name of file or path
     * @return file content type
     * @since 2.3.7
     */
    public static String resolveFileContentType(String filename) {
        return URLConnection.getFileNameMap().getContentTypeFor(filename);
    }

    public static String getEtag(String name, long size, long lastModifid) {
        return new StringBuilder()
                .append(name)
                .append(Constant.PATH_SEPARATOR)
                .append(size)
                .append(Constant.PATH_SEPARATOR)
                .append(lastModifid).toString();
    }

    // ---
    public static boolean isMultipart(final RequestContext requestContext) {

        if (!"POST".equals(requestContext.method())) {
            return false;
        }
        final String contentType = requestContext.contentType();
        return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
    }

    /**
     * Is ajax request
     */
    public static boolean isAjax(HttpHeaders request) {
        return Constant.XML_HTTP_REQUEST.equals(request.requestHeader(Constant.X_REQUESTED_WITH));
    }

    public static boolean isHeadRequest(RequestContext requestContext) {
        return "HEAD".equalsIgnoreCase(requestContext.method());
    }

    public static void resolveException(final Object handler,
                                        final Throwable exception,
                                        final RequestContext context,
                                        final ExceptionResolver resolver) throws Throwable //
    {
        resolver.resolveException(context, ExceptionUtils.unwrapThrowable(exception), handler);
    }

    /**
     * Download file to client.
     *
     * @param request
     *            Current request context
     * @param download
     *            {@link Resource} to download
     * @param bufferSize
     *            Download buffer size
     * @since 2.1.x
     */
    public static void downloadFile(final RequestContext context,
                                    final Resource download, final int bufferSize) throws IOException //
    {
        context.contentLength(download.contentLength());
        context.contentType(Constant.APPLICATION_FORCE_DOWNLOAD);

        context.responseHeader(Constant.CONTENT_TRANSFER_ENCODING, Constant.BINARY);
        context.responseHeader(Constant.CONTENT_DISPOSITION, new StringBuilder(Constant.ATTACHMENT_FILE_NAME)//
                .append(StringUtils.encodeUrl(download.getName()))//
                .append(Constant.QUOTATION_MARKS)//
                .toString()//
        );

        try (final InputStream in = download.getInputStream()) {

            writeToOutputStream(in, context.getOutputStream(), bufferSize);
        }
    }

    public static int getStatus(final Throwable ex) {
        if (ex instanceof MethodNotAllowedException) {
            return 405;
        }
        else if (ex instanceof BadRequestException
                 || ex instanceof ValidationException
                 || ex instanceof ConversionException
                 || ex instanceof FileSizeExceededException) //
        {
            return 400;
        }
        else if (ex instanceof NotFoundException) {
            return 404;
        }
        else if (ex instanceof UnauthorizedException) {
            return 401;
        }
        else if (ex instanceof AccessForbiddenException) {
            return 403;
        }
        else {
            final ResponseStatus status = ClassUtils.getAnnotation(ex, ResponseStatus.class);
            if (status != null) {
                return status.value();
            }
        }
        return 500;
    }

    public static ResponseStatus getStatus(final HandlerMethod handler) {

        Objects.requireNonNull(handler, "handler method must not be null");
        ResponseStatus status = handler.getMethodAnnotation(ResponseStatus.class);
        if (status == null) {
            status = handler.getDeclaringClassAnnotation(ResponseStatus.class);
        }
        return status;
    }

    // Utility class for CORS request handling based on the 
    // CORS W3C recommendation: https://www.w3.org/TR/cors
    // -----------------------------------------------------

    /**
     * Returns {@code true} if the request is a valid CORS one by checking
     * {@code Origin} header presence and ensuring that origins are different.
     */
    public static boolean isCorsRequest(final RequestContext request) {
        return request.requestHeader(Constant.ORIGIN) != null;
    }

    /**
     * Returns {@code true} if the request is a valid CORS pre-flight one. To be
     * used in combination with {@link #isCorsRequest(RequestContext)} since regular
     * CORS checks are not invoked here for performance reasons.
     */
    public static boolean isPreFlightRequest(final RequestContext request) {
        return RequestMethod.OPTIONS.name().equals(request.method())
               && request.requestHeader(Constant.ACCESS_CONTROL_REQUEST_METHOD) != null;
    }

    // checkNotModified
    // ---------------------------------------------

    public static boolean checkNotModified(long lastModifiedTimestamp, final RequestContext context) throws IOException {
        return checkNotModified(null, lastModifiedTimestamp, context);
    }

    public static boolean checkNotModified(String etag, final RequestContext context) throws IOException {
        return checkNotModified(etag, -1, context);
    }

    protected static boolean matches(final String matchHeader, final String etag) {
        if (matchHeader != null && StringUtils.isNotEmpty(etag)) {
            return "*".equals(etag) || matchHeader.equals(etag);
        }
        return false;
    }

    public static boolean checkNotModified(final String eTag,
                                           final long lastModified,
                                           final RequestContext context) throws IOException {

        // Validate request headers for caching
        // ---------------------------------------------------

        // If-None-Match header should contain "*" or ETag. If so, then return 304
        final String ifNoneMatch = context.requestHeader(Constant.IF_NONE_MATCH);
        if (matches(ifNoneMatch, eTag)) {
            context.responseHeader(Constant.ETAG, eTag); // 304.
            context.status(304);
            return true;
        }

        // If-Modified-Since header should be greater than LastModified
        // If so, then return 304
        // This header is ignored if any If-None-Match header is specified
        final long ifModifiedSince = context.requestDateHeader(Constant.IF_MODIFIED_SINCE);// If-Modified-Since
        if (ifNoneMatch == null && (ifModifiedSince > 0 && lastModified != 0 && ifModifiedSince >= lastModified)) {
            // if (ifNoneMatch == null && ge(ifModifiedSince, lastModified)) {
            context.responseDateHeader(Constant.LAST_MODIFIED, lastModified); // 304
            context.status(304);
            return true;
        }

        // Validate request headers for resume
        // ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412
        final String ifMatch = context.requestHeader(Constant.IF_MATCH);
        if (ifMatch != null && !matches(ifMatch, eTag)) {
            context.status(412);
            return true;
        }

        // If-Unmodified-Since header should be greater than LastModified.
        // If not, then return 412.
        final long ifUnmodifiedSince = context.requestDateHeader(Constant.IF_UNMODIFIED_SINCE);// "If-Unmodified-Since"

        if (ifUnmodifiedSince > 0 && lastModified > 0 && ifUnmodifiedSince <= lastModified) {
            context.status(412);
            return true;
        }
        return false;
    }

    /***
     * Build method parameter list
     * 
     * @param method
     *            Target Action or Handler
     */
    public static MethodParameter[] createMethodParametersArray(Method method) {
        final List<MethodParameter> params = createMethodParameters(method);
        if (ObjectUtils.isEmpty(params)) {
            return null;
        }
        return params.toArray(new MethodParameter[params.size()]);
    }

    /***
     * Build method parameter list
     * 
     * @param method
     *            Target Action or Handler
     */
    public static List<MethodParameter> createMethodParameters(Method method) {
        if (method == null) {
            return null;
        }
        final Parameter[] parameters = method.getParameters();
        final List<MethodParameter> methodParameters = new ArrayList<>();
        final String[] methodArgsNames = ClassUtils.getMethodArgsNames(method);

        for (int i = 0; i < parameters.length; i++) {
            methodParameters.add(createMethodParameter(parameters[i], methodArgsNames[i]));
        }
        return methodParameters;
    }

    /**
     * Create a method parameter
     * 
     * @param parameter
     *            Reflect parameter
     * @param methodArgsName
     *            method parameter names
     * @return {@link MethodParameter}
     */
    public static MethodParameter createMethodParameter(final Parameter parameter, final String methodArgsName) {

        Type[] genericityClass = null;
        String parameterName = Constant.BLANK;
        Class<?> parameterClass = parameter.getType();
        // annotation
        boolean required = false;
        String defaultValue = null;

        final Type parameterizedType = parameter.getParameterizedType();
        if (parameterizedType instanceof ParameterizedType) {
            genericityClass = ((ParameterizedType) parameterizedType).getActualTypeArguments();
        }

        final AnnotationAttributes attributes = ClassUtils.getAnnotationAttributes(RequestParam.class, parameter);
        if (attributes != null) {
            required = attributes.getBoolean("required");
            defaultValue = attributes.getString("defaultValue");
            parameterName = attributes.getString(Constant.VALUE);
        }

        if (StringUtils.isEmpty(defaultValue) && NumberUtils.isNumber(parameterClass)) {
            defaultValue = "0"; // fix default value
        }

        if (StringUtils.isEmpty(parameterName)) {
            parameterName = methodArgsName; // use method parameter name
        }
        return new MethodParameter(parameterName, required, defaultValue, parameter, genericityClass, parameterClass);
    }

}
