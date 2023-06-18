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

package cn.taketoday.aot.hint.annotation;

import java.lang.reflect.AnnotatedElement;

import cn.taketoday.aot.hint.BindingReflectionHintsRegistrar;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;

/**
 * A {@link ReflectiveProcessor} implementation that registers reflection hints
 * for data binding purpose (class, constructors, fields, properties, record
 * components, including types transitively used on properties and record components).
 *
 * @author Sebastien Deleuze
 * @see RegisterReflectionForBinding @RegisterReflectionForBinding
 * @since 4.0
 */
public class RegisterReflectionForBindingProcessor implements ReflectiveProcessor {

  private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

  @Override
  public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
    RegisterReflectionForBinding registerReflection =
            AnnotationUtils.getAnnotation(element, RegisterReflectionForBinding.class);
    if (registerReflection != null) {
      Class<?>[] classes = registerReflection.classes();
      Assert.state(classes.length != 0, () -> "A least one class should be specified in " +
              "@RegisterReflectionForBinding attributes, and none was provided on " + element);
      for (Class<?> type : classes) {
        this.bindingRegistrar.registerReflectionHints(hints, type);
      }
    }
  }

}
