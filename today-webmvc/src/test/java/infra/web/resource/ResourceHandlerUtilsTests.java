/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.core.io.ClassPathResource;
import infra.core.io.FileSystemResource;
import infra.core.io.Resource;
import infra.core.io.UrlResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 12:39
 */
class ResourceHandlerUtilsTests {

  @Test
  void assertResourceLocationShouldThrowExceptionWhenLocationIsNull() {
    assertThatThrownBy(() -> ResourceHandlerUtils.assertResourceLocation(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Resource location is required");
  }

  @Test
  void initLocationPathShouldAppendSlashWhenMissing() {
    String path = "/test/path";

    String result = ResourceHandlerUtils.initLocationPath(path);

    assertThat(result).isEqualTo("/test/path/");
  }

  @Test
  void initLocationPathShouldNotAppendSlashWhenAlreadyPresent() {
    String path = "/test/path/";

    String result = ResourceHandlerUtils.initLocationPath(path);

    assertThat(result).isEqualTo("/test/path/");
  }

  @Test
  void normalizeInputPathShouldReplaceBackslashWithForwardSlash() {
    String path = "test\\path\\file.txt";

    String result = ResourceHandlerUtils.normalizeInputPath(path);

    assertThat(result).isEqualTo("test/path/file.txt");
  }

  @Test
  void normalizeInputPathShouldRemoveDuplicateSlashes() {
    String path = "test//path///file.txt";

    String result = ResourceHandlerUtils.normalizeInputPath(path);

    assertThat(result).isEqualTo("test/path/file.txt");
  }

  @Test
  void normalizeInputPathShouldCleanLeadingControlCharacters() {
    String path = "  / // test/path";

    String result = ResourceHandlerUtils.normalizeInputPath(path);

    assertThat(result).isEqualTo("/test/path");
  }

  @Test
  void shouldIgnoreInputPathShouldReturnTrueForBlankPath() {
    boolean result = ResourceHandlerUtils.shouldIgnoreInputPath("");

    assertThat(result).isTrue();
  }

  @Test
  void shouldIgnoreInputPathShouldReturnTrueForInvalidPath() {
    String path = "../WEB-INF/web.xml";

    boolean result = ResourceHandlerUtils.shouldIgnoreInputPath(path);

    assertThat(result).isTrue();
  }

  @Test
  void shouldIgnoreInputPathShouldReturnFalseForValidPath() {
    String path = "/static/file.txt";

    boolean result = ResourceHandlerUtils.shouldIgnoreInputPath(path);

    assertThat(result).isFalse();
  }

  @Test
  void isInvalidPathShouldReturnTrueForWebInfPath() {
    String path = "/WEB-INF/web.xml";

    boolean result = ResourceHandlerUtils.isInvalidPath(path);

    assertThat(result).isTrue();
  }

  @Test
  void isInvalidPathShouldReturnTrueForMetaInfPath() {
    String path = "/META-INF/MANIFEST.MF";

    boolean result = ResourceHandlerUtils.isInvalidPath(path);

    assertThat(result).isTrue();
  }

  @Test
  void isInvalidPathShouldReturnTrueForPathWithParentDirectoryTraversal() {
    String path = "/static/../secret/file.txt";

    boolean result = ResourceHandlerUtils.isInvalidPath(path);

    assertThat(result).isTrue();
  }

  @Test
  void isInvalidPathShouldReturnTrueForUrlPath() {
    String path = "/http://example.com/file.txt";

    boolean result = ResourceHandlerUtils.isInvalidPath(path);

    assertThat(result).isTrue();
  }

  @Test
  void isInvalidPathShouldReturnFalseForValidPath() {
    String path = "/static/file.txt";

    boolean result = ResourceHandlerUtils.isInvalidPath(path);

    assertThat(result).isFalse();
  }

  @Test
  void isResourceUnderLocationShouldReturnTrueWhenResourceIsUnderLocation() throws IOException {
    Resource location = new ClassPathResource("static/");
    Resource resource = new ClassPathResource("static/file.txt");

    boolean result = ResourceHandlerUtils.isResourceUnderLocation(location, resource);

    assertThat(result).isTrue();
  }

  @Test
  void isResourceUnderLocationShouldReturnFalseWhenResourceIsNotUnderLocation() throws IOException {
    Resource location = new ClassPathResource("static/");
    Resource resource = new ClassPathResource("secret/file.txt");

    boolean result = ResourceHandlerUtils.isResourceUnderLocation(location, resource);

    assertThat(result).isFalse();
  }

  @Test
  void isResourceUnderLocationShouldReturnFalseWhenResourcesHaveDifferentTypes() throws IOException {
    Resource location = new ClassPathResource("static/");
    Resource resource = mock(UrlResource.class);

    boolean result = ResourceHandlerUtils.isResourceUnderLocation(location, resource);

    assertThat(result).isFalse();
  }

  @Test
  void assertResourceLocationShouldThrowExceptionWhenPathDoesNotEndWithSlash() throws IOException {
    FileSystemResource location = mock(FileSystemResource.class);
    when(location.getPath()).thenReturn("/test/path");

    assertThatThrownBy(() -> ResourceHandlerUtils.assertResourceLocation(location))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Resource location does not end with slash: /test/path");
  }

  @Test
  void assertResourceLocationShouldNotThrowExceptionWhenPathEndsWithSlash() throws IOException {
    FileSystemResource location = mock(FileSystemResource.class);
    when(location.getPath()).thenReturn("/test/path/");

    assertThatNoException().isThrownBy(() -> ResourceHandlerUtils.assertResourceLocation(location));
  }

  @Test
  void initLocationPathShouldUseWindowsSeparatorWhenPathContainsBackslash() {
    String path = "C:\\test\\path";

    String result = ResourceHandlerUtils.initLocationPath(path);

    assertThat(result).isEqualTo("C:\\test\\path\\");
  }

  @Test
  void normalizeInputPathShouldHandleComplexPath() {
    String path = "  / // test\\path//file.txt";

    String result = ResourceHandlerUtils.normalizeInputPath(path);

    assertThat(result).isEqualTo("/test/path/file.txt");
  }

  @Test
  void normalizeInputPathShouldHandleOnlySlashesAndControlCharacters() {
    String path = "  / // /";

    String result = ResourceHandlerUtils.normalizeInputPath(path);

    assertThat(result).isEqualTo("/");
  }

  @Test
  void normalizeInputPathShouldHandleEmptyString() {
    String path = "";

    String result = ResourceHandlerUtils.normalizeInputPath(path);

    assertThat(result).isEqualTo("");
  }

  @Test
  void shouldIgnoreInputPathShouldReturnTrueForInvalidEncodedPath() {
    String path = "%2e%2e%2fWEB-INF/web.xml"; // ../WEB-INF/web.xml encoded

    boolean result = ResourceHandlerUtils.shouldIgnoreInputPath(path);

    assertThat(result).isTrue();
  }

  @Test
  void shouldIgnoreInputPathShouldReturnTrueForNullPath() {
    boolean result = ResourceHandlerUtils.shouldIgnoreInputPath(null);

    assertThat(result).isTrue();
  }

  @Test
  void isInvalidPathShouldReturnTrueForPathWithUrlPrefix() {
    String path = "/url:http://example.com";

    boolean result = ResourceHandlerUtils.isInvalidPath(path);

    assertThat(result).isTrue();
  }

  @Test
  void isInvalidPathShouldReturnFalseForPathWithColonNotInUrlContext() {
    String path = "/file:name.txt";

    boolean result = ResourceHandlerUtils.isInvalidPath(path);

    assertThat(result).isFalse();
  }

  @Test
  void isInvalidEncodedPathShouldReturnTrueForBlankDecodedPath() {
    String path = "%20%09"; // decodes to whitespace only

    boolean result = ResourceHandlerUtils.isInvalidEncodedPath(path);

    assertThat(result).isTrue();
  }

  @Test
  void isInvalidEncodedPathShouldReturnTrueWhenDecodedPathIsAlsoInvalid() {
    String path = "%2e%2e%2fWEB-INF/web.xml"; // ../WEB-INF/web.xml encoded

    boolean result = ResourceHandlerUtils.isInvalidEncodedPath(path);

    assertThat(result).isTrue();
  }

  @Test
  void isInvalidEncodedPathShouldReturnFalseForValidEncodedPath() {
    String path = "%66%69%6c%65%2e%74%78%74"; // file.txt encoded

    boolean result = ResourceHandlerUtils.isInvalidEncodedPath(path);

    assertThat(result).isFalse();
  }

  @Test
  void isResourceUnderLocationShouldReturnTrueForSameLocationAndResource() throws IOException {
    Resource location = new ClassPathResource("static/");
    Resource resource = new ClassPathResource("static/");

    boolean result = ResourceHandlerUtils.isResourceUnderLocation(location, resource);

    assertThat(result).isTrue();
  }

  @Test
  void isResourceUnderLocationShouldReturnTrueForResourceInSubdirectory() throws IOException {
    Resource location = new ClassPathResource("static/");
    Resource resource = new ClassPathResource("static/css/style.css");

    boolean result = ResourceHandlerUtils.isResourceUnderLocation(location, resource);

    assertThat(result).isTrue();
  }

  @Test
  void isResourceUnderLocationShouldReturnFalseForResourceOutsideLocation() throws IOException {
    Resource location = new ClassPathResource("static/");
    Resource resource = new ClassPathResource("templates/index.html");

    boolean result = ResourceHandlerUtils.isResourceUnderLocation(location, resource);

    assertThat(result).isFalse();
  }

  @Test
  void isInvalidEncodedResourcePathShouldReturnTrueForEncodedParentTraversal() {
    String resourcePath = "%2e%2e%2fsecret.txt"; // ../secret.txt encoded

    boolean result = ResourceHandlerUtils.isInvalidEncodedResourcePath(resourcePath);

    assertThat(result).isTrue();
  }

  @Test
  void isInvalidEncodedResourcePathShouldReturnFalseForValidPath() {
    String resourcePath = "static/file.txt";

    boolean result = ResourceHandlerUtils.isInvalidEncodedResourcePath(resourcePath);

    assertThat(result).isFalse();
  }

  @Test
  void isInvalidEncodedResourcePathShouldReturnFalseForPathWithoutPercentEncoding() {
    String resourcePath = "static/../file.txt";

    boolean result = ResourceHandlerUtils.isInvalidEncodedResourcePath(resourcePath);

    assertThat(result).isFalse(); // This checks for % character first
  }

  @Test
  void assertResourceLocationShouldNotThrowForClassPathResourceEndingWithSlash() throws IOException {
    ClassPathResource location = mock(ClassPathResource.class);
    when(location.getPath()).thenReturn("static/");

    assertThatNoException().isThrownBy(() -> ResourceHandlerUtils.assertResourceLocation(location));
  }

  @Test
  void assertResourceLocationShouldThrowForClassPathResourceNotEndingWithSlash() throws IOException {
    ClassPathResource location = mock(ClassPathResource.class);
    when(location.getPath()).thenReturn("static");

    assertThatThrownBy(() -> ResourceHandlerUtils.assertResourceLocation(location))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Resource location does not end with slash: static");
  }

  @Test
  void assertResourceLocationShouldNotThrowForUrlResourceEndingWithSlash() throws IOException {
    UrlResource location = mock(UrlResource.class);
    when(location.getURL()).thenReturn(new java.net.URL("file:///test/"));

    assertThatNoException().isThrownBy(() -> ResourceHandlerUtils.assertResourceLocation(location));
  }

  @Test
  void assertResourceLocationShouldThrowForUrlResourceNotEndingWithSlash() throws IOException {
    UrlResource location = mock(UrlResource.class);
    when(location.getURL()).thenReturn(new java.net.URL("file:///test"));

    assertThatThrownBy(() -> ResourceHandlerUtils.assertResourceLocation(location))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Resource location does not end with slash: file:/test");
  }

  @Test
  void initLocationPathShouldAppendForwardSlashForPathsWithForwardSlash() {
    String path = "/unix/style/path";

    String result = ResourceHandlerUtils.initLocationPath(path);

    assertThat(result).isEqualTo("/unix/style/path/");
  }

  @Test
  void normalizeInputPathShouldHandlePathStartingWithSlashAndControlCharacters() {
    String path = "/\t\n /test";

    String result = ResourceHandlerUtils.normalizeInputPath(path);

    assertThat(result).isEqualTo("/test");
  }

  @Test
  void normalizeInputPathShouldHandleOnlyControlCharacters() {
    String path = "\t\n ";

    String result = ResourceHandlerUtils.normalizeInputPath(path);

    assertThat(result).isEqualTo("");
  }

  @Test
  void shouldIgnoreInputPathShouldReturnTrueForPathWithWebInfAfterNormalization() {
    String path = "  / // WEB-INF/web.xml";

    boolean result = ResourceHandlerUtils.shouldIgnoreInputPath(path);

    assertThat(result).isTrue();
  }

  @Test
  void shouldIgnoreInputPathShouldReturnTrueForPathWithMetaInfAfterNormalization() {
    String path = "\t/META-INF/MANIFEST.MF";

    boolean result = ResourceHandlerUtils.shouldIgnoreInputPath(path);

    assertThat(result).isTrue();
  }

  @Test
  void isInvalidPathShouldReturnFalseForPathContainingColonButNotUrl() {
    String path = "/folder:file.txt";

    boolean result = ResourceHandlerUtils.isInvalidPath(path);

    assertThat(result).isFalse();
  }

  @Test
  void isInvalidEncodedPathShouldReturnTrueForDoubleEncodedPath() {
    String path = "%252e%252e%252fWEB-INF/web.xml"; // double encoded ../

    boolean result = ResourceHandlerUtils.isInvalidEncodedPath(path);

    assertThat(result).isTrue();
  }

  @Test
  void isInvalidEncodedPathShouldReturnFalseForValidPathThatLooksLikeEncoded() {
    String path = "file%20name.txt"; // valid encoded space

    boolean result = ResourceHandlerUtils.isInvalidEncodedPath(path);

    assertThat(result).isFalse();
  }

  @Test
  void isResourceUnderLocationShouldReturnTrueForUrlResourcesWithSameBase() throws IOException {
    UrlResource location = mock(UrlResource.class);
    UrlResource resource = mock(UrlResource.class);

    java.net.URL locationUrl = new java.net.URL("file:///test/");
    java.net.URL resourceUrl = new java.net.URL("file:///test/file.txt");

    when(location.getURL()).thenReturn(locationUrl);
    when(resource.getURL()).thenReturn(resourceUrl);

    boolean result = ResourceHandlerUtils.isResourceUnderLocation(location, resource);

    assertThat(result).isTrue();
  }

  @Test
  void isResourceUnderLocationShouldReturnFalseForUrlResourcesWithDifferentBase() throws IOException {
    UrlResource location = mock(UrlResource.class);
    UrlResource resource = mock(UrlResource.class);

    java.net.URL locationUrl = new java.net.URL("file:///test/");
    java.net.URL resourceUrl = new java.net.URL("file:///other/file.txt");

    when(location.getURL()).thenReturn(locationUrl);
    when(resource.getURL()).thenReturn(resourceUrl);

    boolean result = ResourceHandlerUtils.isResourceUnderLocation(location, resource);

    assertThat(result).isFalse();
  }

  @Test
  void isInvalidEncodedResourcePathShouldReturnFalseForPathWithPercentButNoTraversal() {
    String resourcePath = "file%20name.txt";

    boolean result = ResourceHandlerUtils.isInvalidEncodedResourcePath(resourcePath);

    assertThat(result).isFalse();
  }

  @Test
  void isInvalidEncodedResourcePathShouldHandleExceptionInDecoding() {
    String resourcePath = "%zz"; // invalid percent encoding

    boolean result = ResourceHandlerUtils.isInvalidEncodedResourcePath(resourcePath);

    assertThat(result).isFalse();
  }

}