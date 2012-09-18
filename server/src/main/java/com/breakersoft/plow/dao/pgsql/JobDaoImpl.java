package com.breakersoft.plow.dao.pgsql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.breakersoft.plow.Job;
import com.breakersoft.plow.JobE;
import com.breakersoft.plow.Project;
import com.breakersoft.plow.dao.AbstractDao;
import com.breakersoft.plow.dao.JobDao;
import com.breakersoft.plow.json.Blueprint;
import com.breakersoft.plow.thrift.TaskState;
import com.breakersoft.plow.thrift.JobState;
import com.breakersoft.plow.util.JdbcUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Repository
public final class JobDaoImpl extends AbstractDao implements JobDao {

    @SuppressWarnings("unused")
    private static final Logger logger =
            org.slf4j.LoggerFactory.getLogger(JobDaoImpl.class);

    public static final RowMapper<Job> MAPPER = new RowMapper<Job>() {

        @Override
        public Job mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            JobE job = new JobE();
            job.setJobId((UUID) rs.getObject(1));
            job.setProjectId((UUID) rs.getObject(2));
            return job;
        }
    };

    private static final String GET =
            "SELECT " +
                "pk_job,"+
                "pk_project " +
            "FROM " +
                "plow.job ";

    @Override
    public Job get(String name, JobState state) {
        return jdbc.queryForObject(
                GET + "WHERE str_name=? AND int_state=?",
                MAPPER, name, state.ordinal());
    }

    @Override
    public Job get(UUID id) {
        return jdbc.queryForObject(
                GET + "WHERE pk_job=?",
                MAPPER, id);
    }

    private static final String INSERT[] = {
        JdbcUtils.Insert("plow.job",
                "pk_job", "pk_project", "str_name", "str_active_name",
                "str_user", "int_uid", "int_state", "bool_paused"),
    };

    @Override
    public Job create(Project project, Blueprint blueprint) {

        final UUID jobId = UUID.randomUUID();
        jdbc.update(
                INSERT[0],
                jobId,
                project.getProjectId(),
                blueprint.getFullJobName(),
                blueprint.getFullJobName(),
                blueprint.getUsername(),
                blueprint.getUid(),
                JobState.INITIALIZE.ordinal(),
                blueprint.isPaused());

        jdbc.update("INSERT INTO plow.job_count (pk_job) VALUES (?)", jobId);

        final JobE job = new JobE();
        job.setJobId(jobId);
        job.setProjectId(project.getProjectId());
        job.setFolderId(null); // Don't know folder yet
        return job;
    }

    @Override
    public void updateFrameStatesForLaunch(Job job) {
        jdbc.update("UPDATE plow.task SET int_state=? WHERE pk_layer " +
                "IN (SELECT pk_layer FROM plow.layer WHERE pk_job=?)",
                TaskState.WAITING.ordinal(), job.getJobId());
    }

    private static final String GET_FRAME_STATUS_COUNTS =
            "SELECT " +
                "COUNT(1) AS c, " +
                "task.int_state, " +
                "task.pk_layer  " +
            "FROM " +
                "plow.task," +
                "plow.layer " +
            "WHERE " +
                "task.pk_layer = layer.pk_layer " +
            "AND "+
                "layer.pk_job=? " +
            "GROUP BY " +
                "task.int_state,"+
                "task.pk_layer";

    @Override
    public void updateFrameCountsForLaunch(Job job) {

        Map<Integer, Integer> jobRollup = Maps.newHashMap();
        Map<String, List<Integer>> layerRollup = Maps.newHashMap();

        for (Map<String, Object> entry:
            jdbc.queryForList(GET_FRAME_STATUS_COUNTS, job.getJobId())) {

            String layerId = entry.get("pk_layer").toString();
            int state = (Integer) entry.get("int_state");
            int count = ((Long)entry.get("c")).intValue();

            // Rollup counts for job.
            Integer stateCount = jobRollup.get(state);
            if (stateCount == null) {
                jobRollup.put(state, count);
            }
            else {
                jobRollup.put(state, count + stateCount);
            }

            // Rollup stats for layers.
            List<Integer> layerCounts = layerRollup.get(layerId);
            if (layerCounts == null) {
                layerRollup.put(layerId, Lists.newArrayList(state, count));
            }
            else {
                layerRollup.get(layerId).add(state);
                layerRollup.get(layerId).add(count);
            }
        }

        final StringBuilder sb = new StringBuilder(512);
        final List<Object> values = Lists.newArrayList();

        // Apply layer counts
        for (Map.Entry<String, List<Integer>> entry: layerRollup.entrySet()) {
            List<Integer> d = entry.getValue();
            values.clear();

            sb.setLength(0);
            sb.append("UPDATE plow.layer_count SET");
            for (int i=0; i < entry.getValue().size(); i=i+2) {
                sb.append(" int_");
                sb.append(TaskState.findByValue(d.get(i)).toString().toLowerCase());
                sb.append("=?,");
                values.add(d.get(i+1));
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(" WHERE pk_layer=?");
            values.add(UUID.fromString(entry.getKey()));
            jdbc.update(sb.toString(), values.toArray());
        }

        values.clear();
        sb.setLength(0);
        sb.append("UPDATE plow.job_count SET ");
        for (Map.Entry<Integer,Integer> entry: jobRollup.entrySet()) {
            sb.append("int_");
            sb.append(TaskState.findByValue(entry.getKey()).toString().toLowerCase());
            sb.append("=?,");
            values.add(entry.getValue());
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(" WHERE pk_job=?");
        values.add(job.getJobId());
        jdbc.update(sb.toString(), values.toArray());
    }
}