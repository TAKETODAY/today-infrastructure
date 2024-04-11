/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inspired from {@code org.jetbrains.annotations.Contract}, this variant
 * has been introduce in the {@code cn.taketoday.lang} package to avoid
 * requiring an extra dependency, while still following the same semantics.
 *
 * <p>Specifies some aspects of the method behavior depending on the arguments.
 * Can be used by tools for advanced data flow analysis. Note that this annotation
 * just describes how the code works and doesn't add any functionality by
 * means of code generation.
 *
 * <p>Method contract has the following syntax:<br/>
 * contract ::= (clause ';')* clause<br/>
 * clause ::= args '->' effect<br/>
 * args ::= ((arg ',')* arg )?<br/>
 * arg ::= value-constraint<br/>
 * value-constraint ::= 'any' | 'null' | '!null' | 'false' | 'true'<br/>
 * effect ::= value-constraint | 'fail'
 *
 * The constraints denote the following:<br/>
 * <ul>
 * <li> _ - any value
 * <li> null - null value
 * <li> !null - a value statically proved to be not-null
 * <li> true - true boolean value
 * <li> false - false boolean value
 * <li> fail - the method throws an exception, if the arguments satisfy argument constraints
 * </ul>
 * <p>Examples:
 * <code>@Contract("_, null -> null")</code> - method returns null if its second argument is null<br/>
 * <code>@Contract("_, null -> null; _, !null -> !null")</code> - method returns null if its second argument is null and not-null otherwise<br/>
 * <code>@Contract("true -> fail")</code> - a typical assertFalse method which throws an exception if <code>true</code> is passed to it<br/>
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://github.com/uber/NullAway/wiki/Configuration#custom-contract-annotations">NullAway custom contract annotations</a>
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Contract {

  /**
   * Contains the contract clauses describing causal relations between call
   * arguments and the returned value.
   */
  String value() default "";

  /**
   * Specifies if this method is pure, i.e. has no visible side effects. This may
   * be used for more precise data flow analysis, and to check that the method's
   * return value is actually used in the call place.
   */
  boolean pure() default false;
}
