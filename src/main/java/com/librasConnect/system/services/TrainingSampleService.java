package com.librasConnect.system.services;

import com.librasConnect.system.dto.v1.PostTrainingSampleRequestDto;
import com.librasConnect.system.dto.v1.SampleMetaDto;

public interface TrainingSampleService {

    SampleMetaDto submitSample(PostTrainingSampleRequestDto request);
}
