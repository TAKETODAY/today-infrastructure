/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.aot.nativex.feature;

import org.graalvm.nativeimage.hosted.Feature;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;

/**
 * GraalVM {@link Feature} that substitutes boolean field values that match a certain pattern
 * with values pre-computed AOT without causing class build-time initialization.
 *
 * <p>It is possible to pass <pre style="code">-Dinfra.native.precompute.log=verbose</pre> as a
 * <pre style="code">native-image</pre> compiler build argument to display detailed logs
 * about pre-computed fields.</p>
 *
 * @author Sebastien Deleuze
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class PreComputeFieldFeature implements Feature {

  private static final boolean verbose =
          "verbose".equalsIgnoreCase(System.getProperty("infra.native.precompute.log"));

  private static final Pattern[] patterns = {
          Pattern.compile(Pattern.quote("cn.taketoday.core.NativeDetector#inNativeImage")),
          Pattern.compile(Pattern.quote("cn.taketoday.bytecode.core.AbstractClassGenerator#inNativeImage")),
          Pattern.compile(Pattern.quote("cn.taketoday.aot.AotDetector#inNativeImage")),
          Pattern.compile(Pattern.quote("cn.taketoday.") + ".*#.*Present"),
          Pattern.compile(Pattern.quote("cn.taketoday.") + ".*#.*PRESENT"),
          Pattern.compile(Pattern.quote("reactor.core") + ".*#.*Available")
  };

  private final ThrowawayClassLoader throwawayClassLoader = new ThrowawayClassLoader(getClass().getClassLoader());

  @Override
  public void beforeAnalysis(BeforeAnalysisAccess access) {
    access.registerSubtypeReachabilityHandler(this::iterateFields, Object.class);
  }

  // This method is invoked for every type that is reachable.
  private void iterateFields(DuringAnalysisAccess access, Class<?> subtype) {
    try {
      for (Field field : subtype.getDeclaredFields()) {
        int modifiers = field.getModifiers();
        if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers) || field.isEnumConstant() ||
                (field.getType() != boolean.class && field.getType() != Boolean.class)) {
          continue;
        }
        String fieldIdentifier = field.getDeclaringClass().getName() + "#" + field.getName();
        for (Pattern pattern : patterns) {
          if (pattern.matcher(fieldIdentifier).matches()) {
            try {
              Object fieldValue = provideFieldValue(field);
              access.registerFieldValueTransformer(field, (receiver, originalValue) -> fieldValue);
              if (verbose) {
                System.out.println(
                        "Field " + fieldIdentifier + " set to " + fieldValue + " at build time");
              }
            }
            catch (Throwable ex) {
              if (verbose) {
                System.out.println("Field " + fieldIdentifier + " will be evaluated at runtime " +
                        "due to this error during build time evaluation: " + ex);
              }
            }
          }
        }
      }
    }
    catch (NoClassDefFoundError ex) {
      // Skip classes that have not all their field types in the classpath
    }
  }

  // This method is invoked when the field value is written to the image heap or the field is constant folded.
  private Object provideFieldValue(Field field)
          throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {

    Class<?> throwawayClass = this.throwawayClassLoader.loadClass(field.getDeclaringClass().getName());
    Field throwawayField = throwawayClass.getDeclaredField(field.getName());
    throwawayField.setAccessible(true);
    return throwawayField.get(null);
  }

}
