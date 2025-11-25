package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.common.error.BusinessException;
import com.github.nonfou.mpay.common.error.ErrorCode;
import com.github.nonfou.mpay.common.response.PageResponse;
import com.github.nonfou.mpay.dto.user.UserDTO;
import com.github.nonfou.mpay.entity.MerchantEntity;
import com.github.nonfou.mpay.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试
 * 测试用户服务，包括用户列表、角色管理、状态管理
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 用户服务测试")
class UserServiceImplTest {

    @Mock
    private MerchantRepository merchantRepository;

    private UserServiceImpl userService;

    private static final Long TEST_PID = 1001L;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(merchantRepository);
    }

    @Nested
    @DisplayName("用户列表查询测试")
    class ListUsersTests {

        @Test
        @DisplayName("查询用户列表 - 成功返回分页结果")
        void listUsers_shouldReturnPageResponse_whenSuccess() {
            // Given
            MerchantEntity user1 = createMerchant(1L, 1001L, "user1", 0, 1);
            MerchantEntity user2 = createMerchant(2L, 1002L, "user2", 1, 1);
            Page<MerchantEntity> page = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 10), 2);

            when(merchantRepository.findByRoleAndState(null, null, PageRequest.of(0, 10)))
                    .thenReturn(page);

            // When
            PageResponse<UserDTO> result = userService.listUsers(null, null, 1, 10);

            // Then
            assertEquals(1, result.getPage());
            assertEquals(10, result.getPageSize());
            assertEquals(2, result.getTotal());
            assertEquals(2, result.getItems().size());
        }

        @Test
        @DisplayName("查询用户列表 - 按角色筛选")
        void listUsers_shouldFilterByRole_whenRoleSpecified() {
            // Given
            MerchantEntity admin = createMerchant(1L, 1001L, "admin", 1, 1);
            Page<MerchantEntity> page = new PageImpl<>(List.of(admin), PageRequest.of(0, 10), 1);

            when(merchantRepository.findByRoleAndState(eq(1), any(), any(PageRequest.class)))
                    .thenReturn(page);

            // When
            PageResponse<UserDTO> result = userService.listUsers(1, null, 1, 10);

            // Then
            assertEquals(1, result.getItems().size());
            assertEquals(1, result.getItems().get(0).getRole());
        }

        @Test
        @DisplayName("查询用户列表 - 按状态筛选")
        void listUsers_shouldFilterByState_whenStateSpecified() {
            // Given
            MerchantEntity enabledUser = createMerchant(1L, 1001L, "user", 0, 1);
            Page<MerchantEntity> page = new PageImpl<>(List.of(enabledUser), PageRequest.of(0, 10), 1);

            when(merchantRepository.findByRoleAndState(any(), eq(1), any(PageRequest.class)))
                    .thenReturn(page);

            // When
            PageResponse<UserDTO> result = userService.listUsers(null, 1, 1, 10);

            // Then
            assertEquals(1, result.getItems().size());
            assertEquals(1, result.getItems().get(0).getState());
        }

        @Test
        @DisplayName("查询用户列表 - DTO 正确转换")
        void listUsers_shouldMapToDTO_correctly() {
            // Given
            MerchantEntity user = createMerchant(1L, 1001L, "testuser", 1, 0);
            user.setEmail("test@example.com");
            Page<MerchantEntity> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);

            when(merchantRepository.findByRoleAndState(any(), any(), any(PageRequest.class)))
                    .thenReturn(page);

            // When
            PageResponse<UserDTO> result = userService.listUsers(null, null, 1, 10);

            // Then
            UserDTO dto = result.getItems().get(0);
            assertEquals(1L, dto.getId());
            assertEquals(1001L, dto.getPid());
            assertEquals("testuser", dto.getUsername());
            assertEquals("test@example.com", dto.getEmail());
            assertEquals(1, dto.getRole());
            assertEquals("管理员", dto.getRoleName());
            assertEquals(0, dto.getState());
            assertEquals("禁用", dto.getStateName());
        }

        @Test
        @DisplayName("查询用户列表 - 空结果")
        void listUsers_shouldReturnEmptyList_whenNoUsers() {
            // Given
            Page<MerchantEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

            when(merchantRepository.findByRoleAndState(any(), any(), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            // When
            PageResponse<UserDTO> result = userService.listUsers(null, null, 1, 10);

            // Then
            assertEquals(0, result.getTotal());
            assertTrue(result.getItems().isEmpty());
        }
    }

    @Nested
    @DisplayName("根据 PID 查询用户测试")
    class GetUserByPidTests {

        @Test
        @DisplayName("查询用户 - 成功返回用户信息")
        void getUserByPid_shouldReturnUser_whenExists() {
            // Given
            MerchantEntity user = createMerchant(1L, TEST_PID, "testuser", 0, 1);
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.of(user));

            // When
            Optional<UserDTO> result = userService.getUserByPid(TEST_PID);

            // Then
            assertTrue(result.isPresent());
            assertEquals(TEST_PID, result.get().getPid());
            assertEquals("testuser", result.get().getUsername());
        }

        @Test
        @DisplayName("查询用户 - 用户不存在返回空")
        void getUserByPid_shouldReturnEmpty_whenNotExists() {
            // Given
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.empty());

            // When
            Optional<UserDTO> result = userService.getUserByPid(TEST_PID);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("更新角色测试")
    class UpdateRoleTests {

        @Test
        @DisplayName("更新角色 - 成功设置为管理员")
        void updateRole_shouldSetAdmin_whenValid() {
            // Given
            MerchantEntity user = createMerchant(1L, TEST_PID, "user", 0, 1);
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.of(user));

            // When
            userService.updateRole(TEST_PID, 1);

            // Then
            ArgumentCaptor<MerchantEntity> captor = ArgumentCaptor.forClass(MerchantEntity.class);
            verify(merchantRepository).save(captor.capture());
            assertEquals(1, captor.getValue().getRole());
        }

        @Test
        @DisplayName("更新角色 - 成功设置为普通用户")
        void updateRole_shouldSetNormalUser_whenValid() {
            // Given
            MerchantEntity admin = createMerchant(1L, TEST_PID, "admin", 1, 1);
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.of(admin));

            // When
            userService.updateRole(TEST_PID, 0);

            // Then
            ArgumentCaptor<MerchantEntity> captor = ArgumentCaptor.forClass(MerchantEntity.class);
            verify(merchantRepository).save(captor.capture());
            assertEquals(0, captor.getValue().getRole());
        }

        @Test
        @DisplayName("更新角色 - 无效角色值抛出异常")
        void updateRole_shouldThrowException_whenInvalidRole() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateRole(TEST_PID, 2));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("无效的角色值"));
        }

        @Test
        @DisplayName("更新角色 - 负数角色值抛出异常")
        void updateRole_shouldThrowException_whenNegativeRole() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateRole(TEST_PID, -1));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        }

        @Test
        @DisplayName("更新角色 - 用户不存在抛出异常")
        void updateRole_shouldThrowException_whenUserNotFound() {
            // Given
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateRole(TEST_PID, 1));

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("用户不存在"));
        }
    }

    @Nested
    @DisplayName("更新状态测试")
    class UpdateStateTests {

        @Test
        @DisplayName("更新状态 - 成功启用用户")
        void updateState_shouldEnableUser_whenValid() {
            // Given
            MerchantEntity user = createMerchant(1L, TEST_PID, "user", 0, 0);
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.of(user));

            // When
            userService.updateState(TEST_PID, 1);

            // Then
            ArgumentCaptor<MerchantEntity> captor = ArgumentCaptor.forClass(MerchantEntity.class);
            verify(merchantRepository).save(captor.capture());
            assertEquals(1, captor.getValue().getState());
        }

        @Test
        @DisplayName("更新状态 - 成功禁用用户")
        void updateState_shouldDisableUser_whenValid() {
            // Given
            MerchantEntity user = createMerchant(1L, TEST_PID, "user", 0, 1);
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.of(user));

            // When
            userService.updateState(TEST_PID, 0);

            // Then
            ArgumentCaptor<MerchantEntity> captor = ArgumentCaptor.forClass(MerchantEntity.class);
            verify(merchantRepository).save(captor.capture());
            assertEquals(0, captor.getValue().getState());
        }

        @Test
        @DisplayName("更新状态 - 无效状态值抛出异常")
        void updateState_shouldThrowException_whenInvalidState() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateState(TEST_PID, 2));

            assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("无效的状态值"));
        }

        @Test
        @DisplayName("更新状态 - 用户不存在抛出异常")
        void updateState_shouldThrowException_whenUserNotFound() {
            // Given
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateState(TEST_PID, 1));

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("管理员判断测试")
    class IsAdminTests {

        @Test
        @DisplayName("是管理员 - 返回 true")
        void isAdmin_shouldReturnTrue_whenAdmin() {
            // Given
            MerchantEntity admin = createMerchant(1L, TEST_PID, "admin", 1, 1);
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.of(admin));

            // When
            boolean result = userService.isAdmin(TEST_PID);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("非管理员 - 返回 false")
        void isAdmin_shouldReturnFalse_whenNotAdmin() {
            // Given
            MerchantEntity user = createMerchant(1L, TEST_PID, "user", 0, 1);
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.of(user));

            // When
            boolean result = userService.isAdmin(TEST_PID);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("用户不存在 - 返回 false")
        void isAdmin_shouldReturnFalse_whenUserNotFound() {
            // Given
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.empty());

            // When
            boolean result = userService.isAdmin(TEST_PID);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("角色/状态名称映射测试")
    class RoleStateNameMappingTests {

        @Test
        @DisplayName("角色名称 - 普通用户")
        void toDTO_shouldMapRoleName_forNormalUser() {
            // Given
            MerchantEntity user = createMerchant(1L, TEST_PID, "user", 0, 1);
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.of(user));

            // When
            Optional<UserDTO> result = userService.getUserByPid(TEST_PID);

            // Then
            assertTrue(result.isPresent());
            assertEquals("普通用户", result.get().getRoleName());
        }

        @Test
        @DisplayName("角色名称 - 管理员")
        void toDTO_shouldMapRoleName_forAdmin() {
            // Given
            MerchantEntity admin = createMerchant(1L, TEST_PID, "admin", 1, 1);
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.of(admin));

            // When
            Optional<UserDTO> result = userService.getUserByPid(TEST_PID);

            // Then
            assertTrue(result.isPresent());
            assertEquals("管理员", result.get().getRoleName());
        }

        @Test
        @DisplayName("状态名称 - 禁用")
        void toDTO_shouldMapStateName_forDisabled() {
            // Given
            MerchantEntity user = createMerchant(1L, TEST_PID, "user", 0, 0);
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.of(user));

            // When
            Optional<UserDTO> result = userService.getUserByPid(TEST_PID);

            // Then
            assertTrue(result.isPresent());
            assertEquals("禁用", result.get().getStateName());
        }

        @Test
        @DisplayName("状态名称 - 启用")
        void toDTO_shouldMapStateName_forEnabled() {
            // Given
            MerchantEntity user = createMerchant(1L, TEST_PID, "user", 0, 1);
            when(merchantRepository.findByPid(TEST_PID)).thenReturn(Optional.of(user));

            // When
            Optional<UserDTO> result = userService.getUserByPid(TEST_PID);

            // Then
            assertTrue(result.isPresent());
            assertEquals("启用", result.get().getStateName());
        }
    }

    // ==================== 辅助方法 ====================

    private MerchantEntity createMerchant(Long id, Long pid, String username, int role, int state) {
        MerchantEntity entity = new MerchantEntity();
        entity.setId(id);
        entity.setPid(pid);
        entity.setUsername(username);
        entity.setRole(role);
        entity.setState(state);
        entity.setSecretKey("test-secret");
        return entity;
    }
}
