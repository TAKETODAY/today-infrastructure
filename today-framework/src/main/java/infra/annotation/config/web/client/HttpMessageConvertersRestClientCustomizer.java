/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.annotation.config.web.client;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters;
import infra.lang.Assert;
import infra.web.client.RestClient;
import infra.web.client.config.RestClientCustomizer;

/**
 * {@link RestClientCustomizer} to apply {@link HttpMessageConverter
 * HttpMessageConverters}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HttpMessageConvertersRestClientCustomizer implements RestClientCustomizer {

  @Nullable
  private final Iterable<? extends HttpMessageConverter<?>> messageConverters;

  public HttpMessageConvertersRestClientCustomizer(HttpMessageConverter<?>... messageConverters) {
    Assert.notNull(messageConverters, "MessageConverters is required");
    this.messageConverters = Arrays.asList(messageConverters);
  }

  HttpMessageConvertersRestClientCustomizer(@Nullable HttpMessageConverters messageConverters) {
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
