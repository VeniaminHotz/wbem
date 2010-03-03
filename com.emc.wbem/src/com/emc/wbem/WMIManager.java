package com.emc.wbem;

import java.io.IOException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;


public class WMIManager {
	private static final String WMI_DEFAULT_NAMESPACE = "ROOT\\CIMV2";
	IJIDispatch wbemEventSource = null;

	private static JISession configAndConnectDCom(String domain, String user,
			String pass) throws Exception {
		JISystem.getLogger().setLevel(Level.OFF);

		try {
			JISystem.setInBuiltLogHandler(false);
		} catch (IOException ignored) {
			;
		}

		JISystem.setAutoRegisteration(true);

		JISession dcomSession = JISession.createSession(domain, user, pass);
		dcomSession.useSessionSecurity(true);
		return dcomSession;
	}

	private static IJIDispatch getWmiLocator(String host, JISession dcomSession)
			throws Exception {
		JIComServer wbemLocatorComObj = new JIComServer(JIProgId
				.valueOf("WbemScripting.SWbemLocator"), host, dcomSession);
		return (IJIDispatch) JIObjectFactory.narrowObject(wbemLocatorComObj
				.createInstance().queryInterface(IJIDispatch.IID));
	}

	private static IJIDispatch toIDispatch(JIVariant comObjectAsVariant)
			throws JIException {
		return (IJIDispatch) JIObjectFactory.narrowObject(comObjectAsVariant
				.getObjectAsComObject());
	}
	
	protected javax.swing.event.EventListenerList listenerList 
	= new javax.swing.event.EventListenerList(); 
	// This methods allows classes to register for MyEvents 
	public void addLogEventListener(LogEventListener listener) { 
		listenerList.add(LogEventListener.class, listener); 
	}
	// This methods allows classes to unregister for MyEvents 
	public void removeLogEventListener(LogEventListener listener) { 
		listenerList.remove(LogEventListener.class, listener); 
	} // This private class is used to fire MyEvents 
	void fireLogEvent(LogEvent evt) { 
		Object[] listeners = listenerList.getListenerList(); 
		// Each listener occupies two elements - the first is the listener class 
		// and the second is the listener instance 
		for (int i=0; i<listeners.length; i+=2) { 
			if (listeners[i]==LogEventListener.class) { 
				((LogEventListener)listeners[i+1]).logEventOccurred(evt); 
			}
		} 
	} 

	public void init() throws Exception {
		String domain = "WORKGROUP";
		String host = "nati_note";
		String user = "nati";
		String pass = "gh7bn2kl";

		JISession dcomSession = null;

		// Connect to DCOM on the remote system, and create an instance of
		// the WbemScripting.SWbemLocator object to talk to WMI.
		System.out.println("Connecting to server...");

		dcomSession = configAndConnectDCom(domain, user, pass);
		IJIDispatch wbemLocator = getWmiLocator(host, dcomSession);

		// Invoke the "ConnectServer" method on the SWbemLocator object via
		// it's IDispatch COM pointer. We will connect to
		// the default ROOT\CIMV2 namespace. This will result in us having a
		// reference to a "SWbemServices" object.
		JIVariant results[] = wbemLocator.callMethodA("ConnectServer",
				new Object[] { new JIString(host),
						new JIString(WMI_DEFAULT_NAMESPACE),
						JIVariant.OPTIONAL_PARAM(), JIVariant.OPTIONAL_PARAM(),
						JIVariant.OPTIONAL_PARAM(), JIVariant.OPTIONAL_PARAM(),
						new Integer(0), JIVariant.OPTIONAL_PARAM() });

		IJIDispatch wbemServices = toIDispatch(results[0]);

		// Now that we have a SWbemServices DCOM object reference, we
		// prepare a WMI Query Language (WQL) request to be informed
		// whenever a
		// new instance of the "Win32_NTLogEvent" WMI class is created on
		// the remote host. This is submitted to the remote host via the
		// "ExecNotificationQuery" method on SWbemServices. This gives us
		// all events as they come in. Refer to WQL documentation to
		// learn how to restrict the query if you want a narrower focus.
		final String QUERY_FOR_ALL_LOG_EVENTS = "SELECT * FROM __InstanceCreationEvent WHERE TargetInstance ISA 'Win32_NTLogEvent'";
		final int RETURN_IMMEDIATE = 16;
		final int FORWARD_ONLY = 32;

		JIVariant[] eventSourceSet = wbemServices.callMethodA(
				"ExecNotificationQuery", new Object[] {
						new JIString(QUERY_FOR_ALL_LOG_EVENTS),
						new JIString("WQL"),
						new JIVariant(new Integer(RETURN_IMMEDIATE
								+ FORWARD_ONLY)) });
		wbemEventSource = (IJIDispatch) JIObjectFactory
				.narrowObject((eventSourceSet[0]).getObjectAsComObject());

		System.out.println("Connected to server.");

	}

