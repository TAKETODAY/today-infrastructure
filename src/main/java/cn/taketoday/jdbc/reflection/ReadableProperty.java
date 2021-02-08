package cn.taketoday.jdbc.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.reflect.GetterMethod;
import cn.taketoday.context.utils.Mappings;
import cn.taketoday.context.utils.ReflectionUtils;

import static java.beans.Introspector.decapitalize;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isStatic;

/**
 * @author TODAY
 * @date 2021/1/8 18:52
 */
public class ReadableProperty {

  public final String name;
  public final Class<?> type;
  final GetterMethod getterMethod;

  public ReadableProperty(String name, Class<?> type, GetterMethod getterMethod) {
    this.name = name;
    this.type = type;
    this.getterMethod = getterMethod;
  }

  public Object get(Object instance) {
    return getterMethod.get(instance);
  }

  // static

  private static final Mappings<Map<String, ReadableProperty>, Void> rpCache
          = new Mappings<Map<String, ReadableProperty>, Void>() {
    @Override
    protected Map<String, ReadableProperty> createValue(Object key, Void param) {
      return collectReadableProperties((Class<?>) key);
    }
  };

  private static Map<String, ReadableProperty> collectReadableProperties(Class<?> cls) {
    Map<String, ReadableProperty> map = new HashMap<>();
    collectPropertyGetters(map, cls);
    collectReadableFields(map, cls);
    return map;
  }

  public static Map<String, ReadableProperty> readableProperties(Class<?> ofClass) {
    return rpCache.get(ofClass, (Void) null);
  }

  private static void collectReadableFields(Map<String, ReadableProperty> map, Class<?> cls) {

    for (final Field field : ReflectionUtils.getFields(cls)) {

      if (isStaticOrPrivate(field)) {
        continue;
      }
      String propName = field.getName();
      if (map.containsKey(propName)) {
        continue;
      }

      ReflectionUtils.makeAccessible(field);
      final GetterMethod getterMethod = ReflectionUtils.newGetterMethod(field);

      Class<?> returnType = field.getType();
      ReadableProperty rp = new ReadableProperty(propName, returnType, getterMethod);
      map.put(propName, rp);
    }
  }

  private static boolean isStaticOrPrivate(Member m) {
    final int modifiers = m.getModifiers();
    return isStatic(modifiers) || isPrivate(modifiers);
  }

  private static void collectPropertyGetters(Map<String, ReadableProperty> map, Class<?> cls) {

    for (final Method m : ReflectionUtils.getDeclaredMethods(cls)) {
      if (isStaticOrPrivate(m)) {
        continue;
      }
      if (0 != m.getParameterTypes().length) {
        continue;
      }
      Class<?> returnType = m.getReturnType();
      if (returnType == Void.TYPE || returnType == Void.class) {
        continue;
      }
      String name = m.getName();
      String propName = null;
      if (name.startsWith("get") && name.length() > 3) {
        propName = decapitalize(name.substring(3));
      }
      else if (name.startsWith("is") && name.length() > 2 && returnType == Boolean.TYPE) {
        propName = decapitalize(name.substring(2));
      }
      if (propName == null) {
        continue;
      }
      if (map.containsKey(propName)) {
        continue;
      }

      ReflectionUtils.makeAccessible(m);
      final GetterMethod getterMethod = ReflectionUtils.newGetterMethod(m);
      ReadableProperty rp = new ReadableProperty(propName, returnType, getterMethod);
      map.put(propName, rp);
    }
  }

}
