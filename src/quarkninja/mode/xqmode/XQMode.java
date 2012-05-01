package quarkninja.mode.xqmode;

import java.io.File;
import processing.app.Base;
import processing.mode.java.JavaMode;

/**
 * Mode Template for extending Java mode in Processing IDE 2.0a5 or later.
 *
 */
public class XQMode extends JavaMode {

    public XQMode(Base base, File folder) {
        super(base, folder);
        System.out.println("Mode initialized.");
    }

    /**
     * Called by PDE
     */
    @Override
    public String getTitle() {
        return "TEH XQMode";
    }
}
