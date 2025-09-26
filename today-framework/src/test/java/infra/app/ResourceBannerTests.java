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

package infra.app;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.ansi.AnsiOutput;
import infra.core.env.AbstractPropertyResolver;
import infra.core.env.Environment;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertyResolver;
import infra.core.io.ByteArrayResource;
import infra.core.io.Resource;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/8/20 14:43
 */
class ResourceBannerTests {

  @AfterEach
  void reset() {
    AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
  }

  @Test
  void renderVersions() {
    Resource resource = new ByteArrayResource(
            "banner ${a} ${infra.version} ${app.version}".getBytes());
    String banner = printBanner(resource, "10.2", "2.0", null);
    assertThat(banner).startsWith("banner 1 10.2 2.0");
  }

  @Test
  void renderWithoutVersions() {
    Resource resource = new ByteArrayResource(
            "banner ${a} ${infra.version} ${app.version}".getBytes());
    String banner = printBanner(resource, null, null, null);
    assertThat(banner).startsWith("banner 1  ");
  }

  @Test
  void renderFormattedVersions() {
    Resource resource = new ByteArrayResource(
            "banner ${a}${infra.formatted-version}${app.formatted-version}".getBytes());
    String banner = printBanner(resource, "10.2", "2.0", null);
    assertThat(banner).startsWith("banner 1 (v10.2) (v2.0)");
  }

  @Test
  void renderWithoutFormattedVersions() {
    Resource resource = new ByteArrayResource(
            "banner ${a}${infra.formatted-version}${app.formatted-version}".getBytes());
    String banner = printBanner(resource, null, null, null);
    assertThat(banner).startsWith("banner 1");
  }

  @Test
  void renderWithColors() {
    Resource resource = new ByteArrayResource("${Ansi.RED}This is red.${Ansi.NORMAL}".getBytes());
    AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
    String banner = printBanner(resource, null, null, null);
    assertThat(banner).startsWith("\u001B[31mThis is red.\u001B[0m");
  }

  @Test
  void renderWithColorsButDisabled() {
    Resource resource = new ByteArrayResource("${Ansi.RED}This is red.${Ansi.NORMAL}".getBytes());
    AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
    String banner = printBanner(resource, null, null, null);
    assertThat(banner).startsWith("This is red.");
  }

  @Test
  void renderWith256Colors() {
    Resource resource = new ByteArrayResource("${AnsiColor.208}This is orange.${Ansi.NORMAL}".getBytes());
    AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
    String banner = printBanner(resource, null, null, null);
    assertThat(banner).startsWith("\033[38;5;208mThis is orange.\u001B[0m");
  }

  @Test
  void renderWith256ColorsButDisabled() {
    Resource resource = new ByteArrayResource("${AnsiColor.208}This is orange.${Ansi.NORMAL}".getBytes());
    AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
    String banner = printBanner(resource, null, null, null);
    assertThat(banner).startsWith("This is orange.");
  }

  @Test
  void renderWithTitle() {
    Resource resource = new ByteArrayResource("banner ${app.title} ${a}".getBytes());
    String banner = printBanner(resource, null, null, "title");
    assertThat(banner).startsWith("banner title 1");
  }

  @Test
  void renderWithoutTitle() {
    Resource resource = new ByteArrayResource("banner ${app.title} ${a}".getBytes());
    String banner = printBanner(resource, null, null, null);
    assertThat(banner).startsWith("banner  1");
  }

  @Test
  void renderWithDefaultValues() {
    Resource resource = new ByteArrayResource(
            "banner ${a:default-a} ${b:default-b} ${infra.version:default-infra-version} ${app.version:default-app-version}"
                    .getBytes());
    String banner = printBanner(resource, "10.2", "1.0", null);
    assertThat(banner).startsWith("banner 1 default-b 10.2 1.0");
  }

  @Test
  void renderWithMutation() {
    Resource resource = new ByteArrayResource("banner ${foo}".getBytes());
    String banner = printBanner(new MutatingResourceBanner(resource, "1", null), "2");
    assertThat(banner).startsWith("banner bar");
  }

  private String printBanner(Resource resource, String infraVersion, String applicationVersion, String applicationTitle) {
    return printBanner(new MockResourceBanner(resource, infraVersion, applicationTitle), applicationVersion);
  }

  private String printBanner(ResourceBanner banner, @Nullable String applicationVersion) {
    MockEnvironment environment = new MockEnvironment();
    if (applicationVersion != null) {
      environment.setProperty("app.version", applicationVersion);
    }
    Map<String, Object> source = Collections.singletonMap("a", "1");
    environment.getPropertySources().addLast(new MapPropertySource("map", source));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    banner.printBanner(environment, getClass(), new PrintStream(out));
    return out.toString();
  }

  static class MockResourceBanner extends ResourceBanner {

    private final String infraVersion;

    private final String applicationTitle;

    MockResourceBanner(Resource resource, String infraVersion, String applicationTitle) {
      super(resource);
      this.infraVersion = infraVersion;
      this.applicationTitle = applicationTitle;
    }

    @Override
    protected String getInfraVersion() {
      return this.infraVersion;
    }

    @Override
    protected String getApplicationTitle(@Nullable Class<?> sourceClass) {
      return this.applicationTitle;
    }

  }

  static class MutatingResourceBanner extends MockResourceBanner {

    MutatingResourceBanner(Resource resource, String infraVersion, String applicationTitle) {
      super(resource, infraVersion, applicationTitle);
    }

    @Override
    protected void addPropertyResolvers(List<PropertyResolver> resolvers, Environment environment, @Nullable Class<?> sourceClass) {
      super.addPropertyResolvers(resolvers, environment, sourceClass);
      PropertyResolver resolver = new AbstractPropertyResolver() {

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getProperty(String key, Class<T> targetType) {
          return String.class.equals(targetType) ? (T) getPropertyAsRawString(key) : null;
        }

        @Override
        protected String getPropertyAsRawString(String key) {
          return ("foo".equals(key)) ? "bar" : null;
        }

      };
      resolvers.add(resolver);
    }

  }

}