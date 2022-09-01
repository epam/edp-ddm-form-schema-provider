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


  public FormSchemaProviderServiceImpl(FormRepository repository,
      FormSchemaValidationService formSchemaValidationService) {
    this.formSchemaValidationService = formSchemaValidationService;
    this.repository = repository;
  }

  @Override
  public void saveForm(String formSchemaData) {
    var formSchemaName = retrieveFormSchemaName(formSchemaData, this::checkForSaveIsFormExists);

    saveOrUpdate(formSchemaName, formSchemaData);
  }


  private void saveOrUpdate(String formSchemaName, String formSchemaData) {
    execute(() -> repository.save(FormSchema.builder()
        .id(formSchemaName)
        .formData(formSchemaData)
        .build()));
  }

  private String retrieveFormSchemaName(String formSchemaData,
      BiConsumer<Boolean, String> performIfFormExist) {
    validateFormSchema(formSchemaData);

    var schemaFormName = getSchemaFormKey(formSchemaData);

    boolean isExists = isExistsByKey(schemaFormName);

    performIfFormExist.accept(isExists, schemaFormName);

    return schemaFormName;
  }

  private void validateFormSchema(String formSchemaData) {
    Map<String, ValidationError> validationErrors = formSchemaValidationService.validate(
        formSchemaData);

    if (!validationErrors.isEmpty()) {
      validationErrors.values().forEach(validationError -> log.error(validationError.toString()));
      throw new FormSchemaValidationException("Form Schema is not valid.", validationErrors);
    }
  }

  private String getSchemaFormKey(String formSchemaData) {
    return JSONValue.parse(formSchemaData, JSONObject.class).getAsString(NAME);
  }

  @Override
  public JSONObject getFormByKey(String key) {
    Optional<FormSchema> formSchema = execute(() -> repository.findById(key));

    var schema = formSchema.orElseThrow(() ->
        new FormSchemaDataException(
            String.format("The UI form scheme for the specified key '%s' is missing.", key)));
    var formSchemaData = new String(schema.getFormData().getBytes(StandardCharsets.UTF_8),
        StandardCharsets.UTF_8);

    return JSONValue.parse(formSchemaData, JSONObject.class);
  }

  @Override
  public void updateForm(String key, String formSchemaData) {
    var formSchemaName = retrieveFormSchemaName(formSchemaData, this::checkForUpdateIsFromExists);

    if (!StringUtils.equals(key, formSchemaName)) {
      var errorMessage = String.format(
          "The 'key: %s' from request must be equal to the 'name: %s' from the form data.", key,
          formSchemaName);
      throw new FormSchemaValidationException(errorMessage,
          Map.of(NAME, ValidationError.builder()
              .path(NAME)
              .massage(errorMessage)
              .build()));
    }

    saveOrUpdate(formSchemaName, formSchemaData);
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
      repository.deleteById(key);
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