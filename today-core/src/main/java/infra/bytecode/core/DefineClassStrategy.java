/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.bytecode.core;

import org.jspecify.annotations.Nullable;

import java.security.ProtectionDomain;

/**
 * define class strategy
 *
 * @author TODAY 2021/11/10 16:29
 * @since 4.0
 */
public interface DefineClassStrategy {

  /**
   * define a class file to class
   *
   * @param className the name of the loaded class.
   * @param neighbor the class contained in the same package as the loaded class.
   * @param classLoader the class loader.  It can be null if {@code neighbor} is not null
   * and the JVM is Java 11 or later.
   * @param domain if it is null, a default domain is used.
   * @param classFile the bytecode for the loaded class.
   */
  Class<?> defineClass(String className, ClassLoader classLoader,
          @Nullable ProtectionDomain domain, @Nullable Class<?> neighbor, byte[] classFile);
}
