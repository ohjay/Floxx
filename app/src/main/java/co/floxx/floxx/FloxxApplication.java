package co.floxx.floxx;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

/**
 * Created by owenjow on 4/30/16.
 */
public class FloxxApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
