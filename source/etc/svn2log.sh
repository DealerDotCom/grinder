#!/bin/bash

svn log -v --xml https://grinder.svn.sourceforge.net/svnroot/grinder/trunk/source | python /work/opt/svn2log/svn2log.py --users=svn2logusers --strip-comments --only-date -o ../ChangeLog --prefix="(/trunk/source|/source/trunk)" --no-host

