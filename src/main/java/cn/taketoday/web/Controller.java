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
package cn.taketoday.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.servlet.RequestContextHolder;
import cn.taketoday.web.ui.Model;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.ui.RedirectModel;

/**
 * @author TODAY <br>
 *         2019-12-25 11:21
 */
public /*abstract*/ class Controller implements RequestContext {

    public RequestContext getRequestContext() {
        return RequestContextHolder.currentContext();
    }

    // --------

    public <T> T getModel(Class<T> modelClass) {
        return null;
    }

    /**
     * Get model from http request body.
     */
    public <T> T getModel(Class<T> modelClass, String modelName) {
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
    public HttpHeaders responseHeader(String name, String value) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public HttpHeaders addResponseHeader(String name, String value) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public HttpHeaders responseDateHeader(String name, long date) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public HttpHeaders addResponseDateHeader(String name, long date) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public HttpHeaders responseIntHeader(String name, int value) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public HttpHeaders addResponseIntHeader(String name, int value) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public HttpHeaders contentType(String contentType) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public void flush() throws IOException {
        // TODO 自动生成的方法存根

    }

    @Override
    public String contextPath() {
        // TODO 自动生成的方法存根
        return null;
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
    public String queryString() {
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
    public RequestContext addCookie(HttpCookie cookie) {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public Map<String, String[]> parameters() {
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
    public String method() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public String remoteAddress() {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public long contentLength() {
        // TODO 自动生成的方法存根
        return 0;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        // TODO 自动生成的方法存根
        return null;
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
    public RequestContext sendError(int sc) throws IOException {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public RequestContext sendError(int sc, String msg) throws IOException {
        // TODO 自动生成的方法存根
        return null;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
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
