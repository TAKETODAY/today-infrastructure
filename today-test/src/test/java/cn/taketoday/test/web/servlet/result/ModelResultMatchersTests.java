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

package cn.taketoday.test.web.servlet.result;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.test.web.servlet.StubMvcResult;
import cn.taketoday.validation.BeanPropertyBindingResult;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.web.servlet.ModelAndView;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/**
 * Unit tests for {@link ModelResultMatchers}.
 *
 * @author Craig Walls
 * @author Sam Brannen
 */
class ModelResultMatchersTests {

	private final ModelResultMatchers matchers = new ModelResultMatchers();
	private MvcResult mvcResult;
	private MvcResult mvcResultWithError;


	@BeforeEach
	void setUp() throws Exception {
		ModelAndView mav = new ModelAndView("view", "good", "good");
		BindingResult bindingResult = new BeanPropertyBindingResult("good", "good");
		mav.addObject(BindingResult.MODEL_KEY_PREFIX + "good", bindingResult);

		this.mvcResult = getMvcResult(mav);

		Date date = new Date();
		BindingResult bindingResultWithError = new BeanPropertyBindingResult(date, "date");
		bindingResultWithError.rejectValue("time", "error");

		ModelAndView mavWithError = new ModelAndView("view", "good", "good");
		mavWithError.addObject("date", date);
		mavWithError.addObject(BindingResult.MODEL_KEY_PREFIX + "date", bindingResultWithError);

		this.mvcResultWithError = getMvcResult(mavWithError);
	}

	@Test
	void attributeExists() throws Exception {
		this.matchers.attributeExists("good").match(this.mvcResult);
	}

	@Test
	void attributeExists_doesNotExist() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.matchers.attributeExists("bad").match(this.mvcResult));
	}

	@Test
	void attributeDoesNotExist() throws Exception {
		this.matchers.attributeDoesNotExist("bad").match(this.mvcResult);
	}

	@Test
	void attributeDoesNotExist_doesExist() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.matchers.attributeDoesNotExist("good").match(this.mvcResultWithError));
	}

	@Test
	void attribute_equal() throws Exception {
		this.matchers.attribute("good", is("good")).match(this.mvcResult);
	}

	@Test
	void attribute_notEqual() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.matchers.attribute("good", is("bad")).match(this.mvcResult));
	}

	@Test
	void hasNoErrors() throws Exception {
		this.matchers.hasNoErrors().match(this.mvcResult);
	}

	@Test
	void hasNoErrors_withErrors() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.matchers.hasNoErrors().match(this.mvcResultWithError));
	}

	@Test
	void attributeHasErrors() throws Exception {
		this.matchers.attributeHasErrors("date").match(this.mvcResultWithError);
	}

	@Test
	void attributeErrorCount() throws Exception {
		this.matchers.attributeErrorCount("date", 1).match(this.mvcResultWithError);
	}

	@Test
	void attributeErrorCount_withWrongErrorCount() throws Exception {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> this.matchers.attributeErrorCount("date", 2).match(this.mvcResultWithError))
			.withMessage("Binding/validation error count for attribute 'date', expected:<2> but was:<1>");
	}

	@Test
	void attributeHasErrors_withoutErrors() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.matchers.attributeHasErrors("good").match(this.mvcResultWithError));
	}

	@Test
	void attributeHasNoErrors() throws Exception {
		this.matchers.attributeHasNoErrors("good").match(this.mvcResult);
	}

	@Test
	void attributeHasNoErrors_withoutAttribute() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.matchers.attributeHasNoErrors("missing").match(this.mvcResultWithError));
	}

	@Test
	void attributeHasNoErrors_withErrors() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.matchers.attributeHasNoErrors("date").match(this.mvcResultWithError));
	}

	@Test
	void attributeHasFieldErrors() throws Exception {
		this.matchers.attributeHasFieldErrors("date", "time").match(this.mvcResultWithError);
	}

	@Test
	void attributeHasFieldErrors_withoutAttribute() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.matchers.attributeHasFieldErrors("missing", "bad").match(this.mvcResult));
	}

	@Test
	void attributeHasFieldErrors_withoutErrorsForAttribute() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.matchers.attributeHasFieldErrors("date", "time").match(this.mvcResult));
	}

	@Test
	void attributeHasFieldErrors_withoutErrorsForField() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.matchers.attributeHasFieldErrors("date", "good", "time").match(this.mvcResultWithError));
	}

	@Test
	void attributeHasFieldErrorCode() throws Exception {
		this.matchers.attributeHasFieldErrorCode("date", "time", "error").match(this.mvcResultWithError);
	}

	@Test
	void attributeHasFieldErrorCode_withoutErrorOnField() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.matchers.attributeHasFieldErrorCode("date", "time", "incorrectError").match(this.mvcResultWithError));
	}

	@Test
	void attributeHasFieldErrorCode_startsWith() throws Exception {
		this.matchers.attributeHasFieldErrorCode("date", "time", startsWith("err")).match(this.mvcResultWithError);
	}

	@Test
	void attributeHasFieldErrorCode_startsWith_withoutErrorOnField() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.matchers.attributeHasFieldErrorCode("date", "time", startsWith("inc")).match(this.mvcResultWithError));
	}

	private MvcResult getMvcResult(ModelAndView modelAndView) {
		return new StubMvcResult(null, null, null, null, modelAndView, null, null);
	}

}
