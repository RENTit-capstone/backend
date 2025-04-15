package com.capstone.rentit.login.dto;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class JwtToken {
    private String grantType;
    private String accessToken;
    private String refreshToken;
}
