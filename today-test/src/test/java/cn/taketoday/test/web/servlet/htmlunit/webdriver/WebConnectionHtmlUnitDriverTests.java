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

package cn.taketoday.test.web.servlet.htmlunit.webdriver;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openqa.selenium.WebDriverException;

import java.io.IOException;

import cn.taketoday.test.web.servlet.htmlunit.webdriver.WebConnectionHtmlUnitDriver;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link WebConnectionHtmlUnitDriver}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class WebConnectionHtmlUnitDriverTests {

	private final WebConnectionHtmlUnitDriver driver = new WebConnectionHtmlUnitDriver();

	@Mock
	private WebConnection connection;

	@BeforeEach
	void setup() throws Exception {
		given(this.connection.getResponse(any(WebRequest.class))).willThrow(new IOException(""));
	}


	@Test
	void getWebConnectionDefaultNotNull() {
		assertThat(this.driver.getWebConnection()).isNotNull();
	}

	@Test
	void setWebConnectionToNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.driver.setWebConnection(null));
	}

	@Test
	public void setWebConnection() {
		this.driver.setWebConnection(this.connection);
		assertThat(this.driver.getWebConnection()).isEqualTo(this.connection);
		assertThatExceptionOfType(WebDriverException.class).isThrownBy(() -> this.driver.get("https://example.com"));
	}

}
