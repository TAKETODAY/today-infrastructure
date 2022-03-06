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

package cn.taketoday.beans.factory.parsing;

import cn.taketoday.lang.Nullable;

import java.util.ArrayDeque;

/**
 * Simple {@link ArrayDeque}-based structure for tracking the logical position during
 * a parsing process. {@link Entry entries} are added to the ArrayDeque at each point
 * during the parse phase in a reader-specific manner.
 *
 * <p>Calling {@link #toString()} will render a tree-style view of the current logical
 * position in the parse phase. This representation is intended for use in error messages.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public final class ParseState {

	/**
	 * Internal {@link ArrayDeque} storage.
	 */
	private final ArrayDeque<Entry> state;


	/**
	 * Create a new {@code ParseState} with an empty {@link ArrayDeque}.
	 */
	public ParseState() {
		this.state = new ArrayDeque<>();
	}

	/**
	 * Create a new {@code ParseState} whose {@link ArrayDeque} is a clone
	 * of the state in the passed-in {@code ParseState}.
	 */
	private ParseState(ParseState other) {
		this.state = other.state.clone();
	}


	/**
	 * Add a new {@link Entry} to the {@link ArrayDeque}.
	 */
	public void push(Entry entry) {
		this.state.push(entry);
	}

	/**
	 * Remove an {@link Entry} from the {@link ArrayDeque}.
	 */
	public void pop() {
		this.state.pop();
	}

	/**
	 * Return the {@link Entry} currently at the top of the {@link ArrayDeque} or
	 * {@code null} if the {@link ArrayDeque} is empty.
	 */
	@Nullable
	public Entry peek() {
		return this.state.peek();
	}

	/**
	 * Create a new instance of {@link ParseState} which is an independent snapshot
	 * of this instance.
	 */
	public ParseState snapshot() {
		return new ParseState(this);
	}


	/**
	 * Returns a tree-style representation of the current {@code ParseState}.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(64);
		int i = 0;
		for (Entry entry : this.state) {
			if (i > 0) {
				sb.append('\n');
				for (int j = 0; j < i; j++) {
					sb.append('\t');
				}
				sb.append("-> ");
			}
			sb.append(entry);
			i++;
		}
		return sb.toString();
	}


	/**
	 * Marker interface for entries into the {@link ParseState}.
	 */
	public interface Entry {
	}

}
