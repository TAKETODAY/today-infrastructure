/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.cglib.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import cn.taketoday.asm.ClassReader;
import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.MethodVisitor;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.commons.MethodSignature;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class DuplicatesPredicate implements Predicate<Method> {

  private final Set<Object> unique = new HashSet<>();
  private final Set<Method> rejected;

  /**
   * Constructs a DuplicatesPredicate that will allow subclass bridge methods to
   * be preferred over superclass non-bridge methods.
   */
  public DuplicatesPredicate() {
    rejected = Collections.emptySet();
  }

  /**
   * Constructs a DuplicatesPredicate that prefers using superclass non-bridge
   * methods despite a subclass method with the same signtaure existing (if the
   * subclass is a bridge method).
   */
  public DuplicatesPredicate(List<Method> allMethods) {
    rejected = new HashSet<>();

    // Traverse through the methods and capture ones that are bridge
    // methods when a subsequent method (from a non-interface superclass)
    // has the same signature but isn't a bridge. Record these so that
    // we avoid using them when filtering duplicates.
    HashMap<Object, Method> scanned = new HashMap<>();
    HashMap<Object, Method> suspects = new HashMap<>();
    for (final Method method : allMethods) {
      final Object sig = MethodWrapper.create(method);
      final Method existing = scanned.get(sig);
      if (existing == null) {
        scanned.put(sig, method);
      }
      else if (!suspects.containsKey(sig) && existing.isBridge() && !method.isBridge()) {
        // TODO: this currently only will capture a single bridge. it will not work
        // if there's Child.bridge1 Middle.bridge2 Parent.concrete. (we'd offer the 2nd
        // bridge).
        // no idea if that's even possible tho...
        suspects.put(sig, existing);
      }
    }

    if (!suspects.isEmpty()) {
      HashSet<Class<?>> classes = new HashSet<>();
      UnnecessaryBridgeFinder finder = new UnnecessaryBridgeFinder(rejected);
      for (final Method m : suspects.values()) {
        classes.add(m.getDeclaringClass());
        finder.addSuspectMethod(m);
      }
      for (final Class c : classes) {
        final ClassLoader cl = getClassLoader(c);
        if (cl != null) {
          try (final InputStream is = cl.getResourceAsStream(c.getName().replace('.', '/') + ".class")) {
            if (is != null) {
              new ClassReader(is).accept(finder, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            }
          }
          catch (IOException ignored) { }
        }
      }
    }

  }

  @Override
  public boolean test(Method arg) {
    return !rejected.contains(arg) && unique.add(MethodWrapper.create(arg));
  }

  private static ClassLoader getClassLoader(Class c) {
    ClassLoader cl = c.getClassLoader();
    if (cl == null) {
      cl = DuplicatesPredicate.class.getClassLoader();
    }
    if (cl == null) {
      cl = Thread.currentThread().getContextClassLoader();
    }
    return cl;
  }

  private static class UnnecessaryBridgeFinder extends ClassVisitor {
    private final Set<Method> rejected;

    private MethodSignature currentMethodSig = null;
    private HashMap<MethodSignature, Method> methods = new HashMap<>();

    UnnecessaryBridgeFinder(Set<Method> rejected) {
      this.rejected = rejected;
    }

    void addSuspectMethod(Method m) {
      methods.put(MethodSignature.from(m), m);
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) { }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      MethodSignature sig = new MethodSignature(name, desc);
      final Method currentMethod = methods.remove(sig);
      if (currentMethod != null) {
        currentMethodSig = sig;
        return new MethodVisitor() {
          public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == Opcodes.INVOKESPECIAL && currentMethodSig != null) {
              MethodSignature target = new MethodSignature(name, desc);
              if (target.equals(currentMethodSig)) {
                rejected.add(currentMethod);
              }
              currentMethodSig = null;
            }
          }
        };
      }
      else {
        return null;
      }
    }
  }
}
