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

package infra.http.codec;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.TypeReference;
import infra.http.codec.support.DefaultClientCodecConfigurer;
import infra.http.codec.support.DefaultServerCodecConfigurer;
import infra.lang.Nullable;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers runtime hints for
 * implementations listed in {@code CodecConfigurer.properties}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class CodecConfigurerRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    hints.resources().registerPattern(
            "infra/http/codec/CodecConfigurer.properties");
    hints.reflection().registerTypes(
            TypeReference.listOf(DefaultClientCodecConfigurer.class, DefaultServerCodecConfigurer.class),
            typeHint -> typeHint.onReachableType(CodecConfigurerFactory.class)
                    .withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
  }

}
