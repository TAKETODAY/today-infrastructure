/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.groovy.template.config;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import groovy.text.markup.BaseTemplate;
import groovy.text.markup.MarkupTemplateEngine;
import infra.beans.factory.InitializingBean;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.properties.ConfigurationProperties;
import infra.core.Ordered;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.ui.template.TemplateLocation;
import infra.util.MimeType;
import infra.web.view.AbstractTemplateViewResolver;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Groovy
 * templates.
 *
 * @author Dave Syer
 * @author Marten Deinum
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@ConfigurationProperties("groovy.template")
public class GroovyTemplateProperties implements InitializingBean, ApplicationContextAware {

  private static final Logger log = LoggerFactory.getLogger(GroovyTemplateProperties.class);

  public static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:/templates/";

  public static final String DEFAULT_PREFIX = "";

  public static final String DEFAULT_SUFFIX = ".tpl";

  public static final String DEFAULT_REQUEST_CONTEXT_ATTRIBUTE = "request";

  private static final MimeType DEFAULT_CONTENT_TYPE = MimeType.valueOf("text/html");

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

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
  private Charset charset = DEFAULT_CHARSET;

  /**
   * View names that can be resolved.
   */
  private String @Nullable [] viewNames;

  /**
   * Whether to check that the templates location exists.
   */
  private boolean checkTemplateLocation = true;

  /**
   * Prefix that gets prepended to view names when building a URL.
   */
  private String prefix = DEFAULT_PREFIX;

  /**
   * Suffix that gets appended to view names when building a URL.
   */
  private String suffix = DEFAULT_SUFFIX;

  /**
   * Name of the RequestContext attribute for all views.
   */
  private String requestContextAttribute = DEFAULT_REQUEST_CONTEXT_ATTRIBUTE;

  /**
   * Whether all request attributes should be added to the model prior to merging with
   * the template.
   */
  private boolean exposeRequestAttributes;

  /**
   * Whether all HttpSession attributes should be added to the model prior to merging
   * with the template.
   */
  private boolean exposeSessionAttributes;

  /**
   * Whether HttpServletRequest attributes are allowed to override (hide) controller
   * generated model attributes of the same name.
   */
  private boolean allowRequestOverride;

  /**
   * Whether HttpSession attributes are allowed to override (hide) controller generated
   * model attributes of the same name.
   */
  private boolean allowSessionOverride;

  /**
   * Whether models that are assignable to CharSequence are escaped automatically.
   */
  private boolean autoEscape;

  /**
   * Whether indents are rendered automatically.
   */
  private boolean autoIndent;

  /**
   * String used for auto-indents.
   */
  private @Nullable String autoIndentString;

  /**
   * Whether new lines are rendered automatically.
   */
  private boolean autoNewLine;

  /**
   * Template base class.
   */
  private Class<? extends BaseTemplate> baseTemplateClass = BaseTemplate.class;

  /**
   * Encoding used to write the declaration heading.
   */
  private @Nullable String declarationEncoding;

  /**
   * Whether elements without a body should be written expanded (&lt;br&gt;&lt;/br&gt;)
   * or not (&lt;br/&gt;).
   */
  private boolean expandEmptyElements;

  /**
   * Default locale for template resolution.
   */
  private @Nullable Locale locale;

  /**
   * String used to write a new line. Defaults to the system's line separator.
   */
  private @Nullable String newLineString;

  /**
   * Template path.
   */
  private String resourceLoaderPath = DEFAULT_RESOURCE_LOADER_PATH;

  /**
   * Whether attributes should use double quotes.
   */
  private boolean useDoubleQuotes;

  private ApplicationContext applicationContext;

  public boolean isCheckTemplateLocation() {
    return this.checkTemplateLocation;
  }

  public void setCheckTemplateLocation(boolean checkTemplateLocation) {
    this.checkTemplateLocation = checkTemplateLocation;
  }

  public String @Nullable [] getViewNames() {
    return this.viewNames;
  }

