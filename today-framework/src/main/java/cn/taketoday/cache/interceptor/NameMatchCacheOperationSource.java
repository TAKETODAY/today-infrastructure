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

package cn.taketoday.cache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Simple {@link CacheOperationSource} implementation that allows attributes to be matched
 * by registered name.
 *
 * @author Costin Leau
 * @since 4.0
 */
@SuppressWarnings("serial")
public class NameMatchCacheOperationSource implements CacheOperationSource, Serializable {

  /**
   * Logger available to subclasses.
   * <p>Static for optimal serialization.
   */
  protected static final Logger logger = LoggerFactory.getLogger(NameMatchCacheOperationSource.class);

  /** Keys are method names; values are TransactionAttributes. */
  private final Map<String, Collection<CacheOperation>> nameMap = new LinkedHashMap<>();

  /**
   * Set a name/attribute map, consisting of method names
   * (e.g. "myMethod") and CacheOperation instances
   * (or Strings to be converted to CacheOperation instances).
   *
   * @see CacheOperation
   */
  public void setNameMap(Map<String, Collection<CacheOperation>> nameMap) {
    nameMap.forEach(this::addCacheMethod);
  }

  /**
   * Add an attribute for a cacheable method.
   * <p>Method names can be exact matches, or of the pattern "xxx*",
   * "*xxx" or "*xxx*" for matching multiple methods.
   *
   * @param methodName the name of the method
   * @param ops operation associated with the method
   */
  public void addCacheMethod(String methodName, Collection<CacheOperation> ops) {
    if (logger.isDebugEnabled()) {
      logger.debug("Adding method [{}] with cache operations [{}]", methodName, ops);
    }
    this.nameMap.put(methodName, ops);
  }

  @Override
  @Nullable
  public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
    // look for direct name match
    String methodName = method.getName();
    Collection<CacheOperation> ops = this.nameMap.get(methodName);

    if (ops == null) {
      // Look for most specific name match.
      String bestNameMatch = null;
      for (String mappedName : this.nameMap.keySet()) {
        if (isMatch(methodName, mappedName)
                && (bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
          ops = this.nameMap.get(mappedName);
          bestNameMatch = mappedName;
        }
      }
    }

    return ops;
  }

  /**
   * Return if the given method name matches the mapped name.
   * <p>The default implementation checks for "xxx*", "*xxx" and "*xxx*" matches,
   * as well as direct equality. Can be overridden in subclasses.
   *
   * @param methodName the method name of the class
   * @param mappedName the name in the descriptor
   * @return if the names match
   * @see cn.taketoday.util.StringUtils#simpleMatch(String, String)
   */
  protected boolean isMatch(String methodName, String mappedName) {
    return StringUtils.simpleMatch(mappedName, methodName);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof NameMatchCacheOperationSource otherTas)) {
      return false;
    }
    return ObjectUtils.nullSafeEquals(this.nameMap, otherTas.nameMap);
  }

  @Override
  public int hashCode() {
    return NameMatchCacheOperationSource.class.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + this.nameMap;
  }
}
