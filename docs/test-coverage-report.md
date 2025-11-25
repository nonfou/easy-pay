# Easy-Pay 测试覆盖分析报告

## 1. 执行摘要

| 指标 | 数值 | 状态 |
|------|------|------|
| **总体测试覆盖率** | 12.99% | 严重不足 |
| **已测试类** | 10 / 77 | - |
| **Service 层覆盖率** | 27.78% (10/36) | 需改进 |
| **Controller 层覆盖率** | 0% (0/14) | 严重缺失 |
| **Repository 层覆盖率** | 0% (0/6) | 严重缺失 |
| **工具类覆盖率** | 0% (0/21) | 严重缺失 |

---

## 2. 已测试模块

### 2.1 Service 层 (已测试)

| 模块 | 测试文件 | 状态 |
|------|---------|------|
| PriceAllocator | PriceAllocatorTest | 通过 |
| AdminOrderService | AdminOrderServiceTest | 通过 |
| OrderMatchService | OrderMatchServiceTest | 通过 |
| CashierService | CashierServiceTest | 通过 |
| StatisticsService | StatisticsServiceTest | 通过 |

**测试质量评估**：
- 结构清晰，使用 Given-When-Then 模式
- Mock 隔离良好，使用 Mockito
- 中文 DisplayName，可读性强
- 缺少边界值测试
- 缺少并发场景测试

---

## 3. CRITICAL 级别问题 (立即修复)

### 3.1 PublicOrderService - 订单创建核心服务

**文件**: `service/impl/PublicOrderServiceImpl.java`

**缺失的测试用例**:

| 类别 | 测试场景 | 状态 |
|------|---------|------|
| 正常流程 | 创建订单成功，验证返回订单ID和收银台URL | 已有 |
| 参数校验 | money 为 null/0/负数时抛出异常 | 缺失 |
| 参数校验 | money 超大值测试 (999999999.99) | 缺失 |
| 业务异常 | 重复 outTradeNo 抛出 CONFLICT | 缺失 |
| 业务异常 | 商户密钥不存在抛出 UNAUTHORIZED | 缺失 |
| 业务异常 | 签名验证失败抛出 INVALID_ARGUMENT | 缺失 |
| 业务异常 | 无可用通道抛出 SERVICE_UNAVAILABLE | 缺失 |
| 边界值 | 最小金额测试 (0.01) | 缺失 |
| 边界值 | 金额精度测试 (小数点后2位) | 缺失 |
| 集成测试 | 事务回滚测试 | 缺失 |
| 集成测试 | 事件发布测试 (orderEventPublisher) | 缺失 |
| 并发测试 | 相同 outTradeNo 并发创建订单 | 缺失 |

### 3.2 SignatureUtils - 签名工具类

**文件**: `signature/SignatureUtils.java`

**安全关键** - 签名算法如有bug可能导致严重安全漏洞

**缺失的测试用例**:

| 类别 | 测试场景 |
|------|---------|
| 签名构建 | 参数按字典序排序 |
| 签名构建 | null 值过滤 |
| 签名构建 | "sign" 字段过滤 |
| 签名构建 | 特殊字符处理 (&, =, 空格等) |
| MD5算法 | 已知输入输出对比测试 |
| MD5算法 | UTF-8 编码正确性 |
| 边界值 | 空 Map / 空字符串 |
| 安全测试 | SQL注入/XSS攻击字符 |

### 3.3 AuthService - 认证服务

**文件**: `service/impl/AuthServiceImpl.java`

**缺失的测试用例**:

| 类别 | 测试场景 |
|------|---------|
| 登录成功 | 正确的用户名密码，返回有效JWT Token |
| 登录失败 | 用户不存在 |
| 登录失败 | 密码错误 |
| 登录失败 | 账户被禁用 |
| Token | Token 包含正确的用户信息 |
| Token | Token 过期测试 |
| Token | Token 篡改测试 |
| 密码安全 | BCrypt 加密正确性 |

### 3.4 JwtTokenProvider - JWT 令牌提供者

**文件**: `security/JwtTokenProvider.java`

**缺失的测试用例**:

| 类别 | 测试场景 |
|------|---------|
| Token生成 | 生成包含正确 Claims |
| Token生成 | 设置正确的过期时间 |
| Token解析 | 解析有效 Token，提取用户信息 |
| Token验证 | 验证有效 Token |
| Token验证 | 拒绝过期 Token |
| Token验证 | 拒绝无效签名 Token |
| 安全测试 | None 算法攻击防护 |

### 3.5 PaymentMatchService - 支付匹配服务

**文件**: `service/impl/PaymentMatchServiceImpl.java`

**缺失的测试用例**:

| 类别 | 测试场景 |
|------|---------|
| 匹配成功 | 正常匹配流程，更新订单状态，触发商户通知 |
| 匹配失败 | 找不到匹配订单 |
| 匹配失败 | 订单已支付 |
| 匹配失败 | 金额不匹配 |
| 并发测试 | 相同订单并发匹配，防重处理 |

---

## 4. HIGH 级别问题 (尽快修复)

### 4.1 Controller 层 (全部未测试)

| Controller | 缺失测试 |
|------------|---------|
| PublicOrderController | @WebMvcTest 单元测试、参数校验、异常处理、集成测试 |
| OrderMatchController | 匹配接口参数校验、异常响应格式 |
| AdminOrderController | 手动补单、重新通知、权限验证 |
| AuthController | 登录接口、Token 刷新、登出 |

