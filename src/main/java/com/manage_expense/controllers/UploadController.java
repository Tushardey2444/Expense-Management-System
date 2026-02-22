package com.manage_expense.controllers;

import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.services.services_template.CloudinaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/test/api/media")
@Slf4j
public class UploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam MultipartFile file) {
        ApiResponse apiResponse = ApiResponse.builder()
                .message("File uploaded successfully")
                .success(true)
                .status(HttpStatus.OK)
                .build();
        try {
            log.info("Blank Profile Picture URL {}",cloudinaryService.upload(file));
            return ResponseEntity.ok(apiResponse);
        } catch (IOException e) {
            apiResponse.setMessage("failed to upload file: " + e.getMessage());
            apiResponse.setSuccess(false);
            apiResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }
}
