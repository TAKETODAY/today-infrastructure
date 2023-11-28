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

package cn.taketoday.framework.web.server;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.lang.Nullable;

/**
 * Configuration properties for server HTTP encoding.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see StringHttpMessageConverter
 * @since 4.0
 */
public class EncodingProperties {

  /**
   * Default HTTP encoding for Servlet applications.
   */
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  /**
   * Charset of HTTP requests and responses. Added to the "Content-Type" header if not
   * set explicitly.
   */
  private Charset charset = DEFAULT_CHARSET;

  /**
   * Locale in which to encode mapping.
   */
  @Nullable
  private Map<Locale, Charset> mapping;

  public Charset getCharset() {
    return this.charset;
  }

  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  @Nullable
  public Map<Locale, Charset> getMapping() {
    return this.mapping;
  }

  public void setMapping(@Nullable Map<Locale, Charset> mapping) {
    this.mapping = mapping;
  }

}
