package mysterysuperhero.ru.ftracker.data;

import java.util.List;

import mysterysuperhero.ru.ftracker.utils.CustomApplicationClass;

/**
 * Created by dmitri on 25.11.16.
 */

public class DataBaseHelper {

    private DaoSession daoSession;
    private static final long MILLIS_IN_DAY = 24 * 60 * 60 * 1000;

    public DataBaseHelper() {
        daoSession = CustomApplicationClass.getDaoSession();
    }

    public StepsItem getSteps() {
        StepsItemDao stepsItemDao = daoSession.getStepsItemDao();
        List<StepsItem> stepsItems = stepsItemDao.queryBuilder().list();
        return stepsItems.size() != 0 ? stepsItems.get(0) : null;
    }

    public List<BPMItem> getBPMList() {
        BPMItemDao bpmItemDao = daoSession.getBPMItemDao();
        return bpmItemDao.queryBuilder().list();
    }

    public BPMItem getLastBPM() {
        BPMItemDao bpmItemDao = daoSession.getBPMItemDao();
        List<BPMItem> list = bpmItemDao.queryBuilder().list();
        return list.get(list.size() - 1);
    }

    public void saveStepsCount(long steps) {
        StepsItemDao stepsItemDao = daoSession.getStepsItemDao();
        List<StepsItem> stepsItems = stepsItemDao.queryBuilder().list();
        long currTime = System.currentTimeMillis();
        if (stepsItems.size() == 0) {
            stepsItemDao.insertOrReplace(new StepsItem(null, steps, currTime));
            return;
        }
        StepsItem lastStepsItem = stepsItems.get(0);
        if (lastStepsItem.getTime() / MILLIS_IN_DAY == currTime / MILLIS_IN_DAY) {
            lastStepsItem.setSteps(steps);
            lastStepsItem.setTime(currTime);
            stepsItemDao.insertOrReplace(lastStepsItem);
        } else {
            stepsItemDao.insert(new StepsItem(null, steps, currTime));
        }
    }

    public void saveBPM(long bpm) {
        BPMItemDao bpmItemDao = daoSession.getBPMItemDao();
        bpmItemDao.insert(new BPMItem(bpm, System.currentTimeMillis()));
    }

}
