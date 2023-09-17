/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultLaunchScript}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Justin Rosenberg
 */
class DefaultLaunchScriptTests {

	@TempDir
	File tempDir;

	@Test
	void loadsDefaultScript() throws Exception {
		DefaultLaunchScript script = new DefaultLaunchScript(null, null);
		String content = new String(script.toByteArray());
		assertThat(content).contains("Infra Application Startup Script");
	}

	@Test
	void logFilenameCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("logFilename");
	}

	@Test
	void pidFilenameCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("pidFilename");
	}

	@Test
	void initInfoProvidesCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoProvides");
	}

	@Test
	void initInfoRequiredStartCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoRequiredStart");
	}

	@Test
	void initInfoRequiredStopCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoRequiredStop");
	}

	@Test
	void initInfoDefaultStartCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoDefaultStart");
	}

	@Test
	void initInfoDefaultStopCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoDefaultStop");
	}

	@Test
	void initInfoShortDescriptionCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoShortDescription");
	}

	@Test
	void initInfoDescriptionCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoDescription");
	}

	@Test
	void initInfoChkconfigCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoChkconfig");
	}

	@Test
	void modeCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("mode");
	}

	@Test
	void useStartStopDaemonCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("useStartStopDaemon");
	}

	@Test
	void logFolderCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("logFolder");
	}

	@Test
	void pidFolderCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("pidFolder");
	}

	@Test
	void confFolderCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("confFolder");
	}

	@Test
	void stopWaitTimeCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("stopWaitTime");
	}

	@Test
	void inlinedConfScriptFileLoad() throws IOException {
		DefaultLaunchScript script = new DefaultLaunchScript(null,
				createProperties("inlinedConfScript:src/test/resources/example.script"));
		String content = new String(script.toByteArray());
		assertThat(content).contains("FOO=BAR");
	}

	@Test
	void defaultForUseStartStopDaemonIsTrue() throws Exception {
		DefaultLaunchScript script = new DefaultLaunchScript(null, null);
		String content = new String(script.toByteArray());
		assertThat(content).contains("USE_START_STOP_DAEMON=\"true\"");
	}

	@Test
	void defaultForModeIsAuto() throws Exception {
		DefaultLaunchScript script = new DefaultLaunchScript(null, null);
		String content = new String(script.toByteArray());
		assertThat(content).contains("MODE=\"auto\"");
	}

	@Test
	void defaultForStopWaitTimeIs60() throws Exception {
		DefaultLaunchScript script = new DefaultLaunchScript(null, null);
		String content = new String(script.toByteArray());
		assertThat(content).contains("STOP_WAIT_TIME=\"60\"");
	}

	@Test
	void loadFromFile() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("ABC".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, null);
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("ABC");
	}

	@Test
	void expandVariables() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("h{{a}}ll{{b}}".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, createProperties("a:e", "b:o"));
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("hello");
	}

	@Test
	void expandVariablesMultiLine() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("h{{a}}l\nl{{b}}".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, createProperties("a:e", "b:o"));
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("hel\nlo");
	}

	@Test
	void expandVariablesWithDefaults() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("h{{a:e}}ll{{b:o}}".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, null);
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("hello");
	}

	@Test
	void expandVariablesCanDefaultToBlank() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("s{{p:}}{{r:}}ing".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, null);
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("sing");
	}

	@Test
	void expandVariablesWithDefaultsOverride() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("h{{a:e}}ll{{b:o}}".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, createProperties("a:a"));
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("hallo");
	}

	@Test
	void expandVariablesMissingAreUnchanged() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("h{{a}}ll{{b}}".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, null);
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("h{{a}}ll{{b}}");
	}

	private void assertThatPlaceholderCanBeReplaced(String placeholder) throws Exception {
		DefaultLaunchScript script = new DefaultLaunchScript(null, createProperties(placeholder + ":__test__"));
		String content = new String(script.toByteArray());
		assertThat(content).contains("__test__");
	}

	private Map<?, ?> createProperties(String... pairs) {
		Map<Object, Object> properties = new HashMap<>();
		for (String pair : pairs) {
			String[] keyValue = pair.split(":");
			properties.put(keyValue[0], keyValue[1]);
		}
		return properties;
	}

}
