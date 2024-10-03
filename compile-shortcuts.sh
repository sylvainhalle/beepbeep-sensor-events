#! /bin/bash
HOME=/home/sylvain/Workspaces/BeepBeep
VERSION=0.11.3

pushd $HOME/beepbeep-3
ant
cp beepbeep-3-$VERSION.jar $HOME/groovy-bridge/dep
cp beepbeep-3-$VERSION.jar $HOME/beepbeep-sensor-events/Source/dep
popd
pushd $HOME/groovy-bridge
ant
cp beepbeep-groovy-$VERSION.jar $HOME/beepbeep-sensor-events/Source/dep
popd
pushd $HOME/beepeep-sensor-events
ant
popd