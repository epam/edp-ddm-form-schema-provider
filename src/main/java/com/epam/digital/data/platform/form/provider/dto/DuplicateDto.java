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

package com.epam.digital.data.platform.form.provider.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class DuplicateDto {

  private static final String DUPLICATE_FIELDS_VALIDATION_MESSAGE = "The %s must be unique per form.";
  private static final int ONE = 1;

  private final Map<String, Integer> fieldsMap = new HashMap<>();

  @JsonAnySetter
  public void setValues(String key, Object value) {

    var integer = fieldsMap.get(key);
    fieldsMap.put(key, integer != null ? (integer + ONE) : ONE);
  }

  public Map<String, ValidationError> getMessages() {
    return fieldsMap.entrySet()
        .stream()
        .filter(f -> f.getValue() > ONE)
        .map(stringIntegerEntry ->
            Map.entry(stringIntegerEntry.getKey(),
                ValidationError.builder()
                    .path(stringIntegerEntry.getKey())
                    .massage(String.format(DUPLICATE_FIELDS_VALIDATION_MESSAGE,
                        stringIntegerEntry.getKey()))
                    .build()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }
}
