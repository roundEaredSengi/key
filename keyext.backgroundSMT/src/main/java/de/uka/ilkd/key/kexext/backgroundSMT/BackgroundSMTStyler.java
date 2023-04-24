package de.uka.ilkd.key.kexext.backgroundSMT;

import de.uka.ilkd.key.gui.fonticons.FontAwesomeSolid;
import de.uka.ilkd.key.gui.fonticons.IconFactory;
import de.uka.ilkd.key.gui.fonticons.IconFontProvider;
import de.uka.ilkd.key.gui.prooftree.GUIAbstractTreeNode;
import de.uka.ilkd.key.gui.prooftree.Style;
import de.uka.ilkd.key.gui.prooftree.Styler;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Timer;
import java.util.TimerTask;

public class BackgroundSMTStyler implements Styler<GUIAbstractTreeNode> {

    private static final Icon ICON = IconFactory.get(
        new IconFontProvider(FontAwesomeSolid.PLAY), 12);
    private final BackgroundSMTExtension extension;

    public BackgroundSMTStyler(BackgroundSMTExtension extension) {
        this.extension = extension;
    }
    @Override
    public void style(@Nonnull Style current, @Nonnull GUIAbstractTreeNode obj) {
        JButton bsmt_button = current.get(Style.RIGHT_BUTTON);

        if (obj.getChildCount() == 0 && extension.canApply(obj.getNode())) {
            if (bsmt_button == null) {
                bsmt_button = new JButton(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        extension.applyRunner(obj.getNode());
                    }
                });
                bsmt_button.setToolTipText("Apply background SMT results.");
            }
            bsmt_button.setEnabled(true);
            bsmt_button.setIcon(ICON);
            current.background = Color.GREEN;
        } else {
           bsmt_button = null;
           current.background = Color.WHITE;
        }

        current.set(Style.RIGHT_BUTTON, bsmt_button);
    }
}
