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
package cn.taketoday.core.bytecode;

import cn.taketoday.core.bytecode.core.AbstractClassGenerator;
import cn.taketoday.util.ReflectionUtils;

abstract public class TestGenerator extends AbstractClassGenerator {
  private static int counter;

  public TestGenerator(String source) {
    super(source);
  }

  protected ClassLoader getDefaultClassLoader() {
    return null;
  }

  protected Object firstInstance(Class type) throws Exception {
    return ReflectionUtils.newInstance(type);
  }

  protected Object nextInstance(Object instance) throws Exception {
    return instance;
  }

  public Object create() {
    return create(counter++);
  }
}
