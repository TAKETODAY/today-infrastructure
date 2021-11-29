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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.core.bytecode;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.StringUtils;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/29 11:23
 */
public class BytecodeCompiler {
  private static final int CLASSES_DEFINED_LIMIT = 100;

  // A compiler is created for each classloader, it manages a child class loader of that
  // classloader and the child is used to load the compiled classes.
  private static final ConcurrentReferenceHashMap<ClassLoader, BytecodeCompiler> compilers = new ConcurrentReferenceHashMap<>();

  // The child ClassLoader used to load the compiled classes
  private volatile ChildClassLoader childClassLoader;

  private BytecodeCompiler(@Nullable ClassLoader classloader) {
    this.childClassLoader = new ChildClassLoader(classloader);
  }

  public Class<?> compile(String className, byte[] classFile) {
    return loadClass(StringUtils.replace(className, "/", "."), classFile);
  }

  /**
   * Load a class. Makes sure the classloaders aren't used too much
   * because they anchor compiled classes in memory and prevent GC. If you have expressions
   * continually recompiling over time then by replacing the classloader periodically
   * at least some of the older variants can be garbage collected.
   *
   * @param name the name of the class
   * @param classFile the bytecode for the class
   * @return the Class object for the compiled expression
   */
  private Class<?> loadClass(String name, byte[] classFile) {
    ChildClassLoader ccl = this.childClassLoader;
    if (ccl.getClassesDefinedCount() >= CLASSES_DEFINED_LIMIT) {
      synchronized(this) {
        ChildClassLoader currentCcl = this.childClassLoader;
        if (ccl == currentCcl) {
          // Still the same ClassLoader that needs to be replaced...
          ccl = new ChildClassLoader(ccl.getParent());
          this.childClassLoader = ccl;
        }
        else {
          // Already replaced by some other thread, let's pick it up.
          ccl = currentCcl;
        }
      }
    }
    return ccl.defineClass(name, classFile);
  }


  /**
   * Factory method for compiler instances. The returned SpelCompiler will
   * attach a class loader as the child of the given class loader and this
   * child will be used to load compiled expressions.
   *
   * @param classLoader the ClassLoader to use as the basis for compilation
   * @return a corresponding SpelCompiler instance
   */
  public static BytecodeCompiler getCompiler(@Nullable ClassLoader classLoader) {
    ClassLoader clToUse = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
    // Quick check for existing compiler without lock contention
    BytecodeCompiler compiler = compilers.get(clToUse);
    if (compiler == null) {
      // Full lock now since we're creating a child ClassLoader
      synchronized(compilers) {
        compiler = compilers.get(clToUse);
        if (compiler == null) {
          compiler = new BytecodeCompiler(clToUse);
          compilers.put(clToUse, compiler);
        }
      }
    }
    return compiler;
  }

  /**
   * A ChildClassLoader will load the generated compiled  classes.
   */
  private static class ChildClassLoader extends URLClassLoader {
    private static final URL[] NO_URLS = new URL[0];

    private final AtomicInteger classesDefinedCount = new AtomicInteger(0);

    public ChildClassLoader(@Nullable ClassLoader classLoader) {
      super(NO_URLS, classLoader);
    }

    public Class<?> defineClass(String name, byte[] bytes) {
      Class<?> clazz = super.defineClass(name, bytes, 0, bytes.length);
      this.classesDefinedCount.incrementAndGet();
      return clazz;
    }

    public int getClassesDefinedCount() {
      return this.classesDefinedCount.get();
    }
  }

}
