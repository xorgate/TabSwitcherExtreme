package org.intellij.ideaplugins.tabswitcherextreme;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.NamedJDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class Settings implements ApplicationComponent, NamedJDOMExternalizable {

	/**
	 * @noinspection PublicField,NonConstantFieldWithUpperCaseName
	 * externalized in settings file
	 */
	public boolean SHOW_RECENT_FILES;


	public Settings() {
		SHOW_RECENT_FILES = false;
	}

	public void disposeComponent() {
	}

	@NotNull
	public String getComponentName() {
		return "tabswitch.TabSwitchSettings";
	}

	public String getExternalFileName() {
		return "tab_switch";
	}

	public static Settings getInstance() {
		final Application application = ApplicationManager.getApplication();
		return application.getComponent(Settings.class);
	}

	public void initComponent() {
	}

	public void readExternal(Element element) throws InvalidDataException {
		DefaultJDOMExternalizer.readExternal(this, element);
	}

	public void writeExternal(Element element) throws WriteExternalException {
		DefaultJDOMExternalizer.writeExternal(this, element);
	}
}
