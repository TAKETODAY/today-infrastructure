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

package cn.taketoday.test.util;

import org.junit.jupiter.api.Test;

import cn.taketoday.test.util.ExceptionCollector;
import cn.taketoday.test.util.ExceptionCollector.Executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for {@link ExceptionCollector}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class ExceptionCollectorTests {

	private static final char EOL = '\n';

	private final ExceptionCollector collector = new ExceptionCollector();


	@Test
	void noExceptions() {
		this.collector.execute(() -> {});

		assertThat(this.collector.getExceptions()).isEmpty();
		assertThatNoException().isThrownBy(this.collector::assertEmpty);
	}

	@Test
	void oneError() {
		this.collector.execute(error());

		assertOneFailure(Error.class, "error");
	}

	@Test
	void oneAssertionError() {
		this.collector.execute(assertionError());

		assertOneFailure(AssertionError.class, "assertion");
	}

	@Test
	void oneCheckedException() {
		this.collector.execute(checkedException());

		assertOneFailure(Exception.class, "checked");
	}

	@Test
	void oneUncheckedException() {
		this.collector.execute(uncheckedException());

		assertOneFailure(RuntimeException.class, "unchecked");
	}

	@Test
	void oneThrowable() {
		this.collector.execute(throwable());

		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(this.collector::assertEmpty)
			.withMessage("throwable")
			.withCauseExactlyInstanceOf(Throwable.class)
			.satisfies(error -> assertThat(error.getCause()).hasMessage("throwable"))
			.satisfies(error -> assertThat(error).hasNoSuppressedExceptions());
	}

	private void assertOneFailure(Class<? extends Throwable> expectedType, String failureMessage) {
		assertThatExceptionOfType(expectedType)
			.isThrownBy(this.collector::assertEmpty)
			.satisfies(exception ->
				assertThat(exception)
					.isExactlyInstanceOf(expectedType)
					.hasNoSuppressedExceptions()
					.hasNoCause()
					.hasMessage(failureMessage));
	}

	@Test
	void multipleFailures() {
		this.collector.execute(assertionError());
		this.collector.execute(checkedException());
		this.collector.execute(uncheckedException());
		this.collector.execute(error());
		this.collector.execute(throwable());

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(this.collector::assertEmpty)
				.withMessage("Multiple Exceptions (5):" + EOL + //
					"assertion" + EOL + //
					"checked" + EOL + //
					"unchecked" + EOL + //
					"error" + EOL + //
					"throwable"//
				)
				.satisfies(exception ->
					assertThat(exception.getSuppressed()).extracting(Object::getClass).map(Class::getSimpleName)
						.containsExactly("AssertionError", "Exception", "RuntimeException", "Error", "Throwable"));
	}

	private Executable throwable() {
		return () -> {
			throw new Throwable("throwable");
		};
	}

	private Executable error() {
		return () -> {
			throw new Error("error");
		};
	}

	private Executable assertionError() {
		return () -> {
			throw new AssertionError("assertion");
		};
	}

	private Executable checkedException() {
		return () -> {
			throw new Exception("checked");
		};
	}

	private Executable uncheckedException() {
		return () -> {
			throw new RuntimeException("unchecked");
		};
	}

}
