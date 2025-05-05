package com.group.defectapp.domain.cmCode;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_code_m")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(CommonCodeId.class)
public class CommonCode extends BaseEntity {

    @Id
    @Column(name = "upper_cd", length = 24, nullable = false)
    private String upperCode; // 상위코드

    @Id
    @Column(name = "se_cd", length = 24, nullable = false)
    private String seCode; // 구분코드

    @Column(name = "cd_nm", length = 256, nullable = false)
    private String codeName; // 코드명

    @Column(name = "sort_ordr")
    private Integer sortOrder; // 정렬순서

    @Column(name = "etc_info", length = 512)
    private String etcInfo; // 기타정보

    @Column(name = "use_yn", length = 1)
    private String useYn; // 사용여부

    @Column(name = "first_reg_id", length = 48)
    private String firstRegId; // 최초등록아이디

    @Column(name = "fnl_udt_id", length = 48)
    private String fnlUdtId; // 최종수정아이디
}