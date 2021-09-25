package cn.taketoday.core.bytecode.core;

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.GeneratorAdapter;

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
