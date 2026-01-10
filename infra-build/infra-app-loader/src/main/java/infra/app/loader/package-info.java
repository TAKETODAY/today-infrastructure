
/**
 * System that allows self-contained JAR/WAR archives to be launched using
 * {@code java -jar}. Archives can include nested packaged dependency JARs (there is no
 * need to create shade style jars) and are executed without unpacking. The only
 * constraint is that nested JARs must be stored in the archive uncompressed.
 *
 * @see infra.app.loader.JarLauncher
 * @see infra.app.loader.WarLauncher
 */
@NullMarked
package infra.app.loader;

import org.jspecify.annotations.NullMarked;