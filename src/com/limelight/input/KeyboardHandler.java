package com.limelight.input;

import com.limelight.LimeLog;
import com.limelight.gui.StreamFrame;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.KeyboardPacket;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Class that handles keyboard input
 * @author Diego Waxemberg
 */
public class KeyboardHandler implements KeyListener {

    private KeyboardTranslator translator;
    private StreamFrame parent;
    private boolean mouseCaptured = true;

    /**
     * Constructs a new keyboard listener that will send key events to the specified connection
     * and belongs to the specified frame
     * @param conn the connection to send key events to
     * @param parent the frame that owns this handler
     */
    public KeyboardHandler(NvConnection conn, StreamFrame parent) {
        this.translator = new KeyboardTranslator(conn);
        this.parent = parent;
    }

    /**
     * Invoked when a key is pressed and will send that key-down event to the host
     * @param event the key-down event
     */
    public void keyPressed(KeyEvent event) {
        if (event.isConsumed()) return;
        event.consume();

        short keyMap = translator.translate(event.getKeyCode());

        byte modifier = 0x0;

        int modifiers = event.getModifiersEx();
        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            modifier |= KeyboardPacket.MODIFIER_SHIFT;
        }
        if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
            modifier |= KeyboardPacket.MODIFIER_CTRL;
        }
        if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0) {
            modifier |= KeyboardPacket.MODIFIER_ALT;
        }

        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0 &&
            (modifiers & KeyEvent.ALT_DOWN_MASK) != 0 &&
            (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0 &&
            event.getKeyCode() == KeyEvent.VK_Q) {
            LimeLog.info("quitting");
            
            // Free mouse before closing to avoid the mouse code
            // trying to interact with the now closed streaming window.
            parent.freeMouse();
            
            parent.close();
            return;
        } else if (
                (modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0 &&
                (modifiers & KeyEvent.ALT_DOWN_MASK) != 0 &&
                (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
            if (mouseCaptured) {
                parent.freeMouse();
            } else {
                parent.captureMouse();
            }
            mouseCaptured = !mouseCaptured;
            return;
        }



        translator.sendKeyDown(keyMap, modifier);
    }

    /**
     * Invoked when a key is released and will send that key-up event to the host
     * @param event the key-up event
     */
    public void keyReleased(KeyEvent event) {
        if (event.isConsumed()) return;
        event.consume();

        short keyMap = translator.translate(event.getKeyCode());

        byte modifier = 0x0;

        int modifiers = event.getModifiersEx();
        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            modifier |= KeyboardPacket.MODIFIER_SHIFT;
        }
        if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
            modifier |= KeyboardPacket.MODIFIER_CTRL;
        }
        if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0) {
            modifier |= KeyboardPacket.MODIFIER_ALT;
        }

        translator.sendKeyUp(keyMap, modifier);
    }

    /**
     * Unimplemented
     * @param event unused
     */
    public void keyTyped(KeyEvent event) {
    }

}