package com.plr.aduaja.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    @ModelAttribute("devMode")
    public boolean isDevMode() {
        return devMode;
    }
}
