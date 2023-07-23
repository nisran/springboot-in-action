package com.esmile.security.domain;

import com.esmile.security.domain.base.BaseDomain;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.util.Collection;

/**
 * @Description TODO
 * @Date 2023/7/17
 * @Created by nisran
 */
@Data
@Entity
public class Role extends BaseDomain {

    @Id
    private Long roleId;
    private String roleName;

    @ManyToMany
    // @JoinColumn(name = "user_Id")
    private Collection<User> user;

}
