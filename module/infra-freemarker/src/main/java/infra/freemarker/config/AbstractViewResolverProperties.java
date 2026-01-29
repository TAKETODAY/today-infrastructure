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

package infra.freemarker.config;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import infra.context.properties.ConfigurationProperties;
import infra.http.MediaType;
import infra.lang.Constant;
import infra.util.MimeType;

/**
 * Base class for {@link ConfigurationProperties @ConfigurationProperties} of a
 * {@link infra.web.view.ViewResolver}.
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
   * Whether to enable view resolution for this technology.
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
  private String @Nullable [] viewNames;

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

  public String @Nullable [] getViewNames() {
    return this.viewNames;
  }

  public void setViewNames(String @Nullable ... viewNames) {
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
