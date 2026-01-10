
/**
 * AspectJ integration package. Includes Framework AOP advice implementations for AspectJ 5
 * annotation-style methods, and an AspectJExpressionPointcut: a Framework AOP Pointcut
 * implementation that allows use of the AspectJ pointcut expression language with the Framework AOP
 * runtime framework.
 *
 * <p>Note that use of this package does <i>not</i> require the use of the {@code ajc} compiler
 * or AspectJ load-time weaver. It is intended to enable the use of a valuable subset of AspectJ
 * functionality, with consistent semantics, with the proxy-based Framework AOP framework.
 */
@NullMarked
package infra.aop.aspectj;

import org.jspecify.annotations.NullMarked;