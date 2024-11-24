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

package infra.bytecode.transform;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashSet;

import infra.bytecode.Type;
import infra.bytecode.commons.MethodSignature;
import infra.bytecode.core.Block;
import infra.bytecode.core.ClassEmitter;
import infra.bytecode.core.CodeEmitter;
import infra.util.CollectionUtils;
import infra.bytecode.core.EmitUtils;

final class UndeclaredThrowableTransformer extends ClassEmitter {

  private final Type wrapper;

  public UndeclaredThrowableTransformer(Class<?> wrapper) {
    this.wrapper = Type.forClass(wrapper);
    Constructor<?>[] cstructs = wrapper.getConstructors();
    for (final Constructor<?> cstruct : cstructs) {
      Class<?>[] types = cstruct.getParameterTypes();
      if (types.length == 1 && types[0].equals(Throwable.class)) {
        return;
      }
    }
    throw new IllegalArgumentException(
            wrapper + " does not have a single-arg constructor that takes a Throwable");
  }

  @Override
  public CodeEmitter beginMethod(int access, final MethodSignature sig, final Type... exceptions) {
    final CodeEmitter e = super.beginMethod(access, sig, exceptions);
    if (Modifier.isAbstract(access) || sig.equals(MethodSignature.SIG_STATIC)) {
      return e;
    }

    return new CodeEmitter(e) {
      private final boolean isConstructor = MethodSignature.CONSTRUCTOR_NAME.equals(sig.getName());

      private Block handler = begin_block();

      private boolean callToSuperSeen;

      @Override
      public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        if (isConstructor && !callToSuperSeen && MethodSignature.CONSTRUCTOR_NAME.equals(name)) {
          // we start the entry in the exception table after the call to super
          handler = begin_block();
          callToSuperSeen = true;
        }
      }

      @Override
      public void visitMaxs(int maxStack, int maxLocals) {
        handler.end();
        wrapUndeclaredThrowable(this, handler, exceptions, wrapper);
        super.visitMaxs(maxStack, maxLocals);
      }
    };
  }

  /* generates:
     } catch (RuntimeException e) {
       throw e;
     } catch (Error e) {
       throw e;
     } catch (<DeclaredException> e) {
       throw e;
     } catch (Throwable e) {
       throw new <Wrapper>(e);
     }
  */
  static void wrapUndeclaredThrowable(CodeEmitter e, Block handler, Type[] exceptions, Type wrapper) {
    HashSet<Type> set = new HashSet<>();
    CollectionUtils.addAll(set, exceptions);
    if (set.contains(Type.TYPE_THROWABLE)) {
      return;
    }
    boolean needThrow = exceptions != null;
    if (!set.contains(Type.TYPE_RUNTIME_EXCEPTION)) {
      e.catchException(handler, Type.TYPE_RUNTIME_EXCEPTION);
      needThrow = true;
    }
    if (!set.contains(Type.TYPE_ERROR)) {
      e.catchException(handler, Type.TYPE_ERROR);
      needThrow = true;
    }
    if (exceptions != null) {
      for (Type exception : exceptions) {
        e.catchException(handler, exception);
      }
    }
    if (needThrow) {
      e.throwException();
    }
    // e -> eo -> oeo -> ooe -> o
    e.catchException(handler, Type.TYPE_THROWABLE);
    e.newInstance(wrapper);
    e.dupX1();
    e.swap();
    e.invokeConstructor(wrapper, EmitUtils.CSTRUCT_THROWABLE);
    e.throwException();
  }

}
