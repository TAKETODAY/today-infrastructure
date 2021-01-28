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
package cn.taketoday.web.utils;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import cn.taketoday.web.Constant;

import static cn.taketoday.context.Constant.DEFAULT_CHARSET;

/**
 * @author TODAY <br>
 * 2020-05-05 16:04
 */
public abstract class HttpUtils {

  /**
   * get a connection with request body
   *
   * @param method
   *         request method
   * @param urlStr
   *         url
   * @param body
   *         request body
   *
   * @return
   *
   * @throws IOException
   */
  public static HttpURLConnection getConnection(String method, String urlStr, byte[] body) throws IOException {
    HttpURLConnection connection = getConnection(method, urlStr);
    connection.setRequestProperty(Constant.CONTENT_TYPE, Constant.CONTENT_TYPE_JSON);
    OutputStream outputStream = connection.getOutputStream();
    outputStream.write(body);
    return connection;
  }

  /**
   * get a connection
   *
   * @param method
   *         request method
   * @param urlStr
   *         url
   *
   * @return
   *
   * @throws IOException
   */
  public static HttpURLConnection getConnection(String method, String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setConnectTimeout(10000);
    if (!"GET".equals(method)) {
      connection.setDoInput(true);// 允许输入
    }
    connection.setDoOutput(true);// 允许输出
    connection.setUseCaches(false); // 不允许使用缓存
    connection.setRequestMethod(method);
    return connection;
  }

  /**
   * @param urlStr
   *
   * @return
   *
   * @throws IOException
   */
  public static HttpURLConnection getConnection(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    connection.setConnectTimeout(3000);
    connection.setDoOutput(true);// 允许输出
    connection.setUseCaches(true); // 不允许使用缓存

    return connection;
  }

  public static String getResponse(HttpURLConnection conn) throws IOException {

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), DEFAULT_CHARSET))) {
      String line;
      final StringBuilder response = new StringBuilder();
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
      conn.disconnect();
      return response.toString();
    }
  }

  /**
   * get response string
   *
   * @param method
   *         request method
   * @param urlStr
   *         url
   *
   * @return
   *
   * @throws IOException
   */
  public static String getResponse(String method, String urlStr) throws IOException {
    return getResponse(getConnection(method, urlStr));
  }

  /**
   * @param method
   *         request method
   * @param urlStr
   *         url
   * @param body
   *
   * @return
   *
   * @throws IOException
   */
  public static String getResponse(String method, String urlStr, byte[] body) throws IOException {
    return getResponse(getConnection(method, urlStr, body));
  }

  public static String getResponse(String method, String urlStr, String body) throws IOException {
    return getResponse(getConnection(method, urlStr, body.getBytes(DEFAULT_CHARSET)));
  }

  /**
   * @param method
   * @param urlStr
   * @param body
   * @param targetClass
   *
   * @return
   *
   * @throws IOException
   */
  public static <T> T getResponse(String method, String urlStr, byte[] body, Class<T> targetClass) throws IOException {
    return JSON.parseObject(getResponse(method, urlStr, body), targetClass);
  }

  // GET
  // ---------------------------------

  public static String get(String urlStr) throws IOException {
    return getResponse(getConnection("GET", urlStr));
  }

  // POST
  // ---------------------------------

  public static String post(String urlStr) throws IOException {
    return getResponse(getConnection("POST", urlStr));
  }

  public static String post(String urlStr, byte[] body) throws IOException {
    return getResponse(getConnection("POST", urlStr, body));
  }

  public static String post(String urlStr, String body) throws IOException {
    return getResponse(getConnection("POST", urlStr, body.getBytes(DEFAULT_CHARSET)));
  }

  public static <T> T post(String urlStr, byte[] body, Class<T> targetClass) throws IOException {
    return JSON.parseObject(getResponse("POST", urlStr, body), targetClass);
  }

  public static <T> T post(String urlStr, String body, Class<T> targetClass) throws IOException {
    return JSON.parseObject(getResponse("POST", urlStr, body), targetClass);
  }

  // PUT
  // ---------------------------------

  public static String put(String urlStr) throws IOException {
    return getResponse(getConnection("PUT", urlStr));
  }

  public static String put(String urlStr, byte[] body) throws IOException {
    return getResponse(getConnection("PUT", urlStr, body));
  }

  public static String put(String urlStr, String body) throws IOException {
    return getResponse(getConnection("PUT", urlStr, body.getBytes(DEFAULT_CHARSET)));
  }

  public static <T> T put(String urlStr, byte[] body, Class<T> targetClass) throws IOException {
    return JSON.parseObject(getResponse("PUT", urlStr, body), targetClass);
  }

  // DELETE
  // ---------------------------------

  public static String delete(String urlStr) throws IOException {
    return getResponse(getConnection("DELETE", urlStr));
  }

  public static String delete(String urlStr, byte[] body) throws IOException {
    return getResponse(getConnection("DELETE", urlStr, body));
  }

  public static String delete(String urlStr, String body) throws IOException {
    return getResponse(getConnection("DELETE", urlStr, body.getBytes(DEFAULT_CHARSET)));
  }

  public static <T> T delete(String urlStr, byte[] body, Class<T> targetClass) throws IOException {
    return JSON.parseObject(getResponse("DELETE", urlStr, body), targetClass);
  }
}
