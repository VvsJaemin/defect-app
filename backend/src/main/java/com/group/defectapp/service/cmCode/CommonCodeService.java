package com.group.defectapp.service.cmCode;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.repository.cmCode.CommonCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommonCodeService {

    private final CommonCodeRepository commonCodeRepository;

    public CommonCode findBySeCode(String seCode) {

        return commonCodeRepository.findBySeCode(seCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 상태코드를 찾을 수 없습니다: " + seCode));
    }
}
