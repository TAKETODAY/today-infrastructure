package infra.core;

import org.jspecify.annotations.Nullable;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/4/5 22:20
 */
class JavaVersionHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    for (JavaVersion javaVersion : JavaVersion.values()) {
      hints.reflection().registerType(javaVersion.versionSpecificClass);
    }
  }

}