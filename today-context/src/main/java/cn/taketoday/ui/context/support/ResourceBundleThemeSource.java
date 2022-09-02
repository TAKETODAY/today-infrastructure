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

package cn.taketoday.ui.context.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.context.HierarchicalMessageSource;
import cn.taketoday.context.MessageSource;
import cn.taketoday.context.support.ResourceBundleMessageSource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.ui.context.HierarchicalThemeSource;
import cn.taketoday.ui.context.Theme;
import cn.taketoday.ui.context.ThemeSource;

/**
 * {@link ThemeSource} implementation that looks up an individual
 * {@link java.util.ResourceBundle} per theme. The theme name gets
 * interpreted as ResourceBundle basename, supporting a common
 * basename prefix for all themes.
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see #setBasenamePrefix
 * @see java.util.ResourceBundle
 * @see ResourceBundleMessageSource
 * @since 4.0
 */
public class ResourceBundleThemeSource implements HierarchicalThemeSource, BeanClassLoaderAware {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private ThemeSource parentThemeSource;

  private String basenamePrefix = "";

  @Nullable
  private String defaultEncoding;

  @Nullable
  private Boolean fallbackToSystemLocale;

  @Nullable
  private ClassLoader beanClassLoader;

  /** Map from theme name to Theme instance. */
  private final Map<String, Theme> themeCache = new ConcurrentHashMap<>();

  @Override
  public void setParentThemeSource(@Nullable ThemeSource parent) {
    this.parentThemeSource = parent;

    // Update existing Theme objects.
    // Usually there shouldn't be any at the time of this call.
    synchronized(this.themeCache) {
      for (Theme theme : this.themeCache.values()) {
        initParent(theme);
      }
    }
  }

  @Override
  @Nullable
  public ThemeSource getParentThemeSource() {
    return this.parentThemeSource;
  }

  /**
   * Set the prefix that gets applied to the ResourceBundle basenames,
   * i.e. the theme names.
   * E.g.: basenamePrefix="test.", themeName="theme" &rarr; basename="test.theme".
   * <p>Note that ResourceBundle names are effectively classpath locations: As a
   * consequence, the JDK's standard ResourceBundle treats dots as package separators.
   * This means that "test.theme" is effectively equivalent to "test/theme",
   * just like it is for programmatic {@code java.util.ResourceBundle} usage.
   *
   * @see java.util.ResourceBundle#getBundle(String)
   */
  public void setBasenamePrefix(@Nullable String basenamePrefix) {
    this.basenamePrefix = (basenamePrefix != null ? basenamePrefix : "");
  }

  /**
   * Set the default charset to use for parsing resource bundle files.
   * <p>{@link ResourceBundleMessageSource}'s default is the
   * {@code java.util.ResourceBundle} default encoding: ISO-8859-1.
   *
   * @see ResourceBundleMessageSource#setDefaultEncoding
   */
  public void setDefaultEncoding(@Nullable String defaultEncoding) {
    this.defaultEncoding = defaultEncoding;
  }

  /**
   * Set whether to fall back to the system Locale if no files for a
   * specific Locale have been found.
   * <p>{@link ResourceBundleMessageSource}'s default is "true".
   *
   * @see ResourceBundleMessageSource#setFallbackToSystemLocale
   */
  public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
    this.fallbackToSystemLocale = fallbackToSystemLocale;
  }

  @Override
  public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
    this.beanClassLoader = beanClassLoader;
  }

  /**
   * This implementation returns a SimpleTheme instance, holding a
   * ResourceBundle-based MessageSource whose basename corresponds to
   * the given theme name (prefixed by the configured "basenamePrefix").
   * <p>SimpleTheme instances are cached per theme name. Use a reloadable
   * MessageSource if themes should reflect changes to the underlying files.
   *
   * @see #setBasenamePrefix
   * @see #createMessageSource
   */
  @Override
  @Nullable
  public Theme getTheme(String themeName) {
    Theme theme = this.themeCache.get(themeName);
    if (theme == null) {
      synchronized(this.themeCache) {
        theme = this.themeCache.get(themeName);
        if (theme == null) {
          String basename = this.basenamePrefix + themeName;
          MessageSource messageSource = createMessageSource(basename);
          theme = new SimpleTheme(themeName, messageSource);
          initParent(theme);
          this.themeCache.put(themeName, theme);
          if (logger.isDebugEnabled()) {
            logger.debug("Theme created: name '" + themeName + "', basename [" + basename + "]");
          }
        }
      }
    }
    return theme;
  }

  /**
   * Create a MessageSource for the given basename,
   * to be used as MessageSource for the corresponding theme.
   * <p>Default implementation creates a ResourceBundleMessageSource.
   * for the given basename. A subclass could create a specifically
   * configured ReloadableResourceBundleMessageSource, for example.
   *
   * @param basename the basename to create a MessageSource for
   * @return the MessageSource
   * @see cn.taketoday.context.support.ResourceBundleMessageSource
   * @see cn.taketoday.context.support.ReloadableResourceBundleMessageSource
   */
  protected MessageSource createMessageSource(String basename) {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename(basename);
    if (this.defaultEncoding != null) {
      messageSource.setDefaultEncoding(this.defaultEncoding);
    }
    if (this.fallbackToSystemLocale != null) {
      messageSource.setFallbackToSystemLocale(this.fallbackToSystemLocale);
    }
    if (this.beanClassLoader != null) {
      messageSource.setBeanClassLoader(this.beanClassLoader);
    }
    return messageSource;
  }

  /**
   * Initialize the MessageSource of the given theme with the
   * one from the corresponding parent of this ThemeSource.
   *
   * @param theme the Theme to (re-)initialize
   */
  protected void initParent(Theme theme) {
    if (theme.getMessageSource() instanceof HierarchicalMessageSource messageSource) {
      if (getParentThemeSource() != null && messageSource.getParentMessageSource() == null) {
        Theme parentTheme = getParentThemeSource().getTheme(theme.getName());
        if (parentTheme != null) {
          messageSource.setParentMessageSource(parentTheme.getMessageSource());
        }
      }
    }
  }

}
