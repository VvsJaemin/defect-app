package com.group.defectapp.service.defectlog;

import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.defectlog.DefectLogListDto;
import com.group.defectapp.dto.defectlog.DefectLogRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface DefectLogService {

    /**
     * 결함 로그를 저장합니다.
     *
     * @param defectLogRequestDto 결함 로그 요청 DTO
     * @param files 첨부 파일 배열
     */
    void defectLogSave(DefectLogRequestDto defectLogRequestDto, MultipartFile[] files);

    /**
     * 결함 로그 목록을 조회합니다.
     *
     * @param pageRequestDto 페이지 요청 DTO
     * @param defectId 결함 ID
     * @return 결함 로그 목록 페이지
     */
    Page<DefectLogListDto> defectLogList(PageRequestDto pageRequestDto, String defectId);

}