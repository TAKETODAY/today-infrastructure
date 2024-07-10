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

package cn.taketoday.aot.nativex;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import cn.taketoday.aot.hint.JdkProxyHint;
import cn.taketoday.aot.hint.ProxyHints;
import cn.taketoday.aot.hint.TypeReference;

/**
 * Write {@link JdkProxyHint}s contained in a {@link ProxyHints} to the JSON
 * output expected by the GraalVM {@code native-image} compiler, typically named
 * {@code proxy-config.json}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/DynamicProxy/">Dynamic Proxy in Native Image</a>
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 * @since 4.0
 */
class ProxyHintsWriter {

  private static final Comparator<JdkProxyHint> JDK_PROXY_HINT_COMPARATOR = (left, right) -> {
    String leftSignature = left.getProxiedInterfaces().stream()
            .map(TypeReference::getCanonicalName).collect(Collectors.joining(","));
    String rightSignature = right.getProxiedInterfaces().stream()
            .map(TypeReference::getCanonicalName).collect(Collectors.joining(","));
    return leftSignature.compareTo(rightSignature);
  };

  public static void write(BasicJsonWriter writer, ProxyHints hints) {
    writer.writeArray(hints.jdkProxyHints().sorted(JDK_PROXY_HINT_COMPARATOR).map(ProxyHintsWriter::toAttributes).toList());
  }

  private static Map<String, Object> toAttributes(JdkProxyHint hint) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    handleCondition(attributes, hint);
    attributes.put("interfaces", hint.getProxiedInterfaces());
    return attributes;
  }

  private static void handleCondition(Map<String, Object> attributes, JdkProxyHint hint) {
    if (hint.getReachableType() != null) {
      Map<String, Object> conditionAttributes = new LinkedHashMap<>();
      conditionAttributes.put("typeReachable", hint.getReachableType());
      attributes.put("condition", conditionAttributes);
    }
  }

}
