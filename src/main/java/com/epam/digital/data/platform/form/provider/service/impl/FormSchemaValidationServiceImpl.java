/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.form.provider.service.impl;

import com.epam.digital.data.platform.form.provider.dto.DuplicateDto;
import com.epam.digital.data.platform.form.provider.dto.ValidationError;
import com.epam.digital.data.platform.form.provider.exception.FormSchemaValidationException;
import com.epam.digital.data.platform.form.provider.service.FormSchemaValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FormSchemaValidationServiceImpl implements FormSchemaValidationService {

  private static final int PROPERTY_PATH_INDEX = 0;

  private final JsonSchema schema;
  private final ObjectMapper objectMapper;

  public FormSchemaValidationServiceImpl(JsonSchema schema, ObjectMapper objectMapper) {
    this.schema = schema;
    this.objectMapper = objectMapper;
  }

  @Override
  public Map<String, ValidationError> validate(String formSchemaData) {
    try {
      var jsonNode = objectMapper.readTree(formSchemaData);
      var validationErrorMap = validateSchemaStructure(jsonNode);

      validationErrorMap.putAll(validateDuplications(formSchemaData));

      return validationErrorMap;
    } catch (JsonProcessingException e) {
      throw new FormSchemaValidationException(
          "Error during form schema validation: schema is not valid", e);
    }
  }


  private Map<String, ValidationError> validateSchemaStructure(JsonNode jsonNode) {
    var validationMessages = schema.validate(jsonNode);
    Map<String, ValidationError> validationErrorMap = new HashMap<>();

    for (ValidationMessage validationMessage : validationMessages) {
      String propertyWithError = ArrayUtils.get(validationMessage.getArguments(),
          PROPERTY_PATH_INDEX);

      if (StringUtils.isNotBlank(propertyWithError)) {
        validationErrorMap.put(propertyWithError,
            ValidationError.builder()
                .path(propertyWithError)
                .massage(validationMessage.getMessage())
                .build());
      }
    }

    return validationErrorMap;
  }

  private Map<String, ValidationError> validateDuplications(String formSchemaData)
      throws JsonProcessingException {
    var duplicateDto = objectMapper.readValue(formSchemaData, DuplicateDto.class);
    return duplicateDto.getMessages();
  }

}
