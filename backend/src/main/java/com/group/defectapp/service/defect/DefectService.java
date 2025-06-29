package com.group.defectapp.service.defect;

import com.group.defectapp.dto.defect.*;
import com.group.defectapp.dto.project.ProjectUserListDto;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

public interface DefectService {

    /**
     * 새로운 결함을 등록합니다.
     *
     * @param defectRequestDto 결함 등록 요청 정보
     * @param files            첨부 파일 배열
     * @param loginUserId      로그인 사용자 ID
     */
    void saveDefect(DefectRequestDto defectRequestDto, MultipartFile[] files, String loginUserId);

    /**
     * 조건에 맞는 결함 목록을 페이징하여 조회합니다.
     *
     * @param pageRequestDto 페이지 요청 정보
     * @param paramMap       검색 조건 정보가 담긴 맵
     * @return 결함 목록 페이지
     */
    Page<DefectListDto> defectList(PageRequestDto pageRequestDto, Map<String, Object> paramMap);

    /**
     * 특정 결함의 상세 정보를 조회합니다.
     *
     * @param defectId 조회할 결함 ID
     * @return 결함 상세 정보
     */
    DefectResponseDto readDefect(String defectId);

    /**
     * 기존 결함 정보를 수정합니다.
     *
     * @param defectRequestDto 결함 수정 요청 정보
     * @param files            첨부 파일 배열
     * @param principal        로그인 사용자 정보
     */
    void modifyDefect(DefectRequestDto defectRequestDto, MultipartFile[] files, Principal principal);

    /**
     * 결함을 삭제합니다.
     * 본인이 등록한 결함만 삭제할 수 있습니다.
     *
     * @param defectId  삭제할 결함 ID
     * @param principal 삭제를 요청하는 사용자 정보
     */
    void deleteDefect(String defectId, Principal principal);

    List<DefectProjectListDto> defectProjectList();

    DefectDashBoardDto defectDashBoardList();
}