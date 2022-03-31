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

package cn.taketoday.test.context.junit4.statements;

import org.junit.Test;
import org.junit.runners.model.Statement;
import org.mockito.stubbing.Answer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link FailOnTimeout}.
 *
 * @author Igor Suhorukov
 * @author Sam Brannen
 * @since 4.0
 */
public class FailOnTimeoutTests {

	private final Statement statement = mock(Statement.class);


	@Test
	public void nullNextStatement() throws Throwable {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new FailOnTimeout(null, 1));
	}

	@Test
	public void negativeTimeout() throws Throwable {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new FailOnTimeout(statement, -1));
	}

	@Test
	public void userExceptionPropagates() throws Throwable {
		willThrow(new Boom()).given(statement).evaluate();

		assertThatExceptionOfType(Boom.class).isThrownBy(() ->
				new FailOnTimeout(statement, 1).evaluate());
	}

	@Test
	public void timeoutExceptionThrownIfNoUserException() throws Throwable {
		willAnswer((Answer<Void>) invocation -> {
			TimeUnit.MILLISECONDS.sleep(50);
			return null;
		}).given(statement).evaluate();

		assertThatExceptionOfType(TimeoutException.class).isThrownBy(() ->
		new FailOnTimeout(statement, 1).evaluate());
	}

	@Test
	public void noExceptionThrownIfNoUserExceptionAndTimeoutDoesNotOccur() throws Throwable {
		willAnswer((Answer<Void>) invocation -> null).given(statement).evaluate();
		new FailOnTimeout(statement, 100).evaluate();
	}

	@SuppressWarnings("serial")
	private static class Boom extends RuntimeException {
	}

}
