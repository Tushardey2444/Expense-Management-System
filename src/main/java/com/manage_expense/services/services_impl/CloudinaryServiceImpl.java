package com.manage_expense.services.services_impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.manage_expense.config.AppConstants;
import com.manage_expense.services.services_template.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    @Value("${cloudinary.folder.upload}")
    private String folder;

    @Autowired
    public Cloudinary cloudinary;

    @Override
    public String upload(MultipartFile multipartFile) throws IOException {
        Map<?,?> uploadResult = uploadAsMap(multipartFile);
        return uploadResult.get(AppConstants.SECURE_URL).toString();
    }

    @Override
    public Map<?, ?> uploadAsMap(MultipartFile multipartFile) throws IOException {
        Map<String,Object> options = ObjectUtils.asMap("resource_type", "auto", "folder", folder == null ? "" : this.folder);
        return cloudinary.uploader().upload(multipartFile.getBytes(), options);
    }
}
