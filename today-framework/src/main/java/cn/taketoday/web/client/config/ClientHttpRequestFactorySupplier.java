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

package cn.taketoday.web.client.config;

import java.util.function.Supplier;

import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.http.client.OkHttp3ClientHttpRequestFactory;
import cn.taketoday.http.client.SimpleClientHttpRequestFactory;
import cn.taketoday.util.ClassUtils;

/**
 * A supplier for {@link ClientHttpRequestFactory} that detects the preferred candidate
 * based on the available implementations on the classpath.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public class ClientHttpRequestFactorySupplier implements Supplier<ClientHttpRequestFactory> {

  @Override
  public ClientHttpRequestFactory get() {
    ClassLoader classLoader = getClass().getClassLoader();
    if (ClassUtils.isPresent("org.apache.http.client.HttpClient", classLoader)) {
      return new HttpComponentsClientHttpRequestFactory();
    }

    if (ClassUtils.isPresent("okhttp3.OkHttpClient", classLoader)) {
      return new OkHttp3ClientHttpRequestFactory();
    }
    return new SimpleClientHttpRequestFactory();
  }

}
