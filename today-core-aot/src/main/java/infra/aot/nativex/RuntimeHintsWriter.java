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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.aot.hint.RuntimeHints;
import infra.lang.Version;

/**
 * Write a {@link RuntimeHints} instance to the JSON output expected by the
 * GraalVM {@code native-image} compiler, typically named {@code reachability-metadata.json}.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see <a href="https://www.graalvm.org/jdk23/reference-manual/native-image/metadata/#specifying-metadata-with-json">GraalVM Reachability Metadata</a>
 * @since 5.0
 */
class RuntimeHintsWriter {

  public void write(BasicJsonWriter writer, RuntimeHints hints) {
    Map<String, Object> document = new LinkedHashMap<>();
    String version = Version.instance.implementationVersion();
    document.put("comment", "Infra Framework " + version);
    List<Map<String, Object>> reflection = new ReflectionHintsAttributes().reflection(hints);
    if (!reflection.isEmpty()) {
      document.put("reflection", reflection);
    }
    List<Map<String, Object>> jni = new ReflectionHintsAttributes().jni(hints);
    if (!jni.isEmpty()) {
      document.put("jni", jni);
    }
    List<Map<String, Object>> resourceHints = new ResourceHintsAttributes().resources(hints.resources());
    if (!resourceHints.isEmpty()) {
      document.put("resources", resourceHints);
    }
    List<Map<String, Object>> resourceBundles = new ResourceHintsAttributes().resourceBundles(hints.resources());
    if (!resourceBundles.isEmpty()) {
      document.put("bundles", resourceBundles);
    }
    List<Map<String, Object>> serialization = new SerializationHintsAttributes().toAttributes(hints.serialization());
    if (!serialization.isEmpty()) {
      document.put("serialization", serialization);
    }

    writer.writeObject(document);
  }

}
