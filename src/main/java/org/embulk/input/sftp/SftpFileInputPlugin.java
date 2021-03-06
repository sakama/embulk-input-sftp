package org.embulk.input.sftp;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.TransactionalFileInput;

import java.util.List;

public class SftpFileInputPlugin
        implements FileInputPlugin
{
    @Override
    public ConfigDiff transaction(ConfigSource config, FileInputPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        SftpFileInput.validateHost(task);

        // list files recursively
        task.setFiles(SftpFileInput.listFilesByPrefix(task));
        // number of processors is same with number of files
        return resume(task.dump(), task.getFiles().getTaskCount(), control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
                             int taskCount,
                             FileInputPlugin.Control control)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        String lastPath = null;
        if (task.getIncremental()) {
            lastPath = SftpFileInput.getRelativePath(task, task.getFiles().getLastPath(task.getLastPath()));
        }
        control.run(taskSource, taskCount);

        ConfigDiff configDiff = Exec.newConfigDiff();
        if (task.getIncremental() && lastPath != null) {
            configDiff.set("last_path", lastPath);
        }

        return configDiff;
    }

    @Override
    public void cleanup(TaskSource taskSource,
                        int taskCount,
                        List<TaskReport> successTaskReports)
    {
    }

    @Override
    public TransactionalFileInput open(TaskSource taskSource, int taskIndex)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        return new SftpFileInput(task, taskIndex);
    }
}
