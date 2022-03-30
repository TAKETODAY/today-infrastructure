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

package cn.taketoday.test.context.junit.jupiter.event;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * All Spring application events fired during the test execution.
 *
 * <p>Copied from the Moduliths project.
 *
 * @author Oliver Drotbohm
 * @since 5.3.3
 */
public interface PublishedEvents {

	/**
	 * Creates a new {@link PublishedEvents} instance for the given events.
	 *
	 * @param events must not be {@literal null}
	 * @return will never be {@literal null}
	 */
	public static PublishedEvents of(Object... events) {
		return of(Arrays.asList(events));
	}

	/**
	 * Returns all application events of the given type that were fired during the test execution.
	 *
	 * @param <T> the event type
	 * @param type must not be {@literal null}
	 */
	<T> TypedPublishedEvents<T> ofType(Class<T> type);

	/**
	 * All application events of a given type that were fired during a test execution.
	 *
	 * @param <T> the event type
	 */
	interface TypedPublishedEvents<T> extends Iterable<T> {

		/**
		 * Further constrain the event type for downstream assertions.
		 *
		 * @param subType the sub type
		 * @return will never be {@literal null}
		 */
		<S extends T> TypedPublishedEvents<S> ofSubType(Class<S> subType);

		/**
		 * Returns all {@link TypedPublishedEvents} that match the given predicate.
		 *
		 * @param predicate must not be {@literal null}
		 * @return will never be {@literal null}
		 */
		TypedPublishedEvents<T> matching(Predicate<? super T> predicate);

		/**
		 * Returns all {@link TypedPublishedEvents} that match the given predicate
		 * after applying the given mapping step.
		 *
		 * @param <S> the intermediate type to apply the {@link Predicate} on
		 * @param mapper the mapping step to extract a part of the original event
		 * subject to test for the {@link Predicate}
		 * @param predicate the {@link Predicate} to apply on the value extracted
		 * @return will never be {@literal null}
		 */
		<S> TypedPublishedEvents<T> matchingMapped(Function<T, S> mapper, Predicate<? super S> predicate);
	}

}
