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

package cn.taketoday.beans.factory.aot;

import java.util.function.UnaryOperator;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * AOT contribution from a {@link BeanRegistrationAotProcessor} used to register
 * a single bean definition.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanRegistrationAotProcessor
 * @since 4.0
 */
@FunctionalInterface
public interface BeanRegistrationAotContribution {

  /**
   * Customize the {@link BeanRegistrationCodeFragments} that will be used to
   * generate the bean registration code. Custom code fragments can be used if
   * default code generation isn't suitable.
   *
   * @param generationContext the generation context
   * @param codeFragments the existing code fragments
   * @return the code fragments to use, may be the original instance or a
   * wrapper
   */
  default BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(
          GenerationContext generationContext, BeanRegistrationCodeFragments codeFragments) {

    return codeFragments;
  }

  /**
   * Apply this contribution to the given {@link BeanRegistrationCode}.
   *
   * @param generationContext the generation context
   * @param beanRegistrationCode the generated registration
   */
  void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode);

  /**
   * Create a {@link BeanRegistrationAotContribution} that customizes
   * the {@link BeanRegistrationCodeFragments}. Typically used in
   * conjunction with an extension of {@link BeanRegistrationCodeFragmentsDecorator}
   * that overrides a specific callback.
   *
   * @param defaultCodeFragments the default code fragments
   * @return a new {@link BeanRegistrationAotContribution} instance
   * @see BeanRegistrationCodeFragmentsDecorator
   */
  static BeanRegistrationAotContribution withCustomCodeFragments(
          UnaryOperator<BeanRegistrationCodeFragments> defaultCodeFragments) {

    Assert.notNull(defaultCodeFragments, "'defaultCodeFragments' must not be null");

    return new BeanRegistrationAotContribution() {
      @Override
      public BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(
              GenerationContext generationContext, BeanRegistrationCodeFragments codeFragments) {
        return defaultCodeFragments.apply(codeFragments);
      }

      @Override
      public void applyTo(GenerationContext generationContext,
              BeanRegistrationCode beanRegistrationCode) {
      }
    };
  }

  /**
   * Create a contribution that applies the contribution of the first contribution
   * followed by the second contribution. Any contribution can be {@code null} to be
   * ignored and the concatenated contribution is {@code null} if both inputs are
   * {@code null}.
   *
   * @param a the first contribution
   * @param b the second contribution
   * @return the concatenation of the two contributions, or {@code null} if
   * they are both {@code null}.
   */
  @Nullable
  static BeanRegistrationAotContribution concat(@Nullable BeanRegistrationAotContribution a,
          @Nullable BeanRegistrationAotContribution b) {

    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return (generationContext, beanRegistrationCode) -> {
      a.applyTo(generationContext, beanRegistrationCode);
      b.applyTo(generationContext, beanRegistrationCode);
    };
  }

}
