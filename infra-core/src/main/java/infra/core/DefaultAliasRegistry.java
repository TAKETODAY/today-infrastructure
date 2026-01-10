/*
 * Copyright 2002-present the original author or authors.
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

package infra.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.StringUtils;

/**
 * Simple implementation of the {@link AliasRegistry} interface.
 * <p>Serves as base class for
 * {@link infra.beans.factory.support.BeanDefinitionRegistry}
 * implementations.
 *
 * @author Juergen Hoeller
 * @author Qimiao Chen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/9/30 22:40
 */
public class DefaultAliasRegistry implements AliasRegistry {

  /** Logger available to subclasses. */
  protected final Logger log = LoggerFactory.getLogger(getClass());

  /** Map from alias to canonical name. */
  private final ConcurrentHashMap<String, String> aliasMap = new ConcurrentHashMap<>(16);

  /** List of alias names, in registration order. */
  private final ArrayList<String> aliasNames = new ArrayList<>(16);

  @Override
  public void registerAlias(String name, String alias) {
    Assert.hasText(name, "'name' must not be empty");
    Assert.hasText(alias, "'alias' must not be empty");
    synchronized(aliasMap) {
      if (alias.equals(name)) {
        aliasMap.remove(alias);
        aliasNames.remove(alias);
        if (log.isDebugEnabled()) {
          log.debug("Alias definition '{}' ignored since it points to same name", alias);
        }
      }
      else {
        String registeredName = aliasMap.get(alias);
        if (registeredName != null) {
          if (registeredName.equals(name)) {
            // An existing alias - no need to re-register
            return;
          }
          if (!allowAliasOverriding()) {
            throw new IllegalStateException("Cannot define alias '%s' for name '%s': It is already registered for name '%s'."
                    .formatted(alias, name, registeredName));
          }
          if (log.isDebugEnabled()) {
            log.debug("Overriding alias '{}' definition for registered name '{}' with new target name '{}'",
                    alias, registeredName, name);
          }
        }
        checkForAliasCircle(name, alias);
        aliasMap.put(alias, name);
        aliasNames.add(alias);
        if (log.isDebugEnabled()) {
          log.trace("Alias definition '{}' registered for name '{}'", alias, name);
        }
      }
    }
  }

  /**
   * Determine whether alias overriding is allowed.
   * <p>Default is {@code true}.
   */
  protected boolean allowAliasOverriding() {
    return true;
  }

  /**
   * Determine whether the given name has the given alias registered.
   *
   * @param name the name to check
   * @param alias the alias to look for
   */
  public boolean hasAlias(String name, String alias) {
    String registeredName = aliasMap.get(alias);
    return Objects.equals(registeredName, name)
            || (registeredName != null && hasAlias(name, registeredName));
  }

  @Override
  public void removeAlias(String alias) {
    synchronized(aliasMap) {
      String name = aliasMap.remove(alias);
      aliasNames.remove(alias);
      if (name == null) {
        throw new IllegalStateException("No alias '%s' registered".formatted(alias));
      }
    }
  }

  @Override
  public boolean isAlias(String name) {
    return aliasMap.containsKey(name);
  }

  @Override
  public String[] getAliases(String name) {
    return StringUtils.toStringArray(getAliasList(name));
  }

  @Override
  public List<String> getAliasList(String name) {
    ArrayList<String> result = new ArrayList<>();
    synchronized(aliasMap) {
      retrieveAliases(name, result);
    }
    return result;
  }

  /**
   * Transitively retrieve all aliases for the given name.
   *
   * @param name the target name to find aliases for
   * @param result the resulting aliases list
   */
  private void retrieveAliases(String name, ArrayList<String> result) {
    for (Map.Entry<String, String> entry : aliasMap.entrySet()) {
      String alias = entry.getKey();
      String registeredName = entry.getValue();
      if (registeredName.equals(name)) {
        result.add(alias);
        retrieveAliases(alias, result);
      }
    }
  }

  /**
   * Resolve all alias target names and aliases registered in this
   * registry, applying the given {@link StringValueResolver} to them.
   * <p>The value resolver may for example resolve placeholders
   * in target bean names and even in alias names.
   *
   * @param valueResolver the StringValueResolver to apply
   */
  public void resolveAliases(StringValueResolver valueResolver) {
    Assert.notNull(valueResolver, "StringValueResolver is required");
    synchronized(aliasMap) {
      for (final String alias : new ArrayList<>(this.aliasNames)) {
        String registeredName = aliasMap.get(alias);
        if (registeredName != null) {
          String resolvedAlias = valueResolver.resolveStringValue(alias);
          String resolvedName = valueResolver.resolveStringValue(registeredName);
          if (resolvedAlias == null || resolvedName == null || resolvedAlias.equals(resolvedName)) {
            aliasMap.remove(alias);
            aliasNames.remove(alias);
          }
          else if (!resolvedAlias.equals(alias)) {
            String existingName = aliasMap.get(resolvedAlias);
            if (existingName != null) {
              if (existingName.equals(resolvedName)) {
                // Pointing to existing alias - just remove placeholder
                aliasMap.remove(alias);
                aliasNames.remove(alias);
                return;
              }
              throw new IllegalStateException(
                      "Cannot register resolved alias '%s' (original: '%s') for name '%s': It is already registered for name '%s'."
                              .formatted(resolvedAlias, alias, resolvedName, existingName));
            }
            checkForAliasCircle(resolvedName, resolvedAlias);
            aliasMap.remove(alias);
            aliasNames.remove(alias);
            aliasMap.put(resolvedAlias, resolvedName);
            aliasNames.add(resolvedAlias);
          }
          else if (!registeredName.equals(resolvedName)) {
            aliasMap.put(alias, resolvedName);
            aliasNames.add(alias);
          }
        }
      }
    }
  }

  /**
   * Check whether the given name points back to the given alias as an alias
   * in the other direction already, catching a circular reference upfront
   * and throwing a corresponding IllegalStateException.
   *
   * @param name the candidate name
   * @param alias the candidate alias
   * @see #registerAlias
   * @see #hasAlias
   */
  protected void checkForAliasCircle(String name, String alias) {
    if (hasAlias(alias, name)) {
      throw new IllegalStateException(
              "Cannot register alias '%s' for name '%s': Circular reference - '%s' is a direct or indirect alias for '%s' already"
                      .formatted(alias, name, name, alias));
    }
  }

  /**
   * Determine the raw name, resolving aliases to canonical names.
   *
   * @param name the user-specified name
   * @return the transformed name
   */
  public String canonicalName(String name) {
    String canonicalName = name;
    // Handle aliasing...
    String resolvedName;
    do {
      resolvedName = aliasMap.get(canonicalName);
      if (resolvedName != null) {
        canonicalName = resolvedName;
      }
    }
    while (resolvedName != null);
    return canonicalName;
  }

}
