package cn.taketoday.beans.factory.support;

import java.lang.reflect.Method;
import java.util.Set;

import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/5 18:18
 */
abstract class AutowireUtils {

  /**
   * Determine whether the given bean property is excluded from dependency checks.
   * <p>This implementation excludes properties defined by CGLIB.
   *
   * @param property the PropertyDescriptor of the bean property
   * @return whether the bean property is excluded
   */
  public static boolean isExcludedFromDependencyCheck(BeanProperty property) {
    Method wm = property.getWriteMethod();
    if (wm == null) {
      return false;
    }
    if (!wm.getDeclaringClass().getName().contains("$$")) {
      // Not a CGLIB method so it's OK.
      return false;
    }
    // It was declared by CGLIB, but we might still want to autowire it
    // if it was actually declared by the superclass.
    Class<?> superclass = wm.getDeclaringClass().getSuperclass();
    return !ReflectionUtils.hasMethod(superclass, wm);
  }

  /**
   * Return whether the setter method of the given bean property is defined
   * in any of the given interfaces.
   *
   * @param property the PropertyDescriptor of the bean property
   * @param interfaces the Set of interfaces (Class objects)
   * @return whether the setter method is defined by an interface
   */
  public static boolean isSetterDefinedInInterface(BeanProperty property, Set<Class<?>> interfaces) {
    Method setter = property.getWriteMethod();
    if (setter != null) {
      Class<?> targetClass = setter.getDeclaringClass();
      for (Class<?> ifc : interfaces) {
        if (ifc.isAssignableFrom(targetClass) && ReflectionUtils.hasMethod(ifc, setter)) {
          return true;
        }
      }
    }
    return false;
  }

}
