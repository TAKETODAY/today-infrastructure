package infra.beans.support;

import java.lang.reflect.Field;

import infra.beans.BeanInstantiationException;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
abstract class UnsafeUtils {

  private static final Unsafe unsafe;

  static {
    try {
      Field f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      unsafe = (Unsafe) f.get(null);
    }
    catch (NoSuchFieldException | IllegalAccessException e) {
      throw new BeanInstantiationException(null, e);
    }
  }

  public static Unsafe getUnsafe() {
    return unsafe;
  }
}