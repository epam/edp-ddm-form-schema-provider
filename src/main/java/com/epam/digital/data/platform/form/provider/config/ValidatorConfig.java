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

package com.epam.digital.data.platform.form.provider.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class ValidatorConfig {

  @Bean
  @SneakyThrows
  public JsonSchema jsonSchema(
      @Value("${validator.schema.location}") String jsonSchemaLocation,
      ResourceLoader resourceLoader) {
    var resource = resourceLoader.getResource(jsonSchemaLocation);

    var factory = JsonSchemaFactory
        .builder(JsonSchemaFactory.getInstance(VersionFlag.V4))
        .objectMapper(new JsonMapper())
        .build();

    return factory.getSchema(resource.getInputStream());
  }
}
