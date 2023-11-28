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

package cn.taketoday.http.converter.json;

import cn.taketoday.aot.hint.BindingReflectionHintsRegistrar;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers binding reflection entries
 * for {@link ProblemDetail} serialization support with Jackson.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ProblemDetailRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();
    bindingRegistrar.registerReflectionHints(hints.reflection(), ProblemDetail.class);
    if (ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader)) {
      bindingRegistrar.registerReflectionHints(hints.reflection(), ProblemDetailJacksonXmlMixin.class);
    }
    else if (ClassUtils.isPresent("com.fasterxml.jackson.annotation.JacksonAnnotation", classLoader)) {
      bindingRegistrar.registerReflectionHints(hints.reflection(), ProblemDetailJacksonMixin.class);
    }
  }

}
