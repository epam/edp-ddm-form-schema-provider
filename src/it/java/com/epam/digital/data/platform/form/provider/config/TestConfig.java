/*
 * Copyright 2023 EPAM Systems.
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

import com.epam.digital.data.platform.form.provider.FormSchemaProviderApplication;
import com.epam.digital.data.platform.form.provider.service.impl.FormSchemaProviderServiceImpl;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@ConditionalOnMissingBean(FormSchemaProviderApplication.class)
public class TestConfig {

  @Bean
  public FormSchemaProviderServiceImpl testFormSchemaProviderService() {
    return Mockito.mock(FormSchemaProviderServiceImpl.class);
  }

  @Bean
  public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
    return Mockito.mock(RedisConnectionFactory.class);
  }
}