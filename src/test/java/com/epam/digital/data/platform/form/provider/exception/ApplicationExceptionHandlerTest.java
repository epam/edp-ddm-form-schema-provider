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

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.form.provider.controller.FormSchemaProviderController;
import com.epam.digital.data.platform.form.provider.service.impl.FormSchemaProviderServiceImpl;
import lombok.SneakyThrows;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import util.TestUtils;

@WebMvcTest
@ContextConfiguration(
    classes = {FormSchemaProviderController.class, ApplicationExceptionHandler.class}
)
@AutoConfigureMockMvc(addFilters = false)
class ApplicationExceptionHandlerTest {

  static final String BASE_URL = "/api/forms";

  @Autowired
  MockMvc mockMvc;

  @MockBean
  FormSchemaProviderServiceImpl formSchemaProviderService;

  @Test
  @SneakyThrows
  void shouldReturnRuntimeErrorOnGenericException() {
    var form = TestUtils.getContent("valid-form.json");
    doThrow(RuntimeException.class).when(formSchemaProviderService).saveForm(form);

    mockMvc.perform(post(BASE_URL)
            .content(form)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError())
        .andExpectAll(
            jsonPath("$.code").value(is("RUNTIME_ERROR")),
            jsonPath("$.statusDetails").doesNotExist());
  }

  @Test
  @SneakyThrows
  void shouldReturnNoFormDataException() {
    when(formSchemaProviderService.getFormByKey(any())).thenThrow(new FormSchemaDataException("ERROR"));

    mockMvc.perform(get(BASE_URL + "/{key}", "test-key"))
        .andExpect(status().isNotFound())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof FormSchemaDataException))
        .andExpect(
            jsonPath("$.code").value(is("FORM_SCHEMA_NOT_FOUND"))
        );
  }

  @Test
  @SneakyThrows
  void shouldReturnAccessDeniedException() {
    when(formSchemaProviderService.getFormByKey(any())).thenThrow(AccessDeniedException.class);

    mockMvc.perform(get(BASE_URL + "/key", "test-key"))
        .andExpectAll(
            status().isForbidden(),
            jsonPath("$.code").value(is("FORBIDDEN_OPERATION")));
  }


  @Test
  @SneakyThrows
  void shouldReturnFormDataRepositoryCommunicationException() {
    when(formSchemaProviderService.getFormByKey(any())).thenThrow(
        FormDataRepositoryCommunicationException.class);

    mockMvc.perform(get(BASE_URL + "/key", "test-key"))
        .andExpectAll(
            status().isInternalServerError(),
            jsonPath("$.code").value(is("RUNTIME_ERROR")));
  }

  @Test
  @SneakyThrows
  void shouldReturnFormSchemaValidationException() {
    var form = (JSONObject) JSONValue.parse(TestUtils.getContent("valid-form.json"));
    doThrow(FormSchemaValidationException.class).when(formSchemaProviderService).saveForm(any());

    mockMvc.perform(post(BASE_URL)
            .content(form.toJSONString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            status().isUnprocessableEntity(),
            jsonPath("$.code").value(is("FORM_VALIDATION_EXCEPTION")));
  }
}