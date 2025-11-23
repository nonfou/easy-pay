package com.mapy.bff.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MenuItemDTO {
    String id;
    String title;
    String icon;
    String href;
    String openType;
    Integer type;
    List<MenuItemDTO> children;
}
