/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.framework.server.light;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import cn.taketoday.http.HttpHeaders;

/**
 * @author TODAY 2021/4/13 14:43
 */
public class LightHttpServerUtilsTests {
  static String http = "GET /home.html HTTP/1.1\n" +
          "Host: developer.mozilla.org\n" +
          "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:50.0) Gecko/20100101 Firefox/50.0\n" +
          "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\n" +
          "Accept-Language: en-US,en;q=0.5\n" +
          "Accept-Encoding: gzip, deflate, br\n" +
          "Referer: https://developer.mozilla.org/testpage.html\n" +
          "Connection: keep-alive\n" +
          "Upgrade-Insecure-Requests: 1\n" +
          "If-Modified-Since: Mon, 18 Jul 2016 02:36:04 GMT\n" +
          "If-None-Match: \"c561c68d0ba92bbeb8b0fff2a9199f722e3a621a\"\n" +
          "Cache-Control: max-age=0\n" +
          "\r\n" +
          "";

  @Test
  public void testReadHeaders() throws IOException {
    String header = "Host: developer.mozilla.org\n" +
            "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:50.0) Gecko/20100101 Firefox/50.0\n" +
            "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\n" +
            "Accept-Language: en-US,en;q=0.5\n" +
            "Accept-Encoding: gzip, deflate, br\n" +
            "Referer: https://developer.mozilla.org/testpage.html\n" +
            "Connection: keep-alive\n" +
            "Upgrade-Insecure-Requests: 1\n" +
            "If-Modified-Since: Mon, 18 Jul 2016 02:36:04 GMT\n" +
            "If-None-Match: \"c561c68d0ba92bbeb8b0fff2a9199f722e3a621a\"\n" +
            "Cache-Control: max-age=0\n" +
            "\r\n" +
            "";

    final byte[] bytes = http.getBytes(StandardCharsets.UTF_8);

    final LightHttpConfig config = LightHttpConfig.defaultConfig();
    final HttpHeaders strings = Utils.readHeaders(new ByteArrayInputStream(header.getBytes(StandardCharsets.UTF_8)), config);

    System.out.println(strings);
  }

}
