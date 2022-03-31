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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.web.servlet.samples.client.standalone;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.MultipartBodyBuilder;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.reactive.server.EntityExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.ui.Model;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.HttpMethod;
import cn.taketoday.web.bind.annotation.RequestParam;
import cn.taketoday.web.bind.annotation.RequestPart;
import cn.taketoday.web.filter.OncePerRequestFilter;
import cn.taketoday.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.model;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.MultipartControllerTests}.
 *
 * @author Rossen Stoyanchev
 */
public class MultipartControllerTests {

	private final WebTestClient testClient = MockMvcWebTestClient.bindToController(new MultipartController()).build();


	@Test
	public void multipartRequestWithSingleFile() throws Exception {

		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/multipartfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithSingleFileNotPresent() {
		testClient.post().uri("/multipartfile")
				.exchange()
				.expectStatus().isFound();
	}

	@Test
	public void multipartRequestWithFileArray() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/multipartfilearray")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithFileArrayNoMultipart() {
		testClient.post().uri("/multipartfilearray")
				.exchange()
				.expectStatus().isFound();
	}

	@Test
	public void multipartRequestWithOptionalFile() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/optionalfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithOptionalFileNotPresent() throws Exception {
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/optionalfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithOptionalFileArray() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/optionalfilearray")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithOptionalFileArrayNotPresent() throws Exception {
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/optionalfilearray")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithOptionalFileList() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/optionalfilelist")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithOptionalFileListNotPresent() throws Exception {
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/optionalfilelist")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWithServletParts() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("file", fileContent).filename("orig");
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		EntityExchangeResult<Void> exchangeResult = testClient.post().uri("/multipartfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", json));
	}

	@Test
	public void multipartRequestWrapped() throws Exception {
		Map<String, String> json = Collections.singletonMap("name", "yeeeah");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

		WebTestClient client = MockMvcWebTestClient.bindToController(new MultipartController())
				.filter(new RequestWrappingFilter())
				.build();

		EntityExchangeResult<Void> exchangeResult = client.post().uri("/multipartfile")
				.bodyValue(bodyBuilder.build())
				.exchange()
				.expectStatus().isFound()
				.expectBody().isEmpty();

		// Further assertions on the server response
		MockMvcWebTestClient.resultActionsFor(exchangeResult)
				.andExpect(model().attribute("jsonContent", json));
	}


	@Controller
	private static class MultipartController {

		@RequestMapping(value = "/multipartfile", method = HttpMethod.POST)
		public String processMultipartFile(@RequestParam(required = false) MultipartFile file,
				@RequestPart(required = false) Map<String, String> json, Model model) throws IOException {

			if (file != null) {
				model.addAttribute("fileContent", file.getBytes());
			}
			if (json != null) {
				model.addAttribute("jsonContent", json);
			}

			return "redirect:/index";
		}

		@RequestMapping(value = "/multipartfilearray", method = HttpMethod.POST)
		public String processMultipartFileArray(@RequestParam(required = false) MultipartFile[] file,
				@RequestPart(required = false) Map<String, String> json, Model model) throws IOException {

			if (file != null && file.length > 0) {
				byte[] content = file[0].getBytes();
				assertThat(file[1].getBytes()).isEqualTo(content);
				model.addAttribute("fileContent", content);
			}
			if (json != null) {
				model.addAttribute("jsonContent", json);
			}

			return "redirect:/index";
		}

		@RequestMapping(value = "/multipartfilelist", method = HttpMethod.POST)
		public String processMultipartFileList(@RequestParam(required = false) List<MultipartFile> file,
				@RequestPart(required = false) Map<String, String> json, Model model) throws IOException {

			if (file != null && !file.isEmpty()) {
				byte[] content = file.get(0).getBytes();
				assertThat(file.get(1).getBytes()).isEqualTo(content);
				model.addAttribute("fileContent", content);
			}
			if (json != null) {
				model.addAttribute("jsonContent", json);
			}

			return "redirect:/index";
		}

		@RequestMapping(value = "/optionalfile", method = HttpMethod.POST)
		public String processOptionalFile(@RequestParam Optional<MultipartFile> file,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			if (file.isPresent()) {
				model.addAttribute("fileContent", file.get().getBytes());
			}
			model.addAttribute("jsonContent", json);

			return "redirect:/index";
		}

		@RequestMapping(value = "/optionalfilearray", method = HttpMethod.POST)
		public String processOptionalFileArray(@RequestParam Optional<MultipartFile[]> file,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			if (file.isPresent()) {
				byte[] content = file.get()[0].getBytes();
				assertThat(file.get()[1].getBytes()).isEqualTo(content);
				model.addAttribute("fileContent", content);
			}
			model.addAttribute("jsonContent", json);

			return "redirect:/index";
		}

		@RequestMapping(value = "/optionalfilelist", method = HttpMethod.POST)
		public String processOptionalFileList(@RequestParam Optional<List<MultipartFile>> file,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			if (file.isPresent()) {
				byte[] content = file.get().get(0).getBytes();
				assertThat(file.get().get(1).getBytes()).isEqualTo(content);
				model.addAttribute("fileContent", content);
			}
			model.addAttribute("jsonContent", json);

			return "redirect:/index";
		}

		@RequestMapping(value = "/part", method = HttpMethod.POST)
		public String processPart(@RequestParam Part part,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			model.addAttribute("fileContent", part.getInputStream());
			model.addAttribute("jsonContent", json);

			return "redirect:/index";
		}

		@RequestMapping(value = "/json", method = HttpMethod.POST)
		public String processMultipart(@RequestPart Map<String, String> json, Model model) {
			model.addAttribute("json", json);
			return "redirect:/index";
		}
	}


	private static class RequestWrappingFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws IOException, ServletException {

			request = new HttpServletRequestWrapper(request);
			filterChain.doFilter(request, response);
		}
	}

}
