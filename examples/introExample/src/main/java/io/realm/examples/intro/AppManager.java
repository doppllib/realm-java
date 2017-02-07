package io.realm.examples.intro;
import android.content.Context;

/**
 * Created by kgalligan on 1/28/17.
 */

public class AppManager
{
    private static Context appContext;

    public static void setAppContext(Context appContext)
    {
        AppManager.appContext = appContext;
    }

    public static Context getAppContext()
    {
        return appContext;
    }
}
