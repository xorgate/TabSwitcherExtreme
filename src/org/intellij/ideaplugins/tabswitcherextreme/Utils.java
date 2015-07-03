package org.intellij.ideaplugins.tabswitcherextreme;

import com.intellij.openapi.diagnostic.Logger;

public class Utils {

	public static void log( String s ) {
		//Notifications.Bus.notify(new Notification("henk", "aap", s, NotificationType.INFORMATION));
		//com.intellij.openapi.diagnostic.Logger.getInstance("log").info(s);
//		final Logger logger = Logger.getInstance("");
//		logger.info(s);
		System.out.println(s);
	}


	public static int modulo(int a, int b) {
		return (a % b + b) % b;
	}

}
