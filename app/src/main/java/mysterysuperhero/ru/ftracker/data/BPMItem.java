package mysterysuperhero.ru.ftracker.data;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by dmitri on 25.11.16.
 */

@Entity
public class BPMItem {
    @Id
    private Long id;
    Long bpm;
    Long time;

    public BPMItem(long bpm, long time) {
        this.bpm = bpm;
        this.time = time;
    }

    @Generated(hash = 917907512)
    public BPMItem(Long id, Long bpm, Long time) {
        this.id = id;
        this.bpm = bpm;
        this.time = time;
    }

    @Generated(hash = 1778840973)
    public BPMItem() {
    }

    public long getBpm() {
        return bpm;
    }

    public void setBpm(long bpm) {
        this.bpm = bpm;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBpm(Long bpm) {
        this.bpm = bpm;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
