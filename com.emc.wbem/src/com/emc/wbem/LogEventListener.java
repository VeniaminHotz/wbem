package com.emc.wbem;

import java.util.EventListener;

import org.eclipse.swt.widgets.Event;

public interface LogEventListener extends EventListener {
	public void logEventOccurred(Event event);
}