**建议测试代码示例**:

```java
@WebMvcTest(PublicOrderController.class)
class PublicOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicOrderService publicOrderService;

    @Test
    @DisplayName("创建订单成功 - 返回201和订单信息")
    void createOrder_shouldReturn201_whenSuccess() throws Exception {
        // Given
        PublicCreateOrderResult result = new PublicCreateOrderResult(
            "H202411250001", "/cashier/H202411250001");
        when(publicOrderService.createOrder(any())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/public/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderId").value("H202411250001"));
    }
}
```

---

## 5. MEDIUM 级别问题 (计划修复)

### 5.1 未测试的 Service

| Service | 文件路径 |
|---------|---------|
| ChannelSelector | SimpleChannelSelector.java |
| MerchantSecretService | MerchantSecretServiceImpl.java |
| OrderQueryService | OrderQueryServiceImpl.java |
| HttpNotifyClient | HttpNotifyClient.java |
| OrderIdGenerator | OrderIdGenerator.java |

### 5.2 Repository 层 (全部未测试)

| Repository | 需要测试 |
|------------|---------|
| OrderRepository | 自定义查询、分页查询、事务测试 |
| PayChannelRepository | 通道状态查询 |
| PayAccountRepository | 账号查询 |
| MerchantRepository | 商户查询 |
| PluginRepository | 插件查询 |

---

## 6. 现有测试质量问题

### 6.1 PriceAllocatorTest - 缺少极端场景

**需要补充**:
```java
@Test
@DisplayName("金额分配 - 达到最大递增次数")
void allocate_shouldThrowException_whenMaxIncrementReached() {
    // 测试超过100次递增的场景
}

@Test
@DisplayName("金额分配 - 并发场景")
void allocate_shouldHandleConcurrency() throws Exception {
    // 使用 CountDownLatch 模拟并发
}
```

### 6.2 OrderMatchServiceTest - 缺少时间相关测试

**需要补充**:
```java
@Test
@DisplayName("匹配失败 - 订单已过期")
void matchPayment_shouldThrowException_whenOrderExpired() {
    // 测试过期订单不能匹配
}
```

---

## 7. 测试基础设施建议

### 7.1 配置 JaCoCo 测试覆盖率

在 `pom.xml` 添加:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 7.2 测试数据工厂

```java
// backend/src/test/java/com/github/nonfou/mpay/testutil/OrderEntityBuilder.java
public class OrderEntityBuilder {
    private OrderEntity entity = new OrderEntity();

    public static OrderEntityBuilder anOrder() {
        return new OrderEntityBuilder()
            .withId(1L)
            .withOrderId("H202411250001")
            .withPid(1001L)
            .withType("wxpay")
            .withState(0)
            .withMoney(new BigDecimal("100.00"))
            .withReallyPrice(new BigDecimal("100.01"))
            .withCreateTime(LocalDateTime.now());
    }

    public OrderEntityBuilder withOrderId(String orderId) {
        entity.setOrderId(orderId);
        return this;
    }

    public OrderEntityBuilder withState(int state) {
        entity.setState(state);
        return this;
    }

    public OrderEntity build() {
        return entity;
    }
}
```

### 7.3 测试基类

```java
@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceTest {

    @BeforeEach
    void baseSetUp() {
        // 通用设置
    }

    @AfterEach
    void baseTearDown() {
        // 通用清理
    }
}
```

---

## 8. 实施计划

### Week 1: 测试基础设施 + 核心 Service

| Day | 任务 |
|-----|------|
| 1-2 | 配置 JaCoCo、创建测试数据工厂、测试基类 |
| 3 | PublicOrderService 测试 |
| 4 | SignatureUtils + AuthService 测试 |
| 5 | JwtTokenProvider + PaymentMatchService 测试 |

### Week 2: 认证安全 + Controller 测试

| Day | 任务 |
|-----|------|
| 1-2 | PublicOrderController、OrderMatchController 测试 |
| 3-4 | AdminOrderController、AuthController 测试 |
| 5 | 补充现有测试的边界条件和并发测试 |

### Week 3-4: Repository + 工具类 + E2E

| Day | 任务 |
|-----|------|
| 1-4 | Repository 层测试 |
| 5-8 | 工具类测试、E2E 测试 |
| 9-10 | 生成覆盖率报告、编写测试文档 |

---

## 9. 覆盖率目标

| 时间节点 | 行覆盖率目标 | 分支覆盖率目标 |
|---------|------------|--------------|
| 1周后 | > 30% | > 20% |
| 2周后 | > 50% | > 40% |
| 3周后 | > 70% | > 60% |
| 4周后 | > 80% | > 70% |

---

## 10. 风险评估

### 高风险区域

| 风险类型 | 描述 | 影响 |
|---------|------|------|
| 支付安全 | 签名算法未测试 | 可能存在安全漏洞 |
| 支付安全 | JWT 验证未测试 | 可能被绕过 |
| 资金安全 | 订单创建流程未测试 | 可能产生重复订单 |
| 资金安全 | 支付匹配未测试 | 可能导致金额错误 |
| 业务完整性 | Controller 层无测试 | 接口参数校验无保障 |

---

**��成时间**: 2025-11-25
**项目**: Easy-Pay
