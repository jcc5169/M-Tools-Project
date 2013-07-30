//---------------------------------------------------------------------------
// Copyright 2013 PwC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//---------------------------------------------------------------------------

package gov.va.med.iss.meditor.command;

import java.util.ArrayList;
import java.util.List;

import gov.va.med.foundations.adapter.cci.VistaLinkConnection;
import gov.va.med.iss.connection.actions.VistaConnection;
import gov.va.med.iss.meditor.core.LoadRoutineEngine;
import gov.va.med.iss.meditor.core.CommandResult;
import gov.va.med.iss.meditor.core.MServerRoutine;
import gov.va.med.iss.meditor.core.StatusHelper;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;

public class LoadExistingRoutines extends LoadMultipleRoutines {
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		VistaLinkConnection connection = VistaConnection.getConnection();
		if (connection == null) {
			return null;
		}

		String projectName = VistaConnection.getPrimaryProject();
		List<IFile> selectedFiles = CommandCommon.getSelectedMFiles(event, projectName);
		if (selectedFiles == null) {
			return null;
		}
				
		int overallSeverity = IStatus.OK;
		List<IStatus> statuses = new ArrayList<>();
		for (IFile file : selectedFiles) {
			CommandResult<MServerRoutine> r = LoadRoutineEngine.loadRoutine(connection, file);
			String prefixForFile = file.getFullPath().toString() + " -- ";
			IStatus status = r.getStatus();
			overallSeverity = StatusHelper.updateStatuses(status, prefixForFile, overallSeverity, statuses);
		}
		
		CommandCommon.showMultiStatus(overallSeverity, this.getTopMessage(overallSeverity), statuses);
		return null;
	}
}
