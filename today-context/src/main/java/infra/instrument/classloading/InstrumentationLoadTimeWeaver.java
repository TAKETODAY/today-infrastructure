/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.instrument.classloading;

import org.jspecify.annotations.Nullable;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;

import infra.instrument.InstrumentationSavingAgent;
import infra.lang.Assert;
import infra.util.ClassUtils;

/**
 * {@link LoadTimeWeaver} relying on VM {@link Instrumentation}.
 * *
 * <p><code>-javaagent:path/to/today-instrument-{version}.jar</code>
 *
 * <p>In Eclipse, for example, add something similar to the following to the
 * JVM arguments for the Eclipse "Run configuration":
 *
 * <p><code>-javaagent:${project_loc}/lib/today-instrument-{version}.jar</code>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see InstrumentationSavingAgent
 * @since 4.0
 */
public class InstrumentationLoadTimeWeaver implements LoadTimeWeaver {

  private static final boolean AGENT_CLASS_PRESENT = ClassUtils.isPresent(
          "infra.instrument.InstrumentationSavingAgent",
          InstrumentationLoadTimeWeaver.class.getClassLoader());

  @Nullable
  private final ClassLoader classLoader;

  @Nullable
  private final Instrumentation instrumentation;

  final ArrayList<ClassFileTransformer> transformers = new ArrayList<>(4);

  /**
   * Create a new InstrumentationLoadTimeWeaver for the default ClassLoader.
   */
  public InstrumentationLoadTimeWeaver() {
    this(ClassUtils.getDefaultClassLoader());
  }

  /**
   * Create a new InstrumentationLoadTimeWeaver for the given ClassLoader.
   *
   * @param classLoader the ClassLoader that registered transformers are supposed to apply to
   */
  public InstrumentationLoadTimeWeaver(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
    this.instrumentation = getInstrumentation();
  }

  @Override
  public void addTransformer(ClassFileTransformer transformer) {
    Assert.notNull(transformer, "Transformer is required");
    var actualTransformer = new FilteringClassFileTransformer(transformer, this.classLoader);
    synchronized(this.transformers) {
      Assert.state(this.instrumentation != null,
              "Must start with Java agent to use InstrumentationLoadTimeWeaver.");
      this.instrumentation.addTransformer(actualTransformer);
      this.transformers.add(actualTransformer);
    }
  }

  /**
   * We have the ability to weave the current class loader when starting the
   * JVM in this way, so the instrumentable class loader will always be the
   * current loader.
   */
  @Override
  public ClassLoader getInstrumentableClassLoader() {
    Assert.state(this.classLoader != null, "No ClassLoader available");
    return this.classLoader;
  }

  /**
   * This implementation always returns a {@link SimpleThrowawayClassLoader}.
   */
  @Override
  public ClassLoader getThrowawayClassLoader() {
    return new SimpleThrowawayClassLoader(getInstrumentableClassLoader());
  }

  /**
   * Remove all registered transformers, in inverse order of registration.
   */
  public void removeTransformers() {
    synchronized(this.transformers) {
      if (this.instrumentation != null && !this.transformers.isEmpty()) {
        for (int i = this.transformers.size() - 1; i >= 0; i--) {
          this.instrumentation.removeTransformer(this.transformers.get(i));
        }
        this.transformers.clear();
      }
    }
  }

  /**
   * Check whether an Instrumentation instance is available for the current VM.
   *
   * @see #getInstrumentation()
   */
  public static boolean isInstrumentationAvailable() {
    return (getInstrumentation() != null);
  }

  /**
   * Obtain the Instrumentation instance for the current VM, if available.
   *
   * @return the Instrumentation instance, or {@code null} if none found
   * @see #isInstrumentationAvailable()
   */
  @Nullable
  private static Instrumentation getInstrumentation() {
    if (AGENT_CLASS_PRESENT) {
      return InstrumentationAccessor.getInstrumentation();
    }
    else {
      return null;
    }
  }

  /**
   * Inner class to avoid InstrumentationSavingAgent dependency.
   */
  private static final class InstrumentationAccessor {

    public static Instrumentation getInstrumentation() {
      return InstrumentationSavingAgent.getInstrumentation();
    }
  }

  /**
   * Decorator that only applies the given target transformer to a specific ClassLoader.
   */
  private record FilteringClassFileTransformer(
          ClassFileTransformer targetTransformer, @Nullable ClassLoader targetClassLoader) implements ClassFileTransformer {

    @Override
    public byte @Nullable [] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

      if (this.targetClassLoader != loader) {
        return null;
      }
      return this.targetTransformer.transform(
              loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    }

    @Override
    public String toString() {
      return "FilteringClassFileTransformer for: " + this.targetTransformer;
    }
  }

}
