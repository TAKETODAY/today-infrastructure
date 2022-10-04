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

package cn.taketoday.framework.web.client;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.OkHttp3ClientHttpRequestFactory;
import cn.taketoday.test.classpath.ClassPathOverrides;
import cn.taketoday.web.client.config.RestTemplateBuilder;
import okhttp3.OkHttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link RestTemplateBuilder} with OkHttp 3.x.
 *
 * @author Andy Wilkinson
 */
//@ClassPathOverrides("com.squareup.okhttp3:okhttp:3.14.9")
class RestTemplateBuilderTestsOkHttp3Tests {

  private RestTemplateBuilder builder = new RestTemplateBuilder();

  @Test
  void connectTimeoutCanBeConfiguredOnOkHttpRequestFactory() {
    ClientHttpRequestFactory requestFactory = this.builder.requestFactory(OkHttp3ClientHttpRequestFactory.class)
            .setConnectTimeout(Duration.ofMillis(1234)).build().getRequestFactory();
    assertThat(requestFactory).extracting("client", InstanceOfAssertFactories.type(OkHttpClient.class))
            .extracting(OkHttpClient::connectTimeoutMillis).isEqualTo(1234);
  }

  @Test
  void readTimeoutCanBeConfiguredOnOkHttpRequestFactory() {
    ClientHttpRequestFactory requestFactory = this.builder.requestFactory(OkHttp3ClientHttpRequestFactory.class)
            .setReadTimeout(Duration.ofMillis(1234)).build().getRequestFactory();
    assertThat(requestFactory).extracting("client", InstanceOfAssertFactories.type(OkHttpClient.class))
            .extracting(OkHttpClient::readTimeoutMillis).isEqualTo(1234);
  }

  @Test
  void bufferRequestBodyCanNotBeConfiguredOnOkHttpRequestFactory() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.builder.requestFactory(OkHttp3ClientHttpRequestFactory.class)
                    .setBufferRequestBody(false).build().getRequestFactory())
            .withMessageContaining(OkHttp3ClientHttpRequestFactory.class.getName());
  }

}
