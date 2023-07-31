package com.sudhanshujoshi.urlshortner;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ErrorResponse {
    private String errorMessage;
    private Integer errorCode;
}
