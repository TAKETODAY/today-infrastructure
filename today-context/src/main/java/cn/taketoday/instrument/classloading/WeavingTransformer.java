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

package cn.taketoday.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * ClassFileTransformer-based weaver, allowing for a list of transformers to be
 * applied on a class byte array. Normally used inside class loaders.
 *
 * <p>Note: This class is deliberately implemented for minimal external dependencies,
 * since it is included in weaver jars (to be deployed into application servers).
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WeavingTransformer {

  @Nullable
  private final ClassLoader classLoader;

  private final List<ClassFileTransformer> transformers = new ArrayList<>();

  /**
   * Create a new WeavingTransformer for the given class loader.
   *
   * @param classLoader the ClassLoader to build a transformer for
   */
  public WeavingTransformer(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Add a class file transformer to be applied by this weaver.
   *
   * @param transformer the class file transformer to register
   */
  public void addTransformer(ClassFileTransformer transformer) {
    Assert.notNull(transformer, "Transformer must not be null");
    this.transformers.add(transformer);
  }

  /**
   * Apply transformation on a given class byte definition.
   * The method will always return a non-null byte array (if no transformation has taken place
   * the array content will be identical to the original one).
   *
   * @param className the full qualified name of the class in dot format (i.e. some.package.SomeClass)
   * @param bytes class byte definition
   * @return (possibly transformed) class byte definition
   */
  public byte[] transformIfNecessary(String className, byte[] bytes) {
    String internalName = StringUtils.replace(className, ".", "/");
    return transformIfNecessary(className, internalName, bytes, null);
  }

  /**
   * Apply transformation on a given class byte definition.
   * The method will always return a non-null byte array (if no transformation has taken place
   * the array content will be identical to the original one).
   *
   * @param className the full qualified name of the class in dot format (i.e. some.package.SomeClass)
   * @param internalName class name internal name in / format (i.e. some/package/SomeClass)
   * @param bytes class byte definition
   * @param pd protection domain to be used (can be null)
   * @return (possibly transformed) class byte definition
   */
  public byte[] transformIfNecessary(String className, String internalName, byte[] bytes, @Nullable ProtectionDomain pd) {
    byte[] result = bytes;
    for (ClassFileTransformer cft : this.transformers) {
      try {
        byte[] transformed = cft.transform(this.classLoader, internalName, null, pd, result);
        if (transformed != null) {
          result = transformed;
        }
      }
      catch (IllegalClassFormatException ex) {
        throw new IllegalStateException("Class file transformation failed", ex);
      }
    }
    return result;
  }

}
