/*
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.spinnaker.kato.deploy.aws.ops

import com.netflix.spinnaker.kato.data.task.Task
import com.netflix.spinnaker.kato.data.task.TaskRepository
import com.netflix.spinnaker.kato.deploy.aws.description.EnableAsgDescription
import com.netflix.spinnaker.kato.model.aws.AutoScalingProcessType
import com.netflix.spinnaker.kato.orchestration.AtomicOperation
import com.netflix.spinnaker.kato.services.RegionScopedProviderFactory
import org.springframework.beans.factory.annotation.Autowired

class EnableAsgAtomicOperation implements AtomicOperation<Void> {
  private static final String BASE_PHASE = "ENABLE_ASG"

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }

  private final EnableAsgDescription description

  EnableAsgAtomicOperation(EnableAsgDescription description) {
    this.description = description
  }

  @Autowired
  RegionScopedProviderFactory regionScopedProviderFactory

  @Override
  Void operate(List priorOutputs) {
    task.updateStatus BASE_PHASE, "Initializing Enable ASG operation for '$description.asgName'..."
    for (region in description.regions) {
      try {
        def regionScopedProvider = regionScopedProviderFactory.forRegion(description.credentials, region)

        def asgService = regionScopedProvider.asgService
        def asg = asgService.getAutoScalingGroup(description.asgName)
        if (!asg) {
          task.updateStatus BASE_PHASE, "No ASG named '$description.asgName' found in $region"
          continue
        }
        task.updateStatus BASE_PHASE, "Enabling ASG '$description.asgName' in $region..."
        asgService.resumeProcesses(description.asgName, AutoScalingProcessType.getDisableProcesses())

        task.updateStatus BASE_PHASE, "Registering instances with Load Balancers..."
        def elbService = regionScopedProvider.elbService
        elbService.registerInstancesWithLoadBalancer(asg.loadBalancerNames, asg.instances*.instanceId)

        def eurekaService = regionScopedProvider.getEurekaService(task, BASE_PHASE)
        eurekaService.enableInstancesForAsg(asg.autoScalingGroupName, asg.instances*.instanceId)
      } catch (e) {
        task.updateStatus BASE_PHASE, "Could not enable ASG '$description.asgName' in region $region! Reason: $e.message"
      }
    }
    task.updateStatus BASE_PHASE, "Done enabling ASG $description.asgName."
    null
  }

}