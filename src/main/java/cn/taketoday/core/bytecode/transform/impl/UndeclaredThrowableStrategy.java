/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.bytecode.transform.impl;

import java.lang.reflect.Modifier;

import cn.taketoday.core.bytecode.core.ClassGenerator;
import cn.taketoday.core.bytecode.core.DefaultGeneratorStrategy;
import cn.taketoday.core.bytecode.core.GeneratorStrategy;
import cn.taketoday.core.bytecode.transform.ClassTransformer;
import cn.taketoday.core.bytecode.transform.MethodFilter;
import cn.taketoday.core.bytecode.transform.MethodFilterTransformer;
import cn.taketoday.core.bytecode.transform.TransformingClassGenerator;

/**
 * A {@link GeneratorStrategy} suitable for use with
 * {@link cn.taketoday.core.bytecode.proxy.Enhancer} which causes all undeclared
 * exceptions thrown from within a proxied method to be wrapped in an
 * alternative exception of your choice.
 */
@SuppressWarnings({ "rawtypes" })
public class UndeclaredThrowableStrategy extends DefaultGeneratorStrategy {

  private final Class wrapper;

  /**
   * Create a new instance of this strategy.
   *
   * @param wrapper a class which extends either directly or indirectly from
   * <code>Throwable</code> and which has at least one constructor that
   * takes a single argument of type <code>Throwable</code>, for
   * example
   * <code>java.lang.reflect.UndeclaredThrowableException.class</code>
   */
  public UndeclaredThrowableStrategy(Class wrapper) {
    this.wrapper = wrapper;
  }

  private static final MethodFilter TRANSFORM_FILTER = new MethodFilter() {
    public boolean accept(int access, String name, String desc, String signature, String[] exceptions) {
      return !Modifier.isPrivate(access) && name.indexOf('$') < 0;
    }
  };

  protected ClassGenerator transform(ClassGenerator cg) throws Exception {
    ClassTransformer tr = new MethodFilterTransformer(TRANSFORM_FILTER, new UndeclaredThrowableTransformer(wrapper));
    return new TransformingClassGenerator(cg, tr);
  }
}
