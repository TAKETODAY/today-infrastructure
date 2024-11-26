/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.util.Map;

import infra.aot.hint.ConditionalHint;
import infra.aot.hint.JavaSerializationHint;
import infra.aot.hint.SerializationHints;

/**
 * Write a {@link SerializationHints} to the JSON output expected by the
 * GraalVM {@code native-image} compiler, typically named
 * {@code serialization-config.json}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 * @since 4.0
 */
class SerializationHintsWriter {

  public static void write(BasicJsonWriter writer, SerializationHints hints) {
    writer.writeArray(hints.javaSerializationHints()
            .sorted(Comparator.comparing(JavaSerializationHint::getType))
            .map(SerializationHintsWriter::toAttributes).toList());
  }

  private static Map<String, Object> toAttributes(JavaSerializationHint serializationHint) {
    LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
    handleCondition(attributes, serializationHint);
    attributes.put("name", serializationHint.getType());
    return attributes;
  }

  private static void handleCondition(Map<String, Object> attributes, ConditionalHint hint) {
    if (hint.getReachableType() != null) {
      var conditionAttributes = new LinkedHashMap<>();
      conditionAttributes.put("typeReachable", hint.getReachableType());
      attributes.put("condition", conditionAttributes);
    }
  }

}
