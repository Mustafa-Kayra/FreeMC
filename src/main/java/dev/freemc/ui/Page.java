package dev.freemc.ui;

import javax.swing.JComponent;

/** One sidebar destination. */
public interface Page {

    String key();

    JComponent view();

    default void onShow() {
    }
}
