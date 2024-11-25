/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.client.config;

import org.junit.jupiter.api.Disabled;

import java.time.Duration;

import infra.http.client.JdkClientHttpRequestFactory;
import infra.test.classpath.ClassPathExclusions;
import infra.test.util.ReflectionTestUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/9/23 22:41
 */
@ClassPathExclusions({ "httpclient5-*.jar" })
class ClientHttpRequestFactoriesJdkTests
        extends AbstractClientHttpRequestFactoriesTests<JdkClientHttpRequestFactory> {

  ClientHttpRequestFactoriesJdkTests() {
    super(JdkClientHttpRequestFactory.class);
  }

  @Override
  protected long connectTimeout(JdkClientHttpRequestFactory requestFactory) {
    return 0;
  }

  @Override
  protected long readTimeout(JdkClientHttpRequestFactory requestFactory) {
    Duration readTimeout = ReflectionTestUtils.getField(requestFactory, "readTimeout");
    return readTimeout != null ? readTimeout.toMillis() : 0;
  }

  @Disabled
  void getReturnsRequestFactoryWithConfiguredConnectTimeout() {

  }

}