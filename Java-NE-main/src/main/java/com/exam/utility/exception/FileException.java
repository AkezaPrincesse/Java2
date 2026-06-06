package com.exam.utility.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FileException extends RuntimeException {
    public FileException(String message) {
        super(message);
    }
}
