package com.capstone.rentit.inquiry.dto;

import jakarta.validation.constraints.NotEmpty;

public record InquiryAnswerForm(
        @NotEmpty String answer
) { }
