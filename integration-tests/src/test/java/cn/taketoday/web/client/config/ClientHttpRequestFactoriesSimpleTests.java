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

import cn.taketoday.http.client.SimpleClientHttpRequestFactory;
import cn.taketoday.test.classpath.ClassPathExclusions;
import cn.taketoday.test.util.ReflectionTestUtils;

/**
 * Tests for {@link ClientHttpRequestFactories} when the simple JDK-based client is the
 * predominant HTTP client.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions({ "httpclient-*.jar", "okhttp-*.jar" })
class ClientHttpRequestFactoriesSimpleTests
        extends AbstractClientHttpRequestFactoriesTests<SimpleClientHttpRequestFactory> {

  ClientHttpRequestFactoriesSimpleTests() {
    super(SimpleClientHttpRequestFactory.class);
  }

  @Override
  protected long connectTimeout(SimpleClientHttpRequestFactory requestFactory) {
    return (int) ReflectionTestUtils.getField(requestFactory, "connectTimeout");
  }

  @Override
  protected long readTimeout(SimpleClientHttpRequestFactory requestFactory) {
    return (int) ReflectionTestUtils.getField(requestFactory, "readTimeout");
  }

}
