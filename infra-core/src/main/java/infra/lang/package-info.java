/**
 * Language-level annotations and framework-wide contracts sitting at the lowest
 * level of the Infra package dependency arrangement.
 *
 * <p>This package hosts foundational types that are deliberately free of
 * framework-specific logic, including:
 * <ul>
 * <li>language-level annotations for nullability, contract, and JDK API
 * indications (for example {@code NullValue}, {@code CheckReturnValue},
 * {@code VisibleForTesting});</li>
 * <li>generic contracts and marker types used across all modules (for
 * example {@code Constant}, {@code Contract}, {@code Descriptive},
 * {@code Modifiable}, {@code Unmodifiable});</li>
 * <li>versioning support ({@code Version}, {@code VersionExtractor}).</li>
 * </ul>
 *
 * <p>Types here sit below {@code infra.util} in the package dependency
 * arrangement and avoid referencing higher-level framework-specific concepts.
 */
@NullMarked
package infra.lang;

import org.jspecify.annotations.NullMarked;