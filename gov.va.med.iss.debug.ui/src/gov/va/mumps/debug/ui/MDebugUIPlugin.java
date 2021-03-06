package gov.va.mumps.debug.ui;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class MDebugUIPlugin extends AbstractUIPlugin {
	private OverallUIManager uiManager;
	
	// The plug-in ID
	public static final String PLUGIN_ID = "gov.va.mumps.debug.ui"; //$NON-NLS-1$

	public static final String TERMINAL_VIEW_ID = "us.pwc.vista.eclipse.terminal.VistATerminalView";
	
	// The shared instance
	private static MDebugUIPlugin plugin;
	
	/**
	 * The constructor
	 */
	public MDebugUIPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		IWorkbench wb = PlatformUI.getWorkbench();
		this.uiManager = new OverallUIManager();
		wb.addWorkbenchListener(this.uiManager);
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this.uiManager);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static MDebugUIPlugin getDefault() {
		return plugin;
	}











}
