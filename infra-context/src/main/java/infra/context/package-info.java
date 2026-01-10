
/**
 * This package builds on the beans package to add support for
 * message sources and for the Observer design pattern, and the
 * ability for application objects to obtain resources using a
 * consistent API.
 *
 * <p>There is no necessity for Infra applications to depend
 * on ApplicationContext or even BeanFactory functionality
 * explicitly. One of the strengths of the Infra architecture
 * is that application objects can often be configured without
 * any dependency on Infra-specific APIs.
 */
@NullMarked
package infra.context;

import org.jspecify.annotations.NullMarked;