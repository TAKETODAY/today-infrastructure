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

package cn.taketoday.http.client;

import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;

import cn.taketoday.http.HttpHeaders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Nicoll
 */
public class SimpleClientHttpRequestFactoryTests {

  @Test // SPR-13225
  public void headerWithNullValue() {
    HttpURLConnection urlConnection = mock(HttpURLConnection.class);
    given(urlConnection.getRequestMethod()).willReturn("GET");
    HttpHeaders headers = HttpHeaders.create();
    headers.set("foo", null);
    SimpleBufferingClientHttpRequest.addHeaders(urlConnection, headers);
    verify(urlConnection, times(1)).addRequestProperty("foo", "");
  }

}
