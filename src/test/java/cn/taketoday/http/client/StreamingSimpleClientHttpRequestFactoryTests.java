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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Random;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class StreamingSimpleClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

  @Override
  protected ClientHttpRequestFactory createRequestFactory() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setBufferRequestBody(false);
    return factory;
  }

  @Test  // SPR-8809
  public void interceptor() throws Exception {
    final String headerName = "MyHeader";
    final String headerValue = "MyValue";
    ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
      request.getHeaders().add(headerName, headerValue);
      return execution.execute(request, body);
    };
    InterceptingClientHttpRequestFactory factory = new InterceptingClientHttpRequestFactory(
            createRequestFactory(), Collections.singletonList(interceptor));

    ClientHttpResponse response = null;
    try {
      ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/echo"), HttpMethod.GET);
      response = request.execute();
      assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
      HttpHeaders responseHeaders = response.getHeaders();
      assertThat(responseHeaders.getFirst(headerName)).as("Custom header invalid").isEqualTo(headerValue);
    }
    finally {
      if (response != null) {
        response.close();
      }
    }
  }

  @Test
  @Disabled
  public void largeFileUpload() throws Exception {
    Random rnd = new Random();
    ClientHttpResponse response = null;
    try {
      ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/methods/post"), HttpMethod.POST);
      final int BUF_SIZE = 4096;
      final int ITERATIONS = Integer.MAX_VALUE / BUF_SIZE;
      // final int contentLength = ITERATIONS * BUF_SIZE;
      // request.getHeaders().setContentLength(contentLength);
      OutputStream body = request.getBody();
      for (int i = 0; i < ITERATIONS; i++) {
        byte[] buffer = new byte[BUF_SIZE];
        rnd.nextBytes(buffer);
        body.write(buffer);
      }
      response = request.execute();
      assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
    }
    finally {
      if (response != null) {
        response.close();
      }
    }
  }

}
