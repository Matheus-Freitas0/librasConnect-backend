package com.librasConnect.system.controllers.v1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.librasConnect.system.dto.v1.PostTrainingSampleRequestDto;
import com.librasConnect.system.dto.v1.SampleMetaDto;
import com.librasConnect.system.services.TrainingSampleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/samples")
@Validated
public class SamplesApiController {

    private final TrainingSampleService trainingSampleService;

    public SamplesApiController(TrainingSampleService trainingSampleService) {
        this.trainingSampleService = trainingSampleService;
    }

    @PostMapping
    public ResponseEntity<SampleMetaDto> submitSample(@Valid @RequestBody PostTrainingSampleRequestDto body) {
        SampleMetaDto created = trainingSampleService.submitSample(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
