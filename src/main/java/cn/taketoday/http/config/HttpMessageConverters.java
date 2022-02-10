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

package cn.taketoday.http.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.json.GsonHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.config.WebMvcConfigurationSupport;

/**
 * Bean used to manage the {@link HttpMessageConverter}s used in application.
 * Provides a convenient way to add and merge additional
 * {@link HttpMessageConverter}s to a web application.
 * <p>
 * An instance of this bean can be registered with specific
 * {@link #HttpMessageConverters(HttpMessageConverter...) additional converters} if
 * needed, otherwise default converters will be used.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #HttpMessageConverters(HttpMessageConverter...)
 * @see #HttpMessageConverters(Collection)
 * @see #getConverters()
 * @since 4.0 2022/1/16 15:26
 */
public class HttpMessageConverters implements Iterable<HttpMessageConverter<?>> {

  private static final Map<Class<?>, Class<?>> EQUIVALENT_CONVERTERS;

  static {
    Map<Class<?>, Class<?>> equivalentConverters = new HashMap<>();
    try {
      equivalentConverters.put(MappingJackson2HttpMessageConverter.class, GsonHttpMessageConverter.class);
    }
    catch (Throwable ignored) { }
    EQUIVALENT_CONVERTERS = Collections.unmodifiableMap(equivalentConverters);
  }

  private final List<HttpMessageConverter<?>> converters;

  /**
   * Create a new {@link HttpMessageConverters} instance with the specified additional
   * converters.
   *
   * @param additionalConverters additional converters to be added. Items are added just
   * before any default converter of the same type (or at the front of the list if no
   * default converter is found). The {@link #postProcessConverters(List)} method can be
   * used for further converter manipulation.
   */
  public HttpMessageConverters(HttpMessageConverter<?>... additionalConverters) {
    this(Arrays.asList(additionalConverters));
  }

  /**
   * Create a new {@link HttpMessageConverters} instance with the specified additional
   * converters.
   *
   * @param additionalConverters additional converters to be added. Items are added just
   * before any default converter of the same type (or at the front of the list if no
   * default converter is found). The {@link #postProcessConverters(List)} method can be
   * used for further converter manipulation.
   */
  public HttpMessageConverters(Collection<HttpMessageConverter<?>> additionalConverters) {
    this(true, additionalConverters);
  }

  /**
   * Create a new {@link HttpMessageConverters} instance with the specified converters.
   *
   * @param addDefaultConverters if default converters should be added
   * @param converters converters to be added. Items are added just before any default
   * converter of the same type (or at the front of the list if no default converter is
   * found). The {@link #postProcessConverters(List)} method can be used for further
   * converter manipulation.
   */
  public HttpMessageConverters(boolean addDefaultConverters, Collection<HttpMessageConverter<?>> converters) {
    List<HttpMessageConverter<?>> combined = getCombinedConverters(
            converters, addDefaultConverters ? getDefaultConverters() : Collections.emptyList());
    combined = postProcessConverters(combined);
    this.converters = Collections.unmodifiableList(combined);
  }

  private List<HttpMessageConverter<?>> getCombinedConverters(
          Collection<HttpMessageConverter<?>> converters, List<HttpMessageConverter<?>> defaultConverters) {

    ArrayList<HttpMessageConverter<?>> combined = new ArrayList<>();
    ArrayList<HttpMessageConverter<?>> processing = new ArrayList<>(converters);
    for (HttpMessageConverter<?> defaultConverter : defaultConverters) {
      Iterator<HttpMessageConverter<?>> iterator = processing.iterator();
      while (iterator.hasNext()) {
        HttpMessageConverter<?> candidate = iterator.next();
        if (isReplacement(defaultConverter, candidate)) {
          combined.add(candidate);
          iterator.remove();
        }
      }
      combined.add(defaultConverter);
      if (defaultConverter instanceof AllEncompassingFormHttpMessageConverter allEncompassingConverters) {
        configurePartConverters(allEncompassingConverters, converters);
      }
    }
    combined.addAll(0, processing);
    return combined;
  }

  private boolean isReplacement(HttpMessageConverter<?> defaultConverter, HttpMessageConverter<?> candidate) {
    Class<?> converterClass = defaultConverter.getClass();
    if (ClassUtils.isAssignableValue(converterClass, candidate)) {
      return true;
    }
    Class<?> equivalentClass = EQUIVALENT_CONVERTERS.get(converterClass);
    return equivalentClass != null && ClassUtils.isAssignableValue(equivalentClass, candidate);
  }

  private void configurePartConverters(
          AllEncompassingFormHttpMessageConverter formConverter,
          Collection<HttpMessageConverter<?>> converters) {
    List<HttpMessageConverter<?>> partConverters = formConverter.getPartConverters();
    List<HttpMessageConverter<?>> combinedConverters = getCombinedConverters(converters, partConverters);
    combinedConverters = postProcessPartConverters(combinedConverters);
    formConverter.setPartConverters(combinedConverters);
  }

  /**
   * Method that can be used to post-process the {@link HttpMessageConverter} list
   * before it is used.
   *
   * @param converters a mutable list of the converters that will be used.
   * @return the final converts list to use
   */
  protected List<HttpMessageConverter<?>> postProcessConverters(List<HttpMessageConverter<?>> converters) {
    return converters;
  }

  /**
   * Method that can be used to post-process the {@link HttpMessageConverter} list
   * before it is used to configure the part converters of
   * {@link AllEncompassingFormHttpMessageConverter}.
   *
   * @param converters a mutable list of the converters that will be used.
   * @return the final converts list to use
   */
  protected List<HttpMessageConverter<?>> postProcessPartConverters(List<HttpMessageConverter<?>> converters) {
    return converters;
  }

  private List<HttpMessageConverter<?>> getDefaultConverters() {
    return new ArrayList<>(new WebMvcConfigurationSupport().getMessageConverters());
  }

  @Override
  public Iterator<HttpMessageConverter<?>> iterator() {
    return getConverters().iterator();
  }

  /**
   * Return an immutable list of the converters in the order that they will be
   * registered.
   *
   * @return the converters
   */
  public List<HttpMessageConverter<?>> getConverters() {
    return this.converters;
  }

}
