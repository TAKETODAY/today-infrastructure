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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.aot.generate.GeneratedClass;
import infra.aot.generate.GeneratedMethods;
import infra.aot.generate.GenerationContext;
import infra.aot.generate.MethodReference;
import infra.beans.factory.aot.BeanFactoryInitializationCode;
import infra.javapoet.ClassName;

/**
 * Mock {@link BeanFactoryInitializationCode} implementation.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class MockBeanFactoryInitializationCode implements BeanFactoryInitializationCode {

	private final GeneratedClass generatedClass;

	private final List<MethodReference> initializers = new ArrayList<>();

	private final DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();


	public MockBeanFactoryInitializationCode(GenerationContext generationContext) {
		this.generatedClass = generationContext.getGeneratedClasses()
				.addForFeature("TestCode", this.typeBuilder);
	}

	public ClassName getClassName() {
		return this.generatedClass.getName();
	}

	public DeferredTypeBuilder getTypeBuilder() {
		return this.typeBuilder;
	}

	@Override
	public GeneratedMethods getMethods() {
		return this.generatedClass.getMethods();
	}

	@Override
	public void addInitializer(MethodReference methodReference) {
		this.initializers.add(methodReference);
	}

	public List<MethodReference> getInitializers() {
		return Collections.unmodifiableList(this.initializers);
	}

}
