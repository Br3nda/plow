
import os
import platform
import socket
import logging
import subprocess

import plow.rndaemon.conf as conf
import plow.rndaemon.client as client
import plow.rndaemon.rpc.ttypes as ttypes

logger = logging.getLogger(__name__)

class AbstractProfiler(object):
    def __init__(self):
        self.data = { "platform": platform.platform() }
        self.update()

        for key, value in self.data.iteritems():
            logger.info("%s = %s" % (key, value))

    def sendPing(self, tasks, isReboot=False):
        
        # Update the values (calls subclass impl)
        self.update()

        if conf.NETWORK_DISABLED:
            return

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

        # Create a ping
        ping = ttypes.Ping()
        ping.hostname = socket.getfqdn()
        ping.ipAddr = socket.gethostbyname(socket.getfqdn())
        ping.isReboot = isReboot
        ping.bootTime = self.bootTime
        ping.hw = hw
        ping.tasks = tasks

        logger.info("Sending ping: %s" % ping)
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
        self._update()

        for name in ('logicalCpus', 'physicalCpus', 'totalRamMb'):
            val = conf.getint('profile', name)
            if val is not None and val > 0:

                # Limit the total ram value, and also
                # adjust the free ram to cap out as well.
                if name == 'totalRamMb':
                    if val >= self.totalRamMb:
                        continue
                    self.freeRamMb = min(val, self.freeRamMb)

                setattr(self, name, val)
                logger.debug("Using profile override: %s = %s" % (name, val))


    def reboot(self):
        """
        reboot()

        Platform-specific procedure to reboot the system. 
        Should be implemented in a subclass if platform is not POSIX 
        """
        p = subprocess.Popen(['/usr/bin/sudo', '-n', '/sbin/reboot'], 
                    stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        
        out, _ = p.communicate()

        # if we have gotten here, then the reboot obviously failed
        err = "System failed to reboot (status %d): %s" % (p.returncode, out.strip())
        logger.warn(err)
        raise ttypes.RndException(p.returncode, err)



    def __getattr__(self, k):
        return self.data[k]

    def __str__(self):
        return str(self.data)
