package us.pwc.vista.eclipse.server.core;

import gov.va.med.foundations.utilities.FoundationsException;
import gov.va.med.iss.connection.VistAConnection;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.statushandlers.StatusManager;

import us.pwc.vista.eclipse.core.ServerData;
import us.pwc.vista.eclipse.core.helper.MessageConsoleHelper;
import us.pwc.vista.eclipse.core.resource.InvalidFileException;
import us.pwc.vista.eclipse.core.resource.ResourceUtilExtension;
import us.pwc.vista.eclipse.server.Messages;
import us.pwc.vista.eclipse.server.VistAServerPlugin;

public class SaveRoutineEngine {
	private static final String SAVE_ROUTINE_CONSOLE = "Save Routine Console";

	private static ListRoutineBuilder getListRoutineBuilder(IFile file) throws InvalidFileException, CoreException, BadLocationException {		
		IDocument document = ResourceUtilExtension.getDocument(file);
		ListRoutineBuilder target = new ListRoutineBuilder();
		boolean updated = ResourceUtilExtension.cleanCode(document, target);
		if (updated) {
			String message = Messages.bind(Messages.NOT_SUPPORTED_MFILE_CONTENT, file.getName());
			IStatus status = new Status(IStatus.ERROR, VistAServerPlugin.PLUGIN_ID, message);
			throw new CoreException(status);
		}
		return target;
	}
	
	private static IStatus saveRoutineToServer(VistAConnection vistaConnection, String routineName, ListRoutineBuilder builder, IFile backupFile, StringBuilder consoleMessage) {
		String warningMessage = "";
		try {
			List<String> contents = builder.getRoutineLines();
			IStatus result = saveRoutineToServer(vistaConnection, routineName, contents, consoleMessage);
			if (result.getSeverity() == IStatus.ERROR) {
				return result;
			}
			if (result.getSeverity() != IStatus.OK) {
				String message = result.getMessage();
				warningMessage += "\n" + message;
			}
			
			MessageConsoleHelper.writeToConsole(SAVE_ROUTINE_CONSOLE, consoleMessage.toString(), true);
			
			if (backupFile != null) {
				String routine = builder.getRoutine();
				synchBackupFile(backupFile, routine);
			}
		} catch (BackupSynchException bse) {
			String message = bse.getMessage();
			IStatus errStatus = new Status(IStatus.ERROR, VistAServerPlugin.PLUGIN_ID, message, bse);
			StatusManager.getManager().handle(errStatus, StatusManager.LOG);
			warningMessage += "\n" + message;
		} catch (Throwable t) {
			String message = Messages.bind(Messages.UNABLE_RTN_SAVE, routineName, t.getMessage());
			return new Status(IStatus.ERROR, VistAServerPlugin.PLUGIN_ID, message, t);
		}		

		if (warningMessage.length() > 0) {
			warningMessage = Messages.bind(Messages.ROUTINE_SAVED_W_WARNINGS, routineName) + warningMessage;
			return new Status(IStatus.WARNING, VistAServerPlugin.PLUGIN_ID, warningMessage); 
		} else {
			return Status.OK_STATUS;
		}
	}
	
	private static StringBuilder startConsoleMessage(String routineName, ServerData serverData, BackupSynchResult synchResult) {
		StringBuilder result = new StringBuilder();
		String header = routineName + " saved to: " + serverData.toUIString() + ")\n";
		result.append(header);
		
		IFile backupFile = synchResult.getFile();
		BackupSynchStatus status = synchResult.getStatus();
		if (status == BackupSynchStatus.INITIATED) {
			String message = Messages.bind(Messages.SERVER_FIRST_SAVE, backupFile.getFullPath().toString());
			result.append(message);
			result.append("\n");
		}
		if (status == BackupSynchStatus.NO_CHANGE_SERVER_DELETED) {
			result.append(Messages.SERVER_DELETED);				
			result.append("\n");
		}
		result.append("\n");
		return result;
	}

	private static IStatus synchBackupFile(IFile backupFile, String routine) throws BackupSynchException {
		try {
			InputStream stream = new ByteArrayInputStream(routine.getBytes("UTF-8"));
			if (backupFile.exists()) {
				backupFile.setContents(stream, true, true, null);
			} else {
				ResourceUtilExtension.prepareFolders((IFolder) backupFile.getParent());			
				backupFile.create(stream, true, null);
			}
			stream.close();
			return Status.OK_STATUS;
		} catch (Throwable t) {
			String message = Messages.bind(Messages.SAVE_BACKUP_SYNCH_ERROR, backupFile.getName());
			throw new BackupSynchException(message, t);
		} 
	}
	
