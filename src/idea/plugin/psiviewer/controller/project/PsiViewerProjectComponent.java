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

import com.intellij.icons.AllIcons;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.xmlb.XmlSerializerUtil;
import idea.plugin.psiviewer.PsiViewerConstants;
import idea.plugin.psiviewer.controller.actions.PropertyToggleAction;
import idea.plugin.psiviewer.controller.application.PsiViewerApplicationSettings;
import idea.plugin.psiviewer.util.Helpers;
import idea.plugin.psiviewer.view.PsiViewerPanel;
import org.jdesktop.swingx.combobox.ListComboBoxModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@State(name = "PsiViewerSettings")
public class PsiViewerProjectComponent implements PersistentStateComponent<PsiViewerProjectComponent>, ProjectComponent, PsiViewerConstants {

    private static final Logger LOG = Logger.getInstance("idea.plugin.psiviewer.controller.project.PsiViewerProjectComponent");
    private boolean HIGHLIGHT = false;
    private boolean FILTER_WHITESPACE = false;
    private boolean SHOW_PROPERTIES = true;
    private int SPLIT_DIVIDER_POSITION = 300;
    private boolean AUTOSCROLL_TO_SOURCE = false;
    private boolean AUTOSCROLL_FROM_SOURCE = false;

    private ComboBox myLanguagesComboBox;
    private ItemListener myLanguagesComboBoxListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e)
        {
            if (e.getStateChange() == ItemEvent.SELECTED)
            {
                _viewerPanel.refreshRootElement();
                _viewerPanel.selectElementAtCaret();
            }
        }
    };

    private Project _project;
    private EditorListener _editorListener;
    private PsiViewerPanel _viewerPanel;


    private PsiViewerProjectComponent()
    {

    }

    public PsiViewerProjectComponent(Project project) {
        _project = project;
    }

    public void projectOpened()
    {
        if (PsiViewerApplicationSettings.getInstance().PLUGIN_ENABLED)
        {
            initToolWindow();
        }
    }

    public void projectClosed()
    {
        unregisterToolWindow();
    }

    public void initComponent()
    {
    }

    public void disposeComponent()
    {
    }

    @NotNull
    public String getComponentName()
    {
        return PLUGIN_NAME + '.' + PROJECT_COMPONENT_NAME;
    }

    public void initToolWindow()
    {
        _viewerPanel = new PsiViewerPanel(this);

        _viewerPanel.addPropertyChangeListener("ancestor", evt -> handleCurrentState());
        ActionManager actionManager = ActionManager.getInstance();

        DefaultActionGroup actionGroup = new DefaultActionGroup(ID_ACTION_GROUP, false);
        actionGroup.add(new PropertyToggleAction("Filter Whitespace",
                "Remove whitespace elements",
                Helpers.getIcon(ICON_FILTER_WHITESPACE),
                this,
                "filterWhitespace") {
            @Override
            public void setSelected(AnActionEvent anactionevent, boolean flag) {
                setFilterWhitespace(flag);
                getViewerPanel().applyWhitespaceFilter();
            }
        });

        actionGroup.add(new PropertyToggleAction("Highlight",
                "Highlight selected PSI element",
                Helpers.getIcon(ICON_TOGGLE_HIGHLIGHT),
                this,
                "highlighted") {
            @Override
            public void setSelected(AnActionEvent anactionevent, boolean flag) {
                debug("set highlight to " + flag);
                setHighlighted(flag);
                getViewerPanel().applyHighlighting();
            }
        });
        actionGroup.add(new PropertyToggleAction("Properties",
                "Show PSI element properties",
                AllIcons.General.Settings,
                this,
                "showProperties") {
            @Override
            public void setSelected(AnActionEvent anactionevent, boolean flag) {
                setShowProperties(flag);
                getViewerPanel().showProperties(flag);
            }
        });
        actionGroup.add(new PropertyToggleAction("Autoscroll to Source",
                "Autoscroll to Source",
                AllIcons.General.AutoscrollToSource,
                this,
                "autoScrollToSource") {
            @Override
            public void setSelected(AnActionEvent anactionevent, boolean flag) {
                debug("autoscrolltosource=" + flag);
                setAutoScrollToSource(flag);
            }
        });
        actionGroup.add(new PropertyToggleAction("Autoscroll from Source",
                "Autoscroll from Source111",
                AllIcons.General.AutoscrollFromSource,
                this,
                "autoScrollFromSource") {
            @Override
            public void setSelected(AnActionEvent anactionevent, boolean flag) {
                debug("autoscrollfromsource=" + flag);
                setAutoScrollFromSource(flag);
            }
        });

        ActionToolbar toolBar = actionManager.createActionToolbar(ID_ACTION_TOOLBAR, actionGroup, true);

        JPanel panel = new JPanel(new HorizontalLayout(0));
        panel.add(toolBar.getComponent());

        myLanguagesComboBox = new ComboBox();
        panel.add(myLanguagesComboBox);
        updateLanguagesList(Collections.emptyList());

        _viewerPanel.add(panel, BorderLayout.NORTH);

        ToolWindow toolWindow = getToolWindow();
        toolWindow.setIcon(Helpers.getIcon(ICON_TOOL_WINDOW));
        _viewerPanel.setToolWindow(toolWindow);

        _editorListener = new EditorListener(_viewerPanel, _project);
    }

    private void handleCurrentState()
    {
        if (_viewerPanel == null)
            return;

        if (_viewerPanel.isDisplayable())
        {
            _editorListener.start();
            _viewerPanel.selectElementAtCaret();
        }
        else
        {
            _editorListener.stop();
            _viewerPanel.removeHighlighting();
        }
    }

    public void unregisterToolWindow()
    {
        if (_viewerPanel != null)
        {
            _viewerPanel.removeHighlighting();
            _viewerPanel = null;
        }

        if (_editorListener != null)
        {
            _editorListener.stop();
            _editorListener = null;
        }
        if (isToolWindowRegistered())
            ToolWindowManager.getInstance(_project).unregisterToolWindow(ID_TOOL_WINDOW);
    }

    private ToolWindow getToolWindow()
    {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(_project);
        if (isToolWindowRegistered())
            return toolWindowManager.getToolWindow(ID_TOOL_WINDOW);
        else {
            ToolWindow toolWindow = toolWindowManager.registerToolWindow(ID_TOOL_WINDOW,
                    true, ToolWindowAnchor.RIGHT);

            ContentManager contentManager = toolWindow.getContentManager();
            contentManager.addContent(contentManager.getFactory().createContent(_viewerPanel, null, false));

            return toolWindow;
        }
    }

    private boolean isToolWindowRegistered()
    {
        return ToolWindowManager.getInstance(_project).getToolWindow(ID_TOOL_WINDOW) != null;
    }

    @Nullable
    @Override
    public PsiViewerProjectComponent getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PsiViewerProjectComponent state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public PsiViewerPanel getViewerPanel()
    {
        return _viewerPanel;
    }

    public boolean isHighlighted()
    {
        return HIGHLIGHT;
    }

    public void setHighlighted(boolean highlight)
    {
        HIGHLIGHT = highlight;
    }

    public boolean isFilterWhitespace()
    {
        return FILTER_WHITESPACE;
    }

    public void setFilterWhitespace(boolean filterWhitespace)
    {
        FILTER_WHITESPACE = filterWhitespace;
    }

    public boolean isShowProperties()
    {
        return SHOW_PROPERTIES;
    }

    public void setShowProperties(boolean showProperties)
    {
        SHOW_PROPERTIES = showProperties;
    }

    public int getSplitDividerLocation()
    {
        return SPLIT_DIVIDER_POSITION;
    }

    public void setSplitDividerLocation(int location)
    {
        SPLIT_DIVIDER_POSITION = location;
    }

    public boolean isAutoScrollToSource()
    {
        return AUTOSCROLL_TO_SOURCE;
    }

    public void setAutoScrollToSource(boolean isAutoScrollToSource)
    {
        AUTOSCROLL_TO_SOURCE = isAutoScrollToSource;
    }

    public boolean isAutoScrollFromSource()
    {
        return AUTOSCROLL_FROM_SOURCE;
    }

    public void setAutoScrollFromSource(boolean isAutoScrollFromSource)
    {
        AUTOSCROLL_FROM_SOURCE = isAutoScrollFromSource;
    }

    public Project getProject()
    {
        return _project;
    }

    public static PsiViewerProjectComponent getInstance(Project project)
    {
        return project.getComponent(PsiViewerProjectComponent.class);
    }

    private static void debug(String message)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug(message);
        }
    }

    @Nullable
    public Language getSelectedLanguage()
    {
        return (Language) myLanguagesComboBox.getSelectedItem();
    }

    public void updateLanguagesList(Collection<Language> languages)
    {
        Language selectedLanguage = getSelectedLanguage();

        myLanguagesComboBox.removeItemListener(myLanguagesComboBoxListener);

        //noinspection Since15
        myLanguagesComboBox.setModel(new ListComboBoxModel<Language>(new ArrayList<Language>(languages)));

        if (selectedLanguage != null && languages.contains(selectedLanguage))
        {
            myLanguagesComboBox.setSelectedItem(selectedLanguage);
        }

        if (languages.size() < 2)
        {
            myLanguagesComboBox.setVisible(false);
        }
        else
        {
            myLanguagesComboBox.setVisible(true);
        }

        myLanguagesComboBox.addItemListener(myLanguagesComboBoxListener);
    }
}
