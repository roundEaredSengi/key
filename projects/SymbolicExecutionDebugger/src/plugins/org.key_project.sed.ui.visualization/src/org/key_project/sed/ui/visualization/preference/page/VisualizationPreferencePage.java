package org.key_project.sed.ui.visualization.preference.page;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.key_project.sed.ui.visualization.util.VisualizationPreferences;

/**
 * Preference page for the visualization.
 * @author Martin Hentschel
 */
public class VisualizationPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
   /**
    * Constructor
    */
   public VisualizationPreferencePage() {
      super(GRID);
      setPreferenceStore(VisualizationPreferences.getStore());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void init(IWorkbench workbench) {
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void createFieldEditors() {
      RadioGroupFieldEditor switchToStateVisualizationPerspectiveEditor = new RadioGroupFieldEditor(VisualizationPreferences.SWITCH_TO_STATE_VISUALIZATION_PERSPECTIVE, "Open the associated perspective when a state visualization is requested", 3, new String[][] {{"Always", MessageDialogWithToggle.ALWAYS}, {"Prompt", MessageDialogWithToggle.PROMPT}, {"Never", MessageDialogWithToggle.NEVER}}, getFieldEditorParent(), true);
      addField(switchToStateVisualizationPerspectiveEditor);
   }
}