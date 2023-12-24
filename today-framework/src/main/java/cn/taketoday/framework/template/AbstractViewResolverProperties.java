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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.template;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import cn.taketoday.web.view.ViewResolver;

/**
 * Base class for {@link ConfigurationProperties @ConfigurationProperties} of a
 * {@link ViewResolver}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractTemplateViewResolverProperties
 * @since 4.0
 */
public abstract class AbstractViewResolverProperties {

  private static final MimeType DEFAULT_CONTENT_TYPE = MediaType.TEXT_HTML;

  /**
   * Whether to enable MVC view resolution for this technology.
   */
  private boolean enabled = true;

  /**
   * Whether to enable template caching.
   */
  private boolean cache;

  /**
   * Content-Type value.
   */
  private MimeType contentType = DEFAULT_CONTENT_TYPE;

  /**
   * Template encoding.
   */
  private Charset charset = Constant.DEFAULT_CHARSET;

  /**
   * View names that can be resolved.
   */
  private String[] viewNames;

  /**
   * Whether to check that the templates location exists.
   */
  private boolean checkTemplateLocation = true;

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void setCheckTemplateLocation(boolean checkTemplateLocation) {
    this.checkTemplateLocation = checkTemplateLocation;
  }

  public boolean isCheckTemplateLocation() {
    return this.checkTemplateLocation;
  }

  public String[] getViewNames() {
    return this.viewNames;
  }

  public void setViewNames(String[] viewNames) {
    this.viewNames = viewNames;
  }

  public boolean isCache() {
    return this.cache;
  }

  public void setCache(boolean cache) {
    this.cache = cache;
  }

  public MimeType getContentType() {
    if (this.contentType.getCharset() == null) {
      Map<String, String> parameters = new LinkedHashMap<>();
      parameters.put("charset", this.charset.name());
      parameters.putAll(this.contentType.getParameters());
      return new MimeType(this.contentType, parameters);
    }
    return this.contentType;
  }

  public void setContentType(@Nullable MimeType contentType) {
    this.contentType = contentType == null ? DEFAULT_CONTENT_TYPE : contentType;
  }

  public Charset getCharset() {
    return this.charset;
  }

  public String getCharsetName() {
    return this.charset.name();
  }

  public void setCharset(@Nullable Charset charset) {
    this.charset = charset == null ? Constant.DEFAULT_CHARSET : charset;
  }

}
