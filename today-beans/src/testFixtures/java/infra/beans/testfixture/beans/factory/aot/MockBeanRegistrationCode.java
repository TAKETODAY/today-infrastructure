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
import infra.beans.factory.aot.BeanRegistrationCode;
import infra.javapoet.ClassName;

/**
 * Mock {@link BeanRegistrationCode} implementation.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class MockBeanRegistrationCode implements BeanRegistrationCode {

	private final GeneratedClass generatedClass;

	private final List<MethodReference> instancePostProcessors = new ArrayList<>();

	private final DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();


	public MockBeanRegistrationCode(GenerationContext generationContext) {
		this.generatedClass = generationContext.getGeneratedClasses().addForFeature("TestCode", this.typeBuilder);
	}


	public DeferredTypeBuilder getTypeBuilder() {
		return this.typeBuilder;
	}

	@Override
	public ClassName getClassName() {
		return this.generatedClass.getName();
	}

	@Override
	public GeneratedMethods getMethods() {
		return this.generatedClass.getMethods();
	}

	@Override
	public void addInstancePostProcessor(MethodReference methodReference) {
		this.instancePostProcessors.add(methodReference);
	}

	public List<MethodReference> getInstancePostProcessors() {
		return Collections.unmodifiableList(this.instancePostProcessors);
	}

}
