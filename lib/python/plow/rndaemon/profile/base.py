import os
import platform
import socket
import logging

from threading import RLock 

from .. import conf
from .. import client
from ..rpc import ttypes

logger = logging.getLogger(__name__)


class AbstractProfiler(object):

    def __init__(self):

        self.data = {
            "platform": platform.platform(),
            "load": (-1.0, -1.0, -1.0)
        }

        self.__updateLock = RLock()
        self.update()

        logCpu = self.data.get('logicalCpus', 1)
        physCpu = self.data.get('physicalCpus', 1)
        ht_factor = logCpu // physCpu
        self.hyperthread_factor = max(ht_factor, 1)

        for key, value in self.data.iteritems():
            logger.debug("%s = %s" % (key, value))

    def __getattr__(self, k):
        return self.data[k]

    def __str__(self):
        return str(self.data)

    def getPing(self, update=False):
        if update:
            self.update()

        # Create the hardware profile
        hw = ttypes.Hardware()
        hw.physicalCpus = self.physicalCpus
        hw.logicalCpus = self.logicalCpus
        hw.totalRamMb = self.totalRamMb
        hw.freeRamMb = self.freeRamMb
        hw.totalSwapMb = self.totalSwapMb
        hw.freeSwapMb = self.freeSwapMb
        hw.cpuModel = self.cpuModel
        hw.platform = self.platform
        hw.load = self.load

        # Create a ping
        ping = ttypes.Ping()
        ping.hostname = socket.getfqdn()
        ping.ipAddr = socket.gethostbyname(ping.hostname)
        ping.bootTime = self.bootTime
        ping.hw = hw

        return ping

    def sendPing(self, tasks, isReboot=False):
        if conf.NETWORK_DISABLED:
            self.update()
            return

        ping = self.getPing(update=True)
        ping.isReboot = isReboot

        logger.info("Sending ping with %d running tasks: %s", len(tasks), ping)

        ping.tasks = tasks
        logger.debug("Running tasks sent with ping: %s", tasks)

        try:
            service, transport = client.getPlowConnection()
            service.sendPing(ping)
            transport.close()
        except Exception, e:
            logger.warn("Unable to send ping to plow server, %s" % e)

    def _update(self):
        """
        Protected update method, for subclasses to define
        how to populate the profile data on the specific platform.
        The class will call this method first, followed by post operations
        to clean the data if needed.
        """
        pass

    def update(self):
        """
        Public update() method

        Don't re-implement this method in subclasses.
        Instead, re-implement the protected _update().
        This method will be called after running _update().
        """
        with self.__updateLock:
            try:
                self.data['load'] = os.getloadavg()
            except OSError:
                self.data['load'] = (-1.0, -1.0, -1.0)

            self._update()

            for name in ('logicalCpus', 'physicalCpus', 'totalRamMb'):
                val = conf.getint('profile', name)
                if val is not None:# and val > 0:

                    # Limit the total ram value, and also
                    # adjust the free ram to cap out as well.
                    if name == 'totalRamMb':

                        if val >= self.totalRamMb:
                            logger.warn("Config setting totalRamMb override " \
                                        "value higher than system total of %d - Ignoring", 
                                        self.totalRamMb)
                            continue

                        self.data['totalRamMb'] = val
                        self.data['freeRamMb'] = min(val, self.data['freeRamMb'])

                    else:
                        self.data[name] = val
                    
                    logger.debug("Using profile override: %s = %s" % (name, val))

            # Make sure that no matter what, is should be impossible
            # for cpu counts to be zero
            self.data['physicalCpus'] = max(self.data['physicalCpus'], 1)
            self.data['logicalCpus'] = max(self.data['logicalCpus'], 1)

    def reboot(self):
        """
        reboot()

        Abstract method for performing a reboot of the system

        Should raise plow.rndaemon.rpc.ttypes.RndException if not successful
        """
        raise NotImplementedError("reboot() is an abstract method")

    def getSubprocessOpts(self, cmd, **kwargs):
        """
        getSubprocessOpts(list|str cmd, **kwargs) -> (cmd, dict)

        Method for returning the appropriate subprocess.Popen
        arguments and keyword arguments for a given platform.

        """
        env = kwargs.get('env') or os.environ.copy()

        core_count = len(kwargs.get('cpus', [0]))

        if not 'PLOW_CORES' in env:
            env['PLOW_CORES'] = str(core_count)

        if not 'PLOW_THREADS' in env:
            env['PLOW_THREADS'] = str(core_count * self.hyperthread_factor)

        opts = dict(
            shell=False,
            stdout=kwargs.get('stdout'),
            stderr=kwargs.get('stderr'),
            cwd=kwargs.get('cwd'),
            env=env,
        )

        return cmd, opts
