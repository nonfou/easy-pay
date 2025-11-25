package com.github.nonfou.mpay.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManualSettleRequest {

    @NotBlank(message = "补单原因不能为空")
    private String remark;
}
