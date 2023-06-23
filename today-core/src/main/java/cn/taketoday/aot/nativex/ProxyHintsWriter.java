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

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.aot.hint.JdkProxyHint;
import cn.taketoday.aot.hint.ProxyHints;

/**
 * Write {@link JdkProxyHint}s contained in a {@link ProxyHints} to the JSON
 * output expected by the GraalVM {@code native-image} compiler, typically named
 * {@code proxy-config.json}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/DynamicProxy/">Dynamic Proxy in Native Image</a>
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 * @since 4.0
 */
class ProxyHintsWriter {

  public static final ProxyHintsWriter INSTANCE = new ProxyHintsWriter();

  public void write(BasicJsonWriter writer, ProxyHints hints) {
    writer.writeArray(hints.jdkProxyHints().map(this::toAttributes).toList());
  }

  private Map<String, Object> toAttributes(JdkProxyHint hint) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    handleCondition(attributes, hint);
    attributes.put("interfaces", hint.getProxiedInterfaces());
    return attributes;
  }

  private void handleCondition(Map<String, Object> attributes, JdkProxyHint hint) {
    if (hint.getReachableType() != null) {
      Map<String, Object> conditionAttributes = new LinkedHashMap<>();
      conditionAttributes.put("typeReachable", hint.getReachableType());
      attributes.put("condition", conditionAttributes);
    }
  }

}
