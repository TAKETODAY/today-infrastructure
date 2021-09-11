/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.util.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
class FutureAdapterTests {

	private FutureAdapter<String, Integer> adapter;

	private Future<Integer> adaptee;


	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		adaptee = mock(Future.class);
		adapter = new FutureAdapter<String, Integer>(adaptee) {
			@Override
			protected String adapt(Integer adapteeResult) throws ExecutionException {
				return adapteeResult.toString();
			}
		};
	}

	@Test
	void cancel() throws Exception {
		given(adaptee.cancel(true)).willReturn(true);
		boolean result = adapter.cancel(true);
		assertThat(result).isTrue();
	}

	@Test
	void isCancelled() {
		given(adaptee.isCancelled()).willReturn(true);
		boolean result = adapter.isCancelled();
		assertThat(result).isTrue();
	}

	@Test
	void isDone() {
		given(adaptee.isDone()).willReturn(true);
		boolean result = adapter.isDone();
		assertThat(result).isTrue();
	}

	@Test
	void get() throws Exception {
		given(adaptee.get()).willReturn(42);
		String result = adapter.get();
		assertThat(result).isEqualTo("42");
	}

	@Test
	void getTimeOut() throws Exception {
		given(adaptee.get(1, TimeUnit.SECONDS)).willReturn(42);
		String result = adapter.get(1, TimeUnit.SECONDS);
		assertThat(result).isEqualTo("42");
	}


}
