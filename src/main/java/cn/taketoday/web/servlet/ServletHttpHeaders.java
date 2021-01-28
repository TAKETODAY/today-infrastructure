/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  @Override public Map<String, String> toSingleValueMap() {
    return null;
  }

  @Override public int size() {
    return 0;
  }

  @Override public boolean isEmpty() {
    return false;
  }

  @Override public boolean containsKey(final Object key) {
    return false;
  }

  @Override public boolean containsValue(final Object value) {
    return false;
  }

  @Override
  public List<String> get(Object key) {

    return null;
  }

  @Override public List<String> put(final String key, final List<String> value) {
    return null;
  }

  @Override
  public List<String> remove(Object key) {

    return null;
  }

  @Override public void putAll(final Map<? extends String, ? extends List<String>> m) {

  }

  @Override public void clear() {

  }

  @Override public Set<String> keySet() {
    return null;
  }

  @Override public Collection<List<String>> values() {
    return null;
  }

  @Override public Set<Entry<String, List<String>>> entrySet() {
    return null;
  }

}
