package mysterysuperhero.ru.ftracker.data;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by dmitri on 25.11.16.
 */

@Entity
public class StepsItem {
    @Id
    private Long id;
    Long steps;
    Long time;

    @Generated(hash = 1133515702)
    public StepsItem(Long id, Long steps, Long time) {
        this.id = id;
        this.steps = steps;
        this.time = time;
    }

    @Generated(hash = 175207369)
    public StepsItem() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSteps() {
        return this.steps;
    }

    public void setSteps(Long steps) {
        this.steps = steps;
    }

    public Long getTime() {
        return this.time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
