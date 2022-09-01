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

package com.epam.digital.data.platform.form.provider.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.form.provider.service.impl.FormSchemaProviderServiceImpl;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import util.TestUtils;

@ControllerTest(FormSchemaProviderController.class)
class FormSchemaProviderControllerTest {

  static final String BASE_URL = "/api/forms";

  @Autowired
  MockMvc mockMvc;

  @MockBean
  FormSchemaProviderServiceImpl formSchemaProviderService;

  @Test
  @SneakyThrows
  void saveForm() {
    var form = (JSONObject) JSONValue.parse(TestUtils.getContent("valid-from.json"));

    mockMvc.perform(post(BASE_URL)
            .content(form.toJSONString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            status().isCreated());
  }

  @Test
  @SneakyThrows
  void getForm() {
    var form = (JSONObject) JSONValue.parse(TestUtils.getContent("valid-from.json").getBytes(
        StandardCharsets.UTF_8));
    when(formSchemaProviderService.getFormByKey(any())).thenReturn(form);

    mockMvc.perform(get(BASE_URL + "/{key}", form.getAsString("name")))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            content().json(form.toJSONString()));
  }

  @Test
  @SneakyThrows
  void updateForm() {
    var form = (JSONObject) JSONValue.parse(TestUtils.getContent("valid-from.json").getBytes(
        StandardCharsets.UTF_8));
    when(formSchemaProviderService.getFormByKey(any())).thenReturn(form);

    mockMvc.perform(put(BASE_URL + "/{key}", form.getAsString("name"))
            .content(form.toJSONString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            status().isOk());
  }

  @Test
  @SneakyThrows
  void deleteFormByKey() {
    mockMvc.perform(delete(BASE_URL + "/{key}", "test-key"))
        .andExpectAll(
            status().isNoContent());
  }
}