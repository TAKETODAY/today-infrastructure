/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.client;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import cn.taketoday.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link IntrospectingClientHttpResponse}.
 *
 * @author Yin-Jui Liao
 * @since 4.0
 */
class IntrospectingClientHttpResponseTests {

  private final ClientHttpResponse response = mock(ClientHttpResponse.class);

  private final IntrospectingClientHttpResponse responseWrapper = new IntrospectingClientHttpResponse(response);

  @Test
  void messageBodyDoesNotExist() throws Exception {
    given(response.getBody()).willReturn(null);
    assertThat(responseWrapper.hasEmptyMessageBody()).isTrue();
  }

  @Test
  void messageBodyExists() throws Exception {
    InputStream stream = new ByteArrayInputStream("content".getBytes());
    given(response.getBody()).willReturn(stream);
    assertThat(responseWrapper.hasEmptyMessageBody()).isFalse();
  }

}
