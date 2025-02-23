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

package org.springframework.cloud.gateway.filter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.event.EnableBodyCachingEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * 缓存请求body的全局过滤器
 * AdaptCachedBodyGlobalFilter在全局过滤器中优先级仅次于RemoveCachedBodyFilter
 */
public class AdaptCachedBodyGlobalFilter implements GlobalFilter, Ordered, ApplicationListener<EnableBodyCachingEvent> {

	private ConcurrentMap<String, Boolean> routesToCache = new ConcurrentHashMap<>();

	/**
	 * 支持监听EnableBodyCachingEvent,接收到该事件则对应的路由的请求body则会启用缓存
	 * @param event
	 */
	@Override
	public void onApplicationEvent(EnableBodyCachingEvent event) {
		this.routesToCache.putIfAbsent(event.getRouteId(), true);
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// the cached ServerHttpRequest is used when the ServerWebExchange can not be
		// mutated, for example, during a predicate where the body is read, but still
		// needs to be cached.
		ServerHttpRequest cachedRequest = exchange.getAttributeOrDefault(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR,
				null);
		if (cachedRequest != null) {
			exchange.getAttributes().remove(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
			return chain.filter(exchange.mutate().request(cachedRequest).build());
		}

		//
		DataBuffer body = exchange.getAttributeOrDefault(CACHED_REQUEST_BODY_ATTR, null);
		Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);

		// body 不为空的话，说明之前已经缓存
		if (body != null || !this.routesToCache.containsKey(route.getId())) {
			return chain.filter(exchange);
		}

		// 此处是缓存过滤器的核心，在此工具方法中会将缓存存入网关上下文之中
		// 将请求正文缓存在ServerWebExchange属性中。
		// 该属性为{@link CACHED_REQUEST_BODY_ATTR}。
		// 如果从无法变更ServerWebExchange的位置（例如谓词）调用此方法，
		// 则将cacheDecoratedRequest设置为true会将{@link ServerHttpRequestDecorator}放在属性{@link CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR}中，以备后用。
		return ServerWebExchangeUtils.cacheRequestBody(exchange, (serverHttpRequest) -> {
			// don't mutate and build if same request object
			if (serverHttpRequest == exchange.getRequest()) {
				return chain.filter(exchange);
			}
			return chain.filter(exchange.mutate().request(serverHttpRequest).build());
		});
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1000;
	}

}
