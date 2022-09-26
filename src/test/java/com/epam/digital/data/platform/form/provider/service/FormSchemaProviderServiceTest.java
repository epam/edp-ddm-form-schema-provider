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

package com.epam.digital.data.platform.form.provider.service;

import com.epam.digital.data.platform.form.provider.dto.ValidationError;
import com.epam.digital.data.platform.form.provider.entity.FormSchema;
import com.epam.digital.data.platform.form.provider.exception.FormDataRepositoryCommunicationException;
import com.epam.digital.data.platform.form.provider.exception.FormSchemaValidationException;
import com.epam.digital.data.platform.form.provider.exception.FormSchemaDataException;
import com.epam.digital.data.platform.form.provider.repository.FormRepository;
import com.epam.digital.data.platform.form.provider.service.impl.FormSchemaProviderServiceImpl;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FormSchemaProviderServiceTest {

  @Mock
  FormRepository repository;

  @Mock
  FormSchemaValidationService formSchemaValidationService;

  ObjectMapper objectMapper = new ObjectMapper();

  FormSchemaProviderService formSchemaProviderService;

  @BeforeEach
  void init() {
    this.formSchemaProviderService =
        new FormSchemaProviderServiceImpl(formSchemaValidationService, repository, objectMapper);
  }

  @Test
  void validSaveForm() throws JsonProcessingException {
    var form = TestUtils.getContent("valid-form-put.json");

    formSchemaProviderService.saveForm(form);

    var captor = ArgumentCaptor.forClass(FormSchema.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo("citizen-shared-officer-sign-app");
    assertThat(objectMapper.readTree(captor.getValue().getFormData()))
        .isEqualTo(objectMapper.readTree(TestUtils.getContent("valid-form.json")));
  }

  @Test
  void saveShouldThrowFormSchemaValidationExceptionWhenFormEmpty() {
    var errors = Map.of("name", ValidationError.builder()
        .massage("name: is missing but it is required")
        .path("name").build());
    when(formSchemaValidationService.validate(any())).thenReturn(
        errors);

    var exception = assertThrows(FormSchemaValidationException.class,
        () -> formSchemaProviderService.saveForm(any()));

    assertThat(exception.getMessage()).isEqualTo("Form Schema is not valid.");
    assertThat(exception.getValidationErrors()).isEqualTo(errors);
    verify(repository, never()).save(any());
  }

  @Test
  void saveShouldThrowFormDataRepositoryCommunicationException() {
    var form = TestUtils.getContent("valid-form.json");
    when(repository.save(any())).thenThrow(new RuntimeException());

    var exception = assertThrows(FormDataRepositoryCommunicationException.class,
        () -> formSchemaProviderService.saveForm(form));

    assertThat(exception.getMessage()).isEqualTo("Error during storage invocation");
  }

  @ParameterizedTest
  @ValueSource(strings = {"valid-form.json", "valid-form-with-special-characters.json"})
  void validGetFormByKey(String filePath) {
    var form = (JSONObject) JSONValue.parse(TestUtils.getContent(filePath));
    String formName = form.getAsString("name");
    var formSchema = FormSchema.builder().id(formName)
        .formData(form.toJSONString()).build();
    when(repository.findById(formName)).thenReturn(Optional.of(formSchema));

    JSONObject formByKey = formSchemaProviderService.getFormByKey(formName);

    assertEquals(form, formByKey);
  }

  @Test
  void getFormByKeyShouldThrowNoFormDataException() {
    var key = "test-key";
    when(repository.findById(any())).thenReturn(Optional.empty());

    var exception = assertThrows(FormSchemaDataException.class,
        () -> formSchemaProviderService.getFormByKey(key));

    assertThat(exception.getMessage()).isEqualTo(
        String.format("The UI form scheme for the specified key '%s' is missing.", key));
  }

  @Test
  void getFormByKeyShouldThrowFormDataRepositoryCommunicationException() {
    when(repository.findById("key")).thenThrow(new RuntimeException());

    var exception = assertThrows(FormDataRepositoryCommunicationException.class,
        () -> formSchemaProviderService.getFormByKey("KEY"));

    assertThat(exception.getMessage()).isEqualTo("Error during storage invocation");
  }

  @Test
  void validUpdateForm() throws JsonProcessingException {
    var form = (JSONObject) JSONValue.parse(TestUtils.getContent("valid-form-put.json"));
    when(repository.existsById("citizen-shared-officer-sign-app")).thenReturn(true);

    formSchemaProviderService.updateForm("CITIZEN-SHARED-OFFICER-SIGN-APP", form.toJSONString());

    var captor = ArgumentCaptor.forClass(FormSchema.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo("citizen-shared-officer-sign-app");
    assertThat(objectMapper.readTree(captor.getValue().getFormData()))
            .isEqualTo(objectMapper.readTree(TestUtils.getContent("valid-form.json")));
  }

  @Test
  void updateFormShouldThrowNoFormDataException() {
    var form = (JSONObject) JSONValue.parse(TestUtils.getContent("valid-form.json"));
    String formName = form.getAsString("name");

    var exception = assertThrows(FormSchemaDataException.class,
        () -> formSchemaProviderService.updateForm(formName, form.toJSONString()));

    assertThat(exception.getMessage()).isEqualTo(
        String.format("The UI form scheme for the specified key '%s' is missing.", formName));
  }

  @Test
  void updateFormShouldThrowFormSchemaValidationExceptionWhenKeysDifferent() {
    var form = (JSONObject) JSONValue.parse(TestUtils.getContent("valid-form.json"));
    String formName = "another-name";
    when(formSchemaValidationService.validate(any())).thenReturn(Collections.emptyMap());

    var exception = assertThrows(FormSchemaValidationException.class,
        () -> formSchemaProviderService.updateForm(formName, form.toJSONString()));

    assertThat(exception.getMessage()).isEqualTo(
        "The 'key: another-name' from request must be equal to the "
            + "'name: citizen-shared-officer-sign-app' from the form data.");
  }


  @Test
  void shouldBeValidationErrorsWhenEntityExists() {
    var form = (JSONObject) JSONValue.parse(TestUtils.getContent("valid-form.json"));
    when(formSchemaValidationService.validate(any())).thenReturn(Collections.emptyMap());
    when(repository.existsById(any())).thenReturn(true);

    var exception = assertThrows(FormSchemaValidationException.class,
        () -> formSchemaProviderService.saveForm(form.toJSONString()));

    assertThat(exception.getMessage()).isEqualTo(
        "The UI form scheme for the specified key 'citizen-shared-officer-sign-app' is already exist.");
  }

  @Test
  void validDeleteFormByKey() {
    var key = "test-key";
    formSchemaProviderService.deleteFormByKey(key);

    verify(repository).deleteById(key);
  }

  @Test
  void deleteFormByKeyShouldThrowFormDataRepositoryCommunicationException() {
    doThrow(new RuntimeException()).when(repository).deleteById("key");

    var exception = assertThrows(FormDataRepositoryCommunicationException.class,
        () -> formSchemaProviderService.deleteFormByKey("KEY"));

    assertThat(exception.getMessage()).isEqualTo("Error during storage invocation");
  }
}