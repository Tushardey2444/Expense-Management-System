package com.manage_expense.dtos.dto_requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.manage_expense.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
public class UserProfileRequest {

    @Pattern(regexp = "^[a-zA-Z]+$", message = "Please enter valid first name!!")
    private String firstName;

    @Pattern(regexp = "^[a-zA-Z]+$", message = "Please enter valid first name!!")
    private String lastName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dateOfBirth;

    private Gender gender;
}
