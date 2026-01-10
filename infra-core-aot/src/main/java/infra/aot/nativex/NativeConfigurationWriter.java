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

import java.util.function.Consumer;

import infra.aot.hint.RuntimeHints;

/**
 * Write {@link RuntimeHints} as GraalVM native configuration.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Janne Valkealahti
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 * @since 4.0
 */
public abstract class NativeConfigurationWriter {

  /**
   * Write the GraalVM native configuration from the provided hints.
   *
   * @param hints the hints to handle
   */
  public void write(RuntimeHints hints) {
    if (hasAnyHint(hints)) {
      writeTo("reachability-metadata.json",
              writer -> new RuntimeHintsWriter().write(writer, hints));
    }
  }

  private boolean hasAnyHint(RuntimeHints hints) {
    return (hints.serialization().javaSerializationHints().findAny().isPresent()
            || hints.proxies().jdkProxyHints().findAny().isPresent()
            || hints.reflection().typeHints().findAny().isPresent()
            || hints.resources().resourcePatternHints().findAny().isPresent()
            || hints.resources().resourceBundleHints().findAny().isPresent()
            || hints.jni().typeHints().findAny().isPresent());
  }

  /**
   * Write the specified GraalVM native configuration file, using the
   * provided {@link BasicJsonWriter}.
   *
   * @param fileName the name of the file
   * @param writer a consumer for the writer to use
   */
  protected abstract void writeTo(String fileName, Consumer<BasicJsonWriter> writer);

}
