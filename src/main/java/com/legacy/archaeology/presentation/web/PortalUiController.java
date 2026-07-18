package com.legacy.archaeology.presentation.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Web UI エントリポイント。API と同一プロセスで静的 UI を提供する。 */
@Controller
public class PortalUiController {

    @GetMapping("/")
    public String portal() {
        return "forward:/index.html";
    }

    @GetMapping({"/impact", "/impact/"})
    public String impact() {
        return "forward:/impact/index.html";
    }

    @GetMapping({"/review", "/review/"})
    public String review() {
        return "forward:/review/index.html";
    }
}
