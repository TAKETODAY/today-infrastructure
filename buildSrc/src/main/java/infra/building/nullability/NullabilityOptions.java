/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.building.nullability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import net.ltgt.gradle.errorprone.CheckSeverity;
import net.ltgt.gradle.errorprone.ErrorProneOptions;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.compile.JavaCompile;

/**
 * Nullability configuration options for a {@link JavaCompile} task.
 *
 * @author Andy Wilkinson
 */
public abstract class NullabilityOptions {

	/**
	 * Internal use only.
	 * @param errorProne the ErrorProne options to which the nullability options are
	 * applied
	 */
	@Inject
	public NullabilityOptions(ErrorProneOptions errorProne) {
		Provider<Checking> checkingAsEnum = getChecking()
			.map((string) -> Checking.valueOf(string.toUpperCase(Locale.ROOT)));
		errorProne.getEnabled().set(checkingAsEnum.map((checking) -> checking != Checking.DISABLED));
		errorProne.getDisableAllChecks().set(checkingAsEnum.map((checking) -> checking != Checking.DISABLED));
		errorProne.getCheckOptions().putAll(checkingAsEnum.map(this::checkOptions));
		errorProne.getChecks().putAll(checkingAsEnum.map(this::checks));
	}

	private Map<String, String> checkOptions(Checking checking) {
		if (checking == Checking.DISABLED) {
			return Collections.emptyMap();
		}
		Map<String, String> options = new LinkedHashMap<>();
		options.put("NullAway:OnlyNullMarked", "true");
		List<String> customContractAnnotations = new ArrayList<>();
		customContractAnnotations.add("infra.lang.Contract");
		if (checking == Checking.TESTS) {
			customContractAnnotations.add("org.assertj.core.internal.annotation.Contract");
		}
		options.put("NullAway:CustomContractAnnotations", String.join(",", customContractAnnotations));
		options.put("NullAway:JSpecifyMode", "true");
		if (checking == Checking.TESTS) {
			options.put("NullAway:HandleTestAssertionLibraries", "true");
		}
		return options;
	}

	private Map<String, CheckSeverity> checks(Checking checking) {
		if (checking != Checking.DISABLED) {
			return Map.of("NullAway", CheckSeverity.ERROR);
		}
		return Collections.emptyMap();
	}

	/**
	 * Returns the type of checking to perform.
	 * @return the type of checking
	 */
	public abstract Property<String> getChecking();

	/**
	 * The type of null checking to perform for the {@link JavaCompile} task.
	 */
	enum Checking {

		/**
		 * Main code nullability checking is performed.
		 */
		MAIN,

		/**
		 * Test code nullability checking is performed.
		 */
		TESTS,

		/**
		 * Nullability checking is disabled.
		 */
		DISABLED

	}

}