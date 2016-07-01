package co.floxx.floxx;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * A static container for inter-activity message passing.
 * @author owenjow
 */
public class Intermediary {
    public static HashSet<String> mapToPortalOthers;
    public static String mapToPortalMeetupId;
    public static ArrayList<String> firebaseConfirmed;
    public static String firebaseMeetupId;
    public static boolean firebaseToFullscreen;
    public static boolean confToFullscreen;
    public static boolean userPortalToFullscreen;
}
