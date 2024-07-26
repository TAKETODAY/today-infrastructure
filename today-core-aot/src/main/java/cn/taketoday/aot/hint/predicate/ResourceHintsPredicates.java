/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.hint.predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import cn.taketoday.aot.hint.ResourceHints;
import cn.taketoday.aot.hint.ResourcePatternHint;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ConcurrentLruCache;

/**
 * Generator of {@link ResourceHints} predicates, testing whether the given hints
 * match the expected behavior for resources.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.0
 */
public class ResourceHintsPredicates {

  private static final ConcurrentLruCache<ResourcePatternHint, Pattern> CACHED_RESOURCE_PATTERNS = new ConcurrentLruCache<>(32, ResourcePatternHint::toRegex);

  ResourceHintsPredicates() { }

  /**
   * Return a predicate that checks whether a resource hint is registered for the given bundle name.
   *
   * @param bundleName the resource bundle name
   * @return the {@link RuntimeHints} predicate
   */
  public Predicate<RuntimeHints> forBundle(String bundleName) {
    Assert.hasText(bundleName, "resource bundle name should not be empty");
    return runtimeHints -> runtimeHints.resources().resourceBundleHints()
            .anyMatch(bundleHint -> bundleName.equals(bundleHint.getBaseName()));
  }

  /**
   * Return a predicate that checks whether a resource hint is registered for the given
   * resource name, located in the given type's package.
   * <p>For example, {@code forResource(org.example.MyClass, "myResource.txt")}
   * will match against {@code "org/example/myResource.txt"}.
   * <p>If the given resource name is an absolute path (i.e., starts with a
   * leading slash), the supplied type will be ignored. For example,
   * {@code forResource(org.example.MyClass, "/myResource.txt")} will match against
   * {@code "myResource.txt"}.
   *
   * @param type the type's package where to look for the resource
   * @param resourceName the resource name
   * @return the {@link RuntimeHints} predicate
   */
  public Predicate<RuntimeHints> forResource(TypeReference type, String resourceName) {
    String absoluteName = resolveAbsoluteResourceName(type, resourceName);
    return forResource(absoluteName);
  }

  private String resolveAbsoluteResourceName(TypeReference type, String resourceName) {
    // absolute path
    if (resourceName.startsWith("/")) {
      return resourceName.substring(1);
    }
    // default package
    else if (type.getPackageName().isEmpty()) {
      return resourceName;
    }
    // relative path
    else {
      return type.getPackageName().replace('.', '/') + "/" + resourceName;
    }
  }

  /**
   * Return a predicate that checks whether a resource hint is registered for
   * the given resource name.
   * <p>A leading slash will be removed.
   *
   * @param resourceName the absolute resource name
   * @return the {@link RuntimeHints} predicate
   */
  public Predicate<RuntimeHints> forResource(String resourceName) {
    String resourceNameToUse = (resourceName.startsWith("/") ? resourceName.substring(1) : resourceName);
    return hints -> {
      var aggregatedResourcePatternHints = AggregatedResourcePatternHints.of(hints.resources());
      boolean isExcluded = aggregatedResourcePatternHints.excludes().stream().anyMatch(excluded ->
              CACHED_RESOURCE_PATTERNS.get(excluded).matcher(resourceNameToUse).matches());
      if (isExcluded) {
        return false;
      }
      return aggregatedResourcePatternHints.includes().stream().anyMatch(included ->
              CACHED_RESOURCE_PATTERNS.get(included).matcher(resourceNameToUse).matches());
    };
  }

  private record AggregatedResourcePatternHints(List<ResourcePatternHint> includes, List<ResourcePatternHint> excludes) {

    static AggregatedResourcePatternHints of(ResourceHints resourceHints) {
      List<ResourcePatternHint> includes = new ArrayList<>();
      List<ResourcePatternHint> excludes = new ArrayList<>();
      resourceHints.resourcePatternHints().forEach(resourcePatternHint -> {
        includes.addAll(resourcePatternHint.getIncludes());
        excludes.addAll(resourcePatternHint.getExcludes());
      });
      return new AggregatedResourcePatternHints(includes, excludes);
    }

  }

}
