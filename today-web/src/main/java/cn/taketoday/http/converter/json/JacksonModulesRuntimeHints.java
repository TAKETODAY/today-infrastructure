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

package cn.taketoday.http.converter.json;

import java.util.function.Consumer;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.TypeHint;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers reflection entries
 * for {@link Jackson2ObjectMapperBuilder} well-known modules.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JacksonModulesRuntimeHints implements RuntimeHintsRegistrar {

  private static final Consumer<TypeHint.Builder> asJacksonModule = builder ->
          builder.onReachableType(Jackson2ObjectMapperBuilder.class)
                  .withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    hints.reflection()
            .registerTypeIfPresent(classLoader,
                    "com.fasterxml.jackson.datatype.jdk8.Jdk8Module", asJacksonModule)
            .registerTypeIfPresent(classLoader,
                    "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", asJacksonModule)
            .registerTypeIfPresent(classLoader,
                    "com.fasterxml.jackson.module.kotlin.KotlinModule", asJacksonModule);
  }

}
