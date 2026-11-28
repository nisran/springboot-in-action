package com.esmile.jvm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OOM 演示端点接口测试：只测 status/clear 两个安全端点。
 *
 * 刻意不测 /oom/heap——它会把测试 JVM 的堆真的填满直到 OOM，
 * 而且请求会挂住直到堆耗尽，CI 上等于自杀。泄漏行为本身留给
 * Bruno + IDEA 手动演示动线（bruno/oom/），这里只保证只读与
 * 断引用端点的契约，clear 后回读 0 保证静态集合测试自净。
 *
 * @Description OomDemoController 接口测试（安全端点）
 * @Date 11/28/2026
 * @Created by nisran
 */
@AutoConfigureMockMvc
@SpringBootTest
class OomDemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** 现状可查 → 断引用 → 回读归零：一条测试自闭环，不留状态给别的测试 */
    @Test
    void statusAndClearRoundtrip() throws Exception {
        mockMvc.perform(get("/oom/heap/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("静态集合持有")));

        mockMvc.perform(post("/oom/heap/clear"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("已断开引用")));

        mockMvc.perform(get("/oom/heap/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("持有 0 个块")));
    }
}
