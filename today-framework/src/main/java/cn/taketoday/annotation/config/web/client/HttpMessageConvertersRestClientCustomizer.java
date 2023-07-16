/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.web.client;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.client.RestClient;
import cn.taketoday.web.client.config.RestClientCustomizer;

/**
 * {@link RestClientCustomizer} to apply {@link HttpMessageConverter
 * HttpMessageConverters}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HttpMessageConvertersRestClientCustomizer implements RestClientCustomizer {

  private final Iterable<? extends HttpMessageConverter<?>> messageConverters;

  public HttpMessageConvertersRestClientCustomizer(HttpMessageConverter<?>... messageConverters) {
    Assert.notNull(messageConverters, "MessageConverters must not be null");
    this.messageConverters = Arrays.asList(messageConverters);
  }

  HttpMessageConvertersRestClientCustomizer(HttpMessageConverters messageConverters) {
    this.messageConverters = messageConverters;
  }

  @Override
  public void customize(RestClient.Builder restClientBuilder) {
    restClientBuilder.messageConverters(this::configureMessageConverters);
  }

  private void configureMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    if (this.messageConverters != null) {
      messageConverters.clear();
      this.messageConverters.forEach(messageConverters::add);
    }
  }

}
