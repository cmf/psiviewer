/*
    IDEA PsiViewer Plugin
    Copyright (C) 2002 Andrew J. Armstrong

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

	Author:
	Andrew J. Armstrong <andrew_armstrong@bigpond.com>
*/
package idea.plugin.psiviewer.controller.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.messages.MessageBusConnection;
import idea.plugin.psiviewer.view.PsiViewerPanel;
import org.jetbrains.annotations.NotNull;

/**
 * @author <a href="mailto:andrew_armstrong@bigpond.com">Andrew Armstrong</a>
 * @version $Revision: 1.1 $
 */
public class EditorListener implements FileEditorManagerListener, CaretListener {


    private static final Logger LOG = Logger.getInstance("idea.plugin.psiviewer.controller.project.EditorListener");

    private final PsiViewerPanel _viewer;
    private final Project _project;
    private final PsiTreeChangeAdapter _treeChangeListener;
    private Editor _currentEditor;
    private MessageBusConnection _msgbus;

    public EditorListener(PsiViewerPanel viewer, Project project) {
        _viewer = viewer;
        _project = project;
        _treeChangeListener = new PsiTreeChangeAdapter() {
            public void childrenChanged(@NotNull final PsiTreeChangeEvent event) {
                updateTreeFromPsiTreeChange(event);
            }

            public void childAdded(@NotNull PsiTreeChangeEvent event) {
                updateTreeFromPsiTreeChange(event);
            }

            public void childMoved(@NotNull PsiTreeChangeEvent event) {
                updateTreeFromPsiTreeChange(event);
            }

            public void childRemoved(@NotNull PsiTreeChangeEvent event) {
                updateTreeFromPsiTreeChange(event);
            }

            public void childReplaced(@NotNull PsiTreeChangeEvent event) {
                updateTreeFromPsiTreeChange(event);
            }

            public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
                updateTreeFromPsiTreeChange(event);
            }
        };
    }

    private void updateTreeFromPsiTreeChange(final PsiTreeChangeEvent event) {
        if (isElementChangedUnderViewerRoot(event)) {
            LOG.debug("PSI Change, starting update timer");
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    _viewer.refreshRootElement();
                }
            });
        }
    }

    private boolean isElementChangedUnderViewerRoot(final PsiTreeChangeEvent event) {
        PsiElement elementChangedByPsi = event.getParent();
        PsiElement viewerRootElement = _viewer.getRootElement();
        boolean b = false;
        try {
            b = PsiTreeUtil.isAncestor(viewerRootElement, elementChangedByPsi, false);
        } catch (Throwable ignored) {
        }

        return b;
    }

    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        debug("source = [" + source + "], file = [" + file + "]");
    }

    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        debug("source = [" + source + "], file = [" + file + "]");
    }

    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        debug("selection changed " + event.toString());

        if (event.getNewFile() == null || _currentEditor == null) return;

        Editor newEditor = event.getManager().getSelectedTextEditor();

        if (_currentEditor != newEditor) _currentEditor.getCaretModel().removeCaretListener(this);

        _viewer.selectElementAtCaret();

        if (newEditor != null)
            _currentEditor = newEditor;

        _currentEditor.getCaretModel().addCaretListener(this);
    }


    public void caretPositionChanged(CaretEvent event) {
        final Editor editor = event.getEditor();

        debug("caret moved to " + editor.getCaretModel().getOffset() + " in editor " + editor);

        _viewer.selectElementAtCaret();
    }

    public void start() {
        _msgbus = _project.getMessageBus().connect();
        _msgbus.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);

        PsiManager.getInstance(_project).addPsiTreeChangeListener(_treeChangeListener);

        _currentEditor = FileEditorManager.getInstance(_project).getSelectedTextEditor();
        if (_currentEditor != null)
            _currentEditor.getCaretModel().addCaretListener(this);
    }

    public void stop() {
        if (_msgbus != null) {
            _msgbus.disconnect();
            _msgbus = null;
        }

        PsiManager.getInstance(_project).removePsiTreeChangeListener(_treeChangeListener);
    }

    private static void debug(String message) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(message);
        }
    }

}
