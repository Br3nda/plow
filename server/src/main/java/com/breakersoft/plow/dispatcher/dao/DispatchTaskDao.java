package com.breakersoft.plow.dispatcher.dao;

import java.util.List;

import com.breakersoft.plow.JobId;
import com.breakersoft.plow.Task;
import com.breakersoft.plow.dispatcher.domain.DispatchResource;
import com.breakersoft.plow.dispatcher.domain.DispatchTask;
import com.breakersoft.plow.rnd.thrift.RunTaskCommand;
import com.breakersoft.plow.thrift.TaskState;

public interface DispatchTaskDao {

    boolean reserve(Task task);

    boolean unreserve(Task task);

    boolean start(Task task);

    boolean stop(Task task, TaskState newState);

    RunTaskCommand getRunTaskCommand(Task task);

    List<DispatchTask> getDispatchableTasks(JobId job, DispatchResource resource);

}
