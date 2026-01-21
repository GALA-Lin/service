package com.unlimited.sports.globox.prop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SensitiveManifest {
    private String version;
    private List<String> files;
}