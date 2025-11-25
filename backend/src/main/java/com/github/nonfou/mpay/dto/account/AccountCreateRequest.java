package com.github.nonfou.mpay.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

@Data
public class AccountCreateRequest {

    @NotBlank
    private String platform;

    @NotBlank
    private String account;

    @NotBlank
    private String password;

    @NotNull
    private Integer pattern;

    private Map<String, Object> params;
}
