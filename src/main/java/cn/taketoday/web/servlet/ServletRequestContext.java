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
package cn.taketoday.web.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.web.AbstractRequestContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.exception.WebRuntimeException;
import cn.taketoday.web.multipart.DefaultMultipartFile;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.ui.RedirectModel;

/**
 * @author TODAY <br>
 *         2019-07-07 22:27
 * @since 2.3.7
 */
public class ServletRequestContext extends AbstractRequestContext implements RequestContext, Map<String, Object> {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public ServletRequestContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    protected String getContextPathInternal() {
        return request.getContextPath();
    }

    @SuppressWarnings("unchecked")
    public <T> T nativeSession() {
        return (T) request.getSession();
    }

    @SuppressWarnings("unchecked")
    public <T> T nativeRequest() {
        return (T) request;
    }

    @Override
    public <T> T nativeSession(Class<T> sessionClass) {
        return sessionClass.cast(request.getSession());
    }

    @Override
    public <T> T nativeRequest(Class<T> requestClass) {
        return getNativeRequest(request, requestClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getNativeRequest(ServletRequest request, Class<T> requiredType) {
        if (requiredType != null) {
            if (requiredType.isInstance(request)) {
                return (T) request;
            }
            else if (request instanceof ServletRequestWrapper) {
                return getNativeRequest(((ServletRequestWrapper) request).getRequest(), requiredType);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getNativeResponse(ServletResponse response, Class<T> requiredType) {
        if (requiredType != null) {
            if (requiredType.isInstance(response)) {
                return (T) response;
            }
            else if (response instanceof ServletResponseWrapper) {
                return getNativeResponse(((ServletResponseWrapper) response).getResponse(), requiredType);
            }
        }
        return null;
    }

    @Override
    public <T> T nativeResponse(Class<T> responseClass) {
        return getNativeResponse(response, responseClass);
    }

    @SuppressWarnings("unchecked")
    public <T> T nativeResponse() {
        return (T) response;
    }

    @Override
    protected OutputStream getOutputStreamInternal() throws IOException {
        return response.getOutputStream();
    }

    @Override
    protected InputStream getInputStreamInternal() throws IOException {
        return request.getInputStream();
    }

    @Override
    protected PrintWriter getWriterInternal() throws IOException {
        return response.getWriter();
    }

    @Override
    public BufferedReader getReaderInternal() throws IOException {
        return request.getReader();
    }

    @Override
    public String requestURI() {
        return request.getRequestURI();
    }

    @Override
    public String requestURL() {
        return request.getRequestURL().toString();
    }

    @Override
    public String queryString() {
        return request.getQueryString();
    }

    @Override
    protected HttpCookie[] getCookiesInternal() {

        final Cookie[] servletCookies = request.getCookies();
        if (ObjectUtils.isEmpty(servletCookies)) { // there is not cookies
            return null;
        }
        final HttpCookie[] cookies = new HttpCookie[servletCookies.length];

        int i = 0;
        for (final Cookie servletCookie : servletCookies) {

            final HttpCookie httpCookie = //
                    new HttpCookie(servletCookie.getName(), servletCookie.getValue());

            httpCookie.setPath(servletCookie.getPath());
            httpCookie.setDomain(servletCookie.getDomain());
            httpCookie.setMaxAge(servletCookie.getMaxAge());
            httpCookie.setSecure(servletCookie.getSecure());
            httpCookie.setVersion(servletCookie.getVersion());
            httpCookie.setComment(servletCookie.getComment());
            httpCookie.setHttpOnly(servletCookie.isHttpOnly());

            cookies[i++] = httpCookie;//
        }
        return cookies;
    }

    @Override
    public Map<String, String[]> parameters() {
        return request.getParameterMap();
    }

    @Override
    public Enumeration<String> parameterNames() {
        return request.getParameterNames();
    }

    @Override
    public String[] parameters(String name) {
        return request.getParameterValues(name);
    }

    @Override
    public String parameter(String name) {
        return request.getParameter(name);
    }

    @Override
    public String method() {
        return request.getMethod();
    }

    @Override
    public String remoteAddress() {
        return request.getRemoteAddr();
    }

    @Override
    public String requestHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public Enumeration<String> requestHeaders(String name) {
        return request.getHeaders(name);
    }

    @Override
    public Enumeration<String> requestHeaderNames() {
        return request.getHeaderNames();
    }

    @Override
    public long contentLength() {
        return request.getContentLengthLong();
    }

    @Override
    public String contentType() {
        return request.getContentType();
    }

    @Override
    public RequestContext contentType(String contentType) {
        response.setContentType(contentType);
        return this;
    }

    @Override
    public RequestContext contentLength(long length) {
        response.setContentLengthLong(length);
        return this;
    }

    @Override
    public boolean committed() {
        return response.isCommitted();
    }

    @Override
    public RequestContext reset() {
        response.reset();
        return this;
    }

    @Override
    public RequestContext addCookie(final HttpCookie cookie) {

        final Cookie servletCookie = new Cookie(cookie.getName(), cookie.getValue());

        servletCookie.setPath(cookie.getPath());
        if (cookie.getDomain() != null) {
            servletCookie.setDomain(cookie.getDomain());
        }
        servletCookie.setSecure(cookie.getSecure());
        servletCookie.setComment(cookie.getComment());
        servletCookie.setVersion(cookie.getVersion());
        servletCookie.setHttpOnly(cookie.isHttpOnly());
        servletCookie.setMaxAge((int) cookie.getMaxAge());

        response.addCookie(servletCookie);
        return this;
    }

    @Override
    public RequestContext redirect(String location) throws IOException {
        response.sendRedirect(location);
        return this;
    }

    @Override
    public RequestContext status(int sc) {
        response.setStatus(sc);
        return this;
    }

    @Override
    public int status() {
        return response.getStatus();
    }

    @Override
    public String responseHeader(String name) {
        return response.getHeader(name);
    }

    @Override
    public Collection<String> responseHeaders(String name) {
        return response.getHeaders(name);
    }

    @Override
    public Collection<String> responseHeaderNames() {
        return response.getHeaderNames();
    }

    @Override
    public RequestContext responseHeader(String name, String value) {
        response.setHeader(name, value);
        return this;
    }

    @Override
    public RequestContext addResponseHeader(String name, String value) {
        response.addHeader(name, value);
        return this;
    }

    @Override
    public int requestIntHeader(String name) {
        return request.getIntHeader(name);
    }

    @Override
    public long requestDateHeader(String name) {
        return request.getDateHeader(name);
    }

    @Override
    public RequestContext responseDateHeader(String name, long date) {
        response.setDateHeader(name, date);
        return this;
    }

    @Override
    public RequestContext addResponseDateHeader(String name, long date) {
        response.addDateHeader(name, date);
        return this;
    }

    @Override
    public RequestContext responseIntHeader(String name, int value) {
        response.setIntHeader(name, value);
        return this;
    }

    @Override
    public RequestContext addResponseIntHeader(String name, int value) {
        response.addIntHeader(name, value);
        return this;
    }

    @Override
    public RequestContext sendError(int sc) throws IOException {
        response.sendError(sc);
        return this;
    }

    @Override
    public RequestContext sendError(int sc, String msg) throws IOException {
        response.sendError(sc, msg);
        return this;
    }

    @Override
    public RedirectModel redirectModel() {
        final Object attribute = request.getSession().getAttribute("redirect-model");

        if (attribute instanceof RedirectModel) {
            return (RedirectModel) attribute;
        }
        return null;
    }

    @Override
    public RedirectModel redirectModel(RedirectModel redirectModel) {
        request.getSession().setAttribute("redirect-model", redirectModel);
        return redirectModel;
    }

    // --------------- model

    @Override
    public RequestContext attribute(String name, Object value) {
        request.setAttribute(name, value);
        return this;
    }

    @Override
    public RequestContext attributes(Map<String, Object> attributes) {
        attributes.forEach(this::attribute);
        return this;
    }

    @Override
    public Map<String, Object> asMap() {
        return this;
    }

    @Override
    public Object attribute(String name) {
        return get(name);
    }

    @Override
    public Enumeration<String> attributes() {
        return request.getAttributeNames();
    }

    @Override
    public <T> T attribute(String name, Class<T> targetClass) {
        return ConvertUtils.convert(targetClass, get(name));
    }

    @Override
    public RequestContext removeAttribute(String name) {
        request.removeAttribute(name);
        return this;
    }

    @Override
    public int size() {
        int size = 0;
        final Enumeration<String> attributes = attributes();
        while (attributes.hasMoreElements()) {
            size++;
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return containsAttribute((String) key);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(Object key) {
        if (key instanceof String) {
            return request.getAttribute((String) key);
        }
        throw new WebRuntimeException("Attribute name must be a String");
    }

    @Override
    public Object put(String key, Object value) {
        attribute(key, value);
        return null;
    }

    @Override
    public Object remove(Object name) {
        if (name instanceof String) {
            removeAttribute((String) name);
            return null;
        }
        throw new WebRuntimeException("Attribute name must be a String");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putAll(Map<? extends String, ? extends Object> attributes) {
        attributes((Map<String, Object>) attributes);
    }

    @Override
    public void clear() {
        final Enumeration<String> attributeNames = attributes();
        while (attributeNames.hasMoreElements()) {
            removeAttribute(attributeNames.nextElement());
        }
    }

    @Override
    public Set<String> keySet() {
        final Set<String> keySet = new HashSet<>();
        final Enumeration<String> attributeNames = attributes();
        while (attributeNames.hasMoreElements()) {
            keySet.add(attributeNames.nextElement());
        }
        return keySet;
    }

    @Override
    public Collection<Object> values() {
        final Set<Object> valueSet = new HashSet<>();
        final Enumeration<String> attributeNames = attributes();
        while (attributeNames.hasMoreElements()) {
            valueSet.add(get(attributeNames.nextElement()));
        }
        return valueSet;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        final Set<Entry<String, Object>> entries = new HashSet<>();
        final Enumeration<String> attributeNames = attributes();
        while (attributeNames.hasMoreElements()) {
            final String currentKey = attributeNames.nextElement();
            entries.add(new Node(currentKey, get(currentKey)));
        }
        return entries;
    }

    private static final class Node implements Entry<String, Object> {

        private final String key;
        private Object value;

        public Node(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public final Object setValue(Object value) {
            Object oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public final Object getValue() {
            return value;
        }

        @Override
        public final String getKey() {
            return key;
        }
    }

    @Override
    protected Map<String, List<MultipartFile>> parseMultipartFiles() throws IOException {

        final HashMap<String, List<MultipartFile>> multipartFiles = new HashMap<>();

        try {
            for (final Part part : request.getParts()) {
                final String name = part.getName();
                List<MultipartFile> parts = multipartFiles.get(name);
                if (parts == null) {
                    multipartFiles.put(name, parts = new ArrayList<>(4));
                }
                parts.add(new DefaultMultipartFile(part));
            }
            return multipartFiles;
        }
        catch (ServletException e) { // if this request is not of type multipart/form-data
            throw new BadRequestException("This is not a multipart request", e);
        }
    }

    @Override
    public void flush() throws IOException {
        response.flushBuffer();
    }

}
