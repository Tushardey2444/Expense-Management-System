package com.manage_expense.services.services_template;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public interface CloudinaryService {
    String upload(MultipartFile multipartFile) throws IOException;
    Map<?,?> uploadAsMap(MultipartFile multipartFile) throws IOException;
}
