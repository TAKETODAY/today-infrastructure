/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aot.nativex.substitution;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.TargetClass;

import org.jspecify.annotations.Nullable;

/**
 * Allow to reference {@code com.sun.beans.finder.ClassFinder} from
 * {@link Target_Introspector}.
 *
 * TODO Remove once Infra requires GraalVM 23.0+, see <a href="https://github.com/oracle/graal/pull/5224">graal#5224</a>.
 *
 * @author Sebastien Deleuze
 * @since 4.0
 */
@TargetClass(className = "com.sun.beans.finder.ClassFinder")
final class Target_ClassFinder {

  @Alias
  @Nullable
  public static Class<?> findClass(String name, ClassLoader loader) throws ClassNotFoundException {
    return null;
  }
}
