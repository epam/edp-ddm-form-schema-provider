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

import com.epam.digital.data.platform.form.provider.service.impl.FormSchemaProviderServiceImpl;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import net.minidev.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(description = "UI form schemes providing service", name = "form-schemes-providing-api")
@RequestMapping("/api/forms")
public class FormSchemaProviderController {

  private final FormSchemaProviderServiceImpl formSchemaProviderServiceImpl;

  public FormSchemaProviderController(FormSchemaProviderServiceImpl formSchemaProviderServiceImpl) {
    this.formSchemaProviderServiceImpl = formSchemaProviderServiceImpl;
  }

  @PostMapping
  @Operation(summary = "Upload form for business process",
      description = "### Endpoint purpose:\n This endpoint allows to upload a form that being used by process instance for get user input data. " +
          "Input form being validated for duplicate names, validation of form schema structure and required properties fillment. Example : property `name` is required and should be unique for registry ",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
          schema = @Schema(implementation = String.class),
          examples = {
              @ExampleObject(value = "{\n" +
                  "  \"title\": \"Test Form\",\n" +
                  "  \"path\": \"test-form\",\n" +
                  "  \"name\": \"test-form\",\n" +
                  "  \"display\": \"form\",\n" +
                  "  \"components\": [\n" +
                  "    {\n" +
                  "      \"type\": \"button\",\n" +
                  "      \"label\": \"Submit\",\n" +
                  "      \"key\": \"submit\",\n" +
                  "      \"size\": \"md\",\n" +
                  "      \"...\"\n" +
                  "    }\n" +
                  "  ],\n" +
                  "}"
              )
          })),
      responses = {
          @ApiResponse(
              responseCode = "201",
              description = "Form saved successfully"
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "401",
              description = "You are not authorized to add the form",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Form scheme is not valid",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
      })
  public ResponseEntity<Void> saveForm(@RequestBody String formData) {
    formSchemaProviderServiceImpl.saveForm(formData);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping("/{key}")
  @Operation(summary = "Download form by key",
      description = "### Endpoint purpose:\n This endpoint allows to download a form. The form is returned as a JSON object.",
      parameters = {
        @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
          ),
          @Parameter(
              name = "key",
              description = "Form key",
              in = ParameterIn.PATH,
              required = true,
              schema = @Schema(implementation = String.class)
          )
      },
      responses = {
          @ApiResponse(
              description = "Returns uploaded form metadata",
              responseCode = "200",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = String.class),
              examples = {
                  @ExampleObject(value = "{\n" +
                      "  \"title\": \"Test Form\",\n" +
                      "  \"path\": \"test-form\",\n" +
                      "  \"name\": \"test-form\",\n" +
                      "  \"display\": \"form\",\n" +
                      "  \"components\": [\n" +
                      "    {\n" +
                      "      \"type\": \"button\",\n" +
                      "      \"label\": \"Submit\",\n" +
                      "      \"key\": \"submit\",\n" +
                      "      \"size\": \"md\",\n" +
                      "      \"...\"\n" +
                      "    }\n" +
                      "  ],\n" +
                      "}"
                  )
              })
          ),
          @ApiResponse(
              responseCode = "401",
              description = "You are not authorized to get the form",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Form Not Found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
      }
  )
  public ResponseEntity<JSONObject> getForm(@PathVariable("key") String key) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(formSchemaProviderServiceImpl.getFormByKey(key));
  }

  @PutMapping("/{key}")
  @Operation(summary = "Update form for business process",
      description = "### Endpoint purpose:\n This endpoint allows to update a form that being used by process instance for get user input data. Input form being validated for DuplicateNames, and required properties fillment, and validation of form schema structure",
      parameters = {
          @Parameter(
              in = ParameterIn.HEADER,
              name = "X-Access-Token",
              description = "Token used for endpoint security",
              required = true,
              schema = @Schema(type = "string")
          ),
          @Parameter(
              name = "key",
              description = "Form key",
              in = ParameterIn.PATH,
              required = true,
              schema = @Schema(implementation = String.class)
          )

      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = String.class),
              examples = {
                  @ExampleObject(value = "{\n" +
                      "  \"title\": \"Test Form\",\n" +
                      "  \"path\": \"test-form\",\n" +
                      "  \"name\": \"test-form\",\n" +
                      "  \"display\": \"form\",\n" +
                      "  \"components\": [\n" +
                      "    {\n" +
                      "      \"type\": \"button\",\n" +
                      "      \"label\": \"Submit\",\n" +
                      "      \"key\": \"submit\",\n" +
                      "      \"size\": \"md\",\n" +
                      "      \"...\"\n" +
                      "    }\n" +
                      "  ],\n" +
                      "}"
                  )
              })
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Form updated successfully"
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "401",
              description = "You are not authorized to update the form",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "422",
              description = "Form scheme is not valid",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
      })
  public ResponseEntity<Void> updateForm(@PathVariable("key") String key,
      @RequestBody String formSchemaData) {
    formSchemaProviderServiceImpl.updateForm(key, formSchemaData);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @DeleteMapping("/{key}")
  @Operation(
      summary = "Delete form by key",
      description = "### Endpoint purpose:\n This endpoint allows the deletion of a specific form.",
      parameters = {
          @Parameter(
              in = ParameterIn.HEADER,
              name = "X-Access-Token",
              description = "Token used for endpoint security",
              required = true,
              schema = @Schema(type = "string")
          ),
          @Parameter(
              name = "key",
              description = "Form key",
              in = ParameterIn.PATH,
              required = true,
              schema = @Schema(implementation = String.class)
          )
      },
      responses = {
          @ApiResponse(
              description = "Form deleted successfully",
              responseCode = "204"
          ),
          @ApiResponse(
              responseCode = "401",
              description = "You are not authorized to delete the form",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "403",
              description = "Forbidden",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
      }
  )
  public ResponseEntity<Void> deleteFormByKey(@PathVariable("key") String key) {
    formSchemaProviderServiceImpl.deleteFormByKey(key);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}