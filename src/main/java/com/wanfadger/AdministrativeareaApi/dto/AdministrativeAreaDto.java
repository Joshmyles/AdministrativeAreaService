package com.wanfadger.AdministrativeareaApi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdministrativeAreaDto implements Serializable {
    private String code;
    private String name;
    private String latitude;
    private String longitude;
}
