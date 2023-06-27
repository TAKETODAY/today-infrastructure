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

package cn.taketoday.beans.factory.annotation;

import java.util.stream.Stream;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} for Jakarta annotations.
 * <p>Hints are only registered if Jakarta inject is on the classpath.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/23 20:09
 */
class JakartaAnnotationsRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    if (ClassUtils.isPresent("jakarta.inject.Inject", classLoader)) {
      Stream.of("jakarta.inject.Inject", "jakarta.inject.Qualifier").forEach(annotationType ->
              hints.reflection().registerType(ClassUtils.resolveClassName(annotationType, classLoader)));
    }
  }

}

