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

import java.net.URI;
import java.util.Arrays;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BufferingClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

  @Override
  protected ClientHttpRequestFactory createRequestFactory() {
    return new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory());
  }

  @Test
  void repeatableRead() throws Exception {
    ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/echo"), HttpMethod.PUT);
    assertThat(request.getMethod()).as("Invalid HTTP method").isEqualTo(HttpMethod.PUT);
    String headerName = "MyHeader";
    String headerValue1 = "value1";
    request.getHeaders().add(headerName, headerValue1);
    String headerValue2 = "value2";
    request.getHeaders().add(headerName, headerValue2);
    byte[] body = "Hello World".getBytes("UTF-8");
    request.getHeaders().setContentLength(body.length);
    FileCopyUtils.copy(body, request.getBody());
    try (ClientHttpResponse response = request.execute()) {
      assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
      assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);

      assertThat(response.getHeaders().containsKey(headerName)).as("Header not found").isTrue();
      assertThat(response.getHeaders().containsKey(headerName)).as("Header not found").isTrue();

      assertThat(response.getHeaders().get(headerName)).as("Header value not found").isEqualTo(Arrays.asList(headerValue1, headerValue2));
      assertThat(response.getHeaders().get(headerName)).as("Header value not found").isEqualTo(Arrays.asList(headerValue1, headerValue2));

      byte[] result = FileCopyUtils.copyToByteArray(response.getBody());
      assertThat(Arrays.equals(body, result)).as("Invalid body").isTrue();
      FileCopyUtils.copyToByteArray(response.getBody());
      assertThat(Arrays.equals(body, result)).as("Invalid body").isTrue();
    }
  }

}
