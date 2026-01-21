package com.unlimited.sports.globox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.COSClient;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.prop.CosProperties;
import com.unlimited.sports.globox.common.result.GovernanceCode;
import com.unlimited.sports.globox.cos.CosFileDownloadUtils;
import com.unlimited.sports.globox.cos.dto.BatchDownloadResultDto;
import com.unlimited.sports.globox.holder.SensitiveWordsHolder;
import com.unlimited.sports.globox.prop.SensitiveManifest;
import com.unlimited.sports.globox.service.SensitiveWordsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import toolgood.words.WordsSearchEx;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveWordsBootstrap implements ApplicationRunner {

    private final SensitiveWordsService sensitiveWordsService;

    @Override
    public void run(ApplicationArguments args) {
        sensitiveWordsService.reload();
    }


}