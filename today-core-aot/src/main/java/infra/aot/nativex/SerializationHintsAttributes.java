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
import infra.aot.hint.JavaSerializationHint;
import infra.aot.hint.SerializationHints;

/**
 * Collect {@link SerializationHints} as map attributes ready for JSON serialization for the GraalVM
 * {@code native-image} compiler.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see <a href="https://www.graalvm.org/jdk23/reference-manual/native-image/overview/BuildConfiguration/">Native Image Build Configuration</a>
 * @since 5.0
 */
class SerializationHintsAttributes {

  private static final Comparator<JavaSerializationHint> JAVA_SERIALIZATION_HINT_COMPARATOR =
          Comparator.comparing(JavaSerializationHint::getType);

  public List<Map<String, Object>> toAttributes(SerializationHints hints) {
    return hints.javaSerializationHints()
            .sorted(JAVA_SERIALIZATION_HINT_COMPARATOR)
            .map(this::toAttributes).toList();
  }

  private Map<String, Object> toAttributes(JavaSerializationHint serializationHint) {
    LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
    handleCondition(attributes, serializationHint);
    attributes.put("type", serializationHint.getType());
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
