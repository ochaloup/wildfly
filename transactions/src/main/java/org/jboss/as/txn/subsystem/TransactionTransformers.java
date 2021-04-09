/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

import static org.jboss.as.txn.subsystem.TransactionExtension.CURRENT_MODEL_VERSION;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.MAXIMUM_TIMEOUT;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.STALE_TRANSACTION_TIME;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.TRANSACTIONS_ENABLED;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.ORPHAN_SAFETY_INTERVAL;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.RECOVERY_BACKOFF_PERIOD;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.RECOVERY_PERIOD;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.ALLOW_RECOVERY_SUSPENSION;

/**
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class TransactionTransformers implements ExtensionTransformerRegistration{
    static final ModelVersion MODEL_VERSION_EAP62 = ModelVersion.create(1, 3);
    static final ModelVersion MODEL_VERSION_EAP63 = ModelVersion.create(1, 4);
    static final ModelVersion MODEL_VERSION_EAP64 = ModelVersion.create(1, 5);
    static final ModelVersion MODEL_VERSION_EAP70 = ModelVersion.create(3, 0);
    static final ModelVersion MODEL_VERSION_EAP71 = ModelVersion.create(4, 0);
    static final ModelVersion MODEL_VERSION_EAP72 = ModelVersion.create(5, 0);
    static final ModelVersion MODEL_VERSION_EAP73 = ModelVersion.create(5, 1);
    static final ModelVersion MODEL_VERSION_5_2_0 = ModelVersion.create(5, 2);
    static final ModelVersion MODEL_VERSION_6_0_0 = ModelVersion.create(6, 0);


    @Override
    public String getSubsystemName() {
        return TransactionExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(CURRENT_MODEL_VERSION);

        // 6.1.0 --> 6.0.0
        ResourceTransformationDescriptionBuilder builder60 = chainedBuilder.createBuilder(CURRENT_MODEL_VERSION, MODEL_VERSION_6_0_0);
        builder60.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, TRANSACTIONS_ENABLED)
                .addRejectCheck(RejectAttributeChecker.DEFINED, TRANSACTIONS_ENABLED)
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, RECOVERY_PERIOD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, RECOVERY_PERIOD)
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, RECOVERY_BACKOFF_PERIOD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, RECOVERY_BACKOFF_PERIOD)
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, ALLOW_RECOVERY_SUSPENSION)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ALLOW_RECOVERY_SUSPENSION)
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, ORPHAN_SAFETY_INTERVAL)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ORPHAN_SAFETY_INTERVAL)
                .end();
        builder60.addChildResource(TransactionExtension.LOG_STORE_PATH)
                .addOperationTransformationOverride(LogStoreConstants.PROCESS_RECOVERY)
                .setReject();

        // 6.0.0 --> 5.2.0
        ResourceTransformationDescriptionBuilder builder52 = chainedBuilder.createBuilder(MODEL_VERSION_6_0_0, MODEL_VERSION_5_2_0);
        builder52.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, STALE_TRANSACTION_TIME)
                .addRejectCheck(RejectAttributeChecker.DEFINED, STALE_TRANSACTION_TIME)
                .end();

        // 5.2.0 --> 5.1.0
        // CMR resources adds/removes requires restart of JVM
        chainedBuilder.createBuilder(MODEL_VERSION_5_2_0, MODEL_VERSION_EAP73);

        // 5.1.0 --> 5.0.0
        ResourceTransformationDescriptionBuilder builderEap72 = chainedBuilder.createBuilder(MODEL_VERSION_EAP73, MODEL_VERSION_EAP72);
        builderEap72.getAttributeBuilder().end(); // node-id attribute requires restart of JVM instead of reload

        // 5.0.0 --> 4.0.0
        ResourceTransformationDescriptionBuilder builderEap71 = chainedBuilder.createBuilder(MODEL_VERSION_EAP72, MODEL_VERSION_EAP71);
        builderEap71.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, MAXIMUM_TIMEOUT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, MAXIMUM_TIMEOUT)
                .end();

        // 4.0.0 --> 3.0.0
        /*
        Missing attributes in current: []; missing in legacy [number-of-system-rollbacks, average-commit-time] //both runtime
        Different 'default' for attribute 'object-store-relative-to'. Current: undefined; legacy: "jboss.server.data.dir"
        Different 'nillable' for attribute 'process-id-socket-binding'. Current: true; legacy: false //alternatives
        Different 'nillable' for attribute 'process-id-uuid'. Current: true; legacy: false //alternatives
         */

        ResourceTransformationDescriptionBuilder builderEap70 = chainedBuilder.createBuilder(MODEL_VERSION_EAP71, MODEL_VERSION_EAP70);
        builderEap70.getAttributeBuilder()
                .setValueConverter(AttributeConverter.DEFAULT_VALUE, OBJECT_STORE_RELATIVE_TO)
                .end();

        builderEap70.addChildResource(TransactionExtension.LOG_STORE_PATH)
                .addChildResource(TransactionExtension.TRANSACTION_PATH)
                .addChildResource(TransactionExtension.PARTICIPANT_PATH)
                .addOperationTransformationOverride(LogStoreConstants.DELETE)
                .setReject();

        // 3.0.0 --> 1.5.0
        ResourceTransformationDescriptionBuilder builderEap64 = chainedBuilder.createBuilder(MODEL_VERSION_EAP70, MODEL_VERSION_EAP64);
        builderEap64.getAttributeBuilder()
                .addRename(TransactionSubsystemRootResourceDefinition.USE_JOURNAL_STORE, CommonAttributes.USE_HORNETQ_STORE)
                .addRename(TransactionSubsystemRootResourceDefinition.JOURNAL_STORE_ENABLE_ASYNC_IO, CommonAttributes.HORNETQ_STORE_ENABLE_ASYNC_IO)
                .addRename(TransactionSubsystemRootResourceDefinition.STATISTICS_ENABLED, CommonAttributes.ENABLE_STATISTICS)
                .end();

        // 1.5.0 --> 1.4.0
        ResourceTransformationDescriptionBuilder builderEap63 = chainedBuilder.createBuilder(MODEL_VERSION_EAP64, MODEL_VERSION_EAP63);
        builderEap63.rejectChildResource(CMResourceResourceDefinition.PATH_CM_RESOURCE);

        //1.4.0 --> 1.3.0
        chainedBuilder.createBuilder(MODEL_VERSION_EAP63, MODEL_VERSION_EAP62);

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{
                MODEL_VERSION_EAP62,
                MODEL_VERSION_EAP63,
                MODEL_VERSION_EAP64,
                MODEL_VERSION_EAP70,
                MODEL_VERSION_EAP71,
                MODEL_VERSION_EAP72,
                MODEL_VERSION_EAP73,
                MODEL_VERSION_5_2_0,
                MODEL_VERSION_6_0_0
        });
    }
}
