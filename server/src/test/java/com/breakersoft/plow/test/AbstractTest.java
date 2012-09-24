package com.breakersoft.plow.test;

import static org.junit.Assert.*;

import javax.annotation.Resource;

import org.junit.Before;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;

import com.breakersoft.plow.Job;
import com.breakersoft.plow.Project;
import com.breakersoft.plow.dao.ProjectDao;
import com.breakersoft.plow.json.Blueprint;
import com.breakersoft.plow.json.BlueprintLayer;
import com.breakersoft.plow.rnd.thrift.Hardware;
import com.breakersoft.plow.rnd.thrift.Ping;
import com.breakersoft.plow.service.ProjectService;
import com.breakersoft.plow.service.QuotaService;
import com.google.common.collect.Lists;

@Transactional
@ContextConfiguration(locations={
        "file:src/main/webapp/WEB-INF/spring/root-context.xml"
    })
public abstract class AbstractTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Resource
    ProjectService projectService;

    @Resource
    QuotaService quotaService;

    protected Project testProject;

    @Before
    public void initTestProject() {
        testProject = projectService.createProject("unittest", "Unit Test Project");
        quotaService.createQuota(testProject,"unassigned", 10, 15);
    }

    public Blueprint getTestBlueprint() {

        Blueprint bp = new Blueprint();
        bp.setName("test");
        bp.setPaused(false);
        bp.setProject("unittest");
        bp.setScene("seq");
        bp.setShot("shot");
        bp.setUid(100);
        bp.setUsername("gandalf");

        BlueprintLayer layer = new BlueprintLayer();
        layer.setChunk(1);
        layer.setCommand(new String[] { "sleep", "5" });
        layer.setMaxCores(8);
        layer.setMinCores(1);
        layer.setMinMemory(1024);
        layer.setName("test_ls");
        layer.setRange("1-10");

        bp.addLayer(layer);

        return bp;
    }

    public  Ping getTestNodePing() {

        Hardware hw = new Hardware();
        hw.cpuModel = "Intel i7";
        hw.platform = "OSX 10.8.1 x86_64";
        hw.freeRamMb = 4096;
        hw.freeSwapMb = 1024;
        hw.physicalCpus = 4;
        hw.totalRamMb = 4096;
        hw.totalSwapMb = 1024;

        Ping ping = new Ping();
        ping.bootTime = System.currentTimeMillis() - 1000;
        ping.hostname = "localhost";
        ping.ipAddr = "127.0.0.1";
        ping.isReboot = true;
        ping.processes = Lists.newArrayList();
        ping.hw = hw;

        return ping;
    }

    @SuppressWarnings("deprecation")
    public void assertFrameCount(Job job, int count) {
        assertEquals(count, simpleJdbcTemplate.queryForInt(
                "SELECT COUNT(1) FROM plow.task, plow.layer " +
                "WHERE task.pk_layer = layer.pk_layer AND layer.pk_job=?", job.getJobId()));
    }

    @SuppressWarnings("deprecation")
    public void assertLayerCount(Job job, int count) {
        assertEquals(count, simpleJdbcTemplate.queryForInt(
                "SELECT COUNT(1) FROM plow.layer " +
                "WHERE layer.pk_job=?", job.getJobId()));
    }
}
