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

package us.pwc.vista.eclipse.server.command;

import gov.va.med.iss.connection.VistAConnection;
import gov.va.med.iss.connection.VLConnectionPlugin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;

import us.pwc.vista.eclipse.core.helper.MessageDialogHelper;
import us.pwc.vista.eclipse.server.Messages;
import us.pwc.vista.eclipse.server.core.CommandResult;
import us.pwc.vista.eclipse.server.core.LoadRoutineEngine;
import us.pwc.vista.eclipse.server.core.MServerRoutine;

/**
 * This implementation of <code>AbstractHandler</code> updates the M file 
 * in the active Editor from M server.
 *
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class LoadEditorRoutine extends AbstractHandler {
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		IEditorInput input = HandlerUtil.getActiveEditorInput(event);
		IFile file = (IFile) input.getAdapter(IFile.class);
		if (file == null) {
			return null;
		}
		
		IProject project = file.getProject();
		VistAConnection vistaConnection = VLConnectionPlugin.getConnectionManager().getConnection(project);
		
		CommandResult<MServerRoutine> r = LoadRoutineEngine.loadRoutine(vistaConnection, file);
		MessageDialogHelper.logAndShow(Messages.LOAD_MSG_TITLE, r.getStatus());
		return null;
	}
}
