#! /bin/sh
#
# Generate all necessary files by running automake, autoconf, ...
#

aclocal
autoheader
automake -a
autoconf
