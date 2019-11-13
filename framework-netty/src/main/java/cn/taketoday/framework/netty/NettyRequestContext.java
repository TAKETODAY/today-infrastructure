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
package cn.taketoday.framework.netty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.ui.Model;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.ui.RedirectModel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 *         2019-07-04 21:24
 */
@Slf4j
public class NettyRequestContext implements RequestContext {


    private String remoteAddress;
    private String url;
    private String method;

    private boolean initQueryParam;

    private HttpHeaders httpHeaders;

    private io.netty.handler.codec.http.HttpRequest nettyRequest;
//    private HttpPostRequestDecoder decoder;

    private Queue<HttpContent> contents = new LinkedList<>();

    private Map<String, String> headers = null;
    private Map<String, Object> attributes = null;
    private Map<String, String> pathParams = null;
    private Map<String, String[]> parameters = new HashMap<>(8);
    private Map<String, HttpCookie> cookies = new HashMap<>(8);

    private final Set<HttpCookie> responseCookies = new HashSet<>();
    private final Map<String, String> responseHeaders = new HashMap<>();

    HttpRequest request;
//    private HttpCookie[] cookies;
    private String contextPath;
    private ModelAndView modelAndView;

    private Object requestBody;
    private String[] pathVariables;

    private Map<String, List<MultipartFile>> multipartFiles;

    public NettyRequestContext() {

    }

    public NettyRequestContext(HttpRequest request) {

    }

    @Override
    public String remoteAddress() {
        return this.remoteAddress;
    }

    @Override
    public String queryString() {
        if (null == url || !url.contains("?")) {
            return "";
        }
        return url.substring(url.indexOf("?") + 1);
    }

//    @Override
//    public Map<String, List<String>> parameters() {
    public Map<String, String[]> parameters() {
        if (initQueryParam) {
            return this.parameters;
        }

        initQueryParam = true;
        if (!url.contains("?")) {
            return this.parameters;
        }

        Map<String, List<String>> parameters = new QueryStringDecoder(url, CharsetUtil.UTF_8).parameters();

        if (null != parameters) {
//            this.parameters.putAll(parameters);
        }
        return this.parameters;
    }

    @Override
    public String method() {
        return this.method;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

//    @Override
    public Map<String, String> headers() {
        if (null == headers) {
            headers = new HashMap<>(httpHeaders.size());
            Iterator<Map.Entry<String, String>> entryIterator = httpHeaders.iteratorAsString();
            while (entryIterator.hasNext()) {
                Map.Entry<String, String> next = entryIterator.next();
                headers.put(next.getKey(), next.getValue());
            }
        }
        return this.headers;
    }

    public void setNettyRequest(io.netty.handler.codec.http.HttpRequest nettyRequest) {
        this.nettyRequest = nettyRequest;
    }

    public void appendContent(HttpContent msg) {
        this.contents.add(msg.retain());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Model attributes(Map<String, Object> attributes) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Enumeration<String> attributes() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Object attribute(String name) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public <T> T attribute(String name, Class<T> targetClass) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Model attribute(String name, Object value) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Model removeAttribute(String name) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Map<String, Object> asMap() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public void clear() {
        // TODO 自动生成的方法存根

    }

    @Override
    public String requestHeader(String name) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Enumeration<String> requestHeaders(String name) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Enumeration<String> requestHeaderNames() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public int requestIntHeader(String name) {
        // TODO 自动生成的方法存根
        return 0;
    }

    @Override
    public long requestDateHeader(String name) {
        // TODO 自动生成的方法存根
        return 0;
    }

    @Override
    public String contentType() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public String responseHeader(String name) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Collection<String> responseHeaders(String name) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Collection<String> responseHeaderNames() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public cn.taketoday.web.HttpHeaders responseHeader(String name, String value) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public cn.taketoday.web.HttpHeaders addResponseHeader(String name, String value) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public cn.taketoday.web.HttpHeaders responseDateHeader(String name, long date) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public cn.taketoday.web.HttpHeaders addResponseDateHeader(String name, long date) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public cn.taketoday.web.HttpHeaders responseIntHeader(String name, int value) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public cn.taketoday.web.HttpHeaders addResponseIntHeader(String name, int value) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public cn.taketoday.web.HttpHeaders contentType(String contentType) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public String contextPath() {
        if (contextPath == null) {
//            return contextPath = request.getContextPath();
        }
        return contextPath;
    }

    @Override
    public String requestURI() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public String requestURL() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public HttpCookie[] cookies() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public HttpCookie cookie(String name) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Enumeration<String> parameterNames() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public String[] parameters(String name) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public String parameter(String name) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public long contentLength() {
        // TODO 自动生成的方法存根
        return 0;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Object requestBody() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Object requestBody(Object body) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public String[] pathVariables() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public String[] pathVariables(String[] variables) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public RedirectModel redirectModel() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public RedirectModel redirectModel(RedirectModel redirectModel) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Map<String, List<MultipartFile>> multipartFiles() throws IOException {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public ModelAndView modelAndView() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public ModelAndView modelAndView(ModelAndView modelAndView) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public RequestContext contentLength(long length) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public boolean committed() {
        // TODO 自动生成的方法存根
        return false;
    }

    @Override
    public RequestContext reset() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public RequestContext addCookie(HttpCookie cookie) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public RequestContext redirect(String location) throws IOException {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public RequestContext status(int sc) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public int status() {
        // TODO 自动生成的方法存根
        return 0;
    }

    @Override
    public RequestContext sendError(int sc, String msg) throws IOException {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public <T> T nativeSession() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public <T> T nativeSession(Class<T> sessionClass) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public <T> T nativeRequest() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public <T> T nativeRequest(Class<T> requestClass) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public <T> T nativeResponse() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public <T> T nativeResponse(Class<T> responseClass) {
        // TODO 自动生成的方法存根
        return null;
    }

}
