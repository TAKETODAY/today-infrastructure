/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.condition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import cn.taketoday.framework.autoconfigure.condition.ConditionalOnJava.Range;
import cn.taketoday.framework.system.JavaVersion;
import cn.taketoday.framework.test.context.FilteredClassLoader;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.util.ReflectionUtils;

import java.io.Console;
import java.lang.reflect.Method;

import cn.taketoday.context.condition.ConditionOutcome;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnJava @ConditionalOnJava}.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 */
class ConditionalOnJavaTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	private final OnJavaCondition condition = new OnJavaCondition();

	@Test
	@EnabledOnJre(JRE.JAVA_17)
	void doesNotMatchIfBetterVersionIsRequired() {
		this.contextRunner.withUserConfiguration(Java18Required.class)
				.run((context) -> assertThat(context).doesNotHaveBean(String.class));
	}

	@Test
	@EnabledOnJre(JRE.JAVA_18)
	void doesNotMatchIfLowerIsRequired() {
		this.contextRunner.withUserConfiguration(OlderThan18Required.class)
				.run((context) -> assertThat(context).doesNotHaveBean(String.class));
	}

	@Test
	void matchesIfVersionIsInRange() {
		this.contextRunner.withUserConfiguration(Java17Required.class)
				.run((context) -> assertThat(context).hasSingleBean(String.class));
	}

	@Test
	void boundsTests() {
		testBounds(Range.EQUAL_OR_NEWER, JavaVersion.EIGHTEEN, JavaVersion.SEVENTEEN, true);
		testBounds(Range.EQUAL_OR_NEWER, JavaVersion.SEVENTEEN, JavaVersion.SEVENTEEN, true);
		testBounds(Range.EQUAL_OR_NEWER, JavaVersion.SEVENTEEN, JavaVersion.EIGHTEEN, false);
		testBounds(Range.OLDER_THAN, JavaVersion.EIGHTEEN, JavaVersion.SEVENTEEN, false);
		testBounds(Range.OLDER_THAN, JavaVersion.SEVENTEEN, JavaVersion.SEVENTEEN, false);
		testBounds(Range.OLDER_THAN, JavaVersion.SEVENTEEN, JavaVersion.EIGHTEEN, true);
	}

	@Test
	void equalOrNewerMessage() {
		ConditionOutcome outcome = this.condition.getMatchOutcome(Range.EQUAL_OR_NEWER, JavaVersion.EIGHTEEN,
				JavaVersion.SEVENTEEN);
		assertThat(outcome.getMessage()).isEqualTo("@ConditionalOnJava (17 or newer) found 18");
	}

	@Test
	void olderThanMessage() {
		ConditionOutcome outcome = this.condition.getMatchOutcome(Range.OLDER_THAN, JavaVersion.EIGHTEEN,
				JavaVersion.SEVENTEEN);
		assertThat(outcome.getMessage()).isEqualTo("@ConditionalOnJava (older than 17) found 18");
	}

	@Test
	@EnabledOnJre(JRE.JAVA_17)
	void java17IsDetected() throws Exception {
		assertThat(getJavaVersion()).isEqualTo("17");
	}

	@Test
	@EnabledOnJre(JRE.JAVA_17)
	void java17IsTheFallback() throws Exception {
		assertThat(getJavaVersion(Console.class)).isEqualTo("17");
	}

	private String getJavaVersion(Class<?>... hiddenClasses) throws Exception {
		FilteredClassLoader classLoader = new FilteredClassLoader(hiddenClasses);
		Class<?> javaVersionClass = Class.forName(JavaVersion.class.getName(), false, classLoader);
		Method getJavaVersionMethod = ReflectionUtils.findMethod(javaVersionClass, "getJavaVersion");
		Object javaVersion = ReflectionUtils.invokeMethod(getJavaVersionMethod, null);
		classLoader.close();
		return javaVersion.toString();
	}

	private void testBounds(Range range, JavaVersion runningVersion, JavaVersion version, boolean expected) {
		ConditionOutcome outcome = this.condition.getMatchOutcome(range, runningVersion, version);
		assertThat(outcome.isMatch()).as(outcome.getMessage()).isEqualTo(expected);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnJava(JavaVersion.SEVENTEEN)
	static class Java17Required {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnJava(range = Range.OLDER_THAN, value = JavaVersion.EIGHTEEN)
	static class OlderThan18Required {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnJava(JavaVersion.EIGHTEEN)
	static class Java18Required {

		@Bean
		String foo() {
			return "foo";
		}

	}

}
