package com.COMPANY_NAME_REMOVED.cv_extr_task.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ExtractService {

    private final GeminiService geminiService;
    private static final Logger logger = LoggerFactory.getLogger(ExtractService.class);

    public ExtractService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    public ResponseEntity<Map<String, Object>> extractWithGemini(
        @RequestParam("cv") MultipartFile file,
        @RequestParam(defaultValue = "text") String mode
    ) {
        return runExtraction(file, mode);
    }

    /*
    The following function handles the text extraction logic from the pdf, handing it over to the chosen llm (locally used ollama for testing and used gemini for the final solution).
    Had to be really specific in some parts of the prompt as the AI might exclude certain keywords that are not directly mentioned, but a human would associate to them.
    */
    private ResponseEntity<Map<String, Object>> runExtraction(MultipartFile file,  String mode) {
        logger.info("Beginning PDF extraction.");
        try {
            String llmResponse;
            String prompt = """
                Your task is to act as an expert HR analyst and extract the following fields from the CV (curriculum vitae) document provided:
                Follow these rules precisely:

                For the "Work experience" identify relevant experience. Only consider roles listed under sections like "Work Experience," "Employment History," or "Career." Exclude sections like "Education," "Volunteer Work," and "Internships" unless a role is explicitly a long-term, professional position.
                Handle dates, for any role with an end date of "Present," "Current," or similar, use today's date, September 15, 2025, as the end date.
                Calculate the duration for each individual role.
                Handle Overlaps, if there are overlapping employment periods, the time must only be counted once. Calculate the total duration from the start of the earliest job to the end of the latest job in any continuous or overlapping block of time.
                Final Calculation: Sum the durations of all distinct, non-overlapping time blocks.

                For the "Skills", extract skills from across the whole text document. 
                If the candidate had experience in certain technologies, include the collective word as well (if certain technologies fall under multiple categories include all of them). 

                For the "Languages", identify the languages spoken by the candidate, most often found under a distinct languages section. Don't include the level.

                For the "Profile", if the original document contains a "profile" portion, use the text found there, otherwise summarise the candidate's CV in a short text. 
                Response must be a valid JSON. Only respond with the JSON in a valid format. (no explanations, no text outside the valid JSON structure!):

                {
                  "work_experience": "<number of years as a whole number>",
                  "skills": ["<skill1>", "<skill2>", "..."],
                  "languages": ["<language1>", "<language2>", "..."],
                  "profile": "<short text>"
                }
                """;

           if (mode.equals("pdf")) {
                // Gemini with PDF input
                byte[] pdfBytes = file.getBytes();
                String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
                llmResponse = geminiService.generateFromFile(base64Pdf, prompt);
            } else {
                // Gemini with text input
                String pdfText = extractTextFromPdf(file);
                String fullPrompt = prompt + "\n\nCV text:\n" + pdfText;
                llmResponse = geminiService.generate(fullPrompt);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode extractedJson;

            try {
                // Remove markdown formatting if present
                String cleaned = llmResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

                extractedJson = mapper.readTree(cleaned);

            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to parse LLM JSON",
                    "rawResponse", llmResponse
                ));
            }

            Map<String, Object> result = validateExtractedFields(extractedJson);
            logger.info("PDF extraction finished.");
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to process PDF"));
        }
    }

    //Extract the conents of the pdf to handle as text
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(document);
        }
    }

    //Field validation (added ai checks after testing system performance, decided to exclude ollama versions)
    Map<String, Object> validateExtractedFields(JsonNode extractedJson) {
        Map<String, Object> validations = new HashMap<>();

        //#1 work experience: Checking if value between 0 and 2
        int workExp = extractedJson.path("work_experience").asInt(-1);
        validations.put("work_experience_valid", workExp >= 0 && workExp <= 2);

        //#2 skills: 
        boolean skillsValid;
        //Checking with java first before using extra resources
        List<String> skills = new ArrayList<>();
        extractedJson.path("skills").forEach(s -> skills.add(s.asText().toLowerCase()));
        skillsValid = skills.contains("java") && skills.contains("llm");
        
        //If manual validation failed, attempt ai based validation
        if ( skillsValid == false) {
            String skillsValidCheck = geminiService.validate(
            "We are specifically looking for both Java and LLM in a candidate's skills, whether have experience in these fields,  answer true/false. Skills: " 
            + extractedJson.path("skills").toString()
        );
            skillsValid = skillsValidCheck.toLowerCase().contains("true");
            
        }
        validations.put("skills_valid", skillsValid);
        
        //#3 languages: Looking for direct matches
        List<String> langs = new ArrayList<>();
        extractedJson.path("languages").forEach(l -> langs.add(l.asText().toLowerCase()));
        validations.put("languages_valid", langs.contains("hungarian") && langs.contains("english"));

        //#4 profile: 
        boolean profileValid;
        //Checking with java first before using extra resources
        String profile = extractedJson.path("profile").asText("").toLowerCase();
        profileValid = profile.contains("genai") && profile.contains("java");
       
        //If manual validation failed, attempt ai based validation
        if (profileValid == false) {
             String profileValidCheck = geminiService.validate(
                "Does this profile suggest that the candidate is interested in GenAI and Java, and that they are willing to learn more about these? Answer true/false. Profile: "
                + extractedJson.path("profile").asText("")
            );
            profileValid = profileValidCheck.toLowerCase().contains("true");
        }
        validations.put("profile_valid", profileValid);

        return Map.of(
            "extracted", extractedJson,
            "validation", validations
        );
    }
}
 