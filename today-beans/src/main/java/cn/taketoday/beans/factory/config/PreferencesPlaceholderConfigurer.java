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

package cn.taketoday.beans.factory.config;

import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Nullable;

/**
 * Subclass of PropertyPlaceholderConfigurer that supports JDK 1.4's
 * Preferences API ({@code java.util.prefs}).
 *
 * <p>Tries to resolve placeholders as keys first in the user preferences,
 * then in the system preferences, then in this configurer's properties.
 * Thus, behaves like PropertyPlaceholderConfigurer if no corresponding
 * preferences defined.
 *
 * <p>Supports custom paths for the system and user preferences trees. Also
 * supports custom paths specified in placeholders ("myPath/myPlaceholderKey").
 * Uses the respective root node if not specified.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setSystemTreePath
 * @see #setUserTreePath
 * @see java.util.prefs.Preferences
 * @since 4.0 2022/3/9 9:13
 */
public class PreferencesPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements InitializingBean {

  @Nullable
  private String systemTreePath;

  @Nullable
  private String userTreePath;

  private Preferences systemPrefs = Preferences.systemRoot();

  private Preferences userPrefs = Preferences.userRoot();

  /**
   * Set the path in the system preferences tree to use for resolving
   * placeholders. Default is the root node.
   */
  public void setSystemTreePath(@Nullable String systemTreePath) {
    this.systemTreePath = systemTreePath;
  }

  /**
   * Set the path in the system preferences tree to use for resolving
   * placeholders. Default is the root node.
   */
  public void setUserTreePath(@Nullable String userTreePath) {
    this.userTreePath = userTreePath;
  }

  /**
   * This implementation eagerly fetches the Preferences instances
   * for the required system and user tree nodes.
   */
  @Override
  public void afterPropertiesSet() {
    if (systemTreePath != null) {
      this.systemPrefs = systemPrefs.node(systemTreePath);
    }
    if (userTreePath != null) {
      this.userPrefs = userPrefs.node(userTreePath);
    }
  }

  /**
   * This implementation tries to resolve placeholders as keys first
   * in the user preferences, then in the system preferences, then in
   * the passed-in properties.
   */
  @Override
  protected String resolvePlaceholder(String placeholder, Properties props) {
    String path = null;
    String key = placeholder;
    int endOfPath = placeholder.lastIndexOf('/');
    if (endOfPath != -1) {
      path = placeholder.substring(0, endOfPath);
      key = placeholder.substring(endOfPath + 1);
    }
    String value = resolvePlaceholder(path, key, userPrefs);
    if (value == null) {
      value = resolvePlaceholder(path, key, systemPrefs);
      if (value == null) {
        value = props.getProperty(placeholder);
      }
    }
    return value;
  }

  /**
   * Resolve the given path and key against the given Preferences.
   *
   * @param path the preferences path (placeholder part before '/')
   * @param key the preferences key (placeholder part after '/')
   * @param preferences the Preferences to resolve against
   * @return the value for the placeholder, or {@code null} if none found
   */
  @Nullable
  protected String resolvePlaceholder(@Nullable String path, String key, Preferences preferences) {
    if (path != null) {
      // Do not create the node if it does not exist...
      try {
        if (preferences.nodeExists(path)) {
          return preferences.node(path).get(key, null);
        }
        else {
          return null;
        }
      }
      catch (BackingStoreException ex) {
        throw new BeanDefinitionStoreException("Cannot access specified node path [" + path + "]", ex);
      }
    }
    else {
      return preferences.get(key, null);
    }
  }

}
