/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.bytecode.core;

import cn.taketoday.bytecode.ClassWriter;

public class DefaultGeneratorStrategy implements GeneratorStrategy {

  public static final DefaultGeneratorStrategy INSTANCE = new DefaultGeneratorStrategy();

  @Override
  public byte[] generate(ClassGenerator cg) throws Exception {
    ClassWriter cw = createClassVisitor();
    transform(cg).generateClass(cw);
    return transform(cw.toByteArray());
  }

  protected ClassWriter createClassVisitor() {
    return new ClassWriter(ClassWriter.COMPUTE_FRAMES);
  }

  protected byte[] transform(byte[] b) throws Exception {
    return b;
  }

  protected ClassGenerator transform(ClassGenerator cg) throws Exception {
    return cg;
  }
}
