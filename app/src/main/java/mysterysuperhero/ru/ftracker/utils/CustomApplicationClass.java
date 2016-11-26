package mysterysuperhero.ru.ftracker.utils;

import android.app.Application;

import org.greenrobot.greendao.database.Database;

import mysterysuperhero.ru.ftracker.data.DaoMaster;
import mysterysuperhero.ru.ftracker.data.DaoSession;

/**
 * Created by dmitri on 25.11.16.
 */

public class CustomApplicationClass extends Application {

    private static DaoSession daoSession;
    private static DaoMaster daoMaster;
    private static Database db;

    @Override
    public void onCreate() {

        super.onCreate();

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "ftracker-db");
        db = helper.getWritableDb();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();

//        updateSchema();

    }

    public static DaoSession getDaoSession() {
        return daoSession;
    }

    public static void updateSchema() {
        DaoMaster.dropAllTables(db, true);
        DaoMaster.createAllTables(db, true);
    }
}
