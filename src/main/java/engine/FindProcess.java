package engine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FindProcess {

    public boolean exists(String trainerApp) {
        try {
            ProcessPid pid = getProcess(trainerApp);
            return pid != null;
        } catch (Exception e) {
            return false;
        }
    }

    private List<ProcessPid> getProcessList() {
        List<ProcessPid> commands = new ArrayList<>();
        ProcessHandle.allProcesses().forEach(e -> {
            e.info().command().ifPresent(f -> {
                commands.add(new ProcessPid(Path.of(f).getFileName().toString(), e.pid()));
            });
        });
        return commands;
    }

    public List<ProcessPid> getProcesses(String name) {
        List<ProcessPid> commands = getProcessList();
        return commands.stream().filter(e -> e.process.equals(Paths.get(name).getFileName().toString())).collect(Collectors.toList());
    }


    public ProcessPid getProcess(String name) throws Exception {
        List<ProcessPid> commands = getProcessList();
        Optional<ProcessPid> found = commands.stream().filter(e -> e.process.equals(Paths.get(name).getFileName().toString())).findFirst();
        if (found.isEmpty())
            throw new Exception("Could not find process!");
        return found.get();
    }


    public static class ProcessPid {
        String process;
        long pid;

        public ProcessPid(String process, long pid) {
            this.process = process;
            this.pid = pid;
        }

        public String getProcess() {
            return process;
        }

        public long getPid() {
            return pid;
        }

        @Override
        public String toString() {
            return "ProcessPid{" +
                    "process='" + process + '\'' +
                    ", pid=" + pid +
                    '}';
        }
    }
}
