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

package cn.taketoday.test.web.servlet.samples.standalone;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.MediaType;
import cn.taketoday.mock.web.MockMultipartFile;
import cn.taketoday.mock.web.MockPart;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.ui.Model;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.RequestMethod;
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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.post;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.model;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class MultipartControllerTests {

	@Test
	public void multipartRequestWithSingleFile() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfile").file(filePart).file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithSingleFileNotPresent() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfile"))
				.andExpect(status().isFound());
	}

	@Test
	public void multipartRequestWithFileArray() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart1 = new MockMultipartFile("file", "orig", null, fileContent);
		MockMultipartFile filePart2 = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfilearray").file(filePart1).file(filePart2).file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithFileArrayNotPresent() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfilearray"))
				.andExpect(status().isFound());
	}

	@Test
	public void multipartRequestWithFileArrayNoMultipart() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(post("/multipartfilearray"))
				.andExpect(status().isFound());
	}

	@Test
	public void multipartRequestWithFileList() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart1 = new MockMultipartFile("file", "orig", null, fileContent);
		MockMultipartFile filePart2 = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfilelist").file(filePart1).file(filePart2).file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithFileListNotPresent() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfilelist"))
				.andExpect(status().isFound());
	}

	@Test
	public void multipartRequestWithFileListNoMultipart() throws Exception {
		standaloneSetup(new MultipartController()).build()
				.perform(post("/multipartfilelist"))
				.andExpect(status().isFound());
	}

	@Test
	public void multipartRequestWithOptionalFile() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfile").file(filePart).file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithOptionalFileNotPresent() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfile").file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithOptionalFileArray() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart1 = new MockMultipartFile("file", "orig", null, fileContent);
		MockMultipartFile filePart2 = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfilearray").file(filePart1).file(filePart2).file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithOptionalFileArrayNotPresent() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfilearray").file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithOptionalFileList() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile filePart1 = new MockMultipartFile("file", "orig", null, fileContent);
		MockMultipartFile filePart2 = new MockMultipartFile("file", "orig", null, fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfilelist").file(filePart1).file(filePart2).file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithOptionalFileListNotPresent() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/optionalfilelist").file(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attributeDoesNotExist("fileContent"))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test
	public void multipartRequestWithServletParts() throws Exception {
		byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
		MockPart filePart = new MockPart("file", "orig", fileContent);

		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockPart jsonPart = new MockPart("json", json);
		jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		standaloneSetup(new MultipartController()).build()
				.perform(multipart("/multipartfile").part(filePart).part(jsonPart))
				.andExpect(status().isFound())
				.andExpect(model().attribute("fileContent", fileContent))
				.andExpect(model().attribute("jsonContent", Collections.singletonMap("name", "yeeeah")));
	}

	@Test  // SPR-13317
	public void multipartRequestWrapped() throws Exception {
		byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

		Filter filter = new RequestWrappingFilter();
		MockMvc mockMvc = standaloneSetup(new MultipartController()).addFilter(filter).build();

		Map<String, String> jsonMap = Collections.singletonMap("name", "yeeeah");
		mockMvc.perform(multipart("/json").file(jsonPart)).andExpect(model().attribute("json", jsonMap));
	}


	@Controller
	private static class MultipartController {

		@RequestMapping(value = "/multipartfile", method = RequestMethod.POST)
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

		@RequestMapping(value = "/multipartfilearray", method = RequestMethod.POST)
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

		@RequestMapping(value = "/multipartfilelist", method = RequestMethod.POST)
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

		@RequestMapping(value = "/optionalfile", method = RequestMethod.POST)
		public String processOptionalFile(@RequestParam Optional<MultipartFile> file,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			if (file.isPresent()) {
				model.addAttribute("fileContent", file.get().getBytes());
			}
			model.addAttribute("jsonContent", json);

			return "redirect:/index";
		}

		@RequestMapping(value = "/optionalfilearray", method = RequestMethod.POST)
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

		@RequestMapping(value = "/optionalfilelist", method = RequestMethod.POST)
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

		@RequestMapping(value = "/part", method = RequestMethod.POST)
		public String processPart(@RequestParam Part part,
				@RequestPart Map<String, String> json, Model model) throws IOException {

			model.addAttribute("fileContent", part.getInputStream());
			model.addAttribute("jsonContent", json);

			return "redirect:/index";
		}

		@RequestMapping(value = "/json", method = RequestMethod.POST)
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
