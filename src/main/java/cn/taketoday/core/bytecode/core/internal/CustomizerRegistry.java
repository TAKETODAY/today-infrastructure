package cn.taketoday.core.bytecode.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import cn.taketoday.core.bytecode.core.Customizer;
import cn.taketoday.core.bytecode.core.KeyFactoryCustomizer;

/**
 * @author TODAY <br>
 * 2019-10-17 20:45
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CustomizerRegistry {

  private final Class<?>[] customizerTypes;
  private final HashMap<Class<?>, List<KeyFactoryCustomizer>> customizers = new HashMap<>();

  public CustomizerRegistry(Class... customizerTypes) {
    this.customizerTypes = customizerTypes;
  }

  public void add(KeyFactoryCustomizer customizer) {
    Class<? extends KeyFactoryCustomizer> klass = customizer.getClass();
    for (Class<?> type : customizerTypes) {
      if (type.isAssignableFrom(klass)) {
        List<KeyFactoryCustomizer> list = customizers.get(type);
        if (list == null) {
          customizers.put(type, list = new ArrayList<>());
        }
        list.add(customizer);
      }
    }
  }

  public <T> List<T> get(Class<T> klass) {
    List<KeyFactoryCustomizer> list = customizers.get(klass);
    return list == null ? Collections.emptyList() : (List<T>) list;
  }

  public static CustomizerRegistry singleton(Customizer customizer) {
    CustomizerRegistry registry = new CustomizerRegistry(Customizer.class);
    registry.add(customizer);
    return registry;
  }
}