  public void setViewNames(String @Nullable [] viewNames) {
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

  public void setContentType(MimeType contentType) {
    this.contentType = contentType;
  }

  public Charset getCharset() {
    return this.charset;
  }

  public @Nullable String getCharsetName() {
    return (this.charset != null) ? this.charset.name() : null;
  }

  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  public boolean isAutoEscape() {
    return this.autoEscape;
  }

  public void setAutoEscape(boolean autoEscape) {
    this.autoEscape = autoEscape;
  }

  public boolean isAutoIndent() {
    return this.autoIndent;
  }

  public void setAutoIndent(boolean autoIndent) {
    this.autoIndent = autoIndent;
  }

  public @Nullable String getAutoIndentString() {
    return this.autoIndentString;
  }

  public void setAutoIndentString(@Nullable String autoIndentString) {
    this.autoIndentString = autoIndentString;
  }

  public boolean isAutoNewLine() {
    return this.autoNewLine;
  }

  public void setAutoNewLine(boolean autoNewLine) {
    this.autoNewLine = autoNewLine;
  }

  public Class<? extends BaseTemplate> getBaseTemplateClass() {
    return this.baseTemplateClass;
  }

  public void setBaseTemplateClass(Class<? extends BaseTemplate> baseTemplateClass) {
    this.baseTemplateClass = baseTemplateClass;
  }

  public @Nullable String getDeclarationEncoding() {
    return this.declarationEncoding;
  }

  public void setDeclarationEncoding(@Nullable String declarationEncoding) {
    this.declarationEncoding = declarationEncoding;
  }

  public boolean isExpandEmptyElements() {
    return this.expandEmptyElements;
  }

  public void setExpandEmptyElements(boolean expandEmptyElements) {
    this.expandEmptyElements = expandEmptyElements;
  }

  public @Nullable Locale getLocale() {
    return this.locale;
  }

  public void setLocale(@Nullable Locale locale) {
    this.locale = locale;
  }

  public @Nullable String getNewLineString() {
    return this.newLineString;
  }

  public void setNewLineString(@Nullable String newLineString) {
    this.newLineString = newLineString;
  }

  public String getResourceLoaderPath() {
    return this.resourceLoaderPath;
  }

  public void setResourceLoaderPath(String resourceLoaderPath) {
    this.resourceLoaderPath = resourceLoaderPath;
  }

  public boolean isUseDoubleQuotes() {
    return this.useDoubleQuotes;
  }

  public void setUseDoubleQuotes(boolean useDoubleQuotes) {
    this.useDoubleQuotes = useDoubleQuotes;
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

  public String getRequestContextAttribute() {
    return this.requestContextAttribute;
  }

  public void setRequestContextAttribute(String requestContextAttribute) {
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

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    checkTemplateLocationExists();
  }

  private void checkTemplateLocationExists() {
    if (isCheckTemplateLocation() && !isUsingGroovyAllJar()) {
      TemplateLocation location = new TemplateLocation(getResourceLoaderPath());
      if (!location.exists(this.applicationContext)) {
        log.warn("Cannot find template location: %s (please add some templates, check your Groovy "
                + "configuration, or set groovy.template.check-template-location=false)", location);
      }
    }
  }

  /**
   * MarkupTemplateEngine could be loaded from groovy-templates or groovy-all.
   * Unfortunately it's quite common for people to use groovy-all and not actually
   * need templating support. This method attempts to check the source jar so that
   * we can skip the {@code /template} directory check for such cases.
   *
   * @return true if the groovy-all jar is used
   */
  private boolean isUsingGroovyAllJar() {
    try {
      ProtectionDomain domain = MarkupTemplateEngine.class.getProtectionDomain();
      CodeSource codeSource = domain.getCodeSource();
      return codeSource != null && codeSource.getLocation().toString().contains("-all");
    }
    catch (Exception ex) {
      return false;
    }
  }

  /**
   * Apply the given properties to a {@link AbstractTemplateViewResolver}. Use Object in
   * signature to avoid runtime dependency on MVC, which means that the template engine
   * can be used in a non-web application.
   *
   * @param viewResolver the resolver to apply the properties to.
   */
  public void applyToMvcViewResolver(Object viewResolver) {
    Assert.isInstanceOf(AbstractTemplateViewResolver.class, viewResolver,
            () -> "ViewResolver is not an instance of AbstractTemplateViewResolver :" + viewResolver);
    AbstractTemplateViewResolver resolver = (AbstractTemplateViewResolver) viewResolver;
    resolver.setPrefix(getPrefix());
    resolver.setSuffix(getSuffix());
    resolver.setCache(isCache());
    MimeType contentType = getContentType();
    if (contentType != null) {
      resolver.setContentType(contentType.toString());
    }
    resolver.setViewNames(getViewNames());
    resolver.setExposeRequestAttributes(isExposeRequestAttributes());
    resolver.setAllowRequestOverride(isAllowRequestOverride());
    resolver.setAllowSessionOverride(isAllowSessionOverride());
    resolver.setExposeSessionAttributes(isExposeSessionAttributes());
    resolver.setRequestContextAttribute(getRequestContextAttribute());
    // The resolver usually acts as a fallback resolver (e.g. like a
    // InternalResourceViewResolver) so it needs to have low precedence
    resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
  }

}
