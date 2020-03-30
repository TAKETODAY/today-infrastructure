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
package cn.taketoday.framework.reactive;

import static cn.taketoday.context.Constant.DEFAULT_CHARSET;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.framework.Constant;
import cn.taketoday.web.AbstractRequestContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.ui.Model;
import cn.taketoday.web.ui.RedirectModel;
import cn.taketoday.web.utils.WebUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.CombinedHttpHeaders;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;

/**
 * @author TODAY <br>
 *         2019-07-04 21:24
 */
public class NettyRequestContext extends AbstractRequestContext implements RequestContext, Map<String, Object> {

//    private static final Logger log = LoggerFactory.getLogger(NettyRequestContext.class);

    private final String url;
    private String queryString;
    private String remoteAddress;

    private boolean committed = false;

    private final String contextPath;

    private ByteBufInputStream inputStream;
    private ByteBufOutputStream outputStream;

    private Map<String, Object> attributes;
    private Map<String, String[]> parameters;
    private Map<String, List<MultipartFile>> multipartFiles;

    private final FullHttpRequest request;
    private final ChannelHandlerContext handlerContext;

    private String uri;

    public NettyRequestContext(String contextPath, ChannelHandlerContext ctx, FullHttpRequest request) {
        this.request = request;
        this.handlerContext = ctx;
        this.contextPath = contextPath;
        this.url = request.uri();
        this.uri = request.uri();//TODO
    }

    @Override
    public String contextPath() {
        return contextPath;
    }

    @Override
    public String requestURI() {
        return uri;
    }

    @Override
    public String requestURL() {
        return request.uri();
    }

    @Override
    public String remoteAddress() {
        return this.remoteAddress;
    }

    @Override
    public String queryString() {
        final String queryString = this.queryString;
        if (queryString == null) {
            final String url = this.url;
            int index;
            if (url == null || (index = url.indexOf('?')) == -1) {
                return Constant.BLANK;
            }
            return this.queryString = url.substring(index + 1);
        }
        return queryString;
    }

    @Override
    public String method() {
        return this.request.method().name();
    }

    @Override
    public ByteBufInputStream getInputStream() throws IOException {
        final ByteBufInputStream inputStream = this.inputStream;
        if (inputStream == null) {
            return this.inputStream = new ByteBufInputStream(request.content());
        }
        return inputStream;
    }

    @Override
    public ByteBufOutputStream getOutputStream() throws IOException {

        final ByteBufOutputStream outputStream = this.outputStream;
        if (outputStream == null) {
            return this.outputStream = new ByteBufOutputStream(responseBody);
        }
        return outputStream;
    }

