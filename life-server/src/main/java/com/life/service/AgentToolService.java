package com.life.service;

import com.life.context.BaseContext;
import com.life.entity.LifeUserAddress;
import com.life.exception.BaseException;
import com.life.query.BenefitQueryDTO;
import com.life.query.NoticeQueryDTO;
import com.life.query.OrderQueryDTO;
import com.life.query.PostQueryDTO;
import com.life.query.ProductQueryDTO;
import com.life.query.QueryDTO;
import com.life.query.UserBenefitQueryDTO;
import com.life.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Agent 工具网关：第一批只读业务能力的白名单注册表。
 *
 * 安全设计（与 Python 侧 ReAct 工具白名单同构的双层防护）：
 * 1. 白名单硬编码——LLM 编造的工具名在这里直接查不到；
 * 2. 参数面不含 userId——身份由 InternalTokenInterceptor 写入 BaseContext，
 *    下游 Service 的"从登录态取 userId"逻辑原样生效，LLM 无法指定他人身份；
 * 3. 分页护栏——pageSize 封顶 10，防止 LLM 生成超大分页打爆下游与上下文 token；
 * 4. 异常收敛——BaseException 透出业务消息，其余只回简短错误，不泄漏堆栈/SQL。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentToolService {

    private static final int MAX_PAGE_SIZE = 10;

    private final PointsService pointsService;
    private final LifePointsRecordService lifePointsRecordService;
    private final LifePointsTaskService lifePointsTaskService;
    private final LifeProductService lifeProductService;
    private final LifeBenefitService lifeBenefitService;
    private final LifeUserBenefitService lifeUserBenefitService;
    private final OrderService orderService;
    private final PostService postService;
    private final NoticeService noticeService;
    private final SignRecordService signRecordService;
    private final CartService cartService;
    private final UserService userService;
    private final LifeUserAddressService lifeUserAddressService;

    private final Map<String, Function<ArgView, Object>> registry = new HashMap<>();

    @PostConstruct
    public void init() {
        // ---- 积分域 ----
        registry.put("get_points_summary", a -> pointsService.querySummary());
        registry.put("get_points_records", a -> {
            QueryDTO q = pageArgs(a);
            return lifePointsRecordService.pageList(q);
        });
        registry.put("get_points_rank", a -> {
            QueryDTO q = new QueryDTO();
            q.setPageNum(1);
            q.setPageSize(3);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("my_rank", pointsService.queryMyRank());
            out.put("board_top3", pointsService.queryRankBoard(q));
            return out;
        });
        registry.put("get_points_tasks", a -> {
            QueryDTO q = pageArgs(a);
            return lifePointsTaskService.queryCEndPage(q);
        });

        // ---- 商城/权益域 ----
        registry.put("search_products", a -> {
            ProductQueryDTO q = new ProductQueryDTO();
            q.setKeyword(a.str("keyword"));
            q.setCategoryId(a.lng("category_id"));
            q.setSortType(a.integer("sort_type"));
            applyPage(q, a);
            return lifeProductService.pageList(q);
        });
        registry.put("get_product_detail", a -> lifeProductService.getProductDetail(a.requireLng("product_id")));
        registry.put("search_benefits", a -> {
            BenefitQueryDTO q = new BenefitQueryDTO();
            q.setKeyword(a.str("keyword"));
            q.setCategoryId(a.lng("category_id"));
            q.setSortType(a.integer("sort_type"));
            q.setBenefitType(a.integer("benefit_type"));
            applyPage(q, a);
            return lifeBenefitService.pageList(q);
        });
        registry.put("get_benefit_detail", a -> lifeBenefitService.getBenefitDetail(a.requireLng("benefit_id")));
        registry.put("get_my_benefits", a -> {
            UserBenefitQueryDTO q = new UserBenefitQueryDTO();
            q.setStatus(a.integer("status"));
            applyPage(q, a);
            return lifeUserBenefitService.queryMyPage(q);
        });

        // ---- 订单/购物车域 ----
        registry.put("get_my_orders", a -> {
            OrderQueryDTO q = new OrderQueryDTO();
            q.setStatus(a.integer("status"));
            q.setOrderType(a.integer("order_type"));
            applyPage(q, a);
            return orderService.queryMyOrderPage(q);
        });
        registry.put("get_order_detail", a -> orderService.getOrderDetail(a.requireLng("order_id")));
        registry.put("get_cart", a -> cartService.queryCartList());

        // ---- 社区域 ----
        registry.put("search_posts", a -> {
            PostQueryDTO q = new PostQueryDTO();
            q.setKeyword(a.str("keyword"));
            q.setCategory(a.str("category"));
            q.setUserId(a.lng("user_id"));
            q.setType(a.integer("type"));
            applyPage(q, a);
            return postService.pageQuery(q);
        });
        registry.put("get_user_profile", a -> {
            Long target = a.lng("user_id");
            return userService.getUserDetail(target == null ? BaseContext.getCurrentId() : target);
        });

        // ---- 通知/签到/地址域 ----
        registry.put("get_notifications", a -> {
            NoticeQueryDTO q = new NoticeQueryDTO();
            q.setIsRead(a.integer("is_read"));
            q.setType(a.integer("type"));
            applyPage(q, a);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("unread_count", noticeService.queryUnreadCount());
            out.put("page", noticeService.queryNoticePage(q));
            return out;
        });
        registry.put("get_sign_calendar", a -> signCalendar());
        registry.put("get_my_addresses", a -> {
            List<LifeUserAddress> list = lifeUserAddressService.listMyAddresses();
            return list;
        });

        log.info("Agent 工具网关注册完成，共 {} 个只读工具", registry.size());
    }

    /**
     * 统一调用入口。未知工具名/缺参/业务异常都收敛为 Result.error，不抛栈。
     */
    public Result<Object> invoke(String name, Map<String, Object> args) {
        Function<ArgView, Object> fn = registry.get(name);
        if (fn == null) {
            log.warn("Agent 调用了未注册工具: {}", name);
            return Result.error("未注册的工具: " + name);
        }
        try {
            return Result.success(fn.apply(new ArgView(args == null ? Map.of() : args)));
        } catch (BaseException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("工具执行失败 name={}", name, e);
            String msg = String.valueOf(e.getMessage());
            return Result.error("工具执行失败: " + (msg.length() > 200 ? msg.substring(0, 200) : msg));
        }
    }

    /** 本月签到日历：Byte[] 位图转 {已签天数, 日期列表, 今日是否已签} */
    private Map<String, Object> signCalendar() {
        Byte[] bits = signRecordService.querySignRecords();
        List<Integer> days = new ArrayList<>();
        if (bits != null) {
            for (int i = 0; i < bits.length; i++) {
                if (bits[i] != null && bits[i] == 1) {
                    days.add(i + 1);
                }
            }
        }
        int today = LocalDate.now().getDayOfMonth();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", LocalDate.now().getMonthValue());
        out.put("signed_days", days);
        out.put("signed_count", days.size());
        out.put("signed_today", days.contains(today));
        return out;
    }

    private QueryDTO pageArgs(ArgView a) {
        QueryDTO q = new QueryDTO();
        q.setPageNum(a.pageNum());
        q.setPageSize(a.pageSize());
        return q;
    }

    private void applyPage(QueryDTO q, ArgView a) {
        q.setPageNum(a.pageNum());
        q.setPageSize(a.pageSize());
    }

    /** 工具参数视图：LLM 生成的 JSON 参数宽容取值，全部有界 */
    public static class ArgView {
        private final Map<String, Object> args;

        public ArgView(Map<String, Object> args) {
            this.args = args;
        }

        String str(String key) {
            Object v = args.get(key);
            if (v == null) {
                return null;
            }
            String s = String.valueOf(v).trim();
            return s.isEmpty() ? null : s;
        }

        Integer integer(String key) {
            Object v = args.get(key);
            if (v == null) {
                return null;
            }
            if (v instanceof Number n) {
                return n.intValue();
            }
            try {
                return Integer.valueOf(String.valueOf(v).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        Long lng(String key) {
            Object v = args.get(key);
            if (v == null) {
                return null;
            }
            if (v instanceof Number n) {
                return n.longValue();
            }
            try {
                return Long.valueOf(String.valueOf(v).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        long requireLng(String key) {
            Long v = lng(key);
            if (v == null) {
                throw new BaseException("缺少必填参数: " + key);
            }
            return v;
        }

        int pageNum() {
            Integer p = integer("page_num");
            return p == null || p < 1 ? 1 : Math.min(p, 1000);
        }

        int pageSize() {
            Integer s = integer("page_size");
            return s == null || s < 1 ? 5 : Math.min(s, MAX_PAGE_SIZE);
        }
    }
}
