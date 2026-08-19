/**
 * Framework-wide lowest-level building blocks shared by the entire Infra framework.
 *
 * <p>This package hosts two kinds of types:
 * <ul>
 * <li>language-level annotations carrying nullability, contract, and JDK API
 * indications (for example {@code NullValue}, {@code CheckReturnValue},
 * {@code VisibleForTesting});</li>
 * <li>framework-global contracts and helpers used across all modules (for
 * example {@code Assert}, {@code Constant}, {@code Contract}, {@code Version},
 * and {@code TodayStrategies}).</li>
 * </ul>
 *
 * <p>Types here are meant to sit at the lowest level of Infra's package
 * dependency arrangement, even lower than {@code infra.util}, deliberately
 * avoiding references to higher-level framework-specific concepts.
 */
@NullMarked
package infra.lang;

import org.jspecify.annotations.NullMarked;