package com.emc.wbem;

import java.util.ArrayList;

import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String PERSPECTIVE_ID = "com.emc.wbem.perspective";
	
	ArrayList<LogEventListener> listeners = new ArrayList<LogEventListener>();
	public void addListener(LogEventListener listener) {
		listeners.add(listener);
	}


	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}
	
	/*public void postStartUp(){
		WMIManager wmi = new WMIManager();
		while (true){
			try {
				wmi.init();
				String evMsg = wmi.getEvent();
				if (evMsg != null) {
					for( int i = 0; i < listeners.size(); i++ ) {
					      listeners.get(i).handleEvent(new LogEvent("evMsg")); //class A не знает кто его слушает
					    }
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}*/
}
