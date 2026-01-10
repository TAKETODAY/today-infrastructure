/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated type is a source of configuration properties metadata.
 * <p>
 * This annotation has no effect on the actual binding process, but serves as a hint to
 * the {@code infra-configuration-processor} to generate full metadata for the type.
 * <p>
 * Typically, this annotation is only required for types located in a different module
 * than the {@code @ConfigurationProperties} class that references them. When both types
 * are in the same module, the annotation processor can automatically discover full
 * metadata as long as the source is available.
 * <p>
 * Use this annotation when metadata for types located outside the module is needed:
 * <ol>
 * <li>Nested types annotated by {@code @NestedConfigurationProperty}</li>
 * <li>Base classes that a {@code @ConfigurationProperties}-annotated type extends
 * from</li>
 * </ol>
 * <p>
 * In the example below, {@code ServerProperties} is located in module "A" and
 * {@code Host} in module "B":<pre>{@code
 * @ConfigurationProperties("example.server")
 * class ServerProperties {
 *
 *     @NestedConfigurationProperty
 *     public final Host host = new Host();
 *
 *     // Other properties, getter, setter.
 *
 * }}</pre>
 * <p>
 * Properties from {@code Host} are detected as they are based on the type, but
 * description and default value are not. To fix this, add the
 * {@code infra-configuration-processor} to module "B" if it is not present already
 * and update {@code Host} as follows::<pre>{@code
 * @ConfigurationPropertiesSource
 * class Host {
 *
 *     //  URL to use.
 *     public String url = "https://example.com";
 *
 *     // Other properties, getter, setter.
 *
 * }}</pre>
 * <p>
 * Similarly the metadata of a base class that a
 * {@code @ConfigurationProperties}-annotated type extends from can also be detected.
 * Consider the following example:<pre>{@code
 * @ConfigurationProperties("example.client.github")
 * class GitHubClientProperties extends AbstractClientProperties {
 *
 *     // Additional properties, getter, setter.
 *
 * }}</pre>
 * <p>
 * As with nested types, adding {@code @ConfigurationPropertiesSource} to
 * {@code AbstractClientProperties} and the {@code infra-configuration-processor} to
 * its module ensures full metadata generation.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationPropertiesSource {

}
