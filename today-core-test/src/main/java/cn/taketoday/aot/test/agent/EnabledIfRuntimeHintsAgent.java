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

package cn.taketoday.aot.test.agent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIf;

import cn.taketoday.aot.agent.RuntimeHintsAgent;

/**
 * {@code @EnabledIfRuntimeHintsAgent} signals that the annotated test class or test method
 * is only enabled if the {@link RuntimeHintsAgent} is loaded on the current JVM.
 * <p>This is meta-annotated with {@code @Tag("RuntimeHintsTests")} so that test suites
 * can choose to target or ignore those tests.
 *
 * <pre class="code">
 * &#064;EnabledIfRuntimeHintsAgent
 * class MyTestCases {
 *
 *     &#064;Test
 *     void hintsForMethodsReflectionShouldMatch() {
 *         RuntimeHints hints = new RuntimeHints();
 *         hints.reflection().registerType(String.class,
 *             hint -> hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
 *
 *         RuntimeHintsInvocations invocations = RuntimeHintsRecorder.record(() -> {
 *             Method[] methods = String.class.getMethods();
 *         });
 *         assertThat(invocations).match(hints);
 *     }
 *
 * }
 * </pre>
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnabledIf(value = "cn.taketoday.aot.agent.RuntimeHintsAgent#isLoaded",
           disabledReason = "RuntimeHintsAgent is not loaded on the current JVM")
@Tag("RuntimeHintsTests")
public @interface EnabledIfRuntimeHintsAgent {
}
