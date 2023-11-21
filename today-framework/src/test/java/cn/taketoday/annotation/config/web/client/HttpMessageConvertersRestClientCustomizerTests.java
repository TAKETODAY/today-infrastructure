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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpMessageConvertersRestClientCustomizer}
 *
 * @author Phillip Webb
 */
class HttpMessageConvertersRestClientCustomizerTests {

  @Test
  void createWhenNullMessageConvertersArrayThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new HttpMessageConvertersRestClientCustomizer((HttpMessageConverter<?>[]) null))
            .withMessage("MessageConverters is required");
  }

  @Test
  void createWhenNullMessageConvertersDoesNotCustomize() {
    HttpMessageConverter<?> c0 = mock();
    assertThat(apply(new HttpMessageConvertersRestClientCustomizer((HttpMessageConverters) null), c0))
            .containsExactly(c0);
  }

  @Test
  void customizeConfiguresMessageConverters() {
    HttpMessageConverter<?> c0 = mock();
    HttpMessageConverter<?> c1 = mock();
    HttpMessageConverter<?> c2 = mock();
    assertThat(apply(new HttpMessageConvertersRestClientCustomizer(c1, c2), c0)).containsExactly(c1, c2);
  }

  @SuppressWarnings("unchecked")
  private List<HttpMessageConverter<?>> apply(HttpMessageConvertersRestClientCustomizer customizer,
          HttpMessageConverter<?>... converters) {
    List<HttpMessageConverter<?>> messageConverters = new ArrayList<>(Arrays.asList(converters));
    RestClient.Builder restClientBuilder = mock();
    ArgumentCaptor<Consumer<List<HttpMessageConverter<?>>>> captor = ArgumentCaptor.forClass(Consumer.class);
    given(restClientBuilder.messageConverters(captor.capture())).willReturn(restClientBuilder);
    customizer.customize(restClientBuilder);
    captor.getValue().accept(messageConverters);
    return messageConverters;
  }

}
