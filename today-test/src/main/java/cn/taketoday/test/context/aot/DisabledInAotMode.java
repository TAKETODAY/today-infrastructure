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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.aot;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @DisabledInAotMode} signals that an annotated test class is <em>disabled</em>
 * in Infra AOT (ahead-of-time) mode, which means that the {@code ApplicationContext}
 * for the test class will not be processed for AOT optimizations at build time.
 *
 * <p>If a test class is annotated with {@code @DisabledInAotMode}, all other test
 * classes which specify configuration to load the same {@code ApplicationContext}
 * must also be annotated with {@code @DisabledInAotMode}. Failure to annotate
 * all such test classes will result in an exception, either at build time or
 * run time.
 *
 * <p>When used with JUnit Jupiter based tests, {@code @DisabledInAotMode} also
 * signals that the annotated test class or test method is <em>disabled</em> when
 * running the test suite in Infra AOT mode. When applied at the class level,
 * all test methods within that class will be disabled. In this sense,
 * {@code @DisabledInAotMode} has semantics similar to those of JUnit Jupiter's
 * {@link org.junit.jupiter.api.condition.DisabledInNativeImage @DisabledInNativeImage}
 * annotation.
 *
 * <p>This annotation may be used as a meta-annotation in order to create a
 * custom <em>composed annotation</em> that inherits the semantics of this
 * annotation.
 *
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.aot.AotDetector#useGeneratedArtifacts() AotDetector.useGeneratedArtifacts()
 * @see org.junit.jupiter.api.condition.EnabledInNativeImage @EnabledInNativeImage
 * @see org.junit.jupiter.api.condition.DisabledInNativeImage @DisabledInNativeImage
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ExtendWith(DisabledInAotModeCondition.class)
public @interface DisabledInAotMode {

}
