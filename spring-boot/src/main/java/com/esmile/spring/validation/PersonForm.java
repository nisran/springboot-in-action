package com.esmile.spring.validation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description TODO
 * @Date 12/7/2023
 * @Created by nisran
 */
@Data
public class PersonForm implements Serializable {

    @NotNull
    @Size(min=2, max=30)
    private String name;

    @NotNull
    @Min(18)
    private Integer age;

}
