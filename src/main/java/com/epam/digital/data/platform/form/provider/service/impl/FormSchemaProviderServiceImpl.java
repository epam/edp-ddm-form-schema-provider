/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.epam.digital.data.platform.form.provider.service.impl;

import com.epam.digital.data.platform.form.provider.dto.ValidationError;
import com.epam.digital.data.platform.form.provider.entity.FormSchema;
import com.epam.digital.data.platform.form.provider.exception.FormDataRepositoryCommunicationException;
import com.epam.digital.data.platform.form.provider.exception.FormSchemaValidationException;
import com.epam.digital.data.platform.form.provider.exception.FormSchemaDataException;
import com.epam.digital.data.platform.form.provider.repository.FormRepository;
import com.epam.digital.data.platform.form.provider.service.FormSchemaProviderService;
import com.epam.digital.data.platform.form.provider.service.FormSchemaValidationService;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FormSchemaProviderServiceImpl implements FormSchemaProviderService {

  private static final String NAME = "name";

  private final FormSchemaValidationService formSchemaValidationService;
  private final FormRepository repository;
  private final ObjectMapper objectMapper;

  public FormSchemaProviderServiceImpl(
      FormSchemaValidationService formSchemaValidationService,
      FormRepository repository,
      ObjectMapper objectMapper) {
    this.formSchemaValidationService = formSchemaValidationService;
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void saveForm(String formSchemaData) {
    validateFormSchema(formSchemaData);
    JsonNode formSchemaJson = getFormJson(formSchemaData);

    var lowercaseName = formSchemaJson.get(NAME).asText().toLowerCase();
    ((ObjectNode) formSchemaJson).put(NAME, lowercaseName);

    var formName = formSchemaJson.get(NAME).asText();
    validateFormExisting(formName, this::checkForSaveIsFormExists);

    log.debug("Saving form with name: {}", formName);
    saveOrUpdate(formName, formSchemaJson);
  }

  private JsonNode getFormJson(String formSchemaData) {
    try {
      return objectMapper.readTree(formSchemaData);
    } catch (Exception e) {
      throw new FormSchemaDataException("Error while json parsing", e);
    }
  }

  private String serializeFormJson(JsonNode formSchemaJson) {
    try {
      return objectMapper.writeValueAsString(formSchemaJson);
    } catch (Exception e) {
      throw new FormSchemaDataException("Error while json serializing", e);
    }
  }


  private void saveOrUpdate(String formSchemaName, JsonNode formSchemaJson) {
    execute(() -> repository.save(FormSchema.builder()
        .id(formSchemaName)
        .formData(serializeFormJson(formSchemaJson))
        .build()));
  }

  private void validateFormExisting(String formName,
                                      BiConsumer<Boolean, String> performIfFormExist) {

    boolean isExists = isExistsByKey(formName);

    performIfFormExist.accept(isExists, formName);
  }

  private void validateFormSchema(String formSchemaData) {
    Map<String, ValidationError> validationErrors = formSchemaValidationService.validate(
        formSchemaData);

    if (!validationErrors.isEmpty()) {
      validationErrors.values().forEach(validationError -> log.error(validationError.toString()));
      throw new FormSchemaValidationException("Form Schema is not valid.", validationErrors);
    }
  }

  @Override
  public JSONObject getFormByKey(String key) {
    var lowercaseKey = key.toLowerCase();
    Optional<FormSchema> formSchema = execute(() -> repository.findById(lowercaseKey));

    var schema = formSchema.orElseThrow(() ->
        new FormSchemaDataException(
            String.format("The UI form scheme for the specified key '%s' is missing.", key)));
    var formSchemaData = new String(schema.getFormData().getBytes(StandardCharsets.UTF_8),
        StandardCharsets.UTF_8);

    return JSONValue.parse(formSchemaData, JSONObject.class);
  }

  @Override
  public void updateForm(String key, String formSchemaData) {
    validateFormSchema(formSchemaData);
    JsonNode formSchemaJson = getFormJson(formSchemaData);

    var lowercaseName = formSchemaJson.get(NAME).asText().toLowerCase();
    ((ObjectNode) formSchemaJson).put(NAME, lowercaseName);

    var formSchemaName = formSchemaJson.get(NAME).asText();

    if (!StringUtils.equalsIgnoreCase(key, formSchemaName)) {
      var errorMessage = String.format(
          "The 'key: %s' from request must be equal to the 'name: %s' from the form data.", key,
          formSchemaName);
      throw new FormSchemaValidationException(errorMessage,
          Map.of(NAME, ValidationError.builder()
              .path(NAME)
              .massage(errorMessage)
              .build()));
    }

    validateFormExisting(formSchemaName, this::checkForUpdateIsFromExists);
    saveOrUpdate(formSchemaName, formSchemaJson);
  }

  private void checkForUpdateIsFromExists(boolean isExists, String key) {
    if (!isExists) {
      throw new FormSchemaDataException(
          String.format("The UI form scheme for the specified key '%s' is missing.", key));
    }
  }

  private void checkForSaveIsFormExists(boolean isExists, String schemaFormKey) {
    if (isExists) {
      throw new FormSchemaValidationException(
          String.format("The UI form scheme for the specified key '%s' is already exist.",
              schemaFormKey),
          Map.of(NAME, ValidationError.builder().path(NAME)
              .massage("The 'name' must be unique per Project.")
              .build()));
    }
  }

  private boolean isExistsByKey(String key) {
    return execute(() -> repository.existsById(key));
  }

  @Override
  public void deleteFormByKey(String key) {
    try {
      var lowercaseKey = key.toLowerCase();
      repository.deleteById(lowercaseKey);
    } catch (Exception e) {
      throw new FormDataRepositoryCommunicationException("Error during storage invocation", e);
    }
  }

  protected <T> T execute(Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (Exception e) {
      throw new FormDataRepositoryCommunicationException("Error during storage invocation", e);
    }
  }
}