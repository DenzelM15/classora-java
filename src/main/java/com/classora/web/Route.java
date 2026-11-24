package com.classora.web;

import java.util.List;

public record Route(String method, List<String> segments, Access access, Handler handler) {
}
