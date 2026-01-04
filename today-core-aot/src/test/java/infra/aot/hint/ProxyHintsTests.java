/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.aot.hint;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ProxyHints}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class ProxyHintsTests {

	private final ProxyHints proxyHints = new ProxyHints();


	@Test
	void registerJdkProxyWithSealedInterface() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.proxyHints.registerJdkProxy(SealedInterface.class))
				.withMessageContaining(SealedInterface.class.getName());
	}

	@Test
	void registerJdkProxyWithConcreteClass() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.proxyHints.registerJdkProxy(String.class))
				.withMessageContaining(String.class.getName());
	}

	@Test
	void registerJdkProxyWithInterface() {
		this.proxyHints.registerJdkProxy(Function.class);
		assertThat(this.proxyHints.jdkProxyHints()).singleElement().satisfies(proxiedInterfaces(Function.class));
	}

	@Test
	void registerJdkProxyWithTypeReferences() {
		this.proxyHints.registerJdkProxy(TypeReference.of(Function.class), TypeReference.of("com.example.Advised"));
		assertThat(this.proxyHints.jdkProxyHints()).singleElement()
				.satisfies(proxiedInterfaces(Function.class.getName(), "com.example.Advised"));
	}

	@Test
	void registerJdkProxyWithConsumer() {
		this.proxyHints.registerJdkProxy(infraProxy("com.example.Test"));
		assertThat(this.proxyHints.jdkProxyHints()).singleElement().satisfies(proxiedInterfaces(
				"com.example.Test",
				"infra.aop.SpringProxy",
				"infra.aop.framework.Advised",
				"infra.core.DecoratingProxy"));
	}

	@Test
	void registerJdkProxyTwiceExposesOneHint() {
		this.proxyHints.registerJdkProxy(Function.class);
		this.proxyHints.registerJdkProxy(TypeReference.of(Function.class.getName()));
		assertThat(this.proxyHints.jdkProxyHints()).singleElement().satisfies(proxiedInterfaces(Function.class));
	}


	private static Consumer<JdkProxyHint.Builder> infraProxy(String proxiedInterface) {
		return builder -> builder.proxiedInterfaces(toTypeReferences(
				proxiedInterface,
				"infra.aop.SpringProxy",
				"infra.aop.framework.Advised",
				"infra.core.DecoratingProxy"));
	}

	private static Consumer<JdkProxyHint> proxiedInterfaces(String... proxiedInterfaces) {
		return jdkProxyHint -> assertThat(jdkProxyHint.getProxiedInterfaces())
				.containsExactly(toTypeReferences(proxiedInterfaces));
	}

	private static Consumer<JdkProxyHint> proxiedInterfaces(Class<?>... proxiedInterfaces) {
		return jdkProxyHint -> assertThat(jdkProxyHint.getProxiedInterfaces())
				.containsExactlyElementsOf(TypeReference.listOf(proxiedInterfaces));
	}

	private static TypeReference[] toTypeReferences(String... proxiedInterfaces) {
		return Arrays.stream(proxiedInterfaces).map(TypeReference::of).toArray(TypeReference[]::new);
	}


	sealed interface SealedInterface {
	}

	@SuppressWarnings("unused")
	static final class SealedClass implements SealedInterface {
	}

}
