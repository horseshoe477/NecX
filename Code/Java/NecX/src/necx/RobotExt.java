package necx;

// RobotExt.java
import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An extended java.awt.Robot class that implements various
 * functionality, such as a {@link #sendKeys(String)} function; which
 * is intended to be easier to use than calling keyPress + keyRelease.
 *
 * <p>http://sourceforge.net/projects/extendedrobot</p>
 *
 * <p>Copyright(c) 2011 Dennis K. Paulsen, All Rights Reserved.   This
 * source code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License; either version 2
 * of the License or (at your option) any later version.</p>
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTIBILITY or FITNESS FOR A PARTICULAR PURPOSE.   See the
 * GNU General Public License for more details.</p>
 *
 * <p>You can review the license at: http://www.gnu.org/licenses</p>
 *
 * @author Dennis K. Paulsen
 * @version 0.12
 *
 */
public class RobotExt extends Robot {
    
    
    // =======================================================================================
    // InnerClass - thread to enable/disable uPoint's mouse mapping
    // =======================================================================================
    public class UpdateKey implements Runnable
    {
        String strCmd = "";
        public UpdateKey(String strCmd){
            this.strCmd = strCmd;
        }
        
        public void run(){
            try{
                Thread.sleep(10);
                sendKeys(strCmd);
            }
            catch(Exception e){ e.printStackTrace(); }
        }
    }
    // =======================================================================================
    public void mySendKey(String strCmd){
        Thread tUpdate = new Thread(new UpdateKey(strCmd));
        tUpdate.start();
    }
    
    
    
    
    /**
     * @link {#Robot()}
     * @throws AWTException AWTException
     */

    public RobotExt() throws AWTException {
    }
    
    /**
     * @link {#Robot(GraphicsDevice screen)}
     * @throws AWTException AWTException
     */
    public RobotExt(GraphicsDevice screen) throws AWTException {
        super(screen);
    }

    /**
     * Clicks the default mouse button.
     */
    public void mouseClick() {
        mouseClick(this.defaultButtons);
    }
    
    /**
     * Clicks (press + release) the specified mouse button.
     */
    public void mouseClick(int buttons) {
        mousePress(buttons);
        mouseRelease(buttons);
    }

    /**
     * Clicks the default mouse button at the specified
     * location.
     * 
     * @param x coordinate
     * @param y coordinate
     */
    public void mouseClick(int x, int y) {
        mouseMove(x, y);
        mouseClick(this.defaultButtons);
    }
    
    /**
     * Clicks the specified mouse button at the specified
     * location.
     */
    public void mouseClick(int buttons, int x, int y) {
        mouseMove(x, y);
        mouseClick(buttons);
    }
    
    /**
     * Sets the default button mask used for some mouse operations.
     * 
     * @param buttons InputEvent.BUTTON#_MASK values.  Default is
     * InputEvent.BUTTON1_MASK
     */
    public void setDefaultButtons(int buttons) {
        this.defaultButtons = buttons;
    }
    
    
    /**
     * Interface for sending keystrokes to an application.   Provides
     * an easier alternative to the lower-level keyPress/keyRelease
     * functions.
     *
     * @param keys A sequence of keystrokes
     *
     * <pre>
     * <b>Examples:</b>
     * {@code}
     * RobotExt robot = new RobotExt();
     *
     * robot.sendKeys("Hello, how are you today?");
     * robot.sendKeys("%(f)"); // Alt-f
     * robot.sendKeys("^(+(l))"); // Ctrl-Shift-l
     * robot.sendKeys("whoami~"); // type whoami and press enter (~)
     * robot.sendKeys("{}}"); // Escape special } syntax character
     * robot.sendKeys("{SPC 5}"); // Send 5 space characters
     *
     * <b>Syntax:</b>
     * Modifier Keys:
     *    ^    CTRL
     *    %    ALT
     *    +    SHIFT
     *    #    META
     *
     * Other Keys:
     *    ~       ENTER
     *    \n      ENTER
     *    \t      TAB
     *    ( and ) MODIFIER GROUPING
     *    { and } QUOTE / ESCAPE CHARACTERS
     *
     * Special Key Names (for use with {KeyName}):
     *    Name    Action
     *    -------------------
     *    BAC     BackSpace
     *    BS      BackSpace
     *    BKS     BackSpace
     *    BRE     Break
     *    CAN     Cancel
     *    CAP     Caps_Lock
     *    DEL     Delete
     *    DOW     Down
     *    END     End
     *    ENT     Return
     *    ESC     Escape
     *    F1      F1
     *    ...     ...
     *    F12     F12
     *    HEL     Help
     *    HOM     Home
     *    INS     Insert
     *    LAL     Alt_L
     *    LMA     Meta_L
     *    LCT     Control_L
     *    LEF     Left
     *    LSH     Shift_L
     *    LSK     Super_L
     *    NUM     Num_Lock
     *    PGD     Page_Down
     *    PGU     Page_Up
     *    PRT     Print
     *    RAL     Alt_R
     *    RMA     Meta_R
     *    RCT     Control_R
     *    RIG     Right
     *    RSH     Shift_R
     *    RSK     Super_R
     *    SCR     Scroll_Lock
     *    SPA     Space
     *    SPC     Space
     *    TAB     Tab
     *    UP      Up
     * </pre>
     *
     * <p>Maintains a level of compatibility with the sendkeys implementation
     * found in perl X11::GUITest.
     * </p>
     *
     */
    public final void sendKeys(final String keys) {
        // track status of various modifiers
        boolean modLock = false,
            shft = false, ctrl = false,
            alt = false, meta = false;

        for (int i = 0; i < keys.length(); i++) {
            char ch = keys.charAt(i);
            String key = Character.toString(ch);
            int keyCode = getKeyCode(key);

            if (ch == '{') {
                // Handle special key syntax {ENT} or {SPC 5}
                int processed = processBraceSet(keys.substring(i));
                // skip what we've processed...
                if (processed > 0) {
                    i += (processed - 1);
                }
                continue;
            } else if (ch == '~') {
                keyPressRelease(KeyEvent.VK_ENTER);
            } else if (ch == '+') {
                keyPress(KeyEvent.VK_SHIFT);
                shft = true;
            } else if (ch == '^') {
                keyPress(KeyEvent.VK_CONTROL);
                ctrl = true;
            } else if (ch == '%') {
                keyPress(KeyEvent.VK_ALT);
                alt = true;
            } else if (ch == '#') {
                keyPress(KeyEvent.VK_META);
                meta = true;
            } else if (ch == '(') {
                // we'll keep modifiers [On] until we find a closing )
                modLock = true;
            } else if (ch == ')') {
                modLock = false;
            } 
            else {
                // normal key
                boolean needControl = isControlNeeded(key);
                boolean needShift = isShiftNeeded(key);
                if (!ctrl && needControl) {
                    // a small sub-set of "keys" may need this modifier...
                    this.keyPress(KeyEvent.VK_CONTROL);
                }
                if (!shft && needShift) {
                    this.keyPress(KeyEvent.VK_SHIFT);
                }
                ////
                keyPressRelease(keyCode);
                ////
                if (!shft && needShift) {
                    this.keyRelease(KeyEvent.VK_SHIFT);
                }
                if (!ctrl && needControl) {
                    this.keyRelease(KeyEvent.VK_CONTROL);
                }
            }

            // peak ahead
            if ((i + 1) < keys.length() && keys.charAt(i + 1) == '(') {
                // we appear to be up to a modifier lock/group: ( in %(a),
                // so handle it next..
                continue;
            }

            // cleanup any existing modifier locks...
            if (!modLock) {
                if (shft) {
                    keyRelease(KeyEvent.VK_SHIFT);
                    shft = false;
                }
                if (ctrl) {
                    keyRelease(KeyEvent.VK_CONTROL);
                    ctrl = false;
                }
                if (alt) {
                    keyRelease(KeyEvent.VK_ALT);
                    alt = false;
                }
                if (meta) {
                    keyRelease(KeyEvent.VK_META);
                    meta = false;
                }
            }
        } // for each char/key
    }

    /**
     *
     * @param keyPortion Partial key sequence containing braced syntax keys
     * {ENT}, {SPC 5}
     * @return Number of characters processed belonging to brace-set.
     */
    private int processBraceSet(final String keyPortion) {
        Matcher m = bracePattern.matcher(keyPortion);
        if (m.find()) {
            String rawName = keyPortion.substring(1, m.end() - 1);

            String[] nameCount = rawName.split(" "); // [KEY] [COUNT]
            String name = rawName;
            int count = 1;
            if (nameCount.length == 2) {
                name = nameCount[0];
                count = Integer.parseInt(nameCount[1]);
            }
            //System.out.println("Name: '" + name +"', " +
            //        "Raw: '" + rawName +"', " + count);
            int keyCode = getKeyCode(name);
            if (keyCode != KeyEvent.VK_UNDEFINED) {
                boolean needControl = isControlNeeded(name);
                boolean needShift = isShiftNeeded(name);
                for (int i = 0; i < count; i++) {
                    if (needControl) {
                        keyPress(KeyEvent.VK_CONTROL);
                    }
                    if (needShift) {
                        keyPress(KeyEvent.VK_SHIFT);
                    }
                    keyPressRelease(keyCode);
                    if (needShift) {
                        keyRelease(KeyEvent.VK_SHIFT);
                    }
                    if (needControl) {
                        keyRelease(KeyEvent.VK_CONTROL);
                    }
                }
            } else {
                if (name.startsWith("PAUS")) {
                    try {
                        Thread.sleep(count);
                    } catch (InterruptedException e) { /* It'll be ok... */ }
                } else {
                    throw new IllegalArgumentException(
                            "Unable to handle key '" + rawName + "'"
                    );
                }
            }
            return rawName.length() + 2; // include {}
        }
        return 0;
    }

    /**
     * Performs a keyPress and keyRelease.
     *
     * @param keyCode KeyEvent.VK_*
     */
    private void keyPressRelease(final int keyCode) {
        keyPress(keyCode);
        keyRelease(keyCode);
    }

    /**
     * Determines if the specified key needs the shift key.
     *
     * @param key Key to check, can be "a", "+", "ESC", etc.
     * @return true/false
     */
    private static boolean isShiftNeeded(final String key) {
        if (key.length() == 1) {
            if (Character.isUpperCase(key.charAt(0))) {
                return true;
            }
        }
        if (getModifier(key) == KeyEvent.VK_SHIFT) {
            return true;
        }
        return false;
    }

    /**
     * Determines if the specified key needs the control key.
     *
     * @param key Key to check, can be "a", "BRE", etc.
     * @return true/false
     */
    private static boolean isControlNeeded(final String key) {
        if (getModifier(key) == KeyEvent.VK_CONTROL) {
            return true;
        }
        return false;
    }

    /**
     * Gets a KeyEvent.VK_* value for the given key.
     *
     * @param key Key to obtain keycode for
     * @return KeyEvent.VK_* value
     */
    private static int getKeyCode(final String key) {
        if (keyMap.containsKey(key)) {
            // Is known key...
            return keyMap.get(key)[0];
        } else {
            try {
                // Unknown key, resolve it...
                Field f = KeyEvent.class.getField("VK_" + key.toUpperCase());
                f.setAccessible(true);
                Integer val = (Integer) f.get(null);
                keyMap.put(key, new Integer[] {val}); // store it for next time
                return val;
            } catch (NoSuchFieldException ex) {
                return KeyEvent.VK_UNDEFINED;
            } catch (IllegalAccessException ex) {
                return KeyEvent.VK_UNDEFINED;
            }
        }
    }

    /**
     * Gets the modifier required for the given key.
     *
     * @param key Key to obtain modifier for
     * @return KeyEvent.VK_* value for the modifier.
     * KeyEvent.VK_UNDEFINED if none exists.
     */
    private static int getModifier(final String key) {
        if (keyMap.containsKey(key)) {
            Integer[] keyCodes = keyMap.get(key);
            if (keyCodes.length == 2) { // must have Key+Modifier
                return keyCodes[1];
            }
        }
        return KeyEvent.VK_UNDEFINED;
    }

    /**
     * Key mapping table.
     */
    private static HashMap<String, Integer[]> keyMap =
               new HashMap<String, Integer[]>();
    static {
        // "KeyName" => [KeyCode, (Optional: KeyCode of Modifier)]
        keyMap.put("BAC", new Integer[]{KeyEvent.VK_BACK_SPACE});
        keyMap.put("BS", new Integer[]{KeyEvent.VK_BACK_SPACE});
        keyMap.put("BKS", new Integer[]{KeyEvent.VK_BACK_SPACE});
        keyMap.put("BRE", new Integer[]{KeyEvent.VK_PAUSE,KeyEvent.VK_CONTROL});
        keyMap.put("CAN", new Integer[]{KeyEvent.VK_CANCEL});
        keyMap.put("CAP", new Integer[]{KeyEvent.VK_CAPS_LOCK});
        keyMap.put("DEL", new Integer[]{KeyEvent.VK_DELETE});
        keyMap.put("DOW", new Integer[]{KeyEvent.VK_DOWN});
        keyMap.put("END", new Integer[]{KeyEvent.VK_END});
        keyMap.put("ENT", new Integer[]{KeyEvent.VK_ENTER});
        keyMap.put("ESC", new Integer[]{KeyEvent.VK_ESCAPE});
        keyMap.put("HEL", new Integer[]{KeyEvent.VK_HELP});
        keyMap.put("HOM", new Integer[]{KeyEvent.VK_HOME});
        keyMap.put("INS", new Integer[]{KeyEvent.VK_INSERT});
        keyMap.put("LEF", new Integer[]{KeyEvent.VK_LEFT});
        keyMap.put("NUM", new Integer[]{KeyEvent.VK_NUM_LOCK});
        keyMap.put("PGD", new Integer[]{KeyEvent.VK_PAGE_DOWN});
        keyMap.put("PGU", new Integer[]{KeyEvent.VK_PAGE_UP});
        keyMap.put("PRT", new Integer[]{KeyEvent.VK_PRINTSCREEN});
        keyMap.put("RIG", new Integer[]{KeyEvent.VK_RIGHT});
        keyMap.put("SCR", new Integer[]{KeyEvent.VK_SCROLL_LOCK});
        keyMap.put("TAB", new Integer[]{KeyEvent.VK_TAB});
        keyMap.put("UP", new Integer[]{KeyEvent.VK_UP});
        keyMap.put("F1", new Integer[]{KeyEvent.VK_F1});
        keyMap.put("F2", new Integer[]{KeyEvent.VK_F2});
        keyMap.put("F3", new Integer[]{KeyEvent.VK_F3});
        keyMap.put("F4", new Integer[]{KeyEvent.VK_F4});
        keyMap.put("F5", new Integer[]{KeyEvent.VK_F5});
        keyMap.put("F6", new Integer[]{KeyEvent.VK_F6});
        keyMap.put("F7", new Integer[]{KeyEvent.VK_F7});
        keyMap.put("F8", new Integer[]{KeyEvent.VK_F8});
        keyMap.put("F9", new Integer[]{KeyEvent.VK_F9});
        keyMap.put("F10", new Integer[]{KeyEvent.VK_F10});
        keyMap.put("F11", new Integer[]{KeyEvent.VK_F11});
        keyMap.put("F12", new Integer[]{KeyEvent.VK_F12});
        keyMap.put("SPC", new Integer[]{KeyEvent.VK_SPACE});
        keyMap.put("SPA", new Integer[]{KeyEvent.VK_SPACE});
        keyMap.put("LSK", new Integer[]{KeyEvent.VK_WINDOWS});
        keyMap.put("RSK", new Integer[]{KeyEvent.VK_WINDOWS});
        //keyMap.put("MNU", new Integer[]{KeyEvent.VK_UNDEFINED});
        keyMap.put("~", new Integer[]{KeyEvent.VK_BACK_QUOTE,
                KeyEvent.VK_SHIFT});
        keyMap.put("_", new Integer[]{KeyEvent.VK_UNDERSCORE,
                KeyEvent.VK_SHIFT});
        keyMap.put("[", new Integer[]{KeyEvent.VK_OPEN_BRACKET});
        keyMap.put("]", new Integer[]{KeyEvent.VK_CLOSE_BRACKET});
        keyMap.put("!", new Integer[]{KeyEvent.VK_EXCLAMATION_MARK,
                KeyEvent.VK_SHIFT});
        keyMap.put("\"", new Integer[]{KeyEvent.VK_QUOTEDBL});
        keyMap.put("#", new Integer[]{KeyEvent.VK_NUMBER_SIGN,
                KeyEvent.VK_SHIFT});
        keyMap.put("$", new Integer[]{KeyEvent.VK_DOLLAR,
                KeyEvent.VK_SHIFT});
        keyMap.put("%", new Integer[]{KeyEvent.VK_5, KeyEvent.VK_SHIFT});
        keyMap.put("&", new Integer[]{KeyEvent.VK_AMPERSAND,
                KeyEvent.VK_SHIFT});
        keyMap.put("'", new Integer[]{KeyEvent.VK_QUOTE});
        keyMap.put("*", new Integer[]{KeyEvent.VK_ASTERISK,
                KeyEvent.VK_SHIFT});
        keyMap.put("+", new Integer[]{KeyEvent.VK_PLUS,
                KeyEvent.VK_SHIFT});
        keyMap.put(",", new Integer[]{KeyEvent.VK_COMMA});
        keyMap.put("-", new Integer[]{KeyEvent.VK_MINUS});
        keyMap.put(".", new Integer[]{KeyEvent.VK_PERIOD});
        keyMap.put("?", new Integer[]{KeyEvent.VK_SLASH, KeyEvent.VK_SHIFT});
        keyMap.put("<", new Integer[]{KeyEvent.VK_COMMA, KeyEvent.VK_SHIFT});
        keyMap.put(">", new Integer[]{KeyEvent.VK_PERIOD, KeyEvent.VK_SHIFT});
        keyMap.put("=", new Integer[]{KeyEvent.VK_EQUALS});
        keyMap.put("@", new Integer[]{KeyEvent.VK_AT, KeyEvent.VK_SHIFT});
        keyMap.put(":", new Integer[]{KeyEvent.VK_COLON, KeyEvent.VK_SHIFT});
        keyMap.put(";", new Integer[]{KeyEvent.VK_SEMICOLON});
        keyMap.put("\\", new Integer[]{KeyEvent.VK_BACK_SLASH});
        keyMap.put("`", new Integer[]{KeyEvent.VK_BACK_QUOTE});
        keyMap.put("{", new Integer[]{KeyEvent.VK_BRACELEFT,
                KeyEvent.VK_SHIFT});
        keyMap.put("}", new Integer[]{KeyEvent.VK_BRACERIGHT,
                KeyEvent.VK_SHIFT});
        keyMap.put("|", new Integer[]{KeyEvent.VK_BACK_SLASH,
                KeyEvent.VK_SHIFT});
        keyMap.put("^", new Integer[]{KeyEvent.VK_CIRCUMFLEX,
                KeyEvent.VK_SHIFT});
        keyMap.put("(", new Integer[]{KeyEvent.VK_LEFT_PARENTHESIS,
                KeyEvent.VK_SHIFT});
        keyMap.put(")", new Integer[]{KeyEvent.VK_RIGHT_PARENTHESIS,
                KeyEvent.VK_SHIFT});
        keyMap.put(" ", new Integer[]{KeyEvent.VK_SPACE});
        keyMap.put("/", new Integer[]{KeyEvent.VK_SLASH});
        keyMap.put("\t", new Integer[]{KeyEvent.VK_TAB});
        keyMap.put("\n", new Integer[]{KeyEvent.VK_ENTER});
        keyMap.put("LSH", new Integer[]{KeyEvent.VK_SHIFT});
        keyMap.put("RSH", new Integer[]{KeyEvent.VK_SHIFT});
        keyMap.put("LCT", new Integer[]{KeyEvent.VK_CONTROL});
        keyMap.put("RCT", new Integer[]{KeyEvent.VK_CONTROL});
        keyMap.put("LAL", new Integer[]{KeyEvent.VK_ALT});
        keyMap.put("RAL", new Integer[]{KeyEvent.VK_ALT});
        keyMap.put("LMA", new Integer[]{KeyEvent.VK_META});
        keyMap.put("RMA", new Integer[]{KeyEvent.VK_META});
        keyMap.put("Z"  , new Integer[]{KeyEvent.VK_Z});
    }

    /**
     * Pattern to use when processing bracesets.
     *
     * Allow: {}}, {} 5}, {KEY NUM}, {KEY}
     *
     */
    private static final Pattern bracePattern =
            Pattern.compile("}*\\s*\\d*(})");
    private int defaultButtons = InputEvent.BUTTON1_MASK;
}
