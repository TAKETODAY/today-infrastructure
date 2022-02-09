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

package cn.taketoday.http.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.ByteArrayHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.ResourceHttpMessageConverter;
import cn.taketoday.http.converter.ResourceRegionHttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import cn.taketoday.http.converter.feed.AtomFeedHttpMessageConverter;
import cn.taketoday.http.converter.feed.RssChannelHttpMessageConverter;
import cn.taketoday.http.converter.json.GsonHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.http.converter.smile.MappingJackson2SmileHttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/9 23:43
 */
class HttpMessageConvertersTests {

  @Test
  void containsDefaults() {
    HttpMessageConverters converters = new HttpMessageConverters();
    List<Class<?>> converterClasses = new ArrayList<>();
    for (HttpMessageConverter<?> converter : converters) {
      converterClasses.add(converter.getClass());
    }
    assertThat(converterClasses).containsExactly(
            ByteArrayHttpMessageConverter.class,
            StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
            ResourceRegionHttpMessageConverter.class,
            AllEncompassingFormHttpMessageConverter.class,
            AtomFeedHttpMessageConverter.class,
            RssChannelHttpMessageConverter.class,

            MappingJackson2HttpMessageConverter.class,
            MappingJackson2SmileHttpMessageConverter.class, MappingJackson2CborHttpMessageConverter.class);
  }

  @Test
  void addBeforeExistingConverter() {
    MappingJackson2HttpMessageConverter converter1 = new MappingJackson2HttpMessageConverter();
    MappingJackson2HttpMessageConverter converter2 = new MappingJackson2HttpMessageConverter();
    HttpMessageConverters converters = new HttpMessageConverters(converter1, converter2);
    assertThat(converters.getConverters().contains(converter1)).isTrue();
    assertThat(converters.getConverters().contains(converter2)).isTrue();
    List<MappingJackson2HttpMessageConverter> httpConverters = new ArrayList<>();
    for (HttpMessageConverter<?> candidate : converters) {
      if (candidate instanceof MappingJackson2HttpMessageConverter) {
        httpConverters.add((MappingJackson2HttpMessageConverter) candidate);
      }
    }
    // The existing converter is still there, but with a lower priority
    assertThat(httpConverters).hasSize(3);
    assertThat(httpConverters.indexOf(converter1)).isEqualTo(0);
    assertThat(httpConverters.indexOf(converter2)).isEqualTo(1);
    assertThat(converters.getConverters().indexOf(converter1)).isNotEqualTo(0);
  }

  @Test
  void addBeforeExistingEquivalentConverter() {
    GsonHttpMessageConverter converter1 = new GsonHttpMessageConverter();
    HttpMessageConverters converters = new HttpMessageConverters(converter1);
    List<Class<?>> converterClasses = converters.getConverters().stream().map(HttpMessageConverter::getClass)
            .collect(Collectors.toList());
    assertThat(converterClasses).containsSequence(GsonHttpMessageConverter.class,
            MappingJackson2HttpMessageConverter.class);
  }

  @Test
  void addNewConverters() {
    HttpMessageConverter<?> converter1 = mock(HttpMessageConverter.class);
    HttpMessageConverter<?> converter2 = mock(HttpMessageConverter.class);
    HttpMessageConverters converters = new HttpMessageConverters(converter1, converter2);
    assertThat(converters.getConverters().get(0)).isEqualTo(converter1);
    assertThat(converters.getConverters().get(1)).isEqualTo(converter2);
  }

  @Test
  void convertersAreAddedToFormPartConverter() {
    HttpMessageConverter<?> converter1 = mock(HttpMessageConverter.class);
    HttpMessageConverter<?> converter2 = mock(HttpMessageConverter.class);
    List<HttpMessageConverter<?>> converters = new HttpMessageConverters(converter1, converter2).getConverters();
    List<HttpMessageConverter<?>> partConverters = extractFormPartConverters(converters);
    assertThat(partConverters.get(0)).isEqualTo(converter1);
    assertThat(partConverters.get(1)).isEqualTo(converter2);
  }

  @Test
  void postProcessConverters() {
    HttpMessageConverters converters = new HttpMessageConverters() {

      @Override
      protected List<HttpMessageConverter<?>> postProcessConverters(List<HttpMessageConverter<?>> converters) {
        return converters;
      }

    };
    List<Class<?>> converterClasses = new ArrayList<>();
    for (HttpMessageConverter<?> converter : converters) {
      converterClasses.add(converter.getClass());
    }
    assertThat(converterClasses).containsExactly(ByteArrayHttpMessageConverter.class,
            StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
            ResourceRegionHttpMessageConverter.class,
            AllEncompassingFormHttpMessageConverter.class,

            AtomFeedHttpMessageConverter.class,
            RssChannelHttpMessageConverter.class,

            MappingJackson2HttpMessageConverter.class,
            MappingJackson2SmileHttpMessageConverter.class, MappingJackson2CborHttpMessageConverter.class);
  }

  @Test
  void postProcessPartConverters() {
    HttpMessageConverters converters = new HttpMessageConverters() {

      @Override
      protected List<HttpMessageConverter<?>> postProcessPartConverters(
              List<HttpMessageConverter<?>> converters) {
        return converters;
      }

    };
    List<Class<?>> converterClasses = new ArrayList<>();
    for (HttpMessageConverter<?> converter : extractFormPartConverters(converters.getConverters())) {
      converterClasses.add(converter.getClass());
    }
    assertThat(converterClasses).containsExactly(ByteArrayHttpMessageConverter.class,
            StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
            MappingJackson2HttpMessageConverter.class, MappingJackson2SmileHttpMessageConverter.class);
  }

  private List<HttpMessageConverter<?>> extractFormPartConverters(List<HttpMessageConverter<?>> converters) {
    AllEncompassingFormHttpMessageConverter formConverter = findFormConverter(converters);
    return formConverter.getPartConverters();
  }

  private AllEncompassingFormHttpMessageConverter findFormConverter(Collection<HttpMessageConverter<?>> converters) {
    for (HttpMessageConverter<?> converter : converters) {
      if (converter instanceof AllEncompassingFormHttpMessageConverter) {
        return (AllEncompassingFormHttpMessageConverter) converter;
      }
    }
    return null;
  }

}
