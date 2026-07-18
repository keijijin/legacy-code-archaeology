package com.legacy.archaeology.presentation.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Review UI のエントリポイント。ディレクトリアクセスを index.html へ誘導する。 */
@Controller
public class ReviewUiController {

    @GetMapping({"/review", "/review/"})
    public String reviewIndex() {
        return "forward:/review/index.html";
    }
}
