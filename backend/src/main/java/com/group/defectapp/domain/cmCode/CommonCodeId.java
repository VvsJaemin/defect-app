
package com.group.defectapp.domain.cmCode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonCodeId implements Serializable {
    private String upperCode; // 상위코드
    private String seCode; // 구분코드
}