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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.aot.nativex;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.aot.hint.ExecutableHint;
import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.FieldHint;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.TypeHint;
import cn.taketoday.lang.Nullable;

/**
 * Write {@link ReflectionHints} to the JSON output expected by the GraalVM
 * {@code native-image} compiler, typically named {@code reflect-config.json}
 * or {@code jni-config.json}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Janne Valkealahti
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/Reflection/">Reflection Use in Native Images</a>
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/JNI/">Java Native Interface (JNI) in Native Image</a>
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 * @since 4.0
 */
class ReflectionHintsWriter {

  public static void write(BasicJsonWriter writer, ReflectionHints hints) {
    writer.writeArray(hints.typeHints().map(ReflectionHintsWriter::toAttributes).toList());
  }

  private static Map<String, Object> toAttributes(TypeHint hint) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("name", hint.getType());
    handleCondition(attributes, hint);
    handleCategories(attributes, hint.getMemberCategories());
    handleFields(attributes, hint.fields());
    handleExecutables(attributes, Stream.concat(hint.constructors(), hint.methods()).toList());
    return attributes;
  }

  private static void handleCondition(Map<String, Object> attributes, TypeHint hint) {
    if (hint.getReachableType() != null) {
      Map<String, Object> conditionAttributes = new LinkedHashMap<>();
      conditionAttributes.put("typeReachable", hint.getReachableType());
      attributes.put("condition", conditionAttributes);
    }
  }

  private static void handleFields(Map<String, Object> attributes, Stream<FieldHint> fields) {
    addIfNotEmpty(attributes, "fields", fields.map(ReflectionHintsWriter::toAttributes).toList());
  }

  private static Map<String, Object> toAttributes(FieldHint hint) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("name", hint.getName());
    return attributes;
  }

  private static void handleExecutables(Map<String, Object> attributes, List<ExecutableHint> hints) {
    addIfNotEmpty(attributes, "methods", hints.stream()
            .filter(h -> h.getMode().equals(ExecutableMode.INVOKE))
            .map(ReflectionHintsWriter::toAttributes).toList());
    addIfNotEmpty(attributes, "queriedMethods", hints.stream()
            .filter(h -> h.getMode().equals(ExecutableMode.INTROSPECT))
            .map(ReflectionHintsWriter::toAttributes).toList());
  }

  private static Map<String, Object> toAttributes(ExecutableHint hint) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("name", hint.getName());
    attributes.put("parameterTypes", hint.getParameterTypes());
    return attributes;
  }

  private static void handleCategories(Map<String, Object> attributes, Set<MemberCategory> categories) {
    for (MemberCategory category : categories) {
      switch (category) {
        case PUBLIC_FIELDS -> attributes.put("allPublicFields", true);
        case DECLARED_FIELDS -> attributes.put("allDeclaredFields", true);
        case INTROSPECT_PUBLIC_CONSTRUCTORS -> attributes.put("queryAllPublicConstructors", true);
        case INTROSPECT_DECLARED_CONSTRUCTORS -> attributes.put("queryAllDeclaredConstructors", true);
        case INVOKE_PUBLIC_CONSTRUCTORS -> attributes.put("allPublicConstructors", true);
        case INVOKE_DECLARED_CONSTRUCTORS -> attributes.put("allDeclaredConstructors", true);
        case INTROSPECT_PUBLIC_METHODS -> attributes.put("queryAllPublicMethods", true);
        case INTROSPECT_DECLARED_METHODS -> attributes.put("queryAllDeclaredMethods", true);
        case INVOKE_PUBLIC_METHODS -> attributes.put("allPublicMethods", true);
        case INVOKE_DECLARED_METHODS -> attributes.put("allDeclaredMethods", true);
        case PUBLIC_CLASSES -> attributes.put("allPublicClasses", true);
        case DECLARED_CLASSES -> attributes.put("allDeclaredClasses", true);
      }
    }
  }

  private static void addIfNotEmpty(Map<String, Object> attributes, String name, @Nullable Object value) {
    if (value != null && (value instanceof Collection<?> collection && !collection.isEmpty())) {
      attributes.put(name, value);
    }
  }

}
