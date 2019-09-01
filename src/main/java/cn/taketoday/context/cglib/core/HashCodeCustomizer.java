package cn.taketoday.context.cglib.core;

import cn.taketoday.context.asm.Type;

/**
 * 
 * @author TODAY <br>
 *         2019-09-01 21:21
 */
@FunctionalInterface
public interface HashCodeCustomizer extends KeyFactoryCustomizer {
    /**
     * Customizes calculation of hashcode
     * 
     * @param e
     *            code emitter
     * @param type
     *            parameter type
     */
    boolean customize(CodeEmitter e, Type type);
}
