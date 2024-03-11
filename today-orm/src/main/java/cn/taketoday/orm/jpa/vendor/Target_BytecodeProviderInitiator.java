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

package cn.taketoday.orm.jpa.vendor;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import org.hibernate.bytecode.spi.BytecodeProvider;

import java.util.function.Predicate;

import static com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;

/**
 * Hibernate substitution designed to prevent ByteBuddy reachability on native, and to enforce the
 * usage of {@code org.hibernate.bytecode.internal.none.BytecodeProviderImpl} with Hibernate 6.3+.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@TargetClass(className = "org.hibernate.bytecode.internal.BytecodeProviderInitiator",
        onlyWith = Target_BytecodeProviderInitiator.SubstituteOnlyIfPresent.class)
final class Target_BytecodeProviderInitiator {

  @Alias
  public static String BYTECODE_PROVIDER_NAME_NONE;

  @Alias
  @RecomputeFieldValue(kind = Kind.FromAlias)
  public static String BYTECODE_PROVIDER_NAME_DEFAULT = BYTECODE_PROVIDER_NAME_NONE;

  @Substitute
  public static BytecodeProvider buildBytecodeProvider(String providerName) {
    return new org.hibernate.bytecode.internal.none.BytecodeProviderImpl();
  }

  static class SubstituteOnlyIfPresent implements Predicate<String> {

    @Override
    public boolean test(String type) {
      try {
        Class<?> clazz = Class.forName(type, false, getClass().getClassLoader());
        clazz.getDeclaredMethod("buildBytecodeProvider", String.class);
        clazz.getField("BYTECODE_PROVIDER_NAME_NONE");
        clazz.getField("BYTECODE_PROVIDER_NAME_DEFAULT");
        return true;
      }
      catch (ClassNotFoundException | NoClassDefFoundError | NoSuchMethodException | NoSuchFieldException ex) {
        return false;
      }
    }
  }

}