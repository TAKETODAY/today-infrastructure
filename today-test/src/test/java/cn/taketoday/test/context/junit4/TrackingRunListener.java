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

package cn.taketoday.test.context.junit4;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple {@link RunListener} which tracks how many times certain JUnit callback
 * methods were called: only intended for the integration test suite.
 *
 * @author Sam Brannen
 * @since 3.0
 */
public class TrackingRunListener extends RunListener {

	private final AtomicInteger testFailureCount = new AtomicInteger();

	private final AtomicInteger testStartedCount = new AtomicInteger();

	private final AtomicInteger testFinishedCount = new AtomicInteger();

	private final AtomicInteger testAssumptionFailureCount = new AtomicInteger();

	private final AtomicInteger testIgnoredCount = new AtomicInteger();


	public int getTestFailureCount() {
		return this.testFailureCount.get();
	}

	public int getTestStartedCount() {
		return this.testStartedCount.get();
	}

	public int getTestFinishedCount() {
		return this.testFinishedCount.get();
	}

	public int getTestAssumptionFailureCount() {
		return this.testAssumptionFailureCount.get();
	}

	public int getTestIgnoredCount() {
		return this.testIgnoredCount.get();
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		this.testFailureCount.incrementAndGet();
	}

	@Override
	public void testStarted(Description description) throws Exception {
		this.testStartedCount.incrementAndGet();
	}

	@Override
	public void testFinished(Description description) throws Exception {
		this.testFinishedCount.incrementAndGet();
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		this.testAssumptionFailureCount.incrementAndGet();
	}

	@Override
	public void testIgnored(Description description) throws Exception {
		this.testIgnoredCount.incrementAndGet();
	}

}
