/*
 * Copyright 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.kubernetes

import com.netflix.spinnaker.clouddriver.kubernetes.config.KubernetesConfigurationProperties
import com.netflix.spinnaker.clouddriver.kubernetes.deploy.KubernetesUtil
import com.netflix.spinnaker.clouddriver.kubernetes.health.KubernetesHealthIndicator
import com.netflix.spinnaker.clouddriver.kubernetes.security.KubernetesCredentialsInitializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableConfigurationProperties
@EnableScheduling
@ConditionalOnProperty('kubernetes.enabled')
@ComponentScan(["com.netflix.spinnaker.clouddriver.kubernetes"])
@PropertySource(value = "classpath:META-INF/clouddriver-core.properties", ignoreResourceNotFound = true)
@Import([ KubernetesCredentialsInitializer ])
class KubernetesConfiguration {
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Bean
  @ConfigurationProperties("kubernetes")
  KubernetesConfigurationProperties kubernetesConfigurationProperties() {
    new KubernetesConfigurationProperties()
  }

  @Bean
  KubernetesHealthIndicator kubernetesHealthIndicator() {
    new KubernetesHealthIndicator()
  }

  @Bean
  String kubernetesApplicationName(@Value('${Implementation-Version:Unknown}') String implementationVersion) {
    "Spinnaker/$implementationVersion"
  }

  @Bean
  KubernetesUtil kubernetesUtil() {
    new KubernetesUtil()
  }
}
