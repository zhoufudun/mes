/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.0
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.mes.productionCounting.internal.states;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.validators.ErrorMessage;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ComponentState.MessageType;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.GridComponent;

@Service
public class ProductionRecordStateService {

    private static final String STATE_FIELD = "state";

    public void changeRecordStateToAccepted(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        changeRecordState(view, ProductionCountingStates.ACCEPTED.getStringValue());
        state.performEvent(view, "save", new String[0]);
    }

    public void changeRecordStateToDeclined(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        changeRecordState(view, ProductionCountingStates.DECLINED.getStringValue());
        state.performEvent(view, "save", new String[0]);
    }

    private void changeRecordState(final ViewDefinitionState view, final String state) {
        FormComponent form = (FormComponent) view.getComponentByReference("form");
        Entity productionCounting = form.getEntity();
        productionCounting.setField("state", state);
        FieldComponent stateField = (FieldComponent) view.getComponentByReference("state");
        stateField.setFieldValue(state);
    }

    public void disabledFieldWhenStateNotDraft(final ViewDefinitionState view) {
        FormComponent form = (FormComponent) view.getComponentByReference("form");
        if (form.getEntity() == null) {
            return;
        }
        Entity productionRecord = form.getEntity();
        String states = productionRecord.getStringField("state");
        if (!states.equals(ProductionCountingStates.DRAFT.getStringValue())) {
            for (String reference : Arrays.asList("lastRecord", "number", "order", "orderOperationComponent", "shift",
                    "machineTime", "laborTime")) {
                FieldComponent field = (FieldComponent) view.getComponentByReference(reference);
                field.setEnabled(false);
                field.requestComponentUpdateState();
            }
            GridComponent gridProductInComponent = (GridComponent) view
                    .getComponentByReference("recordOperationProductInComponent");
            gridProductInComponent.setEditable(false);
            GridComponent gridProductOutComponent = (GridComponent) view
                    .getComponentByReference("recordOperationProductOutComponent");
            gridProductOutComponent.setEditable(false);
        }
    }

    public void changeRecordState(final ViewDefinitionState view, final ComponentState component, final String[] args) {
        final String targetState = getTargetStateFromArgs(args);
        final FormComponent form = (FormComponent) view.getComponentByReference("form");
        final Entity record = form.getEntity();

        setRecordState(view, component, record, targetState);
    }

    private void setRecordState(ViewDefinitionState view, ComponentState component, final Entity record, String targetState) {
        if (record == null) {
            return;
        }

        final boolean sourceComponentIsForm = component instanceof FormComponent;
        final DataDefinition recordDataDefinition = record.getDataDefinition();
        final ComponentState stateFieldComponent = view.getComponentByReference(STATE_FIELD);

        String oldState = record.getStringField(STATE_FIELD);
        String newState = targetState;

        if (newState.equals(oldState)) {
            return;
        }

        if (sourceComponentIsForm) {
            stateFieldComponent.setFieldValue(newState);
            component.performEvent(view, "save", new String[0]);
            Entity savedRecord = recordDataDefinition.get(record.getId());
            stateFieldComponent.setFieldValue(savedRecord.getStringField(STATE_FIELD));
        } else {
            record.setField(STATE_FIELD, newState);
            Entity savedRecord = recordDataDefinition.save(record);

            List<ErrorMessage> errorMessages = Lists.newArrayList();
            errorMessages.addAll(savedRecord.getErrors().values());
            errorMessages.addAll(savedRecord.getGlobalErrors());

            for (ErrorMessage message : errorMessages) {
                view.getComponentByReference("grid").addMessage(message.getMessage(), MessageType.INFO);
            }
        }

    }

    private String getTargetStateFromArgs(final String[] args) {
        if (args != null && args.length > 0 && !"null".equals(args[0])) {
            return args[0];
        }
        return "";
    }
}