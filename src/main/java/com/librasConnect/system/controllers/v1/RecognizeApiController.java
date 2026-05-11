package com.librasConnect.system.controllers.v1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.librasConnect.system.dto.v1.ClipPayloadDto;
import com.librasConnect.system.dto.v1.RecognizeResponseDto;
import com.librasConnect.system.services.RecognitionOrchestrator;

@RestController
@RequestMapping("/v1")
public class RecognizeApiController {

    private final RecognitionOrchestrator recognitionOrchestrator;

    public RecognizeApiController(RecognitionOrchestrator recognitionOrchestrator) {
        this.recognitionOrchestrator = recognitionOrchestrator;
    }

    @PostMapping("/recognize")
    public RecognizeResponseDto recognize(@RequestBody ClipPayloadDto body) {
        return recognitionOrchestrator.recognize(body);
    }
}
