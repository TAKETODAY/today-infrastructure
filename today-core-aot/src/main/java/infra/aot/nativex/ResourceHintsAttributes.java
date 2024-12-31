/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.aot.nativex;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.aot.hint.ConditionalHint;
import infra.aot.hint.ResourceBundleHint;
import infra.aot.hint.ResourceHints;
import infra.aot.hint.ResourcePatternHint;
import infra.aot.hint.ResourcePatternHints;

/**
 * Collect {@link ResourceHints} as map attributes ready for JSON serialization for the GraalVM
 * {@code native-image} compiler.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/Resources/">Accessing Resources in Native Images</a>
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 * @since 5.0
 */
class ResourceHintsAttributes {

  private static final Comparator<ResourcePatternHint> RESOURCE_PATTERN_HINT_COMPARATOR =
          Comparator.comparing(ResourcePatternHint::getPattern);

  private static final Comparator<ResourceBundleHint> RESOURCE_BUNDLE_HINT_COMPARATOR =
          Comparator.comparing(ResourceBundleHint::getBaseName);

  public List<Map<String, Object>> resources(ResourceHints hint) {
    return hint.resourcePatternHints()
            .map(ResourcePatternHints::getIncludes).flatMap(List::stream).distinct()
            .sorted(RESOURCE_PATTERN_HINT_COMPARATOR)
            .map(this::toAttributes).toList();
  }

  public List<Map<String, Object>> resourceBundles(ResourceHints hint) {
    return hint.resourceBundleHints()
            .sorted(RESOURCE_BUNDLE_HINT_COMPARATOR)
            .map(this::toAttributes).toList();
  }

  private Map<String, Object> toAttributes(ResourceBundleHint hint) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    handleCondition(attributes, hint);
    attributes.put("name", hint.getBaseName());
    return attributes;
  }

  private Map<String, Object> toAttributes(ResourcePatternHint hint) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    handleCondition(attributes, hint);
    attributes.put("glob", hint.getPattern());
    return attributes;
  }

  private void handleCondition(Map<String, Object> attributes, ConditionalHint hint) {
    if (hint.getReachableType() != null) {
      Map<String, Object> conditionAttributes = new LinkedHashMap<>();
      conditionAttributes.put("typeReached", hint.getReachableType());
      attributes.put("condition", conditionAttributes);
    }
  }

}
