package org.mumps.meditor;

public class LoadRoutineException extends MEditorException {
	private static final long serialVersionUID = 1L;

	public LoadRoutineException(String message) {
		super(message);
	}
	
	public LoadRoutineException(String message, Throwable rootCause) {
		super(message, rootCause);
	}
}
