package com.COMPANY_NAME_REMOVED.cv_extr_task.service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

        private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

        private Client client;

        @Value("${gemini.api.key}")
        private String apiKey;

        @PostConstruct
        public void init() {
                if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalStateException("Gemini API key is not configured!");
                }
                this.client = Client.builder()
                        .apiKey(apiKey)
                        .build();
        }

        //Generate response, pass pdf file to model in text form (uses flash as it is sufficient, but it was not available most of the time so I changed the model to the pro variant)
        public String generate(String prompt) {
                logger.info("Sending Gemini API request for extraction");
                GenerateContentResponse response = client.models.generateContent(
                        "gemini-2.5-pro",
                        prompt,
                        null
                );
                return response.text();
        }

        //Generate response, pass pdf file to model directly (limited to the pro model)
        public String generateFromFile(String base64Pdf, String prompt) {
                logger.info("Sending Gemini API request for extraction with PDF");
                
               byte[] pdfBytes = java.util.Base64.getDecoder().decode(base64Pdf);

                // Create parts
                Part pdfPart = Part.fromBytes(pdfBytes, "application/pdf");
                Part textPart = Part.fromText(prompt);

                // Build content
                Content content = Content.builder()
                        .role("user")
                        .parts(Arrays.asList(pdfPart, textPart))
                        .build();

                // Call the Gemini API
                GenerateContentResponse response = client.models.generateContent(
                        "gemini-2.5-pro",
                        content,
                        null
                );
                return response.text();
        }

        //Validation call (prompt as param with text only, used flash but it was not available most of the time so I changed the model to the pro variant)
         public String validate(String prompt) {
                logger.info("Sending Gemini API validation request");
                GenerateContentResponse response = client.models.generateContent(
                        "gemini-2.5-pro",
                        prompt,
                        null
                );
                return response.text();
        }
}
