/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.web.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.UrlBasedResource;
import cn.taketoday.lang.NonNull;
import cn.taketoday.web.context.support.ServletContextResource;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
    String requestPath = "cn/taketoday/web/resource/test/bar.css";
    Resource actual = this.resolver.resolveResource(null, requestPath, Collections.singletonList(location), null);

    assertThat(actual).isNotNull();
  }

  @Test
  public void checkResource() throws IOException {
    Resource location = new ClassPathResource("test/", PathResourceResolver.class);
    testCheckResource(location, "../testsecret/secret.txt");
    testCheckResource(location, "test/../../testsecret/secret.txt");

    location = new UrlBasedResource(getClass().getResource("test/"));
    String secretPath = new UrlBasedResource(getClass().getResource("testsecret/secret.txt")).getLocation().getPath();
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
    if (!location.createRelative(requestPath).exists() && !requestPath.contains(":")) {
      fail(requestPath + " doesn't actually exist as a relative path");
    }
    assertThat(actual).isNull();
  }

  @Test // gh-23463
  public void ignoreInvalidEscapeSequence() throws IOException {
    UrlBasedResource location = new UrlBasedResource(getClass().getResource("test/"));
    Resource resource = location.createRelative("test%file.txt");
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

  @Test // SPR-12432
  public void checkServletContextResource() throws Exception {
    Resource classpathLocation = new ClassPathResource("test/", PathResourceResolver.class);
    MockServletContext context = new MockServletContext();

    ServletContextResource servletContextLocation = new ServletContextResource(context, "/webjars/");
    ServletContextResource resource = new ServletContextResource(context, "/webjars/webjar-foo/1.0/foo.js");

    assertThat(this.resolver.checkResource(resource, classpathLocation)).isFalse();
    assertThat(this.resolver.checkResource(resource, servletContextLocation)).isTrue();
  }

  @Test // SPR-12624
  public void checkRelativeLocation() throws Exception {
    String location = new UrlBasedResource(getClass().getResource("test/")).getLocation().toExternalForm();
    location = location.replace("/test/cn/taketoday", "/test/cn/../cn/taketoday");

    Resource actual = this.resolver.resolveResource(
            null, "main.css", Collections.singletonList(new UrlBasedResource(location)), null);

    assertThat(actual).isNotNull();
  }

  @Test // SPR-12747
  public void checkFileLocation() throws Exception {
    Resource resource = getResource("main.css");
    assertThat(this.resolver.checkResource(resource, resource)).isTrue();
  }

  @Test // SPR-13241
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
    ServletRequestContext request = getContext();
    this.resolver.resolveResource(request, "/Ä ;ä.txt", locations, null);

    assertThat(location.getSavedRelativePath()).isEqualTo("%C3%84%20%3B%C3%A4.txt");
  }

  @NonNull
  private ServletRequestContext getContext() {
    return new ServletRequestContext(null, new MockHttpServletRequest(), null);
  }

  private Resource getResource(String filePath) {
    return new ClassPathResource("test/" + filePath, getClass());
  }

  private static class TestUrlResource extends UrlBasedResource {

    private String relativePath;

    public TestUrlResource(String path) throws MalformedURLException {
      super(path);
    }

    public String getSavedRelativePath() {
      return this.relativePath;
    }

    @Override
    public UrlBasedResource createRelative(String relativePath) {
      this.relativePath = relativePath;
      return this;
    }
  }

}
