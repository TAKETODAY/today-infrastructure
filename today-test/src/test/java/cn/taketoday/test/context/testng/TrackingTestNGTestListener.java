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

package cn.taketoday.test.context.testng;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Simple {@link ITestListener} which tracks how many times certain TestNG
 * callback methods were called: only intended for the integration test suite.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class TrackingTestNGTestListener implements ITestListener {

	public int testStartCount = 0;

	public int testSuccessCount = 0;

	public int testFailureCount = 0;

	public int failedConfigurationsCount = 0;


	@Override
	public void onFinish(ITestContext testContext) {
		this.failedConfigurationsCount += testContext.getFailedConfigurations().size();
	}

	@Override
	public void onStart(ITestContext testContext) {
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult testResult) {
	}

	@Override
	public void onTestFailure(ITestResult testResult) {
		this.testFailureCount++;
	}

	@Override
	public void onTestSkipped(ITestResult testResult) {
	}

	@Override
	public void onTestStart(ITestResult testResult) {
		this.testStartCount++;
	}

	@Override
	public void onTestSuccess(ITestResult testResult) {
		this.testSuccessCount++;
	}

}
