package com.unlimited.sports.globox.prop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.List;


@Data
@RefreshScope
@JsonIgnoreProperties(ignoreUnknown = true)
public class SensitiveManifest {
    private String version;
    private List<String> files;
}