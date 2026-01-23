/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.http.converter.config;

import java.util.Collection;

import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters;
import infra.http.converter.HttpMessageConverters.ClientBuilder;
import infra.http.converter.HttpMessageConverters.ServerBuilder;

class DefaultHttpMessageConvertersCustomizer implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {

  private final Collection<HttpMessageConverter<?>> converters;

  DefaultHttpMessageConvertersCustomizer(Collection<HttpMessageConverter<?>> converters) {
    this.converters = converters;
  }

  @Override
  public void customize(ClientBuilder builder) {
    customizeBuilder(builder);
  }

  @Override
  public void customize(ServerBuilder builder) {
    customizeBuilder(builder);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void customizeBuilder(HttpMessageConverters.Builder builder) {
    builder.registerDefaults().addCustomConverters(converters);
  }

}
