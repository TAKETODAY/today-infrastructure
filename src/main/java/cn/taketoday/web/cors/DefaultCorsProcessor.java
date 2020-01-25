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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.cors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.utils.WebUtils;

/**
 * The default implementation of {@link CorsProcessor}, as defined by the
 * <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>.
 *
 * <p>
 * Note that when input {@link CorsConfiguration} is {@code null}, this
 * implementation does not reject simple or actual requests outright but simply
 * avoid adding CORS headers to the response. CORS processing is also skipped if
 * the response already contains CORS headers.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author TODAY <br>
 *         2019-12-23 21:02
 */
public class DefaultCorsProcessor implements CorsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCorsProcessor.class);

    @Override
    public boolean processCorsRequest(CorsConfiguration config, RequestContext context) throws IOException {

        context.addResponseHeader(Constant.VARY, Constant.ORIGIN);
        context.addResponseHeader(Constant.VARY, Constant.ACCESS_CONTROL_REQUEST_METHOD);
        context.addResponseHeader(Constant.VARY, Constant.ACCESS_CONTROL_REQUEST_HEADERS);

        if (!WebUtils.isCorsRequest(context)) {
            return true;
        }

        if (context.responseHeader(Constant.ACCESS_CONTROL_ALLOW_ORIGIN) != null) {
            logger.trace("Skip: response already contains \"Access-Control-Allow-Origin\"");
            return true;
        }
        final boolean preFlightRequest = WebUtils.isPreFlightRequest(context);
        if (config == null) {
            if (preFlightRequest) {
                rejectRequest(context);
                return false;
            }
            return true;
        }
        return handleInternal(context, config, preFlightRequest);
    }

    /**
     * Invoked when one of the CORS checks failed. The default implementation sets
     * the response status to 403 and writes "Invalid CORS request" to the response.
     */
    protected void rejectRequest(RequestContext context) throws IOException {

        context.status(403);
        context.getOutputStream()
                .write("Invalid CORS request".getBytes(Constant.DEFAULT_CHARSET));
        context.flush();
    }

    /**
     * Handle the given request.
     */
    protected boolean handleInternal(RequestContext context, CorsConfiguration config, boolean preFlightRequest) throws IOException {

        final String requestOrigin = context.requestHeader(Constant.ORIGIN);

        String allowOrigin = checkOrigin(config, requestOrigin);

        if (allowOrigin == null) {
            logger.debug("Reject: '" + requestOrigin + "' origin is not allowed");
            rejectRequest(context);
            return false;
        }

        String requestMethod = getMethodToUse(context, preFlightRequest);
        List<String> allowMethods = checkMethods(config, requestMethod);
        if (allowMethods == null) {
            logger.debug("Reject: HTTP '" + requestMethod + "' is not allowed");
            rejectRequest(context);
            return false;
        }

        List<String> requestHeaders = getHeadersToUse(context, preFlightRequest);
        List<String> allowHeaders = checkHeaders(config, requestHeaders);
        if (preFlightRequest && allowHeaders == null) {
            logger.debug("Reject: headers '" + requestHeaders + "' are not allowed");
            rejectRequest(context);
            return false;
        }

        context.responseHeader(Constant.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigin);

        if (preFlightRequest) {
            context.responseHeader(Constant.ACCESS_CONTROL_ALLOW_METHODS,
                                   StringUtils.listToString(allowMethods, ","));
        }

        if (preFlightRequest && !allowHeaders.isEmpty()) {
            context.responseHeader(Constant.ACCESS_CONTROL_ALLOW_HEADERS,
                                   StringUtils.listToString(allowHeaders, ","));
        }

        if (!ObjectUtils.isEmpty(config.getExposedHeaders())) {
            context.responseHeader(Constant.ACCESS_CONTROL_EXPOSE_HEADERS,
                                   StringUtils.listToString(config.getExposedHeaders(), ","));
        }

        if (Boolean.TRUE.equals(config.getAllowCredentials())) {
            context.responseHeader(Constant.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString());
        }

        if (preFlightRequest && config.getMaxAge() != null) {
            context.responseDateHeader(Constant.ACCESS_CONTROL_MAX_AGE, config.getMaxAge());
        }

        context.flush();
        return true;
    }

    /**
     * Check the origin and determine the origin for the response. The default
     * implementation simply delegates to
     * {@link CorsConfiguration#checkOrigin(String)}.
     */

    protected String checkOrigin(CorsConfiguration config, String requestOrigin) {
        return config.checkOrigin(requestOrigin);
    }

    /**
     * Check the HTTP method and determine the methods for the response of a
     * pre-flight request. The default implementation simply delegates to
     * {@link CorsConfiguration#checkOrigin(String)}.
     */

    protected List<String> checkMethods(CorsConfiguration config, String method) {
        return config.checkHttpMethod(method);
    }

    private String getMethodToUse(RequestContext context, boolean isPreFlight) {
        return isPreFlight ? context.requestHeader(Constant.ACCESS_CONTROL_REQUEST_METHOD) : context.method();
    }

    /**
     * Check the headers and determine the headers for the response of a pre-flight
     * request. The default implementation simply delegates to
     * {@link CorsConfiguration#checkOrigin(String)}.
     */
    protected List<String> checkHeaders(CorsConfiguration config, List<String> requestHeaders) {
        return config.checkHeaders(requestHeaders);
    }

    private List<String> getHeadersToUse(RequestContext context, boolean isPreFlight) {
        if (isPreFlight) {
            final String requestHeader = context.requestHeader(Constant.ACCESS_CONTROL_REQUEST_HEADERS);
            if (StringUtils.isNotEmpty(requestHeader)) {
                ArrayList<String> result = new ArrayList<>();
                if (requestHeader != null) {
                    Collections.addAll(result, StringUtils.tokenizeToStringArray(requestHeader, ","));
                }
                return result;
            }
        }
        return CollectionUtils.enumerationToList(context.requestHeaderNames());
    }

}
