"""
The render job watch panel allows you to 
   1. setup filters to automatically load jobs. (defaults to loading your jobs)
   2. individually add jobs you want to watch.
"""

import plow.core

from plow.gui.manifest import QtCore, QtGui
from plow.gui.common.widgets import RadioBoxArray
from plow.gui.panels import Panel

class RenderJobWatchPanel(Panel):

    def __init__(self, name="My Jobs", parent=None):
        Panel.__init__(self, name, parent)

        self.setWidget(RenderJobWatchWidget(self))
        self.setWindowTitle(name)
        
    def init(self):
        # TODO
        # sweep button (remove finished)
        # refresh button
        # seperator
        # kill button (multi-select)
        # comment button (multi-select)
        # 
        self.titleBarWidget().addAction(
            QtGui.QIcon(":/search.png"), "Search", self.openSearchDialog)
        
        self.titleBarWidget().addAction(
            QtGui.QIcon(":/wrench.png"), "Configure", self.openConfigDialog)

    def openSearchDialog(self):
        print "Open search dialog"

    def openConfigDialog(self):
        d = RenderJobWatchConfigDialog()
        d.exec_()

    def restore(self):
        pass

    def save(self):
        pass

class RenderJobWatchWidget(QtGui.QWidget):

    def __init__(self, parent=None):
        QtGui.QWidget.__init__(self, parent)
        QtGui.QVBoxLayout(self)
        self.__filters = []
        self.__tree = QtGui.QTreeWidget(self)
        self.__tree.setHeaderLabels(["Job", "Status"])

        self.layout().addWidget(self.__tree)

    def refresh(self):
        pass

class RenderJobWatchConfigDialog(QtGui.QDialog):
    """
    A dialog box that lets you configure how the render job widget.
    """
    def __init__(self, parent=None):
        QtGui.QDialog.__init__(self, parent)
        layout = QtGui.QVBoxLayout(self)

        self.checkboxLoadMine = QtGui.QCheckBox(self)
        self.listUsers = QtGui.QListWidget(self)
        self.listUsers.setMaximumHeight(50)
        self.checkboxLoadErrors = QtGui.QCheckBox(self)

        self.listProjects = QtGui.QListWidget(self)
        self.listProjects.setMaximumHeight(50)

        group_box1 = QtGui.QGroupBox("Load Jobs", self)

        form_layout1 = QtGui.QFormLayout(group_box1)
        form_layout1.addRow("Load Mine:", self.checkboxLoadMine)
        form_layout1.addRow("Load User:", self.listUsers)
        form_layout1.addRow("Load With Errors:", self.checkboxLoadErrors)

        # move to project multi-select widget
        group_box2 = QtGui.QGroupBox("Filters", self)
        form_layout2 = QtGui.QFormLayout(group_box2)
        form_layout2.addRow("For Projects:", self.listProjects)

        layout.addWidget(group_box1)
        layout.addWidget(group_box2)
















