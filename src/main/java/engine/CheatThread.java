package engine;

import games.Game;
import games.RunnableCheat;
import io.Cheat;
import io.HotKey;
import message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import response.Failure;
import response.Response;
import response.Success;
import script.Script;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

public class CheatThread implements Runnable {
    static Logger log = LoggerFactory.getLogger(CheatThread.class);
    private BlockingQueue<Message> messageQueue;

    public CheatThread(BlockingQueue<Message> messageQueue) {
        this.messageQueue = messageQueue;
    }

    @Override
    public void run() {
        Message msg;
        while (true) {
            try {
                msg = messageQueue.take();
                if (msg.getData() instanceof ExitMessage)
                    break;
                else if (msg.getData() instanceof RunnableCheat) {
                    startCheat((RunnableCheat) msg.getData(), msg.getResponse());
                }
                else if (msg.getData() instanceof ExitCheat) {
                    exitCheat(msg.getResponse());
                }
                else if (msg.getData() instanceof CheatStatus) {
                    getCheatStatus(msg.getResponse());
                }
                else if (msg.getData() instanceof CheatToggle) {
                    toggleCheat(((CheatToggle)msg.getData()).getId(),  msg.getResponse());
                }
                else if (msg.getData() instanceof CheatTrigger) {
                    triggerCheat(((CheatTrigger)msg.getData()).getId(),  msg.getResponse());
                }
                else if (msg.getData() instanceof CheatReset) {
                    resetCheat(((CheatReset)msg.getData()).getId(),  msg.getResponse());
                }
                else if (msg.getData() instanceof TrainerTrigger) {
                    triggerTrainer(((TrainerTrigger)msg.getData()).getHotKey(),  msg.getResponse());
                }
                else if (msg.getData() instanceof ProcessComplete) {
                    log.info("engine.Process thread completed {}", ((ProcessComplete)msg.getData()).isTerminated());
                    Optional.ofNullable(Process.getInstance()).ifPresent(Process::close);
                }
            } catch (InterruptedException e) {
                log.warn(e.getMessage());
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        System.out.println("Exited cheat thread!");
    }

    private void toggleCheat(int id, CompletableFuture<Response> response) {
        try {
            if (Process.getInstance() != null) {
                Process.getInstance().toggleCheat(id);
                response.complete(new Success());
            }
            else {
                response.complete(new Failure("No cheats running."));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            response.complete(new Failure(e.getMessage()));
        }
    }

    private void triggerCheat(int id, CompletableFuture<Response> response) {
        try {
            if (Process.getInstance() != null) {
                Process.getInstance().triggerCheat(id);
                response.complete(new Success());
            }
            else {
                response.complete(new Failure("No cheats running."));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            response.complete(new Failure(e.getMessage()));
        }
    }

    private void triggerTrainer(HotKey key, CompletableFuture<Response> response) {
        try {
            if (Process.getInstance() != null) {
                Process.getInstance().triggerTrainer(key);
                response.complete(new Success());
            }
            else {
                response.complete(new Failure("No process running."));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            response.complete(new Failure(e.getMessage()));
        }
    }



    private void resetCheat(int id, CompletableFuture<Response> response) {
        try {
            if (Process.getInstance() != null) {
                Process.getInstance().resetCheat(id);
                response.complete(new Success());
            }
            else {
                response.complete(new Failure("No cheats running."));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            response.complete(new Failure(e.getMessage()));
        }
    }

    private void getAppStatus(CompletableFuture<Response> response) {
        if (Process.getInstance() == null) {

        }
    }


    private void getCheatStatus(CompletableFuture<Response> response) {
        List<Cheat> cheats = new ArrayList<>();
        if (Process.getInstance() != null) {
            if (Process.getInstance().cheatList != null) {
                for (Cheat c : Process.getInstance().cheatList) {
                    c.updateData();
                    cheats.add(c);
                }
            }
            if (Process.getInstance().scriptList != null) {
                for (Script script : Process.getInstance().scriptList) {
                    for (Cheat c : script.getAllCheats()) {
                        c.updateData();
                        cheats.add(c);
                    }

                }
            }
            response.complete(new response.CheatStatus(cheats, Process.getInstance().getData().getSystem(), Process.getInstance().getGame(), Process.getInstance().getData().getCht()));
        }
        else {
            response.complete(new response.CheatStatus("Application has exited..."));
        }
        /*
        List<Cheat> cheats = new ArrayList<>();
        if (currentProcess != null) {
            currentProcess.masterList.forEach((value) -> {
                cheats.addAll(value.getCodes());
            });

            currentProcess.scriptList.forEach((value) -> {
                try {
                    cheats.addAll(value.getCheats());
                } catch (Exception e) {
                    log.error("Could not get script cheat status: {}", e.getMessage());
                }
            });

            response.complete(new response.CheatStatus(cheats, currentProcess.getData().getSystem(), currentProcess.getGameName(), currentProcess.getData().getCht()));
        }
        else {
            response.complete(new response.CheatStatus(null, "", "", ""));
        }
*/
    }

    private void exitCheat(CompletableFuture<Response> response) {
        Optional.ofNullable(Process.getInstance()).ifPresent(Process::exit);
        if (response != null)
            response.complete(new Success());
    }

    private void startCheat(RunnableCheat data, CompletableFuture<Response> response) {
        try {
            if (checkRunning(data)) {
                log.debug("Cheat is already running.  No need to rerun");
                response.complete(new Success());
                return;
            }
            exitCheat(null);
            Process.create(data, messageQueue).start();
        } catch (Exception e) {
            response.complete(new Failure(e.getMessage()));
            return;
        }
        response.complete(new Success());
    }

    private boolean checkRunning(RunnableCheat data) {
        return Optional.ofNullable(Process.getInstance()).map(e->e.getData().equals(data)).orElse(false);
    }
}
