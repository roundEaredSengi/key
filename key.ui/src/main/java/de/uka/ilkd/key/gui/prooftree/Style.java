package de.uka.ilkd.key.gui.prooftree;

import de.uka.ilkd.key.pp.LogicPrinter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.Color;
import java.util.*;

/**
 * @author Alexander Weigl
 * @version 1 (20.05.19)
 */
public class Style {
    /** the text of this node */
    public String text;

    /** the tooltip */
    public Tooltip tooltip;

    /** foreground color of the node */
    public Color foreground;

    /** background color of the node */
    public Color background;

    /** border color of the node */
    public Color border;

    /** icon of the node */
    public Icon icon;

    private final Map<Object, Object> styles = new HashMap<>();
    private final Set<Object> sealed = new HashSet<>();

    private static class Key<T> {
        <T> Key(Class<T> clazz) {
        }
    }

    public static final Key<Color> KEY_COLOR_FOREGROUND = new Key<>(Color.class);
    public static final Key<Color> KEY_COLOR_BACKGROUND = new Key<>(Color.class);
    public static final Key<Color> KEY_COLOR_BORDER = new Key<>(Color.class);
    public static final Key<Boolean> KEY_FONT_ITALIC = new Key<>(Boolean.class);
    public static final Key<Boolean> KEY_FONT_BOLD = new Key<>(Boolean.class);
    public static final Key<Icon> KEY_ICON = new Key<>(Icon.class);
    public static final Key<String> KEY_TOOLTIP = new Key<>(String.class);
    public static final Key<String> KEY_TEXT = new Key<>(String.class);

    public static final Key<JButton> RIGHT_BUTTON = new Key<>(JButton.class);

    @Nonnull
    public <T> Style set(@Nonnull Key<T> key, @Nullable T value) {
        if (!sealed.contains(key)) {
            styles.put(key, value);
        }
        return this;
    }

    @Nonnull
    public <T> Style setAndSeal(@Nonnull Key<T> key, @Nullable T value) {
        set(key, value);
        sealed.add(key);
        return this;
    }

    public <T> boolean contains(@Nonnull Key<T> key) {
        return styles.containsKey(key);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(@Nonnull Key<T> key, @Nullable T defaultValue) {
        return (T) styles.getOrDefault(key, defaultValue);
    }

    @Nullable
    public <T> T get(@Nonnull Key<T> key) {
        return get(key, null);
    }

    public boolean getBoolean(Key<Boolean> key) {
        return get(key) == Boolean.TRUE;
    }

    /** Wrapper class for the tooltip */
    public static class Tooltip {
        /** title, can also be null and empty */
        private String title;

        /** infos */
        private final ArrayList<Fragment> additionalInfo = new ArrayList<>();

        /**
         * @return the title
         */
        public String getTitle() {
            return title;
        }

        /**
         * Sets the title.
         *
         * @param title tooltip title
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * Adds a piece of additional information.
         *
         * @param key the key
         * @param value the value
         * @param block whether this should be rendered as a block
         */
        public void addAdditionalInfo(@Nonnull String key, @Nonnull String value, boolean block) {
            additionalInfo.add(new Fragment(key, value, block));
        }

        /**
         * Adds notes
         *
         * @param notes the notes
         */
        public void addNotes(@Nonnull String notes) {
            addAdditionalInfo("Notes", notes, false);
        }

        /**
         * Adds rule information
         *
         * @param rule the rule
         */
        public void addRule(@Nonnull String rule) {
            addAdditionalInfo("Rule", rule, false);
        }

        /**
         * Adds applied on infos
         *
         * @param on the info
         */
        public void addAppliedOn(@Nonnull String on) {
            addAdditionalInfo("Applied on", LogicPrinter.escapeHTML(on, true), true);
        }

        /**
         * @return list of all additional infos, immutable
         */
        public List<Fragment> getAdditionalInfos() {
            return Collections.unmodifiableList(additionalInfo);
        }

        /** wrapper class for additional infos */
        public static final class Fragment {
            /** key */
            public final String key;
            /** value */
            public final String value;
            /** whether this is a block */
            public final boolean block;

            public Fragment(String key, String value, boolean block) {
                this.key = key;
                this.value = value;
                this.block = block;
            }
        }
    }
}
