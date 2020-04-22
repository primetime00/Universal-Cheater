package response;


import io.Code;

import java.util.List;

public class CheatList extends Response {
    private List<Code> cheatList;


    public CheatList(List<Code> codes) {
        super(Response.STATUS_SUCCESS);
        this.cheatList = codes;
    }
}
