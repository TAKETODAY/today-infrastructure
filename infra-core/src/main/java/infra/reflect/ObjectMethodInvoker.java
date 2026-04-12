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

package infra.reflect;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import infra.util.ObjectUtils;

/**
 * A high-performance method invoker specifically optimized for {@link Object} methods.
 * <p>
 * This class avoids both bytecode generation overhead and reflection invocation costs
 * by directly calling the target methods through type casting. It supports common public
 * Object methods including: {@code toString}, {@code hashCode}, {@code equals}, and
 * {@code getClass}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class ObjectMethodInvoker extends MethodInvoker implements MethodAccessor {

  private final ObjectMethodType methodType;

  ObjectMethodInvoker(Method method, ObjectMethodType methodType) {
    super(method);
    this.methodType = methodType;
  }

  @Override
  public @Nullable Object invoke(@Nullable Object obj, @Nullable Object @Nullable [] args) {
    if (obj == null) {
      throw new NullPointerException("Cannot invoke " + method.getName() + " on null object");
    }

    return switch (methodType) {
      case TOSTRING -> obj.toString();
      case HASHCODE -> obj.hashCode();
      case EQUALS -> obj.equals(ObjectUtils.isNotEmpty(args) ? args[0] : null);
      case GETCLASS -> obj.getClass();
    };
  }

}


