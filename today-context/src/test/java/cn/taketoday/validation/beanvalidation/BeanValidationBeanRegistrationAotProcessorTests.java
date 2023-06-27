/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.validation.beanvalidation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.lang.Nullable;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/24 17:27
 */
class BeanValidationBeanRegistrationAotProcessorTests {

  private final BeanValidationBeanRegistrationAotProcessor processor = new BeanValidationBeanRegistrationAotProcessor();

  private final GenerationContext generationContext = new TestGenerationContext();

  @Test
  void shouldSkipNonAnnotatedType() {
    process(EmptyClass.class);
    assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
  }

  @Test
  void shouldProcessMethodParameterLevelConstraint() {
    process(MethodParameterLevelConstraint.class);
    assertThat(RuntimeHintsPredicates.reflection().onType(ExistsValidator.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldProcessConstructorParameterLevelConstraint() {
    process(ConstructorParameterLevelConstraint.class);
    assertThat(RuntimeHintsPredicates.reflection().onType(ExistsValidator.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldProcessPropertyLevelConstraint() {
    process(PropertyLevelConstraint.class);
    assertThat(RuntimeHintsPredicates.reflection().onType(ExistsValidator.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.generationContext.getRuntimeHints());
  }

  private void process(Class<?> beanClass) {
    BeanRegistrationAotContribution contribution = createContribution(beanClass);
    if (contribution != null) {
      contribution.applyTo(this.generationContext, mock());
    }
  }

  @Nullable
  private BeanRegistrationAotContribution createContribution(Class<?> beanClass) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
    return this.processor.processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.getName()));
  }

  private static class EmptyClass { }

  @Constraint(validatedBy = { ExistsValidator.class })
  @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
  @Retention(RUNTIME)
  @Repeatable(Exists.List.class)
  @interface Exists {

    String message() default "Does not exist";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
    @Retention(RUNTIME)
    @Documented
    @interface List {
      Exists[] value();
    }
  }

  static class ExistsValidator implements ConstraintValidator<Exists, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
      return true;
    }
  }

  static class MethodParameterLevelConstraint {

    @SuppressWarnings("unused")
    public String hello(@Exists String name) {
      return "Hello " + name;
    }

  }

  @SuppressWarnings("unused")
  static class ConstructorParameterLevelConstraint {

    private final String name;

    public ConstructorParameterLevelConstraint(@Exists String name) {
      this.name = name;
    }

    public String hello() {
      return "Hello " + this.name;
    }

  }

  @SuppressWarnings("unused")
  static class PropertyLevelConstraint {

    @Exists
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

}