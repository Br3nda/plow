package com.breakersoft.plow.test.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.annotation.Resource;

import org.junit.Test;

import com.breakersoft.plow.Job;
import com.breakersoft.plow.dao.JobDao;
import com.breakersoft.plow.event.JobLaunchEvent;
import com.breakersoft.plow.service.JobService;
import com.breakersoft.plow.test.AbstractTest;
import com.breakersoft.plow.thrift.JobSpecT;
import com.breakersoft.plow.thrift.JobState;
import com.google.common.collect.Maps;

public class JobDaoTests extends AbstractTest {

    @Resource
    JobDao jobDao;

    @Resource
    JobService jobService;

    @Test
    public void testCreate() {
        jobDao.create(TEST_PROJECT, getTestJobSpec(), false);
    }

    @Test
    public void testSetMinCores() {
        JobSpecT spec = getTestJobSpec();
        JobLaunchEvent event = jobService.launch(spec);
        jobDao.setMinCores(event.getJob(), 101);

        int value = jdbc().queryForInt(
                "SELECT int_cores_min FROM plow.job_dsp WHERE pk_job=?",
                event.getJob().getJobId());
        assertEquals(101, value);
    }

    @Test
    public void testSetMaxCores() {
        JobSpecT spec = getTestJobSpec();
        JobLaunchEvent event = jobService.launch(spec);
        jobDao.setMaxCores(event.getJob(), 101);

        int value = jdbc().queryForInt(
                "SELECT int_cores_max FROM plow.job_dsp WHERE pk_job=?",
                event.getJob().getJobId());
        assertEquals(101, value);
    }

    @Test
    public void testGetByNameAndState() {
        JobSpecT spec = getTestJobSpec();
        Job jobA = jobDao.create(TEST_PROJECT, spec, false);
        Job jobB = jobDao.get(spec.getName(), JobState.INITIALIZE);

        assertEquals(jobA.getJobId(), jobB.getJobId());
        assertEquals(jobA.getProjectId(), jobB.getProjectId());
    }

    @Test
    public void testGetById() {
        JobSpecT spec = getTestJobSpec();
        Job jobA = jobDao.create(TEST_PROJECT, spec, false);
        Job jobB = jobDao.get(jobA.getJobId());

        assertEquals(jobA.getJobId(), jobB.getJobId());
        assertEquals(jobA.getProjectId(), jobB.getProjectId());
    }

    @Test
    public void testGetActiveById() {
        JobSpecT spec = getTestJobSpec();
        Job jobA = jobDao.create(TEST_PROJECT, spec, false);
        Job jobB = jobDao.getActive(jobA.getJobId());

        assertEquals(jobA.getJobId(), jobB.getJobId());
        assertEquals(jobA.getProjectId(), jobB.getProjectId());
    }

    @Test
    public void testGetActiveByName() {
        JobSpecT spec = getTestJobSpec();
        Job jobA = jobDao.create(TEST_PROJECT, spec, false);
        Job jobB = jobDao.getActive(spec.getName());

        assertEquals(jobA.getJobId(), jobB.getJobId());
        assertEquals(jobA.getProjectId(), jobB.getProjectId());
    }

    @Test
    public void testGetActiveByNameOrId() {
        JobSpecT spec = getTestJobSpec();
        Job jobA = jobDao.create(TEST_PROJECT, spec, false);
        Job jobB = jobDao.get(jobA.getJobId());
        Job jobC = jobDao.getByActiveNameOrId(spec.getName());

        assertEquals(jobA.getJobId(), jobB.getJobId());
        assertEquals(jobA.getJobId(), jobC.getJobId());
        assertEquals(jobA.getProjectId(), jobB.getProjectId());
    }


    @Test
    public void isFinished() {
        JobSpecT spec = getTestJobSpec();
        JobLaunchEvent event = jobService.launch(spec);
        assertFalse(jobDao.isFinished(event.getJob()));
        jobService.shutdown(event.getJob());
        assertTrue(jobDao.isFinished(event.getJob()));
    }

    @Test
    public void testShutdown() {
        JobSpecT spec = getTestJobSpec();
        JobLaunchEvent event = jobService.launch(spec);
        assertTrue(jobDao.shutdown(event.getJob()));
        assertFalse(jobDao.shutdown(event.getJob()));
    }

    @Test
    public void testPause() {
        JobSpecT spec = getTestJobSpec();
        JobLaunchEvent event = jobService.launch(spec);
        jobDao.setPaused(event.getJob(), true);
        assertTrue(jobDao.isPaused(event.getJob()));
        jobDao.setPaused(event.getJob(), false);
        assertFalse(jobDao.isPaused(event.getJob()));
    }

    @Test
    public void testSetAttrs() {
        JobSpecT spec = getTestJobSpec();
        Job job = jobService.launch(spec).getJob();

        Map<String,String> attrs = Maps.newHashMap();
        attrs.put("foo", "bar");
        attrs.put("bing", "bong");

        jobDao.setAttrs(job, attrs);

        Map<String, String> result = jobDao.getAttrs(job);
        assertEquals("bar", result.get("foo"));
        assertEquals("bong", result.get("bing"));
    }
}