	public String getEvent() throws JIException {

		// this blocks until an event log entry appears.
		JIVariant eventAsVariant = (JIVariant) (wbemEventSource.callMethodA(
				"NextEvent", new Object[] { JIVariant.OPTIONAL_PARAM() }))[0];
		IJIDispatch wbemEvent = toIDispatch(eventAsVariant);

		// WMI gives us events as SWbemObject instances (a base class of
		// any WMI object). We know in our case we asked for a specific
		// object
		// type, so we will go ahead and invoke methods supported by
		// that Win32_NTLogEvent class via the wbemEvent IDispatch
		// pointer.
		// In this case, we simply call the "GetObjectText_" method that
		// returns us the entire object as a CIM formatted string. We
		// could,
		// however, ask the object for its property values via
		// wbemEvent.get("PropertyName"). See the j-interop
		// documentation and examples
		// for how to query COM properties.

		JIVariant objTextAsVariant = (JIVariant) (wbemEvent.callMethodA(
				"GetObjectText_", new Object[] { new Integer(1) }))[0];
		String asText = objTextAsVariant.getObjectAsString().getString();
		String props[] = asText.split("[\n\r(\r\n)\\n]");
		Pattern p = Pattern.compile("^(\\s)*Message(\\s)*=(\\s)*\"?(.*)",
				Pattern.CASE_INSENSITIVE);
		Matcher m = null;
		Pattern p2 = Pattern.compile("\\\\\"");
		Matcher m2 = null;
		for (String s : props) {
			m = p.matcher(s);
			if (m.matches()) {
				m2 = p2.matcher(m.group(4));
				return m2.replaceAll("");
			}
		}
		return null;

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length != 4) {
			System.out.println("Usage: " + WMIManager.class.getSimpleName()
					+ " domain host username password");
			return;
		}

		String domain = args[0];
		String host = args[1];
		String user = args[2];
		String pass = args[3];

		JISession dcomSession = null;

		try {
			// Connect to DCOM on the remote system, and create an instance of
			// the WbemScripting.SWbemLocator object to talk to WMI.
			System.out.println("Connecting to server...");

			dcomSession = configAndConnectDCom(domain, user, pass);
			IJIDispatch wbemLocator = getWmiLocator(host, dcomSession);

			// Invoke the "ConnectServer" method on the SWbemLocator object via
			// it's IDispatch COM pointer. We will connect to
			// the default ROOT\CIMV2 namespace. This will result in us having a
			// reference to a "SWbemServices" object.
			JIVariant results[] = wbemLocator.callMethodA("ConnectServer",
					new Object[] { new JIString(host),
							new JIString(WMI_DEFAULT_NAMESPACE),
							JIVariant.OPTIONAL_PARAM(),
							JIVariant.OPTIONAL_PARAM(),
							JIVariant.OPTIONAL_PARAM(),
							JIVariant.OPTIONAL_PARAM(), new Integer(0),
							JIVariant.OPTIONAL_PARAM() });

			IJIDispatch wbemServices = toIDispatch(results[0]);

			// Now that we have a SWbemServices DCOM object reference, we
			// prepare a WMI Query Language (WQL) request to be informed
			// whenever a
			// new instance of the "Win32_NTLogEvent" WMI class is created on
			// the remote host. This is submitted to the remote host via the
			// "ExecNotificationQuery" method on SWbemServices. This gives us
			// all events as they come in. Refer to WQL documentation to
			// learn how to restrict the query if you want a narrower focus.
			final String QUERY_FOR_ALL_LOG_EVENTS = "SELECT * FROM __InstanceCreationEvent WHERE TargetInstance ISA 'Win32_NTLogEvent'";
			final int RETURN_IMMEDIATE = 16;
			final int FORWARD_ONLY = 32;

			JIVariant[] eventSourceSet = wbemServices.callMethodA(
					"ExecNotificationQuery", new Object[] {
							new JIString(QUERY_FOR_ALL_LOG_EVENTS),
							new JIString("WQL"),
							new JIVariant(new Integer(RETURN_IMMEDIATE
									+ FORWARD_ONLY)) });
			IJIDispatch wbemEventSource = (IJIDispatch) JIObjectFactory
					.narrowObject((eventSourceSet[0]).getObjectAsComObject());

			System.out.println("Connected to server.");

			// The result of the query is a SWbemEventSource object. This object
			// exposes a method that we can call in a loop to retrieve the
			// next Windows Event Log entry whenever it is created. This
			// "NextEvent" operation will block until we are given an event.
			// Note that you can specify timeouts, see the Microsoft
			// documentation for more details.
			while (true) {
				// this blocks until an event log entry appears.
				JIVariant eventAsVariant = (JIVariant) (wbemEventSource
						.callMethodA("NextEvent", new Object[] { JIVariant
								.OPTIONAL_PARAM() }))[0];
				IJIDispatch wbemEvent = toIDispatch(eventAsVariant);

				// WMI gives us events as SWbemObject instances (a base class of
				// any WMI object). We know in our case we asked for a specific
				// object
				// type, so we will go ahead and invoke methods supported by
				// that Win32_NTLogEvent class via the wbemEvent IDispatch
				// pointer.
				// In this case, we simply call the "GetObjectText_" method that
				// returns us the entire object as a CIM formatted string. We
				// could,
				// however, ask the object for its property values via
				// wbemEvent.get("PropertyName"). See the j-interop
				// documentation and examples
				// for how to query COM properties.

				JIVariant objTextAsVariant = (JIVariant) (wbemEvent
						.callMethodA("GetObjectText_",
								new Object[] { new Integer(1) }))[0];
				String asText = objTextAsVariant.getObjectAsString()
						.getString();
				String props[] = asText.split("[\n\r(\r\n)\\n]");
				Pattern p = Pattern.compile(
						"^(\\s)*Message(\\s)*=(\\s)*\"?(.*)",
						Pattern.CASE_INSENSITIVE);
				Matcher m = null;
				Pattern p2 = Pattern.compile("\\\\\"");
				Matcher m2 = null;
				for (String s : props) {
					m = p.matcher(s);
					if (m.matches()) {
						m2 = p2.matcher(m.group(4));
						System.out.println(m2.replaceAll(""));
					}
				}
				// System.out.println(asText);

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != dcomSession) {
				try {
					JISession.destroySession(dcomSession);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

}
