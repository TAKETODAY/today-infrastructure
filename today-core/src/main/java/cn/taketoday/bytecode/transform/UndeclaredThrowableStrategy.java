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

package cn.taketoday.bytecode.transform;

import java.lang.reflect.Modifier;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.core.ClassGenerator;
import cn.taketoday.bytecode.core.DefaultGeneratorStrategy;
import cn.taketoday.bytecode.core.GeneratorStrategy;
import cn.taketoday.bytecode.proxy.Enhancer;

/**
 * A {@link GeneratorStrategy} suitable for use with {@link Enhancer} which
 * causes all undeclared exceptions thrown from within a proxied method to be wrapped
 * in an alternative exception of your choice.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@SuppressWarnings({ "rawtypes" })
public class UndeclaredThrowableStrategy extends DefaultGeneratorStrategy {

  private final Class wrapper;

  /**
   * Create a new instance of this strategy.
   *
   * @param wrapper a class which extends either directly or
   * indirectly from <code>Throwable</code> and which has at least one
   * constructor that takes a single argument of type
   * <code>Throwable</code>, for example
   * <code>java.lang.reflect.UndeclaredThrowableException.class</code>
   */
  public UndeclaredThrowableStrategy(Class wrapper) {
    this.wrapper = wrapper;
  }

  @Override
  protected ClassGenerator transform(ClassGenerator cg) throws Exception {
    ClassTransformer tr = new UndeclaredThrowableTransformer(wrapper);
    tr = new MethodFilterTransformer(tr);
    return new TransformingClassGenerator(cg, tr);
  }

  static class MethodFilterTransformer extends ClassTransformer {

    private final ClassTransformer pass;

    private ClassVisitor direct;

    public MethodFilterTransformer(ClassTransformer pass) {
      this.pass = pass;
      this.cv = pass;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name,
            String desc, String signature, String[] exceptions) {

      if (!Modifier.isPrivate(access) && name.indexOf('$') < 0) {
        return pass.visitMethod(access, name, desc, signature, exceptions);
      }
      return direct.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void setTarget(ClassVisitor target) {
      pass.setTarget(target);
      direct = target;
    }
  }
}