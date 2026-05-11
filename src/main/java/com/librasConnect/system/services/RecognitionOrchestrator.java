package com.librasConnect.system.services;

import com.librasConnect.system.dto.v1.ClipPayloadDto;
import com.librasConnect.system.dto.v1.RecognizeResponseDto;

public interface RecognitionOrchestrator {

    RecognizeResponseDto recognize(ClipPayloadDto clip);
}
