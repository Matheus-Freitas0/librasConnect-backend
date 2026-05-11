package com.librasConnect.system.dto.v1;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecognizeResponseDto(boolean recognized, RecognizedSignDto sign, String message) {

    public static RecognizeResponseDto notRecognized() {
        return new RecognizeResponseDto(false, null,
                "Expressão não encontrada no léxico cadastrado.");
    }

    public static RecognizeResponseDto ok(String id, String label) {
        return new RecognizeResponseDto(true, new RecognizedSignDto(id, label), null);
    }

    public record RecognizedSignDto(String id, String label) {
    }
}
