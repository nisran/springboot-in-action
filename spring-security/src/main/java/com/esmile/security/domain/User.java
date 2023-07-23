package com.esmile.security.domain;

import com.esmile.security.domain.base.BaseDomain;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Access;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Transient;
import lombok.Data;

import java.util.Collection;

/**
 * @Description TODO
 * @Date 9/7/2023
 * @Created by nisran
 */
@Data
@Entity
public class User extends BaseDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long userId;
    private String userName;

    // @JsonIgnore
    // TODO 如果是json ignore register的时候密码就传不过来，尝试一下！
    // TODO: JsonProperty 添加access 属性
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password ;
    private String email ;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    // @JoinColumn
    private Collection<Role> roles;
}
