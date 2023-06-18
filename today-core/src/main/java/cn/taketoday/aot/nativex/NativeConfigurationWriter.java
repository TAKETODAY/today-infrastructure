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

package cn.taketoday.aot.nativex;

import java.util.function.Consumer;

import cn.taketoday.aot.hint.ProxyHints;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.ResourceHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.SerializationHints;

/**
 * Write {@link RuntimeHints} as GraalVM native configuration.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Janne Valkealahti
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
    if (hints.serialization().javaSerializationHints().findAny().isPresent()) {
      writeSerializationHints(hints.serialization());
    }
    if (hints.proxies().jdkProxyHints().findAny().isPresent()) {
      writeProxyHints(hints.proxies());
    }
    if (hints.reflection().typeHints().findAny().isPresent()) {
      writeReflectionHints(hints.reflection());
    }
    if (hints.resources().resourcePatternHints().findAny().isPresent() ||
            hints.resources().resourceBundleHints().findAny().isPresent()) {
      writeResourceHints(hints.resources());
    }
    if (hints.jni().typeHints().findAny().isPresent()) {
      writeJniHints(hints.jni());
    }
  }

  /**
   * Write the specified GraalVM native configuration file, using the
   * provided {@link BasicJsonWriter}.
   *
   * @param fileName the name of the file
   * @param writer a consumer for the writer to use
   */
  protected abstract void writeTo(String fileName, Consumer<BasicJsonWriter> writer);

  private void writeSerializationHints(SerializationHints hints) {
    writeTo("serialization-config.json", writer ->
            SerializationHintsWriter.INSTANCE.write(writer, hints));
  }

  private void writeProxyHints(ProxyHints hints) {
    writeTo("proxy-config.json", writer ->
            ProxyHintsWriter.INSTANCE.write(writer, hints));
  }

  private void writeReflectionHints(ReflectionHints hints) {
    writeTo("reflect-config.json", writer ->
            ReflectionHintsWriter.INSTANCE.write(writer, hints));
  }

  private void writeResourceHints(ResourceHints hints) {
    writeTo("resource-config.json", writer ->
            ResourceHintsWriter.INSTANCE.write(writer, hints));
  }

  private void writeJniHints(ReflectionHints hints) {
    writeTo("jni-config.json", writer ->
            ReflectionHintsWriter.INSTANCE.write(writer, hints));
  }

}
