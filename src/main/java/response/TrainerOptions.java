package response;

import io.HotKey;

import java.util.List;

public class TrainerOptions extends Response {
    List<HotKey> trainerItems;

    public TrainerOptions(List<HotKey> list) {
        super(list == null || list.size() == 0 ?  STATUS_FAIL : STATUS_SUCCESS);
        if (list == null || list.size() == 0) {
            this.message = "No options available.";
        }
        this.trainerItems = list;
    }


    public List<HotKey> getTrainerItems() {
        return trainerItems;
    }
}
