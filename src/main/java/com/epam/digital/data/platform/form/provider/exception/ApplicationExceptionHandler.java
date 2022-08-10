/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.form.provider.exception;

import com.epam.digital.data.platform.form.provider.dto.DetailedErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static com.epam.digital.data.platform.form.provider.util.Header.TRACE_ID;

@Slf4j
@RestControllerAdvice
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String FORM_VALIDATION_EXCEPTION = "FORM_VALIDATION_EXCEPTION";
  private static final String FORM_SCHEMA_NOT_FOUND = "FORM_SCHEMA_NOT_FOUND";
  private static final String FORBIDDEN_OPERATION = "FORBIDDEN_OPERATION";
  private static final String RUNTIME_ERROR = "RUNTIME_ERROR";


  @ExceptionHandler(Exception.class)
  public ResponseEntity<DetailedErrorResponse> handleException(Exception exception) {
    log.error("Runtime error occurred", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(RUNTIME_ERROR, exception));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<DetailedErrorResponse> handleAccessDeniedException(
      AccessDeniedException exception) {
    log.error("Access denied", exception);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(newDetailedResponse(FORBIDDEN_OPERATION, exception));
  }

  @ExceptionHandler(FormSchemaDataException.class)
  public ResponseEntity<DetailedErrorResponse> handleNoFormDataException(
      FormSchemaDataException exception) {
    log.error("No form data", exception);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(newDetailedResponse(FORM_SCHEMA_NOT_FOUND, exception));
  }

  @ExceptionHandler(FormDataRepositoryCommunicationException.class)
  public ResponseEntity<DetailedErrorResponse> handleFormDataRepositoryCommunicationException(
      FormDataRepositoryCommunicationException exception) {
    log.error("Error during communication with repository", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(RUNTIME_ERROR, exception));
  }

  @ExceptionHandler(FormSchemaValidationException.class)
  public ResponseEntity<DetailedErrorResponse> handleFormSchemaValidationException(
      FormSchemaValidationException exception) {
    log.error("Form validation exception", exception);

    var detailedErrorResponse = newDetailedResponse(FORM_VALIDATION_EXCEPTION,
        exception);
    detailedErrorResponse.setErrors(exception.getValidationErrors());

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(detailedErrorResponse);
  }

  private DetailedErrorResponse newDetailedResponse(String code, Exception exception) {
    var response = new DetailedErrorResponse();
    response.setTraceId(MDC.get(TRACE_ID.getHeaderName()));
    response.setCode(code);
    response.setDetails(exception.getMessage());

    return response;
  }
}
