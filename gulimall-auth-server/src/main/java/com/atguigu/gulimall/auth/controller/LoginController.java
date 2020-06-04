package com.atguigu.gulimall.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * @author admin
 */
@Controller
public class LoginController {
    @GetMapping("test")
    @ResponseBody
    public String login(HttpSession httpSession) {
        httpSession.setAttribute("sssss", "111" +
                "1");
        System.out.println(httpSession.getAttribute("sssss"));
        return "login";
    }
//    @GetMapping("login.html")
//    public String login() {
//        return "login";
//    }
//
//    @GetMapping("reg.html")
//    public String reg() {
//        return "reg";
//    }
}
