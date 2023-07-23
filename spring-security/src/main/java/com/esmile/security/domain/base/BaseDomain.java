package com.esmile.security.domain.base;

import lombok.Data;

/**
 * @Description TODO
 * @Date 2023/7/17
 * @Created by nisran
 */
@Data
public class BaseDomain {

    // TODO entity 中使用枚举？？
    // @Enumerated(EnumType.STRING)
    // private ActiveIndicator activeIndicator ;
    private String activeIndicator ;
}
