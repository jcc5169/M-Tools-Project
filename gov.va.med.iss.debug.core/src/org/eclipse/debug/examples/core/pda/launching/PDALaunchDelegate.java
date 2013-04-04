/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.launching;

import gov.va.mumps.debug.core.MDebugConstants;
import gov.va.mumps.debug.core.MDebugCorePlugin;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.examples.core.pda.model.PDADebugTarget;

/**
 * Launches an M Routine.
 */
public class PDALaunchDelegate extends LaunchConfigurationDelegate {
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		List commandList = new ArrayList();
		
		// Perl executable
		IValueVariable perl = VariablesPlugin.getDefault().getStringVariableManager().getValueVariable(MDebugConstants.ID_PERL_EXECUTABLE);
		if (perl == null) {
			abort("Perl executable location undefined. Check value of ${perlExecutable}.", null);
		}
		String path = perl.getValue();
		if (path == null) {
			abort("Perl executable location unspecified. Check value of ${perlExecutable}.", null);
		}
		File exe = new File(path);
		if (!exe.exists()) {
			//abort(MessageFormat.format("Specified Perl executable {0} does not exist. Check value of ${perlExecutable}.", new String[]{path}), null);
			abort("path: " +path, null);
		}
		commandList.add(path);
		
//		// Add PDA VM
//		File vm = MDebugCorePlugin.getFileInPlugin(new Path("pdavm/pda.pl"));
//		if (vm == null) {
//			abort("Missing PDA VM", null);
//		}
		File vm = new File("C:\\sandbox\\pda.pl");
		commandList.add(vm.getAbsolutePath());
		
		// program name
		String program = configuration.getAttribute(MDebugConstants.ATTR_PDA_PROGRAM, (String)null);
		if (program == null) {
			abort("Perl program unspecified.", null);
		}
		
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(program));
		if (!file.exists()) {
			abort(MessageFormat.format("Perl program {0} does not exist.", new String[] {file.getFullPath().toString()}), null);
		}
		
		commandList.add(file.getLocation().toOSString());
		
		int requestPort = -1;
		int eventPort = -1;
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			requestPort = findFreePort();
			eventPort = findFreePort();
			if (requestPort == -1 || eventPort == -1) {
				abort("Unable to find free port", null);
			}
			commandList.add("-debug");
			commandList.add("" + requestPort);
			commandList.add("" + eventPort);
		}
		
		String[] commandLine = (String[]) commandList.toArray(new String[commandList.size()]);
		Process process = DebugPlugin.exec(commandLine, null);
		IProcess p = DebugPlugin.newProcess(launch, process, path);
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			IDebugTarget target = new PDADebugTarget(launch, p, requestPort, eventPort);
			launch.addDebugTarget(target);
		}
	}
	
	/**
	 * Throws an exception with a new status containing the given
	 * message and optional exception.
	 * 
	 * @param message error message
	 * @param e underlying exception
	 * @throws CoreException
	 */
	private void abort(String message, Throwable e) throws CoreException {
		// TODO: the plug-in code should be the example plug-in, not Perl debug model id
		throw new CoreException(new Status(IStatus.ERROR, MDebugConstants.M_DEBUG_MODEL, 0, message, e));
	}
	
	/**
	 * Returns a free port number on localhost, or -1 if unable to find a free port.
	 * 
	 * @return a free port number on localhost, or -1 if unable to find a free port
	 */
	public static int findFreePort() {
		ServerSocket socket= null;
		try {
			socket= new ServerSocket(0);
			return socket.getLocalPort();
		} catch (IOException e) { 
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
		return -1;		
	}		
}
