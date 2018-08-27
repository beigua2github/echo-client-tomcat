package com.spring.app.mvc;

import com.spring.app.mvc.service.EchoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/")
public class HelloController {

    @Autowired
    private EchoService echoService;


    @RequestMapping(method = RequestMethod.GET)
    public String printWelcome(ModelMap model) {
        String kobe = echoService.hello("kobe");
        model.addAttribute("message", kobe);
        return "hello";
    }
}