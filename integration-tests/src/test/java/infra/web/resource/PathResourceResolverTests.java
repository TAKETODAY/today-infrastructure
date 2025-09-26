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

package infra.web.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.core.io.UrlResource;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PathResourceResolver}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class PathResourceResolverTests {

  private final PathResourceResolver resolver = new PathResourceResolver();

  @Test
  public void resolveFromClasspath() throws IOException {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    String requestPath = "bar.css";
    Resource actual = this.resolver.resolveResource(null, requestPath, Collections.singletonList(location), null);

    assertThat(actual).isEqualTo(location.createRelative(requestPath));
  }

  @Test
  public void resolveFromClasspathRoot() {
    Resource location = new ClassPathResource("/");
    String requestPath = "infra/web/resource/test/bar.css";
    Resource actual = this.resolver.resolveResource(null, requestPath, Collections.singletonList(location), null);

    assertThat(actual).isNotNull();
  }

  @Test
  public void checkResource() throws IOException {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    testCheckResource(location, "../testsecret/secret.txt");
    testCheckResource(location, "test/../../testsecret/secret.txt");

    location = new UrlResource(getClass().getResource("test/"));
    String secretPath = new UrlResource(getClass().getResource("testsecret/secret.txt")).getURL().getPath();
    testCheckResource(location, "file:" + secretPath);
    testCheckResource(location, "/file:" + secretPath);
    testCheckResource(location, "/" + secretPath);
    testCheckResource(location, "////../.." + secretPath);
    testCheckResource(location, "/%2E%2E/testsecret/secret.txt");
    testCheckResource(location, "/%2e%2e/testsecret/secret.txt");
    testCheckResource(location, " " + secretPath);
    testCheckResource(location, "/  " + secretPath);
    testCheckResource(location, "url:" + secretPath);
  }

  private void testCheckResource(Resource location, String requestPath) throws IOException {
    List<Resource> locations = Collections.singletonList(location);
    Resource actual = this.resolver.resolveResource(null, requestPath, locations, null);
    assertThat(actual).isNull();
  }

  @Test // gh-23463
  public void ignoreInvalidEscapeSequence() throws IOException {
    UrlResource location = new UrlResource(getClass().getResource("./test/"));

    Resource resource = new UrlResource(location.getURL() + "test%file.txt");
    assertThat(this.resolver.checkResource(resource, location)).isTrue();

    resource = location.createRelative("test%file.txt");
    assertThat(this.resolver.checkResource(resource, location)).isTrue();
  }

  @Test
  public void checkResourceWithAllowedLocations() {
    this.resolver.setAllowedLocations(
            new ClassPathResource("test/", PathResourceResolver.class),
            new ClassPathResource("testalternatepath/", PathResourceResolver.class)
    );

    Resource location = getResource("main.css");
    List<Resource> locations = Collections.singletonList(location);
    String actual = this.resolver.resolveUrlPath("../testalternatepath/bar.css", locations, null);
    assertThat(actual).isEqualTo("../testalternatepath/bar.css");
  }

  @Test
  public void checkRelativeLocation() throws Exception {
    String location = new UrlResource(getClass().getResource("test/")).getURL().toExternalForm();
    location = location.replace("/test/infra", "/test/infra/../infra");

    Resource actual = this.resolver.resolveResource(
            null, "main.css", Collections.singletonList(new UrlResource(location)), null);

    assertThat(actual).isNotNull();
  }

  @Test
  public void checkFileLocation() throws Exception {
    Resource resource = getResource("main.css");
    assertThat(this.resolver.checkResource(resource, resource)).isTrue();
  }

  @Test
  public void resolvePathRootResource() {
    Resource webjarsLocation = new ClassPathResource("/META-INF/resources/webjars/", PathResourceResolver.class);
    String path = this.resolver.resolveUrlPathInternal("", Collections.singletonList(webjarsLocation), null);

    assertThat(path).isNull();
  }

  @Test
  public void relativePathEncodedForUrlResource() throws Exception {
    TestUrlResource location = new TestUrlResource("file:///tmp");
    List<TestUrlResource> locations = Collections.singletonList(location);

    // ISO-8859-1
    resolver.setUrlDecode(true);
    this.resolver.setLocationCharsets(Collections.singletonMap(location, StandardCharsets.ISO_8859_1));
    this.resolver.resolveResource(getContext(), "/Ä ;ä.txt", locations, null);
    assertThat(location.getSavedRelativePath()).isEqualTo("%C4%20%3B%E4.txt");

    // UTF-8
    this.resolver.setLocationCharsets(Collections.singletonMap(location, StandardCharsets.UTF_8));
    this.resolver.resolveResource(getContext(), "/Ä ;ä.txt", locations, null);

    assertThat(location.getSavedRelativePath()).isEqualTo("%C3%84%20%3B%C3%A4.txt");

    // UTF-8 by default
    this.resolver.setLocationCharsets(Collections.emptyMap());
    MockRequestContext request = getContext();
    this.resolver.resolveResource(request, "/Ä ;ä.txt", locations, null);

    assertThat(location.getSavedRelativePath()).isEqualTo("%C3%84%20%3B%C3%A4.txt");
  }

  private MockRequestContext getContext() {
    return new MockRequestContext(null, new HttpMockRequestImpl(), null);
  }

  private Resource getResource(String filePath) {
    return new ClassPathResource("test/" + filePath, getClass());
  }

  private static class TestUrlResource extends UrlResource {

    private String relativePath;

    public TestUrlResource(String path) throws MalformedURLException {
      super(path);
    }

    public String getSavedRelativePath() {
      return this.relativePath;
    }

    @Override
    public UrlResource createRelative(String relativePath) {
      this.relativePath = relativePath;
      return this;
    }
  }

}
