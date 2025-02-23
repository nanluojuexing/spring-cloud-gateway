/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.gateway.event;

import org.springframework.context.ApplicationEvent;

/**
 * 路由刷新结果事件
 * 由缓存路由定位CachingRouteLocator处理刷新路由事件时触发的结果事件
 *
 * @author alvin
 */
public class RefreshRoutesResultEvent extends ApplicationEvent {

	private Throwable throwable;

	public RefreshRoutesResultEvent(Object source, Throwable throwable) {
		super(source);
		this.throwable = throwable;
	}

	public RefreshRoutesResultEvent(Object source) {
		super(source);
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public boolean isSuccess() {
		return throwable == null;
	}

}
