package cn.taketoday.cglib.core;

import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.GeneratorAdapter;

/**
 * @author TODAY <br>
 * 2019-09-01 21:21
 */
@FunctionalInterface
public interface HashCodeCustomizer extends KeyFactoryCustomizer {
  /**
   * Customizes calculation of hashcode
   *
   * @param e
   *         code emitter
   * @param type
   *         parameter type
   */
  boolean customize(GeneratorAdapter e, Type type);
}
