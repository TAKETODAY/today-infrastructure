/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.bytecode.transform;

import java.lang.reflect.Modifier;

import infra.bytecode.ClassVisitor;
import infra.bytecode.MethodVisitor;
import infra.bytecode.core.ClassGenerator;
import infra.bytecode.core.DefaultGeneratorStrategy;
import infra.bytecode.core.GeneratorStrategy;
import infra.bytecode.proxy.Enhancer;

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