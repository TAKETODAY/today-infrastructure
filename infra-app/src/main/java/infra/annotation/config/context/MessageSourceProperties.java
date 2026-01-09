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

package infra.annotation.config.context;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import infra.context.properties.ConfigurationProperties;
import infra.core.io.Resource;
import infra.format.annotation.DurationUnit;

/**
 * Configuration properties for Message Source.
 *
 * @author Stephane Nicoll
 * @author Kedar Joshi
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "infra.messages")
public class MessageSourceProperties {

  /**
   * Comma-separated list of basenames (essentially a fully-qualified classpath
   * location), each following the ResourceBundle convention with relaxed support for
   * slash based locations. If it doesn't contain a package qualifier (such as
   * "org.mypackage"), it will be resolved from the classpath root.
   */
  private String basename = "messages";

  /**
   * List of locale-independent property file resources containing common messages.
   */
  @Nullable
  private List<Resource> commonMessages;

  /**
   * Message bundles encoding.
   */
  @Nullable
  private Charset encoding = StandardCharsets.UTF_8;

  /**
   * Loaded resource bundle files cache duration. When not set, bundles are cached
   * forever. If a duration suffix is not specified, seconds will be used.
   */
  @Nullable
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration cacheDuration;

  /**
   * Whether to fall back to the system Locale if no files for a specific Locale have
   * been found. if this is turned off, the only fallback will be the default file (e.g.
   * "messages.properties" for basename "messages").
   */
  private boolean fallbackToSystemLocale = true;

  /**
   * Whether to always apply the MessageFormat rules, parsing even messages without
   * arguments.
   */
  private boolean alwaysUseMessageFormat = false;

  /**
   * Whether to use the message code as the default message instead of throwing a
   * "NoSuchMessageException". Recommended during development only.
   */
  private boolean useCodeAsDefaultMessage = false;

  public String getBasename() {
    return this.basename;
  }

  public void setBasename(String basename) {
    this.basename = basename;
  }

  @Nullable
  public Charset getEncoding() {
    return this.encoding;
  }

  public void setEncoding(@Nullable Charset encoding) {
    this.encoding = encoding;
  }

  @Nullable
  public Duration getCacheDuration() {
    return this.cacheDuration;
  }

  public void setCacheDuration(@Nullable Duration cacheDuration) {
    this.cacheDuration = cacheDuration;
  }

  public boolean isFallbackToSystemLocale() {
    return this.fallbackToSystemLocale;
  }

  public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
    this.fallbackToSystemLocale = fallbackToSystemLocale;
  }

  public boolean isAlwaysUseMessageFormat() {
    return this.alwaysUseMessageFormat;
  }

  public void setAlwaysUseMessageFormat(boolean alwaysUseMessageFormat) {
    this.alwaysUseMessageFormat = alwaysUseMessageFormat;
  }

  public boolean isUseCodeAsDefaultMessage() {
    return this.useCodeAsDefaultMessage;
  }

  public void setUseCodeAsDefaultMessage(boolean useCodeAsDefaultMessage) {
    this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
  }

  @Nullable
  public List<Resource> getCommonMessages() {
    return this.commonMessages;
  }

  public void setCommonMessages(@Nullable List<Resource> commonMessages) {
    this.commonMessages = commonMessages;
  }

}
