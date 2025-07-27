package com.group.defectapp.repository.cmCode;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.cmCode.CommonCodeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommonCodeRepository extends JpaRepository<CommonCode, String> {

    Optional<CommonCode> findBySeCode(String seCode);
    Optional<CommonCode> findBySeCodeAndUpperCode(String seCode, String UpperCode);
}