    public Map<String, Object> getAttributes() {
        final Map<String, Object> attributes = this.attributes;
        if (attributes == null) {
            return this.attributes = new HashMap<>();
        }
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public Model attributes(Map<String, Object> attributes) {
        getAttributes().putAll(attributes);
        return this;
    }

    @Override
    public Enumeration<String> attributes() {
        return Collections.enumeration(getAttributes().keySet());
    }

    @Override
    public Object attribute(String name) {
        return getAttributes().get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T attribute(String name, Class<T> targetClass) {
        return (T) ConvertUtils.convert(getAttributes().get(name), targetClass);
    }

    @Override
    public Model attribute(String name, Object value) {
        getAttributes().put(name, value);
        return this;
    }

    @Override
    public Model removeAttribute(String name) {
        getAttributes().remove(name);
        return this;
    }

    @Override
    public Map<String, Object> asMap() {
        return attributes;
    }

    @Override
    public String requestHeader(String name) {
        return request.headers().get(name);
    }

    @Override
    public Enumeration<String> requestHeaders(String name) {
        return Collections.enumeration(request.headers().getAll(name));
    }

    @Override
    public Enumeration<String> requestHeaderNames() {
        return Collections.enumeration(request.headers().names());
    }

    @Override
    public int requestIntHeader(String name) {
        return request.headers().getInt(name, 0);
    }

    @Override
    public long requestDateHeader(String name) {
        return request.headers().getTimeMillis(name);
    }

    @Override
    public String contentType() {
        return request.headers().get(HttpHeaderNames.CONTENT_TYPE);
    }

    private HttpHeaders responseHeaders;
    private boolean validateHeaders = false;
    private boolean singleFieldHeaders = true;

    public final HttpHeaders getResponseHeaders() {
        final HttpHeaders responseHeaders = this.responseHeaders;
        if (responseHeaders == null) {
            return this.responseHeaders = singleFieldHeaders
                    ? new DefaultHttpHeaders(validateHeaders)
                    : new CombinedHttpHeaders(validateHeaders);
        }
        return responseHeaders;
    }

    public void setResponseHeaders(HttpHeaders responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    @Override
    public String responseHeader(String name) {
        return getResponseHeaders().get(name);
    }

    @Override
    public Collection<String> responseHeaders(String name) {
        return getResponseHeaders().getAll(name);
    }

    @Override
    public Collection<String> responseHeaderNames() {
        return getResponseHeaders().names();
    }

    @Override
    public cn.taketoday.web.HttpHeaders responseHeader(String name, String value) {
        getResponseHeaders().set(name, value);
        return this;
    }

    @Override
    public cn.taketoday.web.HttpHeaders addResponseHeader(String name, String value) {
        getResponseHeaders().add(name, value);
        return this;
    }

    @Override
    public cn.taketoday.web.HttpHeaders responseDateHeader(String name, long date) {
        getResponseHeaders().set(name, Long.valueOf(date));
        return this;
    }

    @Override
    public cn.taketoday.web.HttpHeaders addResponseDateHeader(String name, long date) {
        getResponseHeaders().add(name, Long.valueOf(date));
        return this;
    }

    @Override
    public cn.taketoday.web.HttpHeaders responseIntHeader(String name, int value) {
        getResponseHeaders().set(name, Integer.valueOf(value));
        return this;
    }

    @Override
    public cn.taketoday.web.HttpHeaders addResponseIntHeader(String name, int value) {
        getResponseHeaders().add(name, Integer.valueOf(value));
        return this;
    }

    @Override
    public cn.taketoday.web.HttpHeaders contentType(String contentType) {
        getResponseHeaders().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        return this;
    }

    @Override
    public HttpCookie[] getCookiesInternal() {
        final String header = request.headers().get(HttpHeaderNames.COOKIE);
        if (StringUtils.isEmpty(header)) {
            return null;
        }
        final List<HttpCookie> parsed = HttpCookie.parse(header);
        return ObjectUtils.isEmpty(parsed) ? null : parsed.toArray(new HttpCookie[parsed.size()]);
    }

    @Override
    public Enumeration<String> parameterNames() {
        return Collections.enumeration(parameters().keySet());
    }

    @Override
    public String[] parameters(String name) {
        return parameters().get(name);
    }

    @Override
    public String parameter(String name) {

        final String[] parameter = parameters().get(name);
        return ObjectUtils.isNotEmpty(parameter) ? parameter[0] : null;
    }

    private static final HttpDataFactory HTTP_DATA_FACTORY = new DefaultHttpDataFactory(true);

    private InterfaceHttpPostRequestDecoder requestDecoder;

    @Override
    public Map<String, String[]> parameters() {
        final Map<String, String[]> params = this.parameters;
        if (params != null) {
            return params;
        }
        return this.parameters = parseParameters();
    }

    protected Map<String, String[]> parseParameters() {

        final String queryString = queryString();
        final Map<String, List<String>> parameters = StringUtils.isNotEmpty(queryString)
                ? StringUtils.parseParameters(queryString)
                : new HashMap<>();

        final List<InterfaceHttpData> bodyHttpDatas = getRequestDecoder().getBodyHttpDatas();
        for (final InterfaceHttpData data : bodyHttpDatas) {

            if (data instanceof Attribute) {
                try {
                    final String name = data.getName();
                    List<String> list = parameters.get(name);
                    if (list == null) {
                        parameters.put(name, list = new ArrayList<>(4));
                    }
                    list.add(((Attribute) data).getValue());
                }
                catch (IOException e) {
                    throw WebUtils.newBadRequest("HttpData", "name", e);
                }
            }
        }

        if (!parameters.isEmpty()) {
            final Map<String, String[]> params = new HashMap<>(parameters.size());
            for (final Entry<String, List<String>> entry : parameters.entrySet()) {
                final List<String> value = entry.getValue();
                params.put(entry.getKey(), value.toArray(new String[value.size()]));
            }
            return params;
        }
        return Collections.emptyMap();
    }

    protected InterfaceHttpPostRequestDecoder getRequestDecoder() {
        InterfaceHttpPostRequestDecoder requestDecoder = this.requestDecoder;
        if (requestDecoder == null) {

            requestDecoder = new HttpPostRequestDecoder(HTTP_DATA_FACTORY, this.request, DEFAULT_CHARSET);
//            requestDecoder = WebUtils.isMultipart(this)
//                    ? new HttpPostMultipartRequestDecoder(HTTP_DATA_FACTORY, request.retain(), DEFAULT_CHARSET)
//                    : new HttpPostStandardRequestDecoder(HTTP_DATA_FACTORY, request.retain(), DEFAULT_CHARSET);

            requestDecoder.setDiscardThreshold(0);
            return this.requestDecoder = requestDecoder;
        }
        return requestDecoder;
    }

    @Override
    public long contentLength() {
        return request.content().readableBytes();
    }

    private PrintWriter writer;
    private BufferedReader reader;

    @Override
    public BufferedReader getReader() throws IOException {
        final BufferedReader reader = this.reader;
        if (reader == null) {
            return this.reader = new BufferedReader(new InputStreamReader(getInputStream(), DEFAULT_CHARSET));
        }
        return reader;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        final PrintWriter writer = this.writer;
        if (writer == null) {
            return this.writer = new PrintWriter(getOutputStream(), true);
        }
        return writer;
    }

    @Override
    public RedirectModel redirectModel() {
        return null;
    }

    @Override
    public RedirectModel redirectModel(RedirectModel redirectModel) {

        //TODO
        return redirectModel;
    }

    @Override
    public RequestContext redirect(String location) throws IOException {

        if (committed) {
            throw new IllegalStateException("The response has been committed");
        }

        getResponse().setStatus(HttpResponseStatus.FOUND);
        getResponseHeaders().set(HttpHeaderNames.LOCATION, location);
        send();

        return this;
    }

    private final ByteBuf responseBody = Unpooled.buffer(16);

    public final ByteBuf responseBody() {
        return responseBody;
    }

    private boolean keepAlive = true;

    public RequestContext send() {

        if (committed) {
            throw new IllegalStateException("The response has been committed");
        }

        final HttpHeaders headers = getResponseHeaders();

        if (!headers.contains(HttpHeaderNames.CONTENT_LENGTH)) {
            headers.setInt(HttpHeaderNames.CONTENT_LENGTH, responseBody.readableBytes());
        }

        final ChannelHandlerContext handlerContext = this.handlerContext;
        if (handlerContext != null) {
            if (keepAlive) {
                headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            else {
                handlerContext.write(Unpooled.EMPTY_BUFFER)
                        .addListener(ChannelFutureListener.CLOSE);
            }
            handlerContext.writeAndFlush(getResponse());
        }

        final InterfaceHttpPostRequestDecoder requestDecoder = this.requestDecoder;
        if (requestDecoder != null) {
            requestDecoder.destroy();
        }

        setCommitted(true);
        return this;
    }

    @Override
    public RequestContext contentLength(long length) {
        getResponse().headers().setInt(HttpHeaderNames.CONTENT_LENGTH, (int) length);
        return this;
    }

    @Override
    public boolean committed() {
        return committed;
    }

    @Override
    public RequestContext reset() {

        if (committed) {
            throw new IllegalStateException("The response has been committed");
        }

        getResponseHeaders().clear();

        responseBody.clear();
        getResponse().setStatus(HttpResponseStatus.OK);
        return this;
    }

    @Override
    public RequestContext addCookie(HttpCookie cookie) {

        final Cookie c = new DefaultCookie(cookie.getName(), cookie.getValue());
        c.setPath(cookie.getPath());
        c.setDomain(cookie.getDomain());
        c.setMaxAge(cookie.getMaxAge());
        c.setSecure(cookie.getSecure());
        c.setHttpOnly(cookie.isHttpOnly());

        getResponseHeaders().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(c)); // TODO 优化 

        return this;
    }

    @Override
    public RequestContext status(int sc) {
        getResponse().setStatus(HttpResponseStatus.valueOf(sc));
        return this;
    }

    @Override
    public int status() {
        return getResponse().status().code();
    }

    @Override
    public RequestContext sendError(int sc) throws IOException {

        final FullHttpResponse response = getResponse();
        response.setStatus(HttpResponseStatus.valueOf(sc));

        handlerContext.writeAndFlush(response);

        setCommitted(true);

        return this;
    }

    @Override
    public RequestContext sendError(int sc, String msg) throws IOException {

        final FullHttpResponse response = getResponse();
        response.setStatus(HttpResponseStatus.valueOf(sc, msg));

        handlerContext.writeAndFlush(response);

        setCommitted(true);

        return this;
    }

    @Override
    public <T> T nativeSession() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T nativeSession(Class<T> sessionClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T nativeRequest() {
        return (T) request;
    }

    @Override
    public <T> T nativeRequest(Class<T> requestClass) {
        if (requestClass.isInstance(request)) {
            return requestClass.cast(request);
        }
        throw new ConfigurationException("The runtime request is not a: [" + requestClass + "] object");
    }

    private FullHttpResponse response;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T nativeResponse() {
        return (T) getResponse();
    }

    public final FullHttpRequest getRequest() {
        return request;
    }

    public final FullHttpResponse getResponse() {
        final FullHttpResponse response = this.response;
        if (response == null) {
            return this.response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                                               HttpResponseStatus.OK,
                                                               responseBody,
                                                               getResponseHeaders(),
                                                               EmptyHttpHeaders.INSTANCE);// TODO trailingHeaders ?
        }
        return response;
    }

    @Override
    public <T> T nativeResponse(Class<T> responseClass) {
        final T ret = nativeResponse();
        if (responseClass.isInstance(ret)) {
            return ret;
        }
        throw new ConfigurationException("The runtime response is not a: [" + responseClass + "] object");
    }

    @Override
    public void flush() throws IOException {

        handlerContext.flush();
    }

    @Override
    public Map<String, List<MultipartFile>> multipartFiles() throws IOException {

        Map<String, List<MultipartFile>> multipartFiles = this.multipartFiles;

        if (multipartFiles != null) {
            return multipartFiles;
        }

        multipartFiles = new HashMap<>(32);

        for (InterfaceHttpData data : getRequestDecoder().getBodyHttpDatas()) {
            if (data instanceof FileUpload) {
                final String name = data.getName();
                List<MultipartFile> parts = multipartFiles.get(name);
                if (parts == null) {
                    multipartFiles.put(name, parts = new ArrayList<>(2));
                }
                parts.add(new FileUploadMultipartFile((FileUpload) data));
            }
        }
        return this.multipartFiles = multipartFiles;
    }

    // Map
    // -----------------------------------------

    @Override
    public int size() {
        return getAttributes().size();
    }

    @Override
    public boolean isEmpty() {
        return getAttributes().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return getAttributes().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return getAttributes().containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return getAttributes().get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return getAttributes().put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return getAttributes().remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        getAttributes().putAll(m);
    }

    @Override
    public Set<String> keySet() {
        return getAttributes().keySet();
    }

    @Override
    public Collection<Object> values() {
        return getAttributes().values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return getAttributes().entrySet();
    }

    @Override
    public void clear() {
        getAttributes().clear();
    }

    // -------------------------------

    public boolean isValidateHeaders() {
        return validateHeaders;
    }

    public void setValidateHeaders(boolean validateHeaders) {
        this.validateHeaders = validateHeaders;
    }

    public boolean isSingleFieldHeaders() {
        return singleFieldHeaders;
    }

    public void setSingleFieldHeaders(boolean singleFieldHeaders) {
        this.singleFieldHeaders = singleFieldHeaders;
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

}
