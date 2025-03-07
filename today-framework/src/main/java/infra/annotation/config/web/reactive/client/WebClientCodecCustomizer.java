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

package infra.annotation.config.web.reactive.client;

import infra.http.codec.CodecCustomizer;
import infra.web.client.reactive.WebClient;

/**
 * {@link WebClientCustomizer} that configures codecs for the HTTP client.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebClientCodecCustomizer implements WebClientCustomizer {

  private final Iterable<CodecCustomizer> codecCustomizers;

  public WebClientCodecCustomizer(Iterable<CodecCustomizer> codecCustomizers) {
    this.codecCustomizers = codecCustomizers;
  }

  @Override
  public void customize(WebClient.Builder webClientBuilder) {
    webClientBuilder.codecs(codecs -> {
      for (CodecCustomizer customizer : codecCustomizers) {
        customizer.customize(codecs);
      }
    });
  }

}
