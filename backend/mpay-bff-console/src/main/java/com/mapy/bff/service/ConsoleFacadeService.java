package com.mapy.bff.service;

import com.mapy.bff.dto.DashboardMetricsDTO;
import com.mapy.bff.dto.MenuItemDTO;
import java.util.List;

public interface ConsoleFacadeService {

    List<MenuItemDTO> fetchMenu(Long userId);

    DashboardMetricsDTO getDashboardMetrics(Long userId);
}
