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

package infra.annotation.config.freemarker;

import org.jspecify.annotations.Nullable;

import infra.context.properties.ConfigurationProperties;
import infra.core.Ordered;
import infra.web.view.AbstractTemplateViewResolver;

/**
 * Base class for {@link ConfigurationProperties @ConfigurationProperties} of a
 * {@link AbstractTemplateViewResolver}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Andy Wilkinson
 * @since 4.0
 */
public abstract class AbstractTemplateViewResolverProperties extends AbstractViewResolverProperties {

  /**
   * Prefix that gets prepended to view names when building a URL.
   */
  private String prefix;

  /**
   * Suffix that gets appended to view names when building a URL.
   */
  private String suffix;

  /**
   * Name of the RequestContext attribute for all views.
   */
  @Nullable
  private String requestContextAttribute;

  /**
   * Whether all request attributes should be added to the model prior to merging with
   * the template.
   */
  private boolean exposeRequestAttributes = false;

  /**
   * Whether all HttpSession attributes should be added to the model prior to merging
   * with the template.
   */
  private boolean exposeSessionAttributes = false;

  /**
   * Whether HttpServletRequest attributes are allowed to override (hide) controller
   * generated model attributes of the same name.
   */
  private boolean allowRequestOverride = false;

  /**
   * Whether HttpSession attributes are allowed to override (hide) controller generated
   * model attributes of the same name.
   */
  private boolean allowSessionOverride = false;

  protected AbstractTemplateViewResolverProperties(String defaultPrefix, String defaultSuffix) {
    this.prefix = defaultPrefix;
    this.suffix = defaultSuffix;
  }

  public String getPrefix() {
    return this.prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getSuffix() {
    return this.suffix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  @Nullable
  public String getRequestContextAttribute() {
    return this.requestContextAttribute;
  }

  public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
    this.requestContextAttribute = requestContextAttribute;
  }

  public boolean isExposeRequestAttributes() {
    return this.exposeRequestAttributes;
  }

  public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
    this.exposeRequestAttributes = exposeRequestAttributes;
  }

  public boolean isExposeSessionAttributes() {
    return this.exposeSessionAttributes;
  }

  public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
    this.exposeSessionAttributes = exposeSessionAttributes;
  }

  public boolean isAllowRequestOverride() {
    return this.allowRequestOverride;
  }

  public void setAllowRequestOverride(boolean allowRequestOverride) {
    this.allowRequestOverride = allowRequestOverride;
  }

  public boolean isAllowSessionOverride() {
    return this.allowSessionOverride;
  }

  public void setAllowSessionOverride(boolean allowSessionOverride) {
    this.allowSessionOverride = allowSessionOverride;
  }

  /**
   * Apply the given properties to a {@link AbstractTemplateViewResolver}. Use Object in
   * signature to avoid runtime dependency on MVC, which means that the template engine
   * can be used in a non-web application.
   *
   * @param viewResolver the resolver to apply the properties to.
   */
  public void applyToMvcViewResolver(Object viewResolver) {
    if (!(viewResolver instanceof AbstractTemplateViewResolver resolver)) {
      throw new IllegalArgumentException(
              "ViewResolver is not an instance of AbstractTemplateViewResolver :" + viewResolver);
    }
    resolver.setPrefix(getPrefix());
    resolver.setSuffix(getSuffix());
    resolver.setCache(isCache());
    resolver.setContentType(getContentType().toString());
    resolver.setViewNames(getViewNames());
    resolver.setExposeRequestAttributes(isExposeRequestAttributes());
    resolver.setAllowRequestOverride(isAllowRequestOverride());
    resolver.setAllowSessionOverride(isAllowSessionOverride());
    resolver.setExposeSessionAttributes(isExposeSessionAttributes());
    resolver.setRequestContextAttribute(getRequestContextAttribute());
    resolver.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
  }

}
