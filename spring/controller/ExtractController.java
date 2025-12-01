package com.COMPANY_NAME_REMOVED.cv_extr_task.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.COMPANY_NAME_REMOVED.cv_extr_task.service.ExtractService;

@RestController
@RequestMapping("/cv")
public class ExtractController {

        private final ExtractService extractService;

        public ExtractController(ExtractService extractService) {
            this.extractService = extractService;
        }

        //Use Google Gemini for real results
        @PostMapping("/extract/gemini")
        public ResponseEntity<Map<String, Object>> extractWithGemini(
            @RequestParam("cv") MultipartFile file,
            @RequestParam(defaultValue = "text") String mode
        ) {
            return extractService.extractWithGemini(file, mode);
        }
}
