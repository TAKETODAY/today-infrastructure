/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.annotation.AnnotationConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link EntityScanPackages}.
 *
 * @author Phillip Webb
 */
class EntityScanPackagesTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void getWhenNoneRegisteredShouldReturnNone() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.refresh();
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages).isNotNull();
		assertThat(packages.getPackageNames()).isEmpty();
	}

	@Test
	void getShouldReturnRegisterPackages() {
		this.context = new AnnotationConfigApplicationContext();
		EntityScanPackages.register(this.context, "a", "b");
		EntityScanPackages.register(this.context, "b", "c");
		this.context.refresh();
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a", "b", "c");
	}

	@Test
	void registerFromArrayWhenRegistryIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> EntityScanPackages.register(null))
				.withMessageContaining("Registry is required");

	}

	@Test
	void registerFromArrayWhenPackageNamesIsNullShouldThrowException() {
		this.context = new AnnotationConfigApplicationContext();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> EntityScanPackages.register(this.context, (String[]) null))
				.withMessageContaining("PackageNames is required");
	}

	@Test
	void registerFromCollectionWhenRegistryIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> EntityScanPackages.register(null, Collections.emptyList()))
				.withMessageContaining("Registry is required");
	}

	@Test
	void registerFromCollectionWhenPackageNamesIsNullShouldThrowException() {
		this.context = new AnnotationConfigApplicationContext();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> EntityScanPackages.register(this.context, (Collection<String>) null))
				.withMessageContaining("PackageNames is required");
	}

	@Test
	void entityScanAnnotationWhenHasValueAttributeShouldSetupPackages() {
		this.context = new AnnotationConfigApplicationContext(EntityScanValueConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a");
	}

	@Test
	void entityScanAnnotationWhenHasValueAttributeShouldSetupPackagesAsm() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.registerBeanDefinition("entityScanValueConfig",
				new RootBeanDefinition(EntityScanValueConfig.class.getName()));
		this.context.refresh();
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a");
	}

	@Test
	void entityScanAnnotationWhenHasBasePackagesAttributeShouldSetupPackages() {
		this.context = new AnnotationConfigApplicationContext(EntityScanBasePackagesConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("b");
	}

	@Test
	void entityScanAnnotationWhenHasValueAndBasePackagesAttributeShouldThrow() {
		assertThatExceptionOfType(AnnotationConfigurationException.class)
				.isThrownBy(() -> this.context = new AnnotationConfigApplicationContext(
						EntityScanValueAndBasePackagesConfig.class));
	}

	@Test
	void entityScanAnnotationWhenHasBasePackageClassesAttributeShouldSetupPackages() {
		this.context = new AnnotationConfigApplicationContext(EntityScanBasePackageClassesConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly(getClass().getPackage().getName());
	}

	@Test
	void entityScanAnnotationWhenNoAttributesShouldSetupPackages() {
		this.context = new AnnotationConfigApplicationContext(EntityScanNoAttributesConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly(getClass().getPackage().getName());
	}

	@Test
	void entityScanAnnotationWhenLoadingFromMultipleConfigsShouldCombinePackages() {
		this.context = new AnnotationConfigApplicationContext(EntityScanValueConfig.class,
				EntityScanBasePackagesConfig.class);
		EntityScanPackages packages = EntityScanPackages.get(this.context);
		assertThat(packages.getPackageNames()).containsExactly("a", "b");
	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan("a")
	static class EntityScanValueConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackages = "b")
	static class EntityScanBasePackagesConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(value = "a", basePackages = "b")
	static class EntityScanValueAndBasePackagesConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackageClasses = EntityScanPackagesTests.class)
	static class EntityScanBasePackageClassesConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan
	static class EntityScanNoAttributesConfig {

	}

}
