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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.UrlResource;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HttpRequestMethodNotSupportedException;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.accept.ContentNegotiationManagerFactoryBean;
import cn.taketoday.web.context.support.StaticWebApplicationContext;
import cn.taketoday.web.servlet.HandlerMapping;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockServletContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ResourceHttpRequestHandler}.
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
@ExtendWith(GzipSupport.class)
public class ResourceHttpRequestHandlerTests {

	private ResourceHttpRequestHandler handler;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@BeforeEach
	public void setup() throws Exception {
		List<Resource> paths = new ArrayList<>(2);
		paths.add(new ClassPathResource("test/", getClass()));
		paths.add(new ClassPathResource("testalternatepath/", getClass()));
		paths.add(new ClassPathResource("META-INF/resources/webjars/"));

		TestServletContext servletContext = new TestServletContext();

		this.handler = new ResourceHttpRequestHandler();
		this.handler.setLocations(paths);
		this.handler.setCacheSeconds(3600);
		this.handler.setServletContext(servletContext);
		this.handler.afterPropertiesSet();

		this.request = new MockHttpServletRequest(servletContext, "GET", "");
		this.response = new MockHttpServletResponse();
	}


	@Test
	public void getResource() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/css");
		assertThat(this.response.getContentLength()).isEqualTo(17);
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test
	public void getResourceHttpHeader() throws Exception {
		this.request.setMethod("HEAD");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getContentType()).isEqualTo("text/css");
		assertThat(this.response.getContentLength()).isEqualTo(17);
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void getResourceHttpOptions() throws Exception {
		this.request.setMethod("OPTIONS");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
	}

