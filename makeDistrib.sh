#!/bin/bash

TAR=grinder.tar.gz

FILES=$(find . \
-name original -prune \
-o -name 'CVS' -prune \
-o -name '*~' -prune \
-o -name 'makeDistrib.sh' -prune \
-o -name 'GNUmakefile' -prune \
-o -name "${TAR}" -prune \
-o -name 'log' -prune \
-o -name 'sniffer-output*' -prune \
-o -name '*.dat' -prune \
-o -name '.cvsignore' -prune \
-o -name '.*~' -prune \
-o -name 'prj.el' -prune \
-o \( -type f -a -print \) )


tar cvzf ${TAR} ${FILES}
