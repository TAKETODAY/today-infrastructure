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

package cn.taketoday.web.client.config;

import org.junit.jupiter.api.Test;

import java.io.File;

import cn.taketoday.http.client.OkHttp3ClientHttpRequestFactory;
import cn.taketoday.test.classpath.ClassPathExclusions;
import cn.taketoday.test.util.ReflectionTestUtils;
import okhttp3.OkHttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ClientHttpRequestFactories} when OkHttp 4 is the predominant HTTP
 * client.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("httpclient-*.jar")
class ClientHttpRequestFactoriesOkHttp4Tests
        extends AbstractClientHttpRequestFactoriesTests<OkHttp3ClientHttpRequestFactory> {

  ClientHttpRequestFactoriesOkHttp4Tests() {
    super(OkHttp3ClientHttpRequestFactory.class);
  }

  @Test
  void okHttp4IsBeingUsed() {
    assertThat(new File(OkHttpClient.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getName())
            .startsWith("okhttp-4.");
  }

  @Test
  void getFailsWhenBufferRequestBodyIsEnabled() {
    assertThatIllegalStateException().isThrownBy(() -> ClientHttpRequestFactories
            .get(ClientHttpRequestFactorySettings.DEFAULTS.withBufferRequestBody(true)));
  }

  @Override
  protected long connectTimeout(OkHttp3ClientHttpRequestFactory requestFactory) {
    return ((OkHttpClient) ReflectionTestUtils.getField(requestFactory, "client")).connectTimeoutMillis();
  }

  @Override
  protected long readTimeout(OkHttp3ClientHttpRequestFactory requestFactory) {
    return ((OkHttpClient) ReflectionTestUtils.getField(requestFactory, "client")).readTimeoutMillis();
  }

}
