package com.group.defectapp.service.defectlog;


import com.group.defectapp.controller.file.util.FileUtil;
import com.group.defectapp.domain.defect.DefectStatusCode;
import com.group.defectapp.domain.defectlog.DefectLog;
import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.defectlog.DefectLogListDto;
import com.group.defectapp.dto.defectlog.DefectLogRequestDto;
import com.group.defectapp.repository.defect.DefectRepository;
import com.group.defectapp.repository.defectlog.DefectLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DefectLogServiceImpl implements DefectLogService {

    private final DefectLogRepository defectLogRepository;
    private final DefectRepository defectRepository;
    private final FileUtil fileUtil;


    @Transactional
    public Long defectLogSave(DefectLogRequestDto defectLogRequestDto, MultipartFile[] files) {

        // 엔티티 생성
        DefectLog defectLog = defectLogRequestDto.toEntity();

        if (files != null && files.length > 0) {

            processSaveFileUpload(files, defectLog);
        }

        DefectLog retDefectLog = defectLogRepository.save(defectLog);

        if (Objects.nonNull(retDefectLog)) {

            if (DefectStatusCode.ASSIGNED_TRANSFER.getCode().equals(retDefectLog.getStatusCd())) {
                defectRepository.updateDefectAssignUserId(defectLogRequestDto.getAssignUserId(), retDefectLog.getDefectId());
            }

            defectRepository.updateDefectStatusCode(retDefectLog.getDefectId(), retDefectLog.getStatusCd());
        }


        return null;
    }

    public Page<DefectLogListDto> defectLogList(PageRequestDto pageRequestDto, String defectId) {
        /* TODO : 별도의 페이징 DTO 필요 */
        Pageable pageable = pageRequestDto.getPageable();
        return defectLogRepository.list(pageable, defectId);
    }


    private void processSaveFileUpload(MultipartFile[] files, DefectLog defectLog) {
        // 기존 첨부 파일 초기화 (재저장 시나리오를 고려)
        defectLog.clearDefectLogFiles();

        // 파일 업로드 처리
        List<String> savedFileNames = fileUtil.upload(files);

        if (!savedFileNames.isEmpty()) {

            for (int i = 0; i < files.length; i++) {
                if (i < savedFileNames.size()) {
                    String savedFileName = savedFileNames.get(i);
                    String originalFileName = files[i].getOriginalFilename();

                    // 결함 로그에 파일 정보 추가
                    defectLog.addDefectLogFile(
                            defectLog.getDefectId(),
                            originalFileName,
                            savedFileName,
                            fileUtil.getUploadPath()
                    );

                }
            }
        }
    }


}
