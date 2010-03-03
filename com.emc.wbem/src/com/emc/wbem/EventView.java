package com.emc.wbem;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.part.ViewPart;

import com.emc.wbem.WMIManager;

import com.emc.wbem.View.ViewContentProvider;
import com.emc.wbem.View.ViewLabelProvider;

public class EventView extends ViewPart {
	public static final String ID = "com.emc.wbem.eventview";

	private TableViewer viewer;
	
	ArrayList<String> list = new ArrayList<String>();
	WMIManager wmi = null;
	
	public EventView(){
		super();		
	}
		
	class ViewContentProvider implements IStructuredContentProvider {
		
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			return list.toArray();
		}
		/*
		public void addElement(String item){
			list.add(item);
		}
		 
		@Override
		public void logEventOccurred(Event event) {
			if (event instanceof LogEvent){
				list.add(((LogEvent) event).getMessage());
			}
			
		}*/
	}
	
	@Override
	public void createPartControl(Composite parent) {
		wmi = new WMIManager();
		try {
			wmi.init();
			wmi.addLogEventListener(new LogEventListener(){
				@Override
				public void logEventOccurred(Event event) {
					list.add(((LogEvent)event).getMessage());
				}				
			});
		
		
			ViewContentProvider model = new ViewContentProvider();
			viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
					| SWT.V_SCROLL);
			viewer.setContentProvider(model);
			//viewer.setLabelProvider(new ViewLabelProvider());
			viewer.setInput(getViewSite());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

}
