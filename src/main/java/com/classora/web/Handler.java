package com.classora.web;

@FunctionalInterface
public interface Handler {
    ApiResponse handle(RequestContext ctx);
}
