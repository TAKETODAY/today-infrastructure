/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.beans.testfixture.beans.factory.aot;

import java.util.function.Consumer;

import infra.javapoet.TypeSpec;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * {@link TypeSpec.Builder} {@link Consumer} that can be used to defer the to
 * another consumer that is set at a later point.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class DeferredTypeBuilder implements Consumer<TypeSpec.Builder> {

	@Nullable
	private Consumer<TypeSpec.Builder> type;

	@Override
	public void accept(TypeSpec.Builder type) {
		Assert.notNull(this.type, "No type builder set");
		this.type.accept(type);
	}

	public void set(Consumer<TypeSpec.Builder> type) {
		this.type = type;
	}

}
