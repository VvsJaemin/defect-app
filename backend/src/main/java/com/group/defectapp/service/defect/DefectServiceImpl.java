package com.group.defectapp.service.defect;

import com.group.defectapp.controller.file.util.FileUtil;
import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.defect.*;
import com.group.defectapp.dto.defectlog.DefectLogRequestDto;
import com.group.defectapp.exception.defect.DefectCode;
import com.group.defectapp.exception.defect.DefectException;
import com.group.defectapp.repository.cmCode.CommonCodeRepository;
import com.group.defectapp.repository.defect.DefectRepository;
import com.group.defectapp.repository.user.UserRepository;
import com.group.defectapp.service.defectlog.DefectLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 결함 관리 서비스
 * 결함 등록, 조회, 수정, 삭제 및 ID 생성 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefectServiceImpl implements DefectService {


    private final DefectRepository defectRepository;
    private final UserRepository userRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final FileUtil fileUtil;
    private final DefectLogService defectLogService;

    /**
     * 새로운 결함을 등록합니다.
     *
     * @param defectRequestDto 결함 등록 요청 정보
     * @param loginUserId
     */
    @Transactional
    public void saveDefect(DefectRequestDto defectRequestDto, MultipartFile[] files, String loginUserId) {

        User assignee = findAssigneeIfExists(loginUserId);

        CommonCode statusCode = commonCodeRepository.findBySeCode(defectRequestDto.getStatusCode())
                .orElseThrow(DefectCode.DEFECT_STATUS_CODE_ERROR::getDefectException);

        CommonCode orderCode = commonCodeRepository.findBySeCode(defectRequestDto.getOrderCode())
                .orElseThrow(DefectCode.DEFECT_STATUS_CODE_ERROR::getDefectException);

        CommonCode seriousCode = commonCodeRepository.findBySeCode(defectRequestDto.getSeriousCode())
                .orElseThrow(DefectCode.DEFECT_STATUS_CODE_ERROR::getDefectException);


        String newDefectId = defectRepository.generateDefectIdUsingSequence();
        String retStatusCode = statusCode.getSeCode();
        String retOrderCode = orderCode.getSeCode();
        String retSeriousCode = seriousCode.getSeCode();

        Defect defect = defectRequestDto.toEntity(assignee, newDefectId, retStatusCode, retOrderCode, retSeriousCode);

//        if (files != null && files.length > 0) {
//
//            processSaveFileUpload(files, defect);
//        }

        Defect retDefect = defectRepository.save(defect);

        /* 결함 등록 후 결함 로그에도 insert */
        DefectLogRequestDto defectLogRequestDto = DefectLogRequestDto.builder()
                .defectId(retDefect.getDefectId())
                .logTitle(retDefect.getDefectTitle())
                .logCt(retDefect.getDefectContent())
                .statusCd(retDefect.getStatusCode())
                .createdBy(retDefect.getCreatedBy())
                .build();

        defectLogService.defectLogSave(defectLogRequestDto, files);
    }

    /**
     * 조건에 맞는 결함 목록을 페이징하여 조회합니다.
     *
     * @param pageRequestDto 페이지 요청 정보
     * @param paramMap       검색 조건 정보를 담은 맵
     * @return 결함 목록 페이지
     */
    public Page<DefectListDto> defectList(PageRequestDto pageRequestDto, Map<String, Object> paramMap) {
        DefectSearchCondition condition = createSearchCondition(paramMap);
        Pageable pageable = pageRequestDto.getPageable(Sort.by("createdAt").descending());
        return defectRepository.list(pageable, condition);
    }

    /**
     * 특정 결함의 상세 정보를 조회합니다.
     *
     * @param userId   조회하는 사용자 ID
     * @param defectId 조회할 결함 ID
     * @return 결함 상세 정보
     */
    public DefectResponseDto readDefect(String defectId) {
        Defect defect = findDefectById(defectId);

        // 빌더 패턴 사용 시 모든 필드를 한 번에 설정
        DefectResponseDto.DefectResponseDtoBuilder builder = DefectResponseDto.builder()
                .defectId(defect.getDefectId())
                .projectId(defect.getProjectId())
                .statusCode(defect.getStatusCode())
                .seriousCode(defect.getSeriousCode())
                .assigneeId(defect.getAssignee())
                .orderCode(defect.getOrderCode())
                .defectDivCode(defect.getDefectDivCode())
                .defectTitle(defect.getDefectTitle())
                .defectMenuTitle(defect.getDefectMenuTitle())
                .defectUrlInfo(defect.getDefectUrlInfo())
                .defectContent(defect.getDefectContent())
                .defectEtcContent(defect.getDefectEtcContent())
                .createdAt(defect.getCreatedAt())
                .createdBy(defect.getCreatedBy())
                .updatedAt(defect.getUpdatedAt())
                .updatedBy(defect.getUpdatedBy())
                .openYn(defect.getOpenYn());


        return builder.build();
    }

    /**
     * 기존 결함 정보를 수정합니다.
     * @param defectRequestDto 결함 수정 요청 정보
     * @param files 첨부 파일 배열
     * @param principal 로그인 사용자 정보
     */
    @Transactional
    public void modifyDefect(DefectRequestDto defectRequestDto, MultipartFile[] files, Principal principal) {

        Defect defect = findDefectById(defectRequestDto.getDefectId());

        if (files != null && files.length > 0) {

            processSaveFileUpload(files, defect);
        }

        // 결함 정보 업데이트
        updateDefectInfo(defect, defectRequestDto, principal);
    }

    /**
     * 결함을 삭제합니다.
     * 본인이 등록한 결함만 삭제할 수 있습니다.
     *
     * @param defectId  삭제할 결함 ID
     * @param principal 삭제를 요청하는 사용자 ID
     */
    @Transactional
    public void deleteDefect(String defectId, Principal principal) {

        String userId = principal.getName();

        User user = userRepository.findByUserId(userId)
                .orElseThrow(DefectCode.DEFECT_WRITER_DELETE_ERROR::getDefectException);

        Defect defect = findDefectById(defectId);

        validateDeletePermission(defect, user);


        defectRepository.delete(defect);
    }


    /**
     * 담당자 ID가 존재하는 경우 해당하는 사용자를 조회합니다.
     *
     * @param assigneeId 담당자 ID
     * @return 조회된 담당자 또는 null
     */
    private User findAssigneeIfExists(String assigneeId) {
        if (assigneeId == null || assigneeId.isEmpty()) {
            return null;
        }
        return userRepository.findByUserId(assigneeId)
                .orElseThrow(DefectCode.DEFECT_WRITER_ERROR::getDefectException);
    }

    /**
     * 검색 조건 파라미터 맵으로부터 검색 조건 객체를 생성합니다.
     *
     * @param paramMap 검색 조건 파라미터 맵
     * @return 생성된 검색 조건 객체
     */
    private DefectSearchCondition createSearchCondition(Map<String, Object> paramMap) {
        return DefectSearchCondition.builder()
                .projectId((String) paramMap.get("projectId"))
                .assigneeId((String) paramMap.get("assigneeId"))
                .statusCode((String) paramMap.get("statusCode"))
                .searchType((String) paramMap.get("searchType"))
                .searchText((String) paramMap.get("searchText"))
                .build();
    }

    /**
     * 결함 ID로 결함을 조회합니다.
     * 결함이 존재하지 않으면 예외를 발생시킵니다.
     *
     * @param defectId 조회할 결함 ID
     * @return 조회된 결함
     * @throws IllegalArgumentException 결함이 존재하지 않는 경우
     */
    private Defect findDefectById(String defectId) {
        return defectRepository.findByDefectId(defectId)
                .orElseThrow(DefectCode.DEFECT_NOT_FOUND::getDefectException);
    }


    /**
     * 결함 정보를 업데이트합니다.
     *
     * @param defect    업데이트할 결함 객체
     * @param dto       업데이트 요청 정보
     * @param principal
     */
    private void updateDefectInfo(Defect defect, DefectRequestDto dto, Principal principal) {
        defect.changeDefectTitle(dto.getDefectTitle());
        defect.changeDefectContent(dto.getDefectContent());
        defect.changeStatusCode(dto.getStatusCode());
        defect.changeSeriousCode(dto.getSeriousCode());
        defect.changeOrderCode(dto.getOrderCode());
        defect.changeOpenYn(dto.getOpenYn());
        defect.changeAssignee(dto.getAssigneeId());
        defect.changeDefectDivCode(dto.getDefectDivCode());
        defect.changeDefectMenuTitle(dto.getDefectMenuTitle());
        defect.changeDefectUrlInfo(dto.getDefectUrlInfo());
        defect.changeDefectEtcContent(dto.getDefectEtcContent());
        defect.changeUpdatedBy(principal.getName());
    }

    /**
     * 결함 삭제 권한을 검증합니다.
     * 결함을 등록한 사용자만 삭제할 수 있습니다.
     *
     * @param defect 삭제하려는 결함
     * @param user   삭제를 요청한 사용자
     * @throws IllegalArgumentException 권한이 없는 경우
     */
    private void validateDeletePermission(Defect defect, User user) {
        if (!defect.getCreatedBy().equals(user.getUserId())) {
            throw DefectCode.DEFECT_WRITER_DELETE_ERROR.getDefectException();
        }
    }

    private void processSaveFileUpload(MultipartFile[] files, Defect defect) {
        // 기존 첨부 파일 초기화
        defect.clearDefectImages();

        // 파일 업로드 처리
        List<String> savedFileNames = fileUtil.upload(files);

        if (!savedFileNames.isEmpty()) {

            for (int i = 0; i < files.length; i++) {
                if (i < savedFileNames.size()) {
                    String savedFileName = savedFileNames.get(i);
                    String originalFileName = files[i].getOriginalFilename();

                    // 결함 로그에 파일 정보 추가
                    defect.addDefectFile(
                            originalFileName,
                            savedFileName,
                            fileUtil.getUploadPath()
                    );

                }
            }
        }
    }

}