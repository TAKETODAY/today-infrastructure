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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
class PathResourceResolverTests {

  private final PathResourceResolver resolver = new PathResourceResolver();

  @Test
  void resolveFromClasspath() throws IOException {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    String requestPath = "bar.css";
    Resource actual = this.resolver.resolveResource(null, requestPath, Collections.singletonList(location), null);

    assertThat(actual).isEqualTo(location.createRelative(requestPath));
  }

  @Test
  void resolveFromClasspathRoot() {
    Resource location = new ClassPathResource("/");
    String requestPath = "infra/web/resource/test/bar.css";
    Resource actual = this.resolver.resolveResource(null, requestPath, Collections.singletonList(location), null);

    assertThat(actual).isNotNull();
  }

  @Test
  void checkResource() throws IOException {
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

  @Test
    // gh-23463
  void ignoreInvalidEscapeSequence() throws IOException {
    UrlResource location = new UrlResource(getClass().getResource("./test/"));

    Resource resource = new UrlResource(location.getURL() + "test%file.txt");
    assertThat(this.resolver.checkResource(resource, location)).isTrue();

    resource = location.createRelative("test%file.txt");
    assertThat(this.resolver.checkResource(resource, location)).isTrue();
  }

  @Test
  void checkResourceWithAllowedLocations() {
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
  void checkRelativeLocation() throws Exception {
    String location = new UrlResource(getClass().getResource("test/")).getURL().toExternalForm();
    location = location.replace("/test/infra", "/test/infra/../infra");

    Resource actual = this.resolver.resolveResource(
            null, "main.css", Collections.singletonList(new UrlResource(location)), null);

    assertThat(actual).isNotNull();
  }

  @Test
  void checkFileLocation() throws Exception {
    Resource resource = getResource("main.css");
    assertThat(this.resolver.checkResource(resource, resource)).isTrue();
  }

  @Test
  void resolvePathRootResource() {
    Resource webjarsLocation = new ClassPathResource("/META-INF/resources/webjars/", PathResourceResolver.class);
    String path = this.resolver.resolveUrlPathInternal("", Collections.singletonList(webjarsLocation), null);

    assertThat(path).isNull();
  }

  @Test
  void relativePathEncodedForUrlResource() throws Exception {
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

  @Test
  void setAllowedLocationsShouldUpdateAllowedLocations() {
    Resource[] locations = { new ClassPathResource("test/") };

    resolver.setAllowedLocations(locations);

    assertThat(resolver.getAllowedLocations()).isEqualTo(locations);
  }

  @Test
  void setAllowedLocationsWithNullShouldSetToNull() {
    resolver.setAllowedLocations((Resource[]) null);

    assertThat(resolver.getAllowedLocations()).isNull();
  }

  @Test
  void setLocationCharsetsShouldUpdateCharsets() {
    Map<Resource, Charset> charsets = new HashMap<>();
    Resource resource = new ClassPathResource("test/");
    charsets.put(resource, StandardCharsets.UTF_8);

    resolver.setLocationCharsets(charsets);

    assertThat(resolver.getLocationCharsets()).containsEntry(resource, StandardCharsets.UTF_8);
  }

  @Test
  void setLocationCharsetsShouldClearPreviousCharsets() {
    Map<Resource, Charset> charsets1 = new HashMap<>();
    Resource resource1 = new ClassPathResource("test1/");
    charsets1.put(resource1, StandardCharsets.UTF_8);
    resolver.setLocationCharsets(charsets1);

    Map<Resource, Charset> charsets2 = new HashMap<>();
    Resource resource2 = new ClassPathResource("test2/");
    charsets2.put(resource2, StandardCharsets.ISO_8859_1);

    resolver.setLocationCharsets(charsets2);

    assertThat(resolver.getLocationCharsets()).doesNotContainKey(resource1);
    assertThat(resolver.getLocationCharsets()).containsEntry(resource2, StandardCharsets.ISO_8859_1);
  }

  @Test
  void setUrlDecodeShouldUpdateFlag() {
    resolver.setUrlDecode(true);

    assertThat(resolver.isUrlDecode()).isTrue();

    resolver.setUrlDecode(false);

    assertThat(resolver.isUrlDecode()).isFalse();
  }

  @Test
  void resolveResourceInternalShouldReturnResourceWhenFound() throws IOException {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    String requestPath = "bar.css";

    Resource actual = resolver.resolveResource(null, requestPath, Collections.singletonList(location), null);

    assertThat(actual).isNotNull();
    assertThat(actual.exists()).isTrue();
  }

  @Test
  void resolveResourceInternalShouldReturnNullWhenNotFound() throws IOException {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    String requestPath = "nonexistent.css";

    Resource actual = resolver.resolveResource(null, requestPath, Collections.singletonList(location), null);

    assertThat(actual).isNull();
  }

  @Test
  void resolveUrlPathInternalShouldReturnPathWhenResourceExists() {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    String resourcePath = "bar.css";

    String actual = resolver.resolveUrlPathInternal(resourcePath, Collections.singletonList(location), null);

    assertThat(actual).isEqualTo(resourcePath);
  }

  @Test
  void resolveUrlPathInternalShouldReturnNullWhenResourceDoesNotExist() {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    String resourcePath = "nonexistent.css";

    String actual = resolver.resolveUrlPathInternal(resourcePath, Collections.singletonList(location), null);

    assertThat(actual).isNull();
  }

  @Test
  void resolveUrlPathInternalShouldReturnNullWhenResourcePathIsNull() {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);

    String actual = resolver.resolveUrlPathInternal(null, Collections.singletonList(location), null);

    assertThat(actual).isNull();
  }

  @Test
  void resolveUrlPathInternalShouldReturnNullWhenResourcePathIsEmpty() {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);

    String actual = resolver.resolveUrlPathInternal("", Collections.singletonList(location), null);

    assertThat(actual).isNull();
  }

  @Test
  void getResourceShouldReturnResourceWhenReadable() throws IOException {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    String resourcePath = "bar.css";

    Resource actual = resolver.getResource(resourcePath, location);

    assertThat(actual).isNotNull();
    assertThat(actual.isReadable()).isTrue();
  }

  @Test
  void getResourceShouldReturnNullWhenNotReadable() throws IOException {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    String resourcePath = "nonexistent.css";

    Resource actual = resolver.getResource(resourcePath, location);

    assertThat(actual).isNull();
  }

  @Test
  void checkResourceShouldReturnTrueWhenResourceIsUnderLocation() throws IOException {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    Resource resource = new ClassPathResource("test/bar.css", PathResourceResolver.class);

    boolean result = resolver.checkResource(resource, location);

    assertThat(result).isTrue();
  }

  @Test
  void checkResourceShouldReturnTrueWhenResourceIsUnderAllowedLocations() throws IOException {
    Resource allowedLocation = new ClassPathResource("testalternatepath/", PathResourceResolver.class);
    resolver.setAllowedLocations(allowedLocation);

    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    Resource resource = new ClassPathResource("testalternatepath/bar.css", PathResourceResolver.class);

    boolean result = resolver.checkResource(resource, location);

    assertThat(result).isTrue();
  }

  @Test
  void checkResourceShouldReturnFalseWhenResourceIsNotUnderLocationOrAllowedLocations() throws IOException {
    resolver.setAllowedLocations();

    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    Resource resource = new ClassPathResource("secret/secret.txt", PathResourceResolver.class);

    boolean result = resolver.checkResource(resource, location);

    assertThat(result).isFalse();
  }

  @Test
  void encodeOrDecodeIfNecessaryShouldDecodePathWhenNecessary() {
    Resource location = new ClassPathResource("test/");
    MockRequestContext request = getContext();
    String path = "%E4%B8%AD%E6%96%87.txt"; // "中文.txt" URL encoded

    String result = resolver.encodeOrDecodeIfNecessary(path, request, location);

    assertThat(result).isEqualTo("中文.txt");
  }

  @Test
  void shouldDecodeRelativePathShouldReturnTrueForNonUrlResource() {
    Resource location = new ClassPathResource("test/");

    // Using reflection to test private method
    boolean result = resolver.shouldDecodeRelativePath(location);

    assertThat(result).isTrue();
  }

  @Test
  void shouldDecodeRelativePathShouldReturnFalseForUrlResource() throws MalformedURLException {
    Resource location = new UrlResource("file:///tmp");

    // Using reflection to test private method
    boolean result = resolver.shouldDecodeRelativePath(location);

    assertThat(result).isFalse();
  }

  @Test
  void shouldEncodeRelativePathShouldReturnTrueWhenUrlDecodeIsTrueAndLocationIsUrlResource() throws MalformedURLException {
    resolver.setUrlDecode(true);
    Resource location = new UrlResource("file:///tmp");

    // Using reflection to test private method
    boolean result = resolver.shouldEncodeRelativePath(location);

    assertThat(result).isTrue();
  }

  @Test
  void shouldEncodeRelativePathShouldReturnFalseWhenUrlDecodeIsFalse() throws MalformedURLException {
    resolver.setUrlDecode(false);
    Resource location = new UrlResource("file:///tmp");

    // Using reflection to test private method
    boolean result = resolver.shouldEncodeRelativePath(location);

    assertThat(result).isFalse();
  }

  @Test
  void shouldEncodeRelativePathShouldReturnFalseWhenLocationIsNotUrlResource() {
    resolver.setUrlDecode(true);
    Resource location = new ClassPathResource("test/");

    // Using reflection to test private method
    boolean result = resolver.shouldEncodeRelativePath(location);

    assertThat(result).isFalse();
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
