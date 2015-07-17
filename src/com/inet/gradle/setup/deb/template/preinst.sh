#!/bin/sh
# preinst script for {{baseName}}
#
# see: dh_installdeb(1)

set -e

# summary of how this script can be called:
#        * <new-preinst> `install'
#        * <new-preinst> `install' <old-version>
#        * <new-preinst> `upgrade' <old-version>
#        * <old-preinst> `abort-upgrade' <new-version>
# for details, see http://www.debian.org/doc/debian-policy/ or
# the debian-policy package

. /usr/share/debconf/confmodule

{{head}}

case "$1" in
    install|upgrade)
    ;;

    abort-upgrade)
    ;;

    *)
        echo "preinst called with unknown argument \`$1'" >&2
        exit 1
    ;;
esac

{{tail}}

exit 0