	private static boolean updateConsoleMessage(String doc, StringBuilder consoleMessage) {
		boolean isErrorsOrWarnings = false;
		int n = 0;
		if (doc.contains("no tags with variables to list")) {
			doc = doc.replace("Variables which are neither NEWed or arguments","");
			doc = doc.replace("no tags with variables to list","");
		}
		while (doc.contains("\n\n")) {
			doc = doc.replaceAll("\n\n","\n");
		}
		while (n < doc.length()) {
			int n1 = doc.indexOf('\n',n);
			String str = doc.substring(n,n1);
			int nbase = n;
			n = n1+1;
			if (str.indexOf("Compiled list of Errors and Warnings") == 0) {
				n1 = doc.indexOf('\n',n);
				String str1 = doc.substring(n,n1);
				if (str1.compareTo("No errors or warnings to report") == 0) {
					String str2 = "";
					if (nbase > 0) {
						str2 = doc.substring(0,nbase);
					}
					doc = str2 + doc.substring(n1+1,doc.length());
					n = nbase;
				}
				else
					isErrorsOrWarnings = true;
			}
			if (str.compareTo("Variables which are neither NEWed or arguments") == 0) {
				n = n + 1;  // skip blank line
				n1 = doc.indexOf('\n',n);
				String str1 = doc.substring(n,n1);
				if (str1.compareTo("no tags with variables to list") == 0) {
					String str2 = "";
					if (nbase > 0) {
						str2 = doc.substring(0,nbase);
					}
					doc = str2 + doc.substring(n1,doc.length());
					n = nbase;
				}
			}
		}
		while (doc.indexOf('\n') == 0) {
			if (doc.length() > 1)
				doc = doc.substring(1);
			else
				doc = "";
		}
		consoleMessage.append(doc);
		return isErrorsOrWarnings;
	}
	
	private static IStatus saveRoutineToServer(VistAConnection vistaConnection, String routineName, List<String> contents, StringBuilder consoleMessage) throws FoundationsException {
		String rpcResult = vistaConnection.rpc("XT ECLIPSE M EDITOR", "RS", contents, routineName, "0^^0");
		int index = rpcResult.indexOf('\n');
		if (index > -1) {
			String line1 = rpcResult.substring(0, index);
			if (line1.indexOf("-1") == 0) {
				String[] pieces = line1.split("\\^");
				String message = (pieces.length > 1) ? pieces[1] : "Unknown error saving the routine to server.";
				IStatus r = new Status(IStatus.ERROR, VistAServerPlugin.PLUGIN_ID, message);
				return r;
			}
			String doc = rpcResult.substring(rpcResult.indexOf('\n'));
			boolean isErrorsOrWarnings = updateConsoleMessage(doc, consoleMessage);
			if (isErrorsOrWarnings) {
				IStatus r = new Status(IStatus.WARNING, VistAServerPlugin.PLUGIN_ID, Messages.XINDEX_IN_CONSOLE);
				return r;
			}			
		}
		return Status.OK_STATUS;
	}
	
	public static IStatus saveRoutine(VistAConnection vistaConnection, IFile file) {
		try {
			ListRoutineBuilder routineContent = getListRoutineBuilder(file);

			MServerRoutine serverRoutine = MServerRoutine.load(vistaConnection, file);
			String routineName = serverRoutine.getRoutineName();
			BackupSynchResult synchResult = serverRoutine.getSynchResult();
			IFile backupFile = synchResult.getFile();
			if (synchResult.getStatus() == BackupSynchStatus.UPDATED) {
				String message = Messages.bind(Messages.SERVER_BACKUP_CONFLICT, backupFile.getFullPath().toString());
				IStatus status = new Status(IStatus.ERROR, VistAServerPlugin.PLUGIN_ID, message);
				return status;
			}
			
			if (serverRoutine.isLoaded() && serverRoutine.compareTo(file)) {
				IStatus status = new Status(IStatus.ERROR, VistAServerPlugin.PLUGIN_ID, Messages.SERVER_CLIENT_EQUAL);
				return status;
			}
			
			StringBuilder consoleMessage = startConsoleMessage(routineName, vistaConnection.getServerData(), synchResult);
			return saveRoutineToServer(vistaConnection, routineName, routineContent, backupFile, consoleMessage);
		} catch (CoreException coreException) {
			IStatus result = coreException.getStatus();
			StatusManager.getManager().handle(result, StatusManager.LOG);
			return result;
		} catch (Throwable t) {
			IStatus result = new Status(IStatus.ERROR, VistAServerPlugin.PLUGIN_ID, t.getMessage(), t);
			StatusManager.getManager().handle(result, StatusManager.LOG);
			return result;
		}
	}	
}
