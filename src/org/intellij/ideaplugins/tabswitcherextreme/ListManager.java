package org.intellij.ideaplugins.tabswitcherextreme;

import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.IconUtil;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ListManager {
	public List<ListDescription> mListDescriptions;
	private int mActiveListIndex = 0;
	public int mDesiredIndexInList;
	private Project mProject;
	public List<VirtualFile> mRecentFiles;

	public ListManager(Project project, List<VirtualFile> recentFiles) {
		mListDescriptions = new ArrayList<ListDescription>();
		mProject = project;
		mRecentFiles = cleanupRecentFiles(recentFiles);
	}



	public List<VirtualFile> cleanupRecentFiles(List<VirtualFile> recentFiles) {
        mRecentFiles = new ArrayList<VirtualFile>();

        for (VirtualFile file : recentFiles) {
            if (!mRecentFiles.contains(file)) {
                mRecentFiles.add(file);
            }
        }


		return mRecentFiles;
	}

	public void generateFileLists(List<VirtualFile> allFiles) {
		// copy the list, and if found one, remove from shadowlist. If any left, make a new list with leftovers
		Collections.sort(allFiles, new Comparator <VirtualFile>() {
			public int compare(VirtualFile a, VirtualFile b) {
				return a.getName().compareTo(b.getName());
			}
		});

		List<VirtualFile> controlList = new ArrayList<VirtualFile>(allFiles.size());
		controlList.addAll(allFiles);
		List<String> controlListStrings = new ArrayList<String> (allFiles.size());
		for (VirtualFile f : controlList) {
			controlListStrings.add(f.getPath());
		}

		List<ListDescription> removeList = new ArrayList<ListDescription>();

		for (ListDescription desc : mListDescriptions) {
			List<VirtualFile> filtered = new ArrayList<VirtualFile>();
			for (VirtualFile f : allFiles) {
				String filename = f.getPath();
				// don't keep doubles
				if (controlListStrings.contains(filename)) {
					//Utils.log("Controllist bevat niet " + filename);
					if (filename.matches(desc.mMatchRegex)) {
						filtered.add(f);
						controlList.remove(f);
						controlListStrings.remove(filename);
					}
				}
			}
			if (filtered.isEmpty()) {
				removeList.add(desc);
			} else {
				desc.setFilteredFileList(filtered);
			}
		}

		for (ListDescription desc : removeList) {
			mListDescriptions.remove(desc);
		}

		// check if we have some lost souls
		if (!controlList.isEmpty()) {
			ListDescription leftovers = new ListDescription(".*", "Other", ".xml");
			leftovers.mFilteredFileList = controlList;
			mListDescriptions.add(leftovers);
		}
	}

	public JList getActiveList() {
		return getListFromIndex(mActiveListIndex);
	}

	public int getActiveListIndex() {
		return mActiveListIndex;
	}

	public void setActiveListIndex(int listIndex) {
		mActiveListIndex = Utils.modulo(listIndex, getListCount());
	}

	public int getListCount() {
		return mListDescriptions.size();
	}

	public JList getListFromIndex(int index) {
		int correctedIndex = Utils.modulo(index, getListCount());
		return mListDescriptions.get(correctedIndex).mList;
	}

	public void addListDescription(String pattern, String description, String removerRegex) {
		ListDescription desc = new ListDescription(pattern, description, removerRegex);
		mListDescriptions.add(desc);
	}

	public VirtualFile getSelectedFile() {
		return (VirtualFile) getActiveList().getSelectedValue();
	}

	public void insertIntoPanel(JPanel panel, JLabel pathLabel) {


		int rows = 2; // header + list
		int cols = getListCount() * 2 - 1; // separator between filenamelist

		GridLayoutManager manager = new GridLayoutManager(rows, cols);
		panel.setLayout(manager);

		// add them in
		for (int i = 0; i < getListCount(); i++) {
			int rowNr = i;

			ListDescription desc = mListDescriptions.get(i);

			// label
			GridConstraints labelConstraints = new GridConstraints();
			labelConstraints.setRow(0);
			labelConstraints.setColumn(rowNr);

			panel.add(desc.getLabel(), labelConstraints);

			// list
			GridConstraints listConstraints = new GridConstraints();
			listConstraints.setRow(1);
			listConstraints.setColumn(rowNr);
			listConstraints.setFill(GridConstraints.FILL_VERTICAL);
			panel.add(desc.getList(), listConstraints);

			setListData(desc, desc.mFilteredFileList, pathLabel);
		}
	}

	private void setListData(ListManager.ListDescription desc, final List<VirtualFile> filtered, JLabel pathLabel) {
		desc.mList.setModel(new AbstractListModel() {
			public int getSize() {
				return filtered.size();
			}

			public Object getElementAt(int index) {
				return filtered.get(index);
			}
		});

		desc.mList.setCellRenderer(getRenderer(mProject));
		desc.mList.getSelectionModel().addListSelectionListener(getListener(desc.mList, pathLabel));
		desc.mList.setVisibleRowCount(filtered.size());
	}

	private static ListSelectionListener getListener(final JList list, final JLabel path) {
		return new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						updatePath(list, path);
					}
				});
			}
		};
	}
	private static void updatePath(JList list, JLabel path) {
		String text = " ";
		final Object[] values = list.getSelectedValues();
		if ((values != null) && (values.length == 1)) {
			final VirtualFile parent = ((VirtualFile) values[0]).getParent();
			if (parent != null) {
				text = parent.getPresentableUrl();
				final FontMetrics metrics = path.getFontMetrics(path.getFont());
				while ((metrics.stringWidth(text) > path.getWidth()) &&
					(text.indexOf(File.separatorChar, 4) > 0)) {
					text = "..." + text.substring(text.indexOf(File.separatorChar, 4));
				}
			}
		}
		path.setText(text);
	}

	private static ListCellRenderer getRenderer(final Project project) {
		return new ColoredListCellRenderer() {
			@Override
			protected void customizeCellRenderer(JList list, Object value, int index,
												 boolean selected, boolean hasFocus) {
				if (value instanceof VirtualFile) {
					final VirtualFile file = (VirtualFile) value;
					setIcon(IconUtil.getIcon(file, Iconable.ICON_FLAG_READ_STATUS, project));

					final FileStatus status = FileStatusManager.getInstance(project).getStatus(file);
					final TextAttributes attributes =
						new TextAttributes(status.getColor(), null, null,
							EffectType.LINE_UNDERSCORE, Font.PLAIN);

					String filename = file.getName();
					String remover = (( ListDescription.JMyList) list).mRemoveRegex;
					if (null != remover) {
						filename = filename.replaceAll(remover, "");
					}

					append(filename, SimpleTextAttributes.fromTextAttributes(attributes));
				}
			}
		};
	}

	public class ListDescription {
		private class JMyList extends JList {
			public String mRemoveRegex;
		}


		private JMyList mList;
		private JLabel mLabel;
		String mMatchRegex;
		String mDescription;
		List<VirtualFile> mFilteredFileList;

		public ListDescription(String matchString, String description, String removeRegex ) {
			mDescription = description;
			mMatchRegex = matchString;
			mFilteredFileList = new ArrayList<VirtualFile>();

			//noinspection UndesirableClassUsage
			mList = new JMyList();
			if (!"".equals(removeRegex)) {
				mList.mRemoveRegex = removeRegex;
			}

			mLabel = new JLabel("<html><b>" + description + "</b></html>", SwingConstants.CENTER);

		}

		public void setFilteredFileList(List<VirtualFile> filteredList) {
			mFilteredFileList = filteredList;
		}

		public JList getList() {
			return mList;
		}

		public JLabel getLabel() {
			return mLabel;
		}
	}

	public void updateSelection(NavigateCommand nav) {
		int previousListIndex = getActiveListIndex();
		JList previousList = getActiveList();
		int previousIndexInList = previousList.getSelectedIndex();

		// logic is done in here
		FilePosition targetFilePosition = getTargetFilePosition(previousListIndex, previousIndexInList, nav);

		// no move possible? Just abort
		if (targetFilePosition == null) {
			return;
		}

		JList nextList = getListFromIndex(targetFilePosition.getListIndex());
		int nextIndexInList = targetFilePosition.getIndexInList();
		nextList.setSelectedIndex(nextIndexInList);
		nextList.ensureIndexIsVisible(nextIndexInList);

		if (targetFilePosition.getListIndex() != previousListIndex ) {
			setActiveListIndex(targetFilePosition.getListIndex());
			// clear the previous one
			previousList.clearSelection();
		}
	}

	private VirtualFile nextRecentFile(Project proj, VirtualFile current, boolean wantOlder) {
		//final VirtualFile[] recentFilesArr = mRecentFiles;
		final FileEditorManager manager = FileEditorManager.getInstance(proj);
		List<VirtualFile> recentFilesList = new ArrayList<VirtualFile>(mRecentFiles.size());

        //Collections.addAll(recentFilesList, mRecentFiles);
        for (VirtualFile file : mRecentFiles) {
            recentFilesList.add(file);
        }

		Utils.log("Recent files : " + recentFilesList.size());
		//Utils.log("Current file: " + current.getName());

		for (VirtualFile file : recentFilesList) {
			Utils.log(file.getName() + (file.equals(current) ? " <-- " : ""));
		}


		if (!wantOlder) {
			Utils.log("want older");
			Collections.reverse(recentFilesList);
		} else {
			Utils.log("want newer");
		}

		for (VirtualFile lister : recentFilesList) {
			if (manager.isFileOpen(lister)) {
				Utils.log("- " + lister.getName());
			}
		}

		boolean currentFound = false;
		for (VirtualFile file : recentFilesList) {
			if (file.equals(current)) {
				currentFound = true;
			} else {
				if (currentFound) {
					if (manager.isFileOpen(file)) {
						Utils.log("-- next is " + file.getName());
						return file;
					}
				}
			}
		}

		// if not found, try again and get first available file
		for (VirtualFile f : recentFilesList) {
			if (manager.isFileOpen(f)) {
				return f;
			}
		}

		return null;
	}


	private FilePosition getNextInHistory(int curListIndex, int curIndexInList, boolean wantNewer) {
		VirtualFile cur = getSelectedFile();
		VirtualFile other = nextRecentFile(mProject, cur, wantNewer);

		return getFilePosition(other);
	}

	private FilePosition getTargetFilePosition(int curListIndex, int curIndexInList, NavigateCommand navCmd) {
		// selection is in a certain list, press a key, figure out where the next selection will be.
		// loop around to this current list again = no action
		if (navCmd == NavigateCommand.TAB) {
			Utils.log("TAB");
			return getNextInHistory(curListIndex, curIndexInList, false);
		}
		if (navCmd == NavigateCommand.SHIFTTAB) {
			Utils.log("SHIFTTAB");
			return getNextInHistory(curListIndex, curIndexInList, true);
		}

		// if targetlist is empty, try one beyond
		if (curIndexInList == -1) {
			Utils.log("Aangeroepen op lijst " + curListIndex + " waar niks in zit...");
			return null;
		}

		// updown
		if (navCmd == NavigateCommand.UP || navCmd == NavigateCommand.DOWN) {
			JList curList = getListFromIndex(curListIndex);
			int size = curList.getModel().getSize();
			Utils.log("Aantal in lijst: " + size);

			int offset = navCmd == NavigateCommand.DOWN ? 1 : -1;
			Utils.log("Offset: " + offset);
			int newIndexInList = Utils.modulo(curIndexInList + offset, size);
			Utils.log("Van " + curIndexInList + " naar " + newIndexInList);
			if (newIndexInList == curIndexInList) {
				return null;
			}

			mDesiredIndexInList = newIndexInList;

			return new FilePosition(curListIndex, newIndexInList);

		} else if (navCmd == NavigateCommand.LEFT || navCmd == NavigateCommand.RIGHT) {
			int direction = navCmd == NavigateCommand.LEFT ? -1 : 1;

			int targetListIndex = curListIndex;
			//Utils.log("we zittne op lijst " + curListIndex);

			// find the first list that is not empty, in specified direction
			int targetIndexInList;
			do {
				targetListIndex = Utils.modulo(targetListIndex + direction, getListCount());
				//Utils.log("Wat zou de index zijn op " + targetListIndex);
				//targetIndexInList = getActualTargetIndexInOtherList(targetListIndex, curIndexInList);
				targetIndexInList = getActualTargetIndexInOtherList(targetListIndex, mDesiredIndexInList);

				//Utils.log("  nou, " + targetIndexInList);
			} while (targetIndexInList == -1);

			if (targetListIndex != curListIndex) {
				return new FilePosition(targetListIndex, targetIndexInList);
			}

			Utils.log("We komen bij onszelf uit");
		} else if (navCmd == NavigateCommand.PAGE_UP || navCmd == NavigateCommand.PAGE_DOWN) {
			JList curList = getListFromIndex(curListIndex);
			int targetIndexInList;
			if (navCmd == NavigateCommand.PAGE_UP) {
				targetIndexInList = 0;
				Utils.log("Pageup naar 0");
			} else {
				targetIndexInList = curList.getModel().getSize() - 1;
				Utils.log("Pagedown naar " + targetIndexInList);
			}
			mDesiredIndexInList = targetIndexInList;
			if (targetIndexInList != curIndexInList) {
				return new FilePosition(curListIndex, targetIndexInList);
			}
		}

		return null;
	}

	private int getActualTargetIndexInOtherList(int listIndex, int requestedIndexInList) {
		// returns -1 if empty
		JList targetList = getListFromIndex(listIndex);
		int size = targetList.getModel().getSize();

		if (size == 0) {
			return -1;
		} else {
			return Math.min( size-1, Math.max(0, requestedIndexInList));
		}
	}

	public FilePosition getFilePosition(VirtualFile f) {
		int initialListIndex = 0;
		int initialIndexInList = 0;

		if (f != null) {
			Utils.log("get position for: " + f.getName());
			int foundListIndex = -1;
			int foundIndexInList = -1;
			for (int i =0; i< mListDescriptions.size(); i++) {
				ListManager.ListDescription desc = mListDescriptions.get(i);
				List<VirtualFile> list = desc.mFilteredFileList;

				int index = list.indexOf(f);
				if (index != -1) {
					foundListIndex = i;
					foundIndexInList = index;
					break;
				}
			}
			if (foundIndexInList != -1) {
				initialListIndex = foundListIndex;
				initialIndexInList = foundIndexInList;
			} else {
				Utils.log("NIET GEVONDEN: " + f.getName());
			}
		}

		return new FilePosition(initialListIndex, initialIndexInList);
	}

	public class FilePosition {
		private int mListIndex;
		private int mIndexInList;

		FilePosition(int listIndex, int indexInList) {
			mListIndex = listIndex;
			mIndexInList = indexInList;
		}

		int getListIndex() {
			return mListIndex;
		}

		int getIndexInList() {
			return mIndexInList;
		}
	}

	public enum NavigateCommand {
		LEFT,
		RIGHT,
		UP,
		DOWN,
		PAGE_UP,
		PAGE_DOWN,
		TAB,
		SHIFTTAB
	}
}

