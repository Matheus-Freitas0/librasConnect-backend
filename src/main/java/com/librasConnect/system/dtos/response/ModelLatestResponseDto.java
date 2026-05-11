package com.librasConnect.system.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelLatestResponseDto {

    private String version;
    private String status;
    private String artifactUrl;
}
