
/**
 * SPI package allowing AOP framework to handle arbitrary advice types.
 *
 * <p>Users who want merely to <i>use</i> the AOP framework, rather than extend
 * its capabilities, don't need to concern themselves with this package.
 *
 * <p>You may wish to use these adapters to wrap Framework-specific advices, such as MethodBeforeAdvice,
 * in MethodInterceptor, to allow their use in another AOP framework supporting the AOP Alliance interfaces.
 *
 * <p>These adapters do not depend on any other framework classes to allow such usage.
 */
@NullMarked
package infra.aop.framework.adapter;

import org.jspecify.annotations.NullMarked;
