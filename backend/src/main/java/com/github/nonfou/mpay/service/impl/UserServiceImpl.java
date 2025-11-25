package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.common.response.PageResponse;
import com.github.nonfou.mpay.dto.user.UserDTO;
import com.github.nonfou.mpay.entity.MerchantEntity;
import com.github.nonfou.mpay.repository.MerchantRepository;
import com.github.nonfou.mpay.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Map<Integer, String> ROLE_NAMES = Map.of(
            0, "普通用户",
            1, "管理员"
    );

    private static final Map<Integer, String> STATE_NAMES = Map.of(
            0, "禁用",
            1, "启用"
    );

    private final MerchantRepository merchantRepository;

    public UserServiceImpl(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Override
    public PageResponse<UserDTO> listUsers(Integer role, Integer state, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize);
        Page<MerchantEntity> result = merchantRepository.findByRoleAndState(role, state, pageRequest);

        List<UserDTO> users = result.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResponse.of(page, pageSize, result.getTotalElements(), users);
    }

    @Override
    public Optional<UserDTO> getUserByPid(Long pid) {
        return merchantRepository.findByPid(pid).map(this::toDTO);
    }

    @Override
    @Transactional
    public void updateRole(Long pid, Integer role) {
        if (role < 0 || role > 1) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "无效的角色值: " + role);
        }

        MerchantEntity entity = merchantRepository.findByPid(pid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在: " + pid));

        entity.setRole(role);
        merchantRepository.save(entity);
    }

    @Override
    @Transactional
    public void updateState(Long pid, Integer state) {
        if (state < 0 || state > 1) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "无效的状态值: " + state);
        }

        MerchantEntity entity = merchantRepository.findByPid(pid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在: " + pid));

        entity.setState(state);
        merchantRepository.save(entity);
    }

    @Override
    public boolean isAdmin(Long pid) {
        return merchantRepository.findByPid(pid)
                .map(MerchantEntity::isAdmin)
                .orElse(false);
    }

    private UserDTO toDTO(MerchantEntity entity) {
        return UserDTO.builder()
                .id(entity.getId())
                .pid(entity.getPid())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .role(entity.getRole())
                .roleName(ROLE_NAMES.getOrDefault(entity.getRole(), "未知"))
                .state(entity.getState())
                .stateName(STATE_NAMES.getOrDefault(entity.getState(), "未知"))
                .build();
    }
}
