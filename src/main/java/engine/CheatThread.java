package engine;

import cheat.Cheat;
import games.RunnableCheat;
import message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import response.Failure;
import response.Response;
import response.Success;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

public class CheatThread implements Runnable {
    static Logger log = LoggerFactory.getLogger(CheatThread.class);
    private BlockingQueue<Message> messageQueue;
    private Process currentProcess;

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
                else if (msg.getData() instanceof CheatReset) {
                    resetCheat(((CheatReset)msg.getData()).getId(),  msg.getResponse());
                }
                else if (msg.getData() instanceof ProcessComplete) {
                    log.info("engine.Process thread completed {}", ((ProcessComplete)msg.getData()).isTerminated());
                    currentProcess.close();
                    currentProcess = null;
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
            if (currentProcess != null) {
                currentProcess.toggleCheat(id);
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

    private void resetCheat(int id, CompletableFuture<Response> response) {
        try {
            if (currentProcess != null) {
                currentProcess.resetCheat(id);
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


    private void getCheatStatus(CompletableFuture<Response> response) {
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

    }

    private void exitCheat(CompletableFuture<Response> response) {
        if (currentProcess != null) {
            currentProcess.exit();
            currentProcess = null;
        }
        if (response != null)
            response.complete(new Success());
    }

    private void startCheat(RunnableCheat data, CompletableFuture<Response> response) {
        try {
            if (checkRunning(data)) {
                log.info("Cheat is already running.  No need to rerun");
                response.complete(new Success());
                return;
            }
            exitCheat(null);
            currentProcess = new Process(data, messageQueue);
            currentProcess.start();
        } catch (Exception e) {
            response.complete(new Failure(e.getMessage()));
            return;
        }
        response.complete(new Success());
    }

    private boolean checkRunning(RunnableCheat data) {
        if (currentProcess == null)
            return false;
        return currentProcess.getData().equals(data);
    }
}
