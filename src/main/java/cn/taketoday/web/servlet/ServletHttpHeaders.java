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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.web.http.HttpHeaders;

/**
 * @author TODAY <br>
 *         2020-01-28 17:29
 */
public class ServletHttpHeaders implements HttpHeaders {

    private static final long serialVersionUID = 1L;

    HttpServletRequest request;
    HttpServletResponse response;

    @Override
    public String getFirst(String headerName) {
        return request.getHeader(headerName);
    }

    @Override
    public void add(String headerName, String headerValue) {
        response.addHeader(headerName, headerValue);
    }

    @Override
    public void set(String headerName, String headerValue) {

    }

    @Override
    public List<String> get(Object key) {

        return null;
    }

    @Override
    public List<String> remove(Object key) {

        return null;
    }

}
