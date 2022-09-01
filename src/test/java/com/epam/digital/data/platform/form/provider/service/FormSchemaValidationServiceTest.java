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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.epam.digital.data.platform.form.provider.dto.ValidationError;
import com.epam.digital.data.platform.form.provider.exception.FormSchemaValidationException;
import com.epam.digital.data.platform.form.provider.service.impl.FormSchemaValidationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.Map;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.collection.IsMapWithSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.ResourceLoader;
import util.TestUtils;

@ExtendWith(MockitoExtension.class)
class FormSchemaValidationServiceTest {

  private static final String FORMS_JSON_SCHEMA = "classpath:schema/forms-schema.json";

  private final ResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());
  FormSchemaValidationService formSchemaValidationService;

  @BeforeEach
  public void setUp() {
    this.formSchemaValidationService = new FormSchemaValidationServiceImpl(testJsonSchema(),
        new ObjectMapper());
  }

  @ParameterizedTest
  @ValueSource(strings = {"valid-from.json", "valid-form-with-special-characters.json"})
  void shouldNotHaveErrors(String path) {
    var formData = TestUtils.getContent(path);

    var validationErrors = formSchemaValidationService.validate(formData);

    assertThat(validationErrors, is(IsMapWithSize.anEmptyMap()));
  }

  @Test
  void shouldBeValidationErrorsOnDuplicateName() {
    var formData = TestUtils.getContent("duplicate-properties-form.json");

    var validationErrors = formSchemaValidationService.validate(formData);
    System.err.println(validationErrors);
    assertThat(validationErrors, is(IsMapWithSize.aMapWithSize(4)));
  }

  @Test
  void shouldBeValidationErrorsOnMissedRequiredProperties() {
    var expectedErrors = Map.of("type", ValidationError.builder()
        .path("type")
        .massage("$.type: is missing but it is required").build());
    var formData = TestUtils.getContent("missed-required-properties-form.json");

    var validationErrors = formSchemaValidationService.validate(formData);

    assertThat(validationErrors, is(IsMapWithSize.aMapWithSize(1)));
    assertEquals(expectedErrors, validationErrors);
  }

  @Test
  void shouldThrowFormSchemaValidationExceptionWhenBrokenStructure() {
    var formData = TestUtils.getContent("broken-structure-form.json");

    var exception = assertThrows(FormSchemaValidationException.class,
        () -> formSchemaValidationService.validate(formData));

    Assertions.assertThat(exception.getMessage())
        .isEqualTo("Error during form schema validation: schema is not valid");
  }

  @Test
  void shouldBeValidationErrorsOnNotEqualNameAndPathProperties() {
    var expectedErrors = Map.of("path", ValidationError.builder()
        .path("path")
        .massage("The 'path' must be equal to the 'name'")
        .build());
    var formData = TestUtils.getContent("not-equal-name-and-path-properties-form.json");

    var validationErrors = formSchemaValidationService.validate(formData);

    assertThat(validationErrors, is(IsMapWithSize.aMapWithSize(1)));
    assertEquals(expectedErrors, validationErrors);
  }

  @SneakyThrows
  public JsonSchema testJsonSchema() {
    var resource = resourceLoader.getResource(FORMS_JSON_SCHEMA);
    var factory = JsonSchemaFactory
        .builder(JsonSchemaFactory.getInstance(VersionFlag.V4))
        .objectMapper(new JsonMapper())
        .build();

    return factory.getSchema(resource.getInputStream());
  }
}