package infra.reflect;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import infra.util.ReflectionUtils;

/**
 * Enum representing all public Object methods that are optimized.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/4/11 21:11
 */
enum ObjectMethodType {
  TOSTRING,
  HASHCODE,
  EQUALS,
  GETCLASS;

  static @Nullable ObjectMethodType forMethod(Method method) {
    if (ReflectionUtils.isToStringMethod(method)) {
      return TOSTRING;
    }
    else if (ReflectionUtils.isEqualsMethod(method)) {
      return EQUALS;
    }
    else if (ReflectionUtils.isHashCodeMethod(method)) {
      return HASHCODE;
    }
    if (method.getName().equals("getClass") && method.getParameterCount() == 0) {
      return GETCLASS;
    }
    return null;
  }

}
