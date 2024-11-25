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

package infra.test.context.junit4.annotation.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.test.context.ActiveProfiles;

/**
 * Custom configuration annotation that is itself meta-annotated with {@link
 * ConfigClassesAndProfilesWithCustomDefaultsMetaConfig} and {@link ActiveProfiles}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ConfigClassesAndProfilesWithCustomDefaultsMetaConfig
// Override default "dev" profile from
// @ConfigClassesAndProfilesWithCustomDefaultsMetaConfig:
@ActiveProfiles("prod")
public @interface MetaMetaConfig {

}