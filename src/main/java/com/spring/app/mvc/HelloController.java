package com.spring.app.mvc;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
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


    @RequestMapping(value = "up", method = RequestMethod.GET)
    public String up(ModelMap model) {
        ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        model.addAttribute("message", "up success");
        return "hello";
    }

    @RequestMapping(value = "down", method = RequestMethod.GET)
    public String down(ModelMap model) {
        ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
        model.addAttribute("message", "down success");
        return "hello";
    }
}