	@Test
	public void getResourceNoCache() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheSeconds(0);
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("no-store");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void getVersionedResource() throws Exception {
		VersionResourceResolver versionResolver = new VersionResourceResolver()
				.addFixedVersionStrategy("versionString", "/**");
		this.handler.setResourceResolvers(Arrays.asList(versionResolver, new PathResourceResolver()));
		this.handler.afterPropertiesSet();

		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "versionString/foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("ETag")).isEqualTo("W/\"versionString\"");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void getResourceHttp10BehaviorCache() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheSeconds(3600);
		this.handler.setUseExpiresHeader(true);
		this.handler.setUseCacheControlHeader(true);
		this.handler.setAlwaysMustRevalidate(true);
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600, must-revalidate");
		assertThat(this.response.getDateHeader("Expires") >= System.currentTimeMillis() - 1000 + (3600 * 1000)).isTrue();
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void getResourceHttp10BehaviorNoCache() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheSeconds(0);
		this.handler.setUseExpiresHeader(true);
		this.handler.setUseCacheControlNoStore(false);
		this.handler.setUseCacheControlHeader(true);
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("Pragma")).isEqualTo("no-cache");
		assertThat(this.response.getHeaderValues("Cache-Control")).hasSize(1);
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("no-cache");
		assertThat(this.response.getDateHeader("Expires") <= System.currentTimeMillis()).isTrue();
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void getResourceWithHtmlMediaType() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.html");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/html");
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.html") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void getResourceFromAlternatePath() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "baz.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/css");
		assertThat(this.response.getContentLength()).isEqualTo(17);
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("testalternatepath/baz.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test
	public void getResourceFromSubDirectory() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/foo.js");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/javascript");
		assertThat(this.response.getContentAsString()).isEqualTo("function foo() { console.log(\"hello world\"); }");
	}

	@Test
	public void getResourceFromSubDirectoryOfAlternatePath() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/baz.js");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/javascript");
		assertThat(this.response.getContentAsString()).isEqualTo("function foo() { console.log(\"hello world\"); }");
	}

	@Test  // SPR-13658
	@SuppressWarnings("deprecation")
	public void getResourceWithRegisteredMediaType() throws Exception {
		ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
		factory.addMediaType("bar", new MediaType("foo", "bar"));
		factory.afterPropertiesSet();
		ContentNegotiationManager manager = factory.getObject();

		List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setServletContext(new MockServletContext());
		handler.setLocations(paths);
		handler.setContentNegotiationManager(manager);
		handler.afterPropertiesSet();

		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.bar");
		handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("foo/bar");
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test  // SPR-14577
	@SuppressWarnings("deprecation")
	public void getMediaTypeWithFavorPathExtensionOff() throws Exception {
		ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
		factory.setFavorPathExtension(false);
		factory.afterPropertiesSet();
		ContentNegotiationManager manager = factory.getObject();

		List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setServletContext(new MockServletContext());
		handler.setLocations(paths);
		handler.setContentNegotiationManager(manager);
		handler.afterPropertiesSet();

		this.request.addHeader("Accept", "application/json,text/plain,*/*");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.html");
		handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/html");
	}

	@Test  // SPR-14368
	public void getResourceWithMediaTypeResolvedThroughServletContext() throws Exception {
		MockServletContext servletContext = new MockServletContext() {
			@Override
			public String getMimeType(String filePath) {
				return "foo/bar";
			}
		};

		List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setServletContext(servletContext);
		handler.setLocations(paths);
		handler.afterPropertiesSet();

		MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "");
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		handler.handleRequest(request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("foo/bar");
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test  // gh-27538, gh-27624
	public void filterNonExistingLocations() throws Exception {
		List<Resource> inputLocations = Arrays.asList(
				new ClassPathResource("test/", getClass()),
				new ClassPathResource("testalternatepath/", getClass()),
				new ClassPathResource("nosuchpath/", getClass()));

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setServletContext(new MockServletContext());
		handler.setLocations(inputLocations);
		handler.setOptimizeLocations(true);
		handler.afterPropertiesSet();

		List<Resource> actual = handler.getLocations();
		assertThat(actual).hasSize(2);
		assertThat(actual.get(0).getURL().toString()).endsWith("test/");
		assertThat(actual.get(1).getURL().toString()).endsWith("testalternatepath/");
	}

	@Test
	public void testInvalidPath() throws Exception {
		// Use mock ResourceResolver: i.e. we're only testing upfront validations...

		Resource resource = mock(Resource.class);
		given(resource.getFilename()).willThrow(new AssertionError("Resource should not be resolved"));
		given(resource.getInputStream()).willThrow(new AssertionError("Resource should not be resolved"));
		ResourceResolver resolver = mock(ResourceResolver.class);
		given(resolver.resolveResource(any(), any(), any(), any())).willReturn(resource);

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setLocations(Collections.singletonList(new ClassPathResource("test/", getClass())));
		handler.setResourceResolvers(Collections.singletonList(resolver));
		handler.setServletContext(new TestServletContext());
		handler.afterPropertiesSet();

		testInvalidPath("../testsecret/secret.txt", handler);
		testInvalidPath("test/../../testsecret/secret.txt", handler);
		testInvalidPath(":/../../testsecret/secret.txt", handler);

		Resource location = new UrlResource(getClass().getResource("./test/"));
		this.handler.setLocations(Collections.singletonList(location));
		Resource secretResource = new UrlResource(getClass().getResource("testsecret/secret.txt"));
		String secretPath = secretResource.getURL().getPath();

		testInvalidPath("file:" + secretPath, handler);
		testInvalidPath("/file:" + secretPath, handler);
		testInvalidPath("url:" + secretPath, handler);
		testInvalidPath("/url:" + secretPath, handler);
		testInvalidPath("/../.." + secretPath, handler);
		testInvalidPath("/%2E%2E/testsecret/secret.txt", handler);
		testInvalidPath("/%2E%2E/testsecret/secret.txt", handler);
		testInvalidPath("%2F%2F%2E%2E%2F%2F%2E%2E" + secretPath, handler);
	}

	private void testInvalidPath(String requestPath, ResourceHttpRequestHandler handler) throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, requestPath);
		this.response = new MockHttpServletResponse();
		handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
	}

	@Test
	public void resolvePathWithTraversal() throws Exception {
		for (HttpMethod method : HttpMethod.values()) {
			this.request = new MockHttpServletRequest("GET", "");
			this.response = new MockHttpServletResponse();
			testResolvePathWithTraversal(method);
		}
	}

	private void testResolvePathWithTraversal(HttpMethod httpMethod) throws Exception {
		this.request.setMethod(httpMethod.name());

		Resource location = new ClassPathResource("test/", getClass());
		this.handler.setLocations(Collections.singletonList(location));

		testResolvePathWithTraversal(location, "../testsecret/secret.txt");
		testResolvePathWithTraversal(location, "test/../../testsecret/secret.txt");
		testResolvePathWithTraversal(location, ":/../../testsecret/secret.txt");

		location = new UrlResource(getClass().getResource("./test/"));
		this.handler.setLocations(Collections.singletonList(location));
		Resource secretResource = new UrlResource(getClass().getResource("testsecret/secret.txt"));
		String secretPath = secretResource.getURL().getPath();

		testResolvePathWithTraversal(location, "file:" + secretPath);
		testResolvePathWithTraversal(location, "/file:" + secretPath);
		testResolvePathWithTraversal(location, "url:" + secretPath);
		testResolvePathWithTraversal(location, "/url:" + secretPath);
		testResolvePathWithTraversal(location, "/" + secretPath);
		testResolvePathWithTraversal(location, "////../.." + secretPath);
		testResolvePathWithTraversal(location, "/%2E%2E/testsecret/secret.txt");
		testResolvePathWithTraversal(location, "%2F%2F%2E%2E%2F%2Ftestsecret/secret.txt");
		testResolvePathWithTraversal(location, "/  " + secretPath);
	}

	private void testResolvePathWithTraversal(Resource location, String requestPath) throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, requestPath);
		this.response = new MockHttpServletResponse();
		this.handler.handleRequest(this.request, this.response);
		if (!location.createRelative(requestPath).exists() && !requestPath.contains(":")) {
			fail(requestPath + " doesn't actually exist as a relative path");
		}
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
	}

	@Test
	public void ignoreInvalidEscapeSequence() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/%foo%/bar.txt");
		this.response = new MockHttpServletResponse();
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(404);
	}

	@Test
	public void processPath() {
		// Unchanged
		assertThat(this.handler.processPath("/foo/bar")).isSameAs("/foo/bar");
		assertThat(this.handler.processPath("foo/bar")).isSameAs("foo/bar");

		// leading whitespace control characters (00-1F)
		assertThat(this.handler.processPath("  /foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath((char) 1 + "/foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath((char) 31 + "/foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath("  foo/bar")).isEqualTo("foo/bar");
		assertThat(this.handler.processPath((char) 31 + "foo/bar")).isEqualTo("foo/bar");

		// leading control character 0x7F (DEL)
		assertThat(this.handler.processPath((char) 127 + "/foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath((char) 127 + "/foo/bar")).isEqualTo("/foo/bar");

		// leading control and '/' characters
		assertThat(this.handler.processPath("  /  foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath("  /  /  foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath("  // /// ////  foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath((char) 1 + " / " + (char) 127 + " // foo/bar")).isEqualTo("/foo/bar");

		// root or empty path
		assertThat(this.handler.processPath("   ")).isEqualTo("");
		assertThat(this.handler.processPath("/")).isEqualTo("/");
		assertThat(this.handler.processPath("///")).isEqualTo("/");
		assertThat(this.handler.processPath("/ /   / ")).isEqualTo("/");
		assertThat(this.handler.processPath("\\/ \\/   \\/ ")).isEqualTo("/");

		// duplicate slash or backslash
		assertThat(this.handler.processPath("//foo/ /bar//baz//")).isEqualTo("/foo/ /bar/baz/");
		assertThat(this.handler.processPath("\\\\foo\\ \\bar\\\\baz\\\\")).isEqualTo("/foo/ /bar/baz/");
		assertThat(this.handler.processPath("foo\\\\/\\////bar")).isEqualTo("foo/bar");

	}

	@Test
	public void initAllowedLocations() {
		PathResourceResolver resolver = (PathResourceResolver) this.handler.getResourceResolvers().get(0);
		Resource[] locations = resolver.getAllowedLocations();

		assertThat(locations.length).isEqualTo(3);
		assertThat(((ClassPathResource) locations[0]).getPath()).isEqualTo("test/");
		assertThat(((ClassPathResource) locations[1]).getPath()).isEqualTo("testalternatepath/");
		assertThat(((ClassPathResource) locations[2]).getPath()).isEqualTo("META-INF/resources/webjars/");
	}

	@Test
	public void initAllowedLocationsWithExplicitConfiguration() throws Exception {
		ClassPathResource location1 = new ClassPathResource("test/", getClass());
		ClassPathResource location2 = new ClassPathResource("testalternatepath/", getClass());

		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(location1);

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setResourceResolvers(Collections.singletonList(pathResolver));
		handler.setServletContext(new MockServletContext());
		handler.setLocations(Arrays.asList(location1, location2));
		handler.afterPropertiesSet();

		Resource[] locations = pathResolver.getAllowedLocations();
		assertThat(locations.length).isEqualTo(1);
		assertThat(((ClassPathResource) locations[0]).getPath()).isEqualTo("test/");
	}

	@Test
	public void notModified() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css"));
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_MODIFIED);
	}

	@Test
	public void modified() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css") / 1000 * 1000 - 1);
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test
	public void directory() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/");
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(404);
	}

	@Test
	public void directoryInJarFile() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "underscorejs/");
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(404);
	}

	@Test
	public void missingResourcePath() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "");
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(404);
	}

	@Test
	public void noPathWithinHandlerMappingAttribute() throws Exception {
		assertThatIllegalStateException().isThrownBy(() ->
				this.handler.handleRequest(this.request, this.response));
	}

	@Test
	public void unsupportedHttpMethod() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.setMethod("POST");
		assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class).isThrownBy(() ->
				this.handler.handleRequest(this.request, this.response));
	}

	@Test
	public void resourceNotFound() throws Exception {
		for (HttpMethod method : HttpMethod.values()) {
			this.request = new MockHttpServletRequest("GET", "");
			this.response = new MockHttpServletResponse();
			resourceNotFound(method);
		}
	}

	private void resourceNotFound(HttpMethod httpMethod) throws Exception {
		this.request.setMethod(httpMethod.name());
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "not-there.css");
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
	}

	@Test
	public void partialContentByteRange() throws Exception {
		this.request.addHeader("Range", "bytes=0-1");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(2);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/10");
		assertThat(this.response.getContentAsString()).isEqualTo("So");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void partialContentByteRangeNoEnd() throws Exception {
		this.request.addHeader("Range", "bytes=9-");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(1);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
		assertThat(this.response.getContentAsString()).isEqualTo(".");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void partialContentByteRangeLargeEnd() throws Exception {
		this.request.addHeader("Range", "bytes=9-10000");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(1);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
		assertThat(this.response.getContentAsString()).isEqualTo(".");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void partialContentSuffixRange() throws Exception {
		this.request.addHeader("Range", "bytes=-1");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(1);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
		assertThat(this.response.getContentAsString()).isEqualTo(".");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void partialContentSuffixRangeLargeSuffix() throws Exception {
		this.request.addHeader("Range", "bytes=-11");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(10);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-9/10");
		assertThat(this.response.getContentAsString()).isEqualTo("Some text.");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void partialContentInvalidRangeHeader() throws Exception {
		this.request.addHeader("Range", "bytes= foo bar");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(416);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes */10");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void partialContentMultipleByteRanges() throws Exception {
		this.request.addHeader("Range", "bytes=0-1, 4-5, 8-9");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType().startsWith("multipart/byteranges; boundary=")).isTrue();

		String boundary = "--" + this.response.getContentType().substring(31);

		String content = this.response.getContentAsString();
		String[] ranges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);

		assertThat(ranges[0]).isEqualTo(boundary);
		assertThat(ranges[1]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[2]).isEqualTo("Content-Range: bytes 0-1/10");
		assertThat(ranges[3]).isEqualTo("So");

		assertThat(ranges[4]).isEqualTo(boundary);
		assertThat(ranges[5]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[6]).isEqualTo("Content-Range: bytes 4-5/10");
		assertThat(ranges[7]).isEqualTo(" t");

		assertThat(ranges[8]).isEqualTo(boundary);
		assertThat(ranges[9]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[10]).isEqualTo("Content-Range: bytes 8-9/10");
		assertThat(ranges[11]).isEqualTo("t.");
	}

	@Test  // gh-25976
	public void partialContentByteRangeWithEncodedResource(GzipSupport.GzippedFiles gzippedFiles) throws Exception {
		String path = "js/foo.js";
		gzippedFiles.create(path);

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setResourceResolvers(Arrays.asList(new EncodedResourceResolver(), new PathResourceResolver()));
		handler.setLocations(Collections.singletonList(new ClassPathResource("test/", getClass())));
		handler.setServletContext(new MockServletContext());
		handler.afterPropertiesSet();

		this.request.addHeader("Accept-Encoding", "gzip");
		this.request.addHeader("Range", "bytes=0-1");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, path);
		handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getHeaderNames()).containsExactlyInAnyOrder(
				"Content-Type", "Content-Length", "Content-Range", "Accept-Ranges",
				"Last-Modified", "Content-Encoding", "Vary");

		assertThat(this.response.getContentType()).isEqualTo("text/javascript");
		assertThat(this.response.getContentLength()).isEqualTo(2);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/66");
		assertThat(this.response.getHeaderValues("Accept-Ranges")).containsExactly("bytes");
		assertThat(this.response.getHeaderValues("Content-Encoding")).containsExactly("gzip");
		assertThat(this.response.getHeaderValues("Vary")).containsExactly("Accept-Encoding");
	}

	@Test  // gh-25976
	public void partialContentWithHttpHead() throws Exception {
		this.request.setMethod("HEAD");
		this.request.addHeader("Range", "bytes=0-1");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(2);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/10");
		assertThat(this.response.getHeaderValues("Accept-Ranges")).containsExactly("bytes");
	}

	@Test  // SPR-14005
	public void doOverwriteExistingCacheControlHeaders() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.response.setHeader("Cache-Control", "no-store");

		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
	}

	@Test
	public void ignoreLastModified() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setUseLastModified(false);
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/css");
		assertThat(this.response.getContentLength()).isEqualTo(17);
		assertThat(this.response.containsHeader("Last-Modified")).isFalse();
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test
	public void servletContextRootValidation() {
		StaticWebApplicationContext context = new StaticWebApplicationContext() {
			@Override
			public Resource getResource(String location) {
				return new FileSystemResource("/");
			}
		};

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setLocationValues(Collections.singletonList("/"));
		handler.setApplicationContext(context);

		assertThatIllegalStateException().isThrownBy(handler::afterPropertiesSet)
				.withMessage("The String-based location \"/\" should be relative to the web application root but " +
						"resolved to a Resource of type: class cn.taketoday.core.io.FileSystemResource. " +
						"If this is intentional, please pass it as a pre-configured Resource via setLocations.");
	}


	private long resourceLastModified(String resourceName) throws IOException {
		return new ClassPathResource(resourceName, getClass()).getFile().lastModified();
	}


	private static class TestServletContext extends MockServletContext {

		@Override
		public String getMimeType(String filePath) {
			if (filePath.endsWith(".css")) {
				return "text/css";
			}
			else if (filePath.endsWith(".js")) {
				return "text/javascript";
			}
			else {
				return super.getMimeType(filePath);
			}
		}
	}

}
