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
 * @see <a href="https://www.graalvm.org/jdk25/reference-manual/native-image/metadata">GraalVM Reachability Metadata</a>
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

    writer.writeObject(document);
  }

}
