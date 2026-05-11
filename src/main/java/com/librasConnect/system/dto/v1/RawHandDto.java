package com.librasConnect.system.dto.v1;

import java.util.List;

public record RawHandDto(String role, List<List<Double>> landmarks) {
}
