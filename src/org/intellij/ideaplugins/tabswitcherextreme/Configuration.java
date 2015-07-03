package org.intellij.ideaplugins.tabswitcherextreme;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class Configuration implements Configurable {
	ConfigurationForm form;

	@Nls
	@Override
	public String getDisplayName() {
		return "TabSwitcher Extreme";
	}

	@Nullable
	@Override
	public String getHelpTopic() {
		return "HALP";
	}

	@Nullable
	@Override
	public JComponent createComponent() {
		form = new ConfigurationForm();

		return form.holder;
	}

	@Override
	public boolean isModified() {
		return true;
	}

	@Override
	public void apply() throws ConfigurationException {
		Utils.log("Apply");
		PropertiesComponent props = PropertiesComponent.getInstance();
		String descs = form.textArea1.getText();
		String regexes = form.textArea2.getText();
		String removers = form.textArea3.getText();

		props.setValue("Descriptions", descs);
		props.setValue("Regexes", regexes);
		props.setValue("Removers", removers);
	}

	@Override
	public void reset() {
		Utils.log("Reset");
		PropertiesComponent props = PropertiesComponent.getInstance();
		String descs = props.getValue("Descriptions", "Everything");
		String regexes = props.getValue("Regexes", ".*");
		String removers = props.getValue("Removers", "");
		Utils.log("Read descriptions: " + descs);
		Utils.log("Read regexes: " + regexes);
		Utils.log("Read removers: " + removers);
		form.textArea1.setText(descs);
		form.textArea2.setText(regexes);
		form.textArea3.setText(removers);
	}

	@Override
	public void disposeUIResources() {
		Utils.log("disposeUIResources");
		form = null;
	}
}
