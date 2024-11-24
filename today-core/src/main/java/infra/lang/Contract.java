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

package infra.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Specifies some aspects of the method behavior depending on the arguments.
 * Can be used by tools for advanced data flow analysis. Note that this annotation
 * just describes how the code works and doesn't add any functionality by means of
 * code generation.
 *
 * <p>Inspired by {@code org.jetbrains.annotations.Contract}, this variant has
 * been introduced in the {@code infra.lang} package to avoid
 * requiring an extra dependency, while still following the same semantics.
 *
 * <p>Method contract has the following syntax:
 * <pre>{@code
 *  contract ::= (clause ';')* clause
 *  clause ::= args '->' effect
 *  args ::= ((arg ',')* arg )?
 *  arg ::= value-constraint
 *  value-constraint ::= '_' | 'null' | '!null' | 'false' | 'true'
 *  effect ::= value-constraint | 'fail' | 'this' | 'new' | 'param<N>'}</pre>
 *
 * <p>The constraints denote the following:
 * <ul>
 * <li>{@code _} - any value
 * <li>{@code null} - null value
 * <li>{@code !null} - a value statically proved to be not-null
 * <li>{@code true} - true boolean value
 * <li>{@code false} - false boolean value
 * </ul>
 *
 * <p>The additional return values denote the following:
 * <ul>
 * <li>{@code fail} - the method throws an exception, if the arguments satisfy argument constraints
 * <li>{@code new} - the method returns a non-null new object which is distinct from any other object existing in the heap prior to method execution. If method is also pure, then we can be sure that the new object is not stored to any field/array and will be lost if method return value is not used.
 * <li>{@code this} - the method returns its qualifier value (not applicable for static methods)
 * <li>{@code param1, param2, ...} - the method returns its first (second, ...) parameter value
 * </ul>
 *
 * <p>Examples:
 * <ul>
 * <li>{@code @Contract("_, null -> null")} - the method returns null if its second argument is null.
 * <li>{@code @Contract("_, null -> null; _, !null -> !null")} - the method returns null if its second argument is null and not-null otherwise.
 * <li>{@code @Contract("true -> fail")} - a typical {@code assertFalse} method which throws an exception if {@code true} is passed to it.
 * <li>{@code @Contract("_ -> this")} - the method always returns its qualifier (e.g. {@link StringBuilder#append(String)}).
 * <li>{@code @Contract("null -> fail; _ -> param1")} - the method throws an exception if the first argument is null,
 * otherwise it returns the first argument (e.g. {@code Objects.requireNonNull}).
 * <li>{@code @Contract("!null, _ -> param1; null, !null -> param2; null, null -> fail")} - the method returns the first non-null argument,
 * or throws an exception if both arguments are null (e.g. {@code Objects.requireNonNullElse}).
 * </ul>
 *
 * @author Sebastien Deleuze
 * @see <a href="https://github.com/JetBrains/java-annotations/blob/master/src/jvmMain/java/org/jetbrains/annotations/Contract.java">org.jetbrains.annotations.Contract</a>
 * @see <a href="https://github.com/uber/NullAway/wiki/Configuration#custom-contract-annotations">
 * NullAway custom contract annotations</a>
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

}